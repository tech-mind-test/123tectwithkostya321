package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.item.UseAction;
import net.minecraft.util.math.vector.Vector3d;
import com.adl.nativeprotect.Native;
import sky.core.events.EventMotion;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.StopWatch;
import sky.core.utils.math.MathUtil;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.misc.ServerUtil;
import sky.core.utils.player.MoveUtil;

public class Flight extends Module {
    private final ModeSetting modeSetting = new ModeSetting("Тип", "Скольжение", "Скольжение", "Прыжки", "Движение", "Обычный", "ReallyWorld Dragon", "Glide Fly");
    private final SliderSetting speed = new SliderSetting("Скорость", 1.5F, 0.1F, 10F, 0.1F, () -> !(modeSetting.is("Прыжки") || modeSetting.is("ReallyWorld Dragon") || modeSetting.is("Glide Fly")));
    private final SliderSetting speedY = new SliderSetting("Скорость по Y", 1.5F, 0.1F, 10F, 0.1F, () -> !(modeSetting.is("Прыжки") || modeSetting.is("ReallyWorld Dragon") || modeSetting.is("Glide Fly")));
    private final SliderSetting glideSpeed = new SliderSetting("Скорость", 0.087f, 0.0f, 0.25f, 0.001f, () -> modeSetting.is("Glide Fly"));

    public Flight() {
        super("Flight", "Позволяет летать, облегчая перемещение", Category.Movement);
        addSettings(modeSetting, speed, speedY, glideSpeed);
    }

    public boolean isGlideFlyMode() {
        return isEnabled() && modeSetting.is("Glide Fly");
    }

    private boolean isConsumableUse(UseAction action) {
        return action == UseAction.EAT || action == UseAction.DRINK;
    }

    private void blockEating() {
        if (mc.player == null) return;
        if (!mc.player.isHandActive()) return;

        UseAction action = mc.player.getActiveItemStack() != null ? mc.player.getActiveItemStack().getUseAction() : null;
        if (action == null || !isConsumableUse(action)) return;

        mc.playerController.onStoppedUsingItem(mc.player);
        mc.gameSettings.keyBindUseItem.setPressed(false);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!isGlideFlyMode() || mc.player == null || !mc.player.isElytraFlying()) {
            return;
        }
        blockEating();
    }

    private TimeUtil ticks = new TimeUtil();
    private final StopWatch glideTicks = new StopWatch();
    int ticksTwo = 0;

    @Native
    @EventTarget
    public void onEvent(EventMotion event) {
        boolean isSneaking = mc.gameSettings.keyBindSneak.isKeyDown();
        boolean isJumping = mc.gameSettings.keyBindJump.isKeyDown();
        float motionSpeed = speedY.get();
        switch (modeSetting.get()) {
            case "Скольжение":
                mc.player.setMotion(0, -0.005F, 0);
                if (isSneaking) {
                    mc.player.motion.y = -motionSpeed;
                } else if (isJumping) {
                    mc.player.motion.y = motionSpeed;
                }
                if (mc.player.isOnGround()) mc.player.jump();
                MoveUtil.setMotion(speed.get());
                break;

            case "Обычный":
                mc.player.setMotion(0, 0, 0);
                if (isSneaking) {
                    mc.player.motion.y = -motionSpeed;
                } else if (isJumping) {
                    mc.player.motion.y = motionSpeed;
                }
                if (mc.player.isOnGround()) mc.player.jump();
                MoveUtil.setMotion(speed.get());
                break;
            case "Движение":
                if (isSneaking) {
                    mc.player.motion.y = -motionSpeed;
                } else if (isJumping) {
                    mc.player.motion.y = motionSpeed;
                }
                MoveUtil.setMotion(speed.get());
                break;
            case "Прыжки":
                if (isJumping) mc.player.jump();
                break;
            case "ReallyWorld Dragon":
                if (mc.player.abilities.isFlying) {
                    mc.player.setMotion(0, 0, 0);
                    boolean moving = MoveUtil.isMoving();
                    boolean vertical = isSneaking || isJumping;
                    if (moving) {
                        MoveUtil.setMotion(vertical ? 0.7 : 1.09);
                    }
                    if (isSneaking && !moving) {
                        mc.player.motion.y = -1;
                    } else if (isJumping && !moving) {
                        mc.player.motion.y = 1;
                    }
                    if (isSneaking) {
                        mc.player.motion.y = -0.74;
                    } else if (isJumping) {
                        mc.player.motion.y = 0.74;
                    }
                }
                break;
            case "Glide Fly":
                if (!mc.player.isElytraFlying()) {
                    break;
                }

                Vector3d pos = mc.player.getPositionVec();
                float yaw = mc.player.rotationYaw;
                double forward = glideSpeed.get();
                double bps = MathUtil.getBps(mc.player, 1);

                float limit = ServerUtil.isConnectedToServer("reallyworld") ? 40 : 100;
                if (bps >= limit) {
                    forward = 0f;
                }

                double dx = -Math.sin(Math.toRadians(yaw)) * forward;
                double dz = Math.cos(Math.toRadians(yaw)) * forward;
                mc.player.setVelocity(dx * MathUtil.randomtest(1.1f, 1.21f), mc.player.getMotion().y - 0.02f, dz * MathUtil.randomtest(1.1f, 1.21f));

                if (glideTicks.finished(50)) {
                    mc.player.setPosition(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                    glideTicks.reset();
                }

                mc.player.setVelocity(dx * MathUtil.randomtest(1.1f, 1.21f), mc.player.getMotion().y + 0.016f, dz * MathUtil.randomtest(1.1f, 1.21f));
                event.setX(event.getX() + dx);
                event.setZ(event.getZ() + dz);
                break;
            default:
                break;
        }
    }
}
