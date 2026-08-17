package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.network.play.server.SChangeBlockPacket;
import net.minecraft.network.play.server.SMultiBlockChangePacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import sky.core.events.EventClickBlockRight;
import sky.core.events.EventPacket;
import sky.core.modules.Category;
import sky.core.modules.Module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NoBlockUpdate extends Module {
    private final Set<BlockPos> ghostBlocks = new HashSet<>();
    private final Map<BlockPos, BlockState> previousStates = new HashMap<>();

    public NoBlockUpdate() {
        super("NoBlockUpdate", "Скрывает апдейты сервера для блоков под собой", Category.Movement);
    }

    @EventTarget
    private void onClickBlock(EventClickBlockRight event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (!(mc.player.getHeldItem(event.getHand()).getItem() instanceof BlockItem)) {
            return;
        }

        BlockRayTraceResult result = event.getResult();
        BlockPos placePos = result.getPos().offset(result.getFace());
        if (!isBlockUnderPlayer(placePos)) {
            return;
        }

        ghostBlocks.add(placePos.toImmutable());
        previousStates.putIfAbsent(placePos.toImmutable(), mc.world.getBlockState(placePos));
    }

    @EventTarget
    private void onPacket(EventPacket event) {
        if (!event.isSend() && event.getPacket() instanceof SChangeBlockPacket packet) {
            if (ghostBlocks.contains(packet.getPos())) {
                event.setCancelled(true);
            }
            return;
        }

        if (!event.isSend() && event.getPacket() instanceof SMultiBlockChangePacket packet) {
            final boolean[] shouldCancel = {false};
            packet.func_244310_a((pos, state) -> {
                if (!shouldCancel[0] && ghostBlocks.contains(pos.toImmutable())) {
                    shouldCancel[0] = true;
                }
            });
            if (shouldCancel[0]) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.isSend() && event.getPacket() instanceof CPlayerTryUseItemOnBlockPacket packet) {
            BlockPos placePos = packet.func_218794_c().getPos().offset(packet.func_218794_c().getFace());
            if (isBlockUnderPlayer(placePos)) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.world != null) {
            for (BlockPos pos : ghostBlocks) {
                BlockState state = previousStates.getOrDefault(pos, Blocks.AIR.getDefaultState());
                mc.world.setBlockState(pos, state, 3);
            }
        }
        ghostBlocks.clear();
        previousStates.clear();
        super.onDisable();
    }

    private boolean isBlockUnderPlayer(BlockPos blockPos) {
        if (mc.player == null) {
            return false;
        }

        int minY = (int) Math.floor(mc.player.getPosY()) - 1;
        if (blockPos.getY() > minY) {
            return false;
        }

        double dx = blockPos.getX() + 0.5D - mc.player.getPosX();
        double dz = blockPos.getZ() + 0.5D - mc.player.getPosZ();
        return dx * dx + dz * dz <= 4.0D;
    }
}
