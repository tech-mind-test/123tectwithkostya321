package sky.core.handlers.impl;

import com.darkmagician6.eventapi.EventTarget;
import sky.core.events.EventUpdate;
import sky.core.utils.Wrapper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;

@Getter
@Setter
public class RotationHandler implements Wrapper {
    private static RotationTask currentTask = RotationTask.IDLE;
    private static float currentAimSpeed;
    private static float currentResetSpeed;
    private static int currentPriority;
    private static int currentTimeout;
    private static int idleTicks;

    @EventTarget
    private void onUpdate(EventUpdate e) {
        ++idleTicks;
        if (currentTask == RotationTask.AIM && idleTicks > currentTimeout) {
            currentTask = RotationTask.RESET;
        }

        if (currentTask == RotationTask.RESET && updateRotation(Rotation.getReported(), currentResetSpeed)) {
            currentTask = RotationTask.IDLE;
            currentPriority = 0;
            LookHandler.setActive(false);
        }
    }

    public static void update(Rotation rotation, float turnSpeed, int timeout, int priority) {
        update(rotation, turnSpeed, turnSpeed, timeout, priority);
    }

    public static void update(Rotation rotation, float aimSpeed, float resetSpeed, int timeout, int priority) {
        if (currentPriority <= priority) {
            if (currentTask == RotationTask.IDLE) {
                LookHandler.setActive(true);
            }

            currentAimSpeed = aimSpeed;
            currentResetSpeed = resetSpeed;
            currentTimeout = timeout;
            currentPriority = priority;
            currentTask = RotationTask.AIM;
            idleTicks = 0;
            updateRotation(rotation, currentAimSpeed);
        }
    }

    private static boolean updateRotation(Rotation rotation, float turnSpeed) {
        Rotation currentRotation = new Rotation(mc.player);
        float yawDelta = MathHelper.wrapDegrees(rotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = rotation.getPitch() - currentRotation.getPitch();
        float totalDelta = Math.abs(yawDelta) + Math.abs(pitchDelta);
        float yawSpeed = totalDelta == 0.0F ? 0.0F : Math.abs(yawDelta / totalDelta) * turnSpeed;
        float pitchSpeed = totalDelta == 0.0F ? 0.0F : Math.abs(pitchDelta / totalDelta) * turnSpeed;
        float newYaw = mc.player.rotationYaw + MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float newPitch = mc.player.rotationPitch + MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);
        newYaw = applySensitivityPatch(mc.player.rotationYaw, newYaw);
        newPitch = applySensitivityPatch(mc.player.rotationPitch, newPitch);
        newPitch = MathHelper.clamp(newPitch, -90.0F, 90.0F);
        mc.player.rotationYaw = newYaw;
        mc.player.rotationPitch = newPitch;
        Rotation finalRotation = new Rotation(mc.player);
        idleTicks = 0;
        return finalRotation.getDelta(rotation) < (double) turnSpeed;
    }

    public static float applySensitivityPatch(float lastYaw, float current) {
        double sens = mc.gameSettings.mouseSensitivity * (double) 0.6F + (double) 0.2F;
        double gcd = sens * sens * sens * (double) 8.0F;
        return (float) ((double) lastYaw + Math.ceil((double) (current - lastYaw) / gcd / (double) 0.15F) * gcd * (double) 0.15F);
    }

    public static boolean isRotating() {
        return currentTask != RotationTask.IDLE;
    }

    public enum RotationTask {AIM, RESET, IDLE}
}