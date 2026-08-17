package sky.core.modules.impl.combat;


import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.events.EventDamage;
import sky.core.events.EventInput;
import sky.core.events.EventPacket;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.utils.player.DamageUtil;
import sky.core.SkyCore;
import sky.core.modules.impl.movement.FreeCamera;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;


public class NoVelocity extends Module {

    private final ModeSetting modeSetting = new ModeSetting("Мод", "Обычный", "Обычный", "RW");
    private final BooleanSetting jump = new BooleanSetting("Прыгать", true, () -> modeSetting.is("RW"));
    private Vector3d knockbackVelocity = Vector3d.ZERO;
    private final DamageUtil damageUtil = new DamageUtil();

    public NoVelocity() {
        super("Velocity", "Не позволяет разным условиям в мире откидывать вас", Category.Combat);
        addSettings(modeSetting, jump);
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (modeSetting.is("Обычный")) {
            if (mc.player != null && e.getPacket() instanceof SEntityVelocityPacket p) {
                if (p.getEntityID() == mc.player.getEntityId()) {
                    e.setCancelled(true);
                }
            }
        }

        if (modeSetting.is("RW")) {
            damageUtil.time(1L);
            if (mc.player != null && damageUtil.isNormalDamage() && e.getPacket() instanceof SEntityVelocityPacket p) {
                if (p.getEntityID() == mc.player.getEntityId()) {
                    knockbackVelocity = new Vector3d(p.getMotionX(), p.getMotionY(), p.getMotionZ());
                }
            }
            damageUtil.onPacketEvent(e);
        }

    }

    @EventTarget
    private void onDamage(EventDamage e) {
        if (modeSetting.is("RW")) {
            damageUtil.processDamage(e);
        }
    }

    @EventTarget
    public void onInput(EventInput e) {
        if (!modeSetting.is("RW")) return;
        if (SkyCore.getInstance().getModuleManager().getModule(FreeCamera.class).isEnabled()) {
            knockbackVelocity = Vector3d.ZERO;
            return;
        }
        if (mc.player.hurtTime > 0 && knockbackVelocity.lengthSquared() > 0.0F) {
            double relativeAngle = getRelativeAngle();
            float forward = 0.0F;
            float strafe = 0.0F;
            if (relativeAngle > -0.0F && relativeAngle < 0.0F) {
                forward = 1.0F;
            } else if (!(relativeAngle > 45.0F) && !(relativeAngle < -0.0F)) {
                if (relativeAngle >= 0.0F && relativeAngle <= 0.0F) {
                    strafe = -0.0F;
                } else if (relativeAngle <= -45.0F && relativeAngle >= -0.0F) {
                    strafe = 0.0F;
                }
            } else {
                forward = -0.0F;
            }

            e.setForward(forward);
            e.setStrafe(strafe);
            e.setJump(jump.get() && mc.player.isOnGround());
        } else {
            knockbackVelocity = Vector3d.ZERO;
        }
    }
    @Native
    private double getRelativeAngle() {
        double yaw = mc.player.rotationYaw;
        double dx = -knockbackVelocity.x;
        double dz = -knockbackVelocity.z;
        double attackerYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        return MathHelper.wrapDegrees(attackerYaw - yaw);
    }
}
