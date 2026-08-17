//package sky.core.modules.impl.player;
//
//import com.darkmagician6.eventapi.EventManager;
//import com.darkmagician6.eventapi.eventapinew.EventTarget;
//import sky.core.SkyCore;
//import events.sky.core.EventBlockDamage;
//import events.sky.core.EventUpdate;
//import impl.managers.sky.core.NukerManager;
//import modules.sky.core.Category;
//import modules.sky.core.Module;
//import impl.constructors.api.modules.sky.core.BooleanSetting;
//import impl.constructors.api.modules.sky.core.SliderSetting;
//import net.minecraft.block.Block;
//import net.minecraft.util.Direction;
//import net.minecraft.util.Hand;
//import net.minecraft.util.math.BlockPos;
//
//import java.util.Comparator;
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//
//
//public class Nuker extends Module {
//    private final SliderSetting radius = new SliderSetting("Дистанция", 3, 1, 5, 1);
//    private final BooleanSetting breakAll = new BooleanSetting("Ломать все блоки", false);
//
//    private BlockPos currentTargetBlock = null;
//    private boolean isBreaking = false;
//
//    public Nuker() {
//        super("Nuker", "Автоматически ломает блоки в радиусе", Category.Player);
//        addSettings(radius, breakAll);
//    }
//
//    @EventTarget
//    public void onUpdate(EventUpdate event) {
//        NukerManager manager = SkyCore.getInstance().getNukerManager();
//        if (!breakAll.get() && manager.isEmpty()) {
//            resetBreaking();
//            return;
//        }
//
//        if (!isCurrentTargetValid()) {
//            findNewTarget();
//        }
//
//        if (currentTargetBlock != null) {
//            breakCurrentTarget();
//        }
//    }
//
//    private boolean isCurrentTargetValid() {
//        if (currentTargetBlock == null) return false;
//
//        if (mc.world.isAirBlock(currentTargetBlock)) {
//            resetBreaking();
//            return false;
//        }
//
//        double distance = mc.player.getDistanceSq(currentTargetBlock.getX() + 0.5D, currentTargetBlock.getY() + 0.5D, currentTargetBlock.getZ() + 0.5D);
//
//        if (distance > radius.get() * radius.get()) {
//            resetBreaking();
//            return false;
//        }
//
//        return true;
//    }
//
//    private void findNewTarget() {
//        NukerManager manager = SkyCore.getInstance().getNukerManager();
//        int range = radius.get().intValue();
//        List<BlockPos> blockPositions = new CopyOnWriteArrayList<>();
//
//        for (int x = -range; x <= range; ++x) {
//            for (int y = 0; y <= range; ++y) {
//                for (int z = -range; z <= range; ++z) {
//                    BlockPos blockPos = mc.player.getPosition().add(x, y, z);
//
//                    if (mc.world.isAirBlock(blockPos)) continue;
//
//                    Block block = mc.world.getBlockState(blockPos).getBlock();
//                    boolean shouldBreak = breakAll.get() || manager.isTargetBlock(block);
//
//                    if (shouldBreak) {
//                        blockPositions.add(blockPos);
//                    }
//                }
//            }
//        }
//
//        if (!blockPositions.isEmpty()) {
//            blockPositions.sort(Comparator.comparingDouble(pos -> mc.player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)));
//
//            currentTargetBlock = blockPositions.get(0);
//            isBreaking = false;
//        }
//    }
//
//    private void breakCurrentTarget() {
//        if (currentTargetBlock == null) return;
//        EventManager.call(new EventBlockDamage(mc.world.getBlockState(currentTargetBlock), currentTargetBlock, EventBlockDamage.State.START));
//        boolean result = mc.playerController.onPlayerDamageBlock(currentTargetBlock, Direction.UP);
//
//        if (result) {
//            mc.player.swingArm(Hand.MAIN_HAND);
//            isBreaking = true;
//        }
//
//        if (isBreaking && !mc.playerController.getIsHittingBlock()) {
//            resetBreaking();
//        }
//        EventManager.call(new EventBlockDamage(mc.world.getBlockState(currentTargetBlock), currentTargetBlock, EventBlockDamage.State.STOP));
//    }
//
//    private void resetBreaking() {
//        currentTargetBlock = null;
//        isBreaking = false;
//
//        if (mc.playerController.getIsHittingBlock()) {
//            mc.playerController.resetBlockRemoving();
//        }
//    }
//
//    @EventTarget
//    public void onDisable() {
//        super.onDisable();
//        resetBreaking();
//    }
//}