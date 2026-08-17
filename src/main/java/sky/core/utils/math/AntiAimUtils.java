//package sky.core.utils.math;
//
//
//import net.minecraft.entity.LivingEntity;
//import net.minecraft.util.math.vector.Vector3d;
//import movement.impl.modules.sky.core.ElytraTarget;
//import utils.sky.core.Wrapper;
//import math.utils.sky.core.MathUtil;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class LeaveUtil implements Wrapper {
//    private static int number;
//    public static List<Vector3d> getLeaveVectors(LivingEntity target, ElytraTarget elytra) {
//        List<Vector3d> vectors = new ArrayList<>();
//
//        if (elytra.getModeLeave().getSelected("Horizontal")) {
//            //x
//            vectors.add(new Vector3d(target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue(), elytra.getLeaveDistnY().getValue(), 0));
//            vectors.add(new Vector3d(-(target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue()), elytra.getLeaveDistnY().getValue(), 0));
//            //z
//            vectors.add(new Vector3d(0, elytra.getLeaveDistnY().getValue(), target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue()));
//            vectors.add(new Vector3d(0, elytra.getLeaveDistnY().getValue(), -(target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue())));
//        } else if (elytra.getModeLeave().getSelected("Diagonal")) {
//            //x
//            vectors.add(new Vector3d(target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue(), elytra.getLeaveDistnY().getValue(), target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue()  : elytra.getLeaveDistnXZ().getValue()));
//            vectors.add(new Vector3d(-(target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue()), elytra.getLeaveDistnY().getValue(), -(target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue())));
//            //z
//            vectors.add(new Vector3d(-(target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue()), elytra.getLeaveDistnY().getValue(), target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue()));
//            vectors.add(new Vector3d(-(target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue()), elytra.getLeaveDistnY().getValue(), (target.isElytraFlying() ? elytra.getLeaveDistnXZ().getValue() + elytra.getElytraLeave().getValue() : elytra.getLeaveDistnXZ().getValue())));
//
//        } else if (elytra.getModeLeave().getSelected("Custom")) {
//            //x
//            vectors.add(new Vector3d(target.isElytraFlying() ? elytra.getHorizontalLeaveDist().getValue() + elytra.getElytraLeave().getValue() : elytra.getHorizontalLeaveDist().getValue(), elytra.getVerticalLeaveDist().getValue(), target.isElytraFlying() ? elytra.getDiagonalLeaveDist().getValue() + elytra.getElytraLeave().getValue()  : elytra.getDiagonalLeaveDist().getValue()));
//            vectors.add(new Vector3d(-(target.isElytraFlying() ? elytra.getHorizontalLeaveDist().getValue() + elytra.getElytraLeave().getValue() : elytra.getHorizontalLeaveDist().getValue()), elytra.getVerticalLeaveDist().getValue(), -(target.isElytraFlying() ? elytra.getDiagonalLeaveDist().getValue() + elytra.getElytraLeave().getValue() : elytra.getDiagonalLeaveDist().getValue())));
//            //z
//            vectors.add(new Vector3d(-(target.isElytraFlying() ? elytra.getHorizontalLeaveDist().getValue() + elytra.getElytraLeave().getValue() : elytra.getHorizontalLeaveDist().getValue()), elytra.getVerticalLeaveDist().getValue(), target.isElytraFlying() ? elytra.getDiagonalLeaveDist().getValue() + elytra.getElytraLeave().getValue() : elytra.getDiagonalLeaveDist().getValue()));
//            vectors.add(new Vector3d(-(target.isElytraFlying() ? elytra.getHorizontalLeaveDist().getValue() + elytra.getElytraLeave().getValue() : elytra.getHorizontalLeaveDist().getValue()), elytra.getVerticalLeaveDist().getValue(), (target.isElytraFlying() ? elytra.getDiagonalLeaveDist().getValue() + elytra.getElytraLeave().getValue() : elytra.getDiagonalLeaveDist().getValue())));
//        }
//
//        return vectors;
//    }
//    public static void updateNumber() {
//        number = MathUtil.randomInRange(1,4);
//    }
//
//    public static float getJitterYaw(LivingEntity entity) {
//        ElytraTarget elytraTarget = Load.getInstance().getHooks().getModuleManagers().getElytraTarget();
//        float jitter = 0;
//        if (mc.player == null || mc.world == null || entity == null) return 0;
//        if (elytraTarget.isToggled() && elytraTarget.getAntiAim().getValue() && elytraTarget.getJitter().getValue() && mc.player.isElytraFlying()) {
//            if (entity != null) {
//                jitter += MathUtil.random(-elytraTarget.getJitterValue().getValue(), elytraTarget.getJitterValue().getValue());
//            }
//        }
//
//        return jitter;
//    }
//
//    public static float getJitterPitch(LivingEntity entity) {
//        ElytraTarget elytraTarget = Load.getInstance().getHooks().getModuleManagers().getElytraTarget();
//        float jitter = 0;
//        if (mc.player == null || mc.world == null || entity == null) return 0;
//        if (elytraTarget.isToggled() && elytraTarget.getAntiAim().getValue() && elytraTarget.getJitter().getValue() && mc.player.isElytraFlying()) {
//            if (entity != null) {
//                jitter += MathUtil.random(-elytraTarget.getJitterValue().getValue(), elytraTarget.getJitterValue().getValue());
//            }
//        }
//
//        return jitter;
//    }
//
//    public static boolean canTarget(LivingEntity target) {
//        if (target == null) return false;
//        if (mc.player.getCooledAttackStrength(1.5f) < 0.94f) return true;
//        if (mc.player.isHandActive()) return true;
//
//        return false;
//
//    }
//    public static boolean fullCheck() {
//        if (mc.player == null || mc.world == null) return false;
//        ElytraTarget elytraTarget = Load.getInstance().getHooks().getModuleManagers().getElytraTarget();
//        ElytraMotion elytraMotion = Load.getInstance().getHooks().getModuleManagers().getElytraMotion();
//        boolean flag = elytraTarget.isToggled() && elytraTarget.antiAim.getValue() && mc.player.isElytraFlying() && !elytraMotion.isToggled();
//
//        return flag;
//
//    }
//
//
//}
