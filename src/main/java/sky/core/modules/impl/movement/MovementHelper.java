package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventInput;
import sky.core.events.EventMotion;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.player.MoveUtil;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

public class MovementHelper extends Module {
    private final BooleanSetting minijump = new BooleanSetting("Незаметные прыжки на ступеньках", true);
    private final BooleanSetting jump = new BooleanSetting("Прыгать на краю блока", false);
    private final BooleanSetting shift = new BooleanSetting("Приседать на краю блока", false);
    private boolean useshift;

    public MovementHelper() {
        super("Movement Helper", "Содержит в себе вспомогательные функции для передвижения", Category.Movement);
        addSettings(minijump, jump, shift);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (minijump.get() && MoveUtil.isMoving() && !mc.player.isCrouching() && shouldClimbBlock() && mc.player.isOnGround() && !mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.player.jump();
        }

        if (jump.get() && MoveUtil.isBlockUnder(0.001f) && mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

    @EventTarget
    public void onUpdate(EventMotion event) {
        if (mc.player.isOnGround()) {
            BlockPos belowPos = new BlockPos(mc.player.getPositionVec()).add(0, -1, 0);
            BlockState belowState = mc.world.getBlockState(belowPos);
            if (isNonSolidBlock(belowState)) {
                if (!useshift) {
                    useshift = true;
                }
            } else if (useshift) {
                useshift = false;
            }
        } else if (useshift) {
            useshift = false;
        }
    }

    @EventTarget
    public void onUpdate(EventInput event) {
        if (shift.get()) {
            if (!mc.gameSettings.keyBindSneak.isKeyDown()) {
                event.setSneak(useshift);
            }
        }
    }

    private boolean shouldClimbBlock() {
        Vector3d playerPos = mc.player.getPositionVec();
        double yaw = Math.toRadians(mc.player.rotationYaw);
        double x = -Math.sin(yaw);
        double z = Math.cos(yaw);

        BlockPos playerBlockPos = new BlockPos(playerPos.x, playerPos.y, playerPos.z);
        BlockPos frontPos = new BlockPos(playerPos.x + x, playerPos.y, playerPos.z + z);
        BlockPos frontUpPos = frontPos.up();
        BlockPos belowPos = playerBlockPos.down();

        boolean isOnStairsOrSlab = mc.world.getBlockState(belowPos).getBlock() instanceof StairsBlock || mc.world.getBlockState(belowPos).getBlock() instanceof SlabBlock;
        boolean isFrontClimbable = mc.world.getBlockState(frontPos).getBlock() instanceof StairsBlock || mc.world.getBlockState(frontPos).getBlock() instanceof SlabBlock || mc.world.getBlockState(frontUpPos).getBlock() instanceof StairsBlock || mc.world.getBlockState(frontUpPos).getBlock() instanceof SlabBlock;

        return isOnStairsOrSlab && isFrontClimbable;
    }

    private boolean isNonSolidBlock(BlockState state) {
        return state.getBlock() instanceof AirBlock || state.getMaterial() == Material.TALL_PLANTS || state.getMaterial() == Material.PLANTS || state.getMaterial() == Material.WATER || state.getMaterial() == Material.LAVA || state.getBlock() == Blocks.SNOW || state.getBlock() instanceof CarpetBlock || state.getBlock() instanceof SlabBlock || state.getCollisionShape(mc.world, new BlockPos(mc.player.getPositionVec()).add(0, -1, 0)).isEmpty();
    }
}