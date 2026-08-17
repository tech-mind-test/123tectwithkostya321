package sky.core.utils.player;


import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.SkyCore;
import sky.core.modules.impl.movement.ElytraMotion;
import sky.core.modules.impl.movement.ElytraTarget;
import sky.core.utils.Wrapper;

public class ElytraTargetUtils implements Wrapper {
    public static void updateNumber() {
    }

    public static float getJitterYaw(LivingEntity entity) {
        return 0;
    }

    public static float getJitterPitch(LivingEntity entity) {
        return 0;
    }

    public static boolean canTarget(LivingEntity target) {
        if (target == null) return false;
        if (mc.player.getCooledAttackStrength(1.5f) < 0.94f) return true;
        if (mc.player.isHandActive()) return true;

        return false;

    }
    public static boolean fullCheck() {
        if (mc.player == null || mc.world == null) return false;
        ElytraTarget elytraTarget = SkyCore.getInstance().getModuleManager().getElytraTarget();
        ElytraMotion elytraMotion = SkyCore.getInstance().getModuleManager().getElytraMotion();
        boolean flag = elytraTarget.isEnabled() && mc.player.isElytraFlying() && !elytraMotion.isEnabled();

        return flag;

    }


}
