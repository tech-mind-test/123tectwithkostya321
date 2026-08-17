package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.SkyCore;
import sky.core.events.EventRender3D;
import sky.core.utils.managers.impl.BlockESPManager;
import sky.core.modules.Category;
import sky.core.modules.Module;
import mods.baritone.utils.IRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.awt.*;
import java.util.*;
import java.util.List;

public class BlockESP extends Module {

    public BlockESP() {
        super("BlockESP", "Подсветка выбранных блоков", Category.Visuals);
    }

    private final Map<Integer, List<BlockPos>> cachedColorToBoxes = new HashMap<>();
    private int lastEntriesHash = 0;
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;
    private int lastRange = -1;

    @EventTarget
    public void onRender(EventRender3D eventRender3D) {
        if (mc.world == null) return;

        BlockESPManager manager = SkyCore.getInstance().getBlockESPManager();
        List<BlockESPManager.BlockEntry> entries = manager.getBlocks();

        int range = 48;
        BlockPos playerPos = mc.player.getPosition();
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;
        int listHash = 1;
        for (BlockESPManager.BlockEntry e : entries) {
            String n = e.getBlock();
            listHash = 31 * listHash + (n == null ? 0 : n.trim().toLowerCase(Locale.ROOT).hashCode());
            listHash = 31 * listHash + e.getColor();
        }
        boolean needRebuild = (chunkX != lastChunkX) || (chunkZ != lastChunkZ) || (listHash != lastEntriesHash) || (range != lastRange);

        if (needRebuild) {
            rebuildCache(entries, range, playerPos);
            lastEntriesHash = listHash;
            lastChunkX = chunkX;
            lastChunkZ = chunkZ;
            lastRange = range;
        }

        MatrixStack stack = new MatrixStack();
        for (Map.Entry<Integer, List<BlockPos>> e : cachedColorToBoxes.entrySet()) {
            int c = e.getKey();

            IRenderer.startLines(new Color((c >> 16) & 0xFF, (c >> 8) & 0xFF, (c) & 0xFF), 2.5f, true);
            List<BlockPos> list = e.getValue();
            list.removeIf(p -> mc.world.isAirBlock(p));
            for (BlockPos p : list) {
                IRenderer.emitAABB(stack, new AxisAlignedBB(p, p.add(1, 1, 1)).grow(0.002D));
            }
            IRenderer.endLines(true);
        }
    }

    private void rebuildCache(List<BlockESPManager.BlockEntry> entries, int range, BlockPos playerPos) {
        cachedColorToBoxes.clear();
        Map<Block, Integer> colorByBlock = new HashMap<>();
        for (BlockESPManager.BlockEntry entry : entries) {
            String s = entry.getBlock().trim().toLowerCase(Locale.ROOT);
            if (!s.contains(":")) s = "minecraft:" + s;
            ResourceLocation rl = new ResourceLocation(s);
            Registry.BLOCK.getOptional(rl).ifPresent(b -> colorByBlock.put(b, entry.getColor()));
        }

        int px = playerPos.getX();
        int py = playerPos.getY();
        int pz = playerPos.getZ();

        int minY = Math.max(0, py - range);
        int maxY = Math.min(255, py + range);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = px - range; x <= px + range; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = pz - range; z <= pz + range; z++) {
                    pos.setPos(x, y, z);
                    if (!mc.world.isBlockPresent(pos)) continue;
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.isAir()) continue;

                    Block block = state.getBlock();
                    Integer color = colorByBlock.get(block);
                    if (color == null) continue;

                    cachedColorToBoxes.computeIfAbsent(color, k -> new ArrayList<>()).add(pos.toImmutable());
                }
            }
        }
    }
}