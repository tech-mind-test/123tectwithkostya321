package sky.core.utils.component.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.util.math.MathHelper;
import sky.core.utils.component.Component;
import sky.core.utils.component.Instance;
import sky.core.events.EventUpdate;
import sky.core.handlers.impl.LookHandler;
import sky.core.handlers.impl.Rotation;

@Getter
@Setter
@Accessors(fluent = true)
public class SmoothRotationComponent extends Component {

    public static SmoothRotationComponent getInstance() {
        return Instance.getComponent(SmoothRotationComponent.class);
    }

    private SmoothRotationComponent.RotationTask currentTask = SmoothRotationComponent.RotationTask.IDLE;
    private float currentYawSpeed;
    private float currentPitchSpeed;
    private float currentYawReturnSpeed;
    private float currentPitchReturnSpeed;
    private int currentPriority;
    private int currentTimeout;
    private int idleTicks;
    private Rotation targetRotation;


    private void resetRotation() {
        Rotation targetRotation = new Rotation(LookHandler.getFreeYaw(), LookHandler.getFreePitch());
        if (updateRotation(targetRotation, currentYawReturnSpeed(), currentPitchReturnSpeed())) {
            stopRotation();
        }
    }

    @EventTarget
    public void onEvent(EventUpdate event) {
        if (currentTask().equals(SmoothRotationComponent.RotationTask.AIM) && idleTicks() > currentTimeout()) {
            currentTask(SmoothRotationComponent.RotationTask.RESET);
        }

        if (currentTask().equals(SmoothRotationComponent.RotationTask.RESET)) {
            resetRotation();
        }
        idleTicks++;
    }

    public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
        final SmoothRotationComponent instance = SmoothRotationComponent.getInstance();
        if (instance.currentPriority() > priority) {
            return;
        }

        if (instance.currentTask().equals(SmoothRotationComponent.RotationTask.IDLE) && !clientRotation) {
            LookHandler.setActive(true);
        }

        instance.currentYawSpeed(yawSpeed);
        instance.currentPitchSpeed(pitchSpeed);
        instance.currentYawReturnSpeed(yawReturnSpeed);
        instance.currentPitchReturnSpeed(pitchReturnSpeed);
        instance.currentTimeout(timeout);
        instance.currentPriority(priority);
        instance.currentTask(SmoothRotationComponent.RotationTask.AIM);
        instance.targetRotation(target);

        instance.updateRotation(target, yawSpeed, pitchSpeed);
    }

    public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
        update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    private boolean updateRotation(Rotation targetRotation, float lazinessH, float lazinessV) {
        if (mc.player == null) return false;

        mc.player.rotationYaw = smoothRotation(mc.player.rotationYaw, targetRotation.getYaw(), lazinessH);
        mc.player.rotationPitch = MathHelper.clamp(smoothRotation(mc.player.rotationPitch, targetRotation.getPitch(), lazinessV), -90F, 90F);

        idleTicks(0);
        return new Rotation(mc.player).getDelta(targetRotation) < 1F;
    }

    public void stopRotation() {
        currentTask(SmoothRotationComponent.RotationTask.IDLE);
        currentPriority(0);
        if (!RotationComponent.getInstance().isRotating()) {
            LookHandler.setActive(false);
        }
    }

    public boolean isRotating() {
        return !currentTask.equals(SmoothRotationComponent.RotationTask.IDLE);
    }

    private float smoothRotation(float currentAngle, double targetAngle, float smoothFactor) {
        float angleDifference = (float) MathHelper.wrapDegrees(targetAngle - currentAngle);
        float adjustmentSpeed = Math.abs(angleDifference / smoothFactor);
        float angleAdjustment = adjustmentSpeed * Math.signum(Math.signum(angleDifference));

        if (Math.abs(angleAdjustment) > Math.abs(angleDifference)) {
            angleAdjustment = angleDifference;
        }

        return currentAngle + SensUtil.getSens(angleAdjustment) - SensUtil.getSens(angleAdjustment) % SensUtil.getGCDValue();
    }

    public enum RotationTask {
        AIM,
        RESET,
        IDLE
    }
}
