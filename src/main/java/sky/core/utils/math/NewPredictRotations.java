package sky.core.utils.math;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.SkyCore;
import sky.core.utils.component.impl.RotationComponent;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.modules.impl.movement.ElytraTarget;
import sky.core.utils.Wrapper;

import static net.minecraft.entity.Entity.horizontalMag;


public class NewPredictRotations implements Wrapper {
    public static boolean predictCondition = false;
    private static int movementTicks = 0;
    private static boolean prePredictCondition = false;
    private static final TimeUtil tickStopWatch = new TimeUtil();

    public void updateRotations(LivingEntity target) {
        final ElytraTarget elytraTarget = SkyCore.getInstance().getModuleManager().getElytraTarget();
        final Vector3d predictedPos = getPredictedPosition(target, elytraTarget.getDistance().get().intValue());
        final Vector3d eyePos = mc.player.getEyePosition(mc.getRenderPartialTicks());

        final double deltaX = predictedPos.x - eyePos.x;
        final double deltaY = predictedPos.y - eyePos.y;
        final double deltaZ = predictedPos.z - eyePos.z;

        final double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        final float yawToTarget = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        final float pitchToTarget = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDist));

        RotationComponent.update(new Rotation(yawToTarget, pitchToTarget), 180, 1, 12);
    }

    public static void smartPredict(LivingEntity target) {
        if (tickStopWatch.isReached(50L)) {
            if (target != null) {
                float x = (float) (target.getPosX() - target.prevPosX);
                float y = (float) (target.getPosY() - target.prevPosY);
                float z = (float) (target.getPosZ() - target.prevPosZ);
                float dist = (float) Math.sqrt(x * x + y * y + z * z);
                float finalSpeed = (float) ((double) dist * (double) 20.0F);
                if (finalSpeed > 20.0F) {
                    movementTicks = 0;
                    prePredictCondition = false;
                    predictCondition = true;
                } else {
                    ++movementTicks;
                    if (predictCondition) {
                        prePredictCondition = true;
                    }

                    if (movementTicks >= 3) {
                        predictCondition = false;
                        movementTicks = 0;
                        prePredictCondition = false;
                    } else {
                        predictCondition = prePredictCondition;
                    }
                }
            }

            tickStopWatch.reset();
        }

    }

    public Vector3d getResolvedPos(LivingEntity target) {
        if (target == null) return null;
        return new Vector3d((target).serverX, (target).serverY, (target).serverZ);
    }


    public Vector3d getPredictedPosition(LivingEntity target, int ticks) {
        final AttackAura aura = SkyCore.getInstance().getModuleManager().getAttackAura();
        final ElytraTarget elytraTarget = SkyCore.getInstance().getModuleManager().getElytraTarget();
        Vector3d pos = this.getResolvedPos(target).add(0, target.getEyeHeight() * 0.9, 0);
        Vector3d velocity = target.getMotion();

        Vector3d lookVec = target.getLookVec();
        float pitch = target.rotationPitch;

        final boolean isSpeed = aura.lastSpeed >= 20 || (aura.lastSpeed != aura.prevSpeed && aura.lastSpeed == 0);
        if (!(elytraTarget.isEnabled() && mc.player.isElytraFlying() && isSpeed)) return pos;

        velocity = simulateElytraTick(velocity, lookVec, pitch, ticks);
        pos = pos.add(velocity);

        return pos;
    }
    public static Vector3d predict(LivingEntity entity, Vector3d pos, int ticks) {
        if (ticks <= 0) return pos;

        if (!entity.isElytraFlying()) {
            Vector3d velocity = entity.getMotion();
            if (velocity.lengthSquared() < 1.0E-6) return pos;
            return pos.add(velocity.multiply(ticks));
        }

        Vector3d velocity = entity.getMotion();

        if (entity.isElytraFlying() && Math.hypot(entity.prevPosX - entity.getPosX(), entity.prevPosZ - entity.getPosZ()) * 20 <= 5 && (entity.getPosY() - entity.prevPosY) * 20 <= 5) return pos;

        for (int i = 0; i < ticks; i++) {
            Vector3d rotation = entity.getRotationVector();
            float pitchRad = (float) Math.toRadians(entity.rotationPitch);
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double velocityLength = velocity.length();
            float cos = MathHelper.cos(pitchRad);
            cos = (float) (cos * cos * Math.min(1.0D, rotation.length() / 0.4D));

            velocity = velocity.add(0.0D, -0.08D * (-1.0D + (double) cos * 0.75D), 0.0D);

            if (velocity.y < 0.0D && horizontalSpeed > 0.0D) {
                double d5 = velocity.y * -0.1D * cos;
                velocity = velocity.add(rotation.x * d5 / horizontalSpeed, d5, rotation.z * d5 / horizontalSpeed);
            }

            if (pitchRad < 0.0F && horizontalSpeed > 0.0D) {
                double lift = velocityLength * (-MathHelper.sin(pitchRad)) * 0.04D;
                velocity = velocity.add(-rotation.x * lift / horizontalSpeed, lift * 3.2D, -rotation.z * lift / horizontalSpeed);
            }

            if (horizontalSpeed > 0.0D) {
                velocity = velocity.add(
                        (rotation.x / horizontalSpeed * velocityLength - velocity.x) * 0.1D,
                        0.0D,
                        (rotation.z / horizontalSpeed * velocityLength - velocity.z) * 0.1D
                );
            }

            velocity = velocity.multiply(0.99D, 0.98D, 0.99D);
            pos = pos.add(velocity);
        }

        return pos;
    }

    private Vector3d simulateElytraTick(Vector3d velocity, Vector3d lookVec, float pitch,int forward) {
        float pitchRad = pitch * ((float) Math.PI / 180.0F);

        double lookHorizontalDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
        double velocityHorizontalSpeed = Math.sqrt(horizontalMag(velocity));
        double lookLength = lookVec.length();

        float cosPitch = MathHelper.cos(pitchRad);
        cosPitch = (float) ((double) cosPitch * (double) cosPitch * Math.min(1.0D, lookLength / 0.4D));

        velocity = velocity.add(0.0D, 0.08D * (-1.0D + (double) cosPitch * 0.75D), 0.0D);

        if (velocity.y < 0.0D && lookHorizontalDist > 0.0D) {
            double lift = velocity.y * -0.1D * (double) cosPitch;
            velocity = velocity.add(
                    lookVec.x * lift / lookHorizontalDist,
                    lift,
                    lookVec.z * lift / lookHorizontalDist
            );
        }

        if (pitchRad < 0.0F && lookHorizontalDist > 0.0D) {
            double boost = velocityHorizontalSpeed * (double) (-MathHelper.sin(pitchRad)) * 0.04D;
            velocity = velocity.add(
                    -lookVec.x * boost / lookHorizontalDist,
                    boost * 3.2D,
                    -lookVec.z * boost / lookHorizontalDist
            );
        }

        if (lookHorizontalDist > 0.0D) {
            velocity = velocity.add(
                    (lookVec.x / lookHorizontalDist * velocityHorizontalSpeed - velocity.x) * 0.1D,
                    0.0D,
                    (lookVec.z / lookHorizontalDist * velocityHorizontalSpeed - velocity.z) * 0.1D
            );
        }

        Vector3d lastVec = velocity.mul(0.99F, 0.98F, 0.99F);
        return lastVec.scale(forward);

    }

}