package sky.core.modules.impl.miscellaneous;


import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.block.BlockState;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import other.bot.Bot;
import other.bot.BotManager;
import com.adl.nativeprotect.Native;
import sky.core.events.EventBlockDamage;
import sky.core.events.EventGameUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;

public class InstantRebreak extends Module {

    private int sinceLastDamage;

    public InstantRebreak() {
        super("AutoLes", "Фармит на /warp les через команду .bot", Category.Miscellaneous);


    }

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable(0, Integer.MIN_VALUE, 0);
    private int ticks;
    private Direction direction;

    @Native
    @Override
    public void onEnable() {
        ticks = 0;
        sinceLastDamage = 999;
        blockPos.setPos(0, Integer.MIN_VALUE, 0);
        direction = null;
        super.onEnable();
    }

    @Native
    @EventTarget
    public void dreak(EventBlockDamage e) {
        direction = e.getDirection();
        blockPos.setPos(e.getPos());
    }

    @Native
    @EventTarget
    public void tick(EventGameUpdate e) {
        if (ticks >= 0) {

            for (Bot bot : BotManager.autoLesBots) {
                if (bot == null || bot.connection == null || bot.connection.bot == null || bot.connection.getWorld() == null) {
                    continue;
                }
                if (!shouldMineBot(bot)) {
                    continue;
                }

                Direction dir = direction == null ? Direction.UP : direction;
                lookAtBlock(bot.connection.bot, blockPos);

                bot.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK, blockPos, dir));
                bot.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
            }
        } else {
            ticks++;
        }
    }

    @Native
    private boolean shouldMineBot(Bot bot) {
        if (bot == null || bot.connection == null || bot.connection.bot == null || bot.connection.getWorld() == null)
            return false;
        if (blockPos.getY() == Integer.MIN_VALUE) return false;
        if (blockPos.getY() < 0 || blockPos.getY() >= 256) return false;

        float reach = bot.connection.botController != null ? bot.connection.botController.getBlockReachDistance() : 4.5F;
        double dist = bot.connection.bot.getPositionVec().distanceTo(new Vector3d(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D));
        if (dist > (double) reach) return false;

        boolean creative = bot.connection.botController != null && bot.connection.botController.isInCreativeMode();
        if (!canBreak(bot.connection.getWorld(), blockPos, creative)) return false;
        return true;
    }

    @Native
    private static void lookAtBlock(net.minecraft.entity.player.PlayerEntity player, BlockPos pos) {
        Vector3d eyes = player.getEyePosition(1.0F);
        Vector3d target = new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        Vector3d diff = target.subtract(eyes);

        double diffXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (MathHelper.atan2(diff.z, diff.x) * (180D / Math.PI)) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(diff.y, diffXZ) * (180D / Math.PI)));

        yaw = MathHelper.wrapDegrees(yaw);
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);

        player.rotationYaw = yaw;
        player.rotationPitch = pitch;
        player.rotationYawHead = yaw;
        player.renderYawOffset = yaw;
    }

    @Native
    private static boolean canBreak(net.minecraft.world.World world, BlockPos blockPos, boolean creative) {
        if (world == null) return false;
        BlockState state = world.getBlockState(blockPos);
        if (state == null) return false;
        if (!creative && state.getBlockHardness(world, blockPos) < 0) return false;
        return state.getShape(world, blockPos) != VoxelShapes.empty();
    }
}
