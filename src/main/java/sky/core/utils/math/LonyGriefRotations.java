package sky.core.utils.math;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector2f; // В 1.16.5 есть свой Vector2f
import sky.core.utils.component.impl.SensUtil;
import sky.core.utils.Wrapper;


public class LonyGriefRotations implements Wrapper {



    public Vector2f calculateRotation(LivingEntity target, Vector2f rotation) {
        Vector3d pos = calculatePoint(target);

        float shortestYawPath = (float) ((((((Math.toDegrees(Math.atan2(pos.z, pos.x)) - 90) - rotation.x) % 360) + 540) % 360) - 180);

        float findPitch = (float) Math.min(90, -Math.toDegrees(Math.atan2(pos.y, Math.hypot(pos.x, pos.z))));

        float targetYaw = SensUtil.getSens(rotation.x + shortestYawPath);
        float targetPitch = SensUtil.getSens(MathHelper.clamp(findPitch, -90, 90));

        return new Vector2f(targetYaw, targetPitch);
    }
    private Vector3d calculatePoint(LivingEntity target) {
        return target.getBoundingBox().getCenter().subtract(mc.player.getEyePosition(1.0F));
    }

    public Vector2f getRotations(Vector3d vec3d) {
        return getRotations(vec3d.x, vec3d.y, vec3d.z);
    }

    public Vector2f getRotations(double x, double y, double z) {
        double deltaX = x - mc.player.getPosX();
        double deltaY = y - mc.player.getPosYEye();
        double deltaZ = z - mc.player.getPosZ();
        double distance = MathHelper.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (MathHelper.atan2(deltaZ, deltaX) * (180D / Math.PI) - 90.0F);
        float pitch = (float) (-MathHelper.atan2(deltaY, distance) * (180D / Math.PI));
        return new Vector2f(yaw, pitch);
    }

    public Vector3d predict(LivingEntity entity, Vector3d pos, int ticks) {
        Vector3d velocity = entity.getMotion(); // В 1.16.5 getDeltaMovement() называется getMotion()

        // Проверка на элитры (isFallFlying -> isElytraFlying)
        if (entity.isElytraFlying() && Math.hypot(entity.prevPosX - entity.getPosX(), entity.prevPosZ - entity.getPosZ()) * 20 <= 5 && (entity.getPosY() - entity.prevPosY) * 20 <= 5) {
            return pos;
        }

        for (int i = 0; i < ticks; i++) {
            Vector3d eyePos = entity.getEyePosition(1.0F);
            float pitchRad = (float) Math.toRadians(entity.rotationPitch); // xRot в 1.16.5 это rotationPitch
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double velocityLength = velocity.length();
            float cos = MathHelper.cos(pitchRad);
            cos = (float) (cos * cos * Math.min(1.0D, eyePos.length() / 0.4D));

            velocity = velocity.add(0.0D, -0.08D * (-1.0D + (double) cos * 0.75D), 0.0D);

            if (velocity.y < 0.0D && horizontalSpeed > 0.0D) {
                double d5 = velocity.y * -0.1D * cos;
                // В 1.16.5 у сущностей нет прямого вектора взгляда в переменной rotation, используем getLookVec
                Vector3d lookVec = entity.getLook(1.0F);
                velocity = velocity.add(lookVec.x * d5 / horizontalSpeed, d5, lookVec.z * d5 / horizontalSpeed);
            }

            if (pitchRad < 0.0F && horizontalSpeed > 0.0D) {
                double lift = velocityLength * (-MathHelper.sin(pitchRad)) * 0.04D;
                Vector3d lookVec = entity.getLook(1.0F);
                velocity = velocity.add(-lookVec.x * lift / horizontalSpeed, lift * 3.2D, -lookVec.z * lift / horizontalSpeed);
            }

            if (horizontalSpeed > 0.0D) {
                Vector3d lookVec = entity.getLook(1.0F);
                velocity = velocity.add(
                        (lookVec.x / horizontalSpeed * velocityLength - velocity.x) * 0.1D,
                        0.0D,
                        (lookVec.z / horizontalSpeed * velocityLength - velocity.z) * 0.1D
                );
            }

            velocity = velocity.mul(0.99D, 0.98D, 0.99D); // multiply -> mul
            pos = pos.add(velocity);
        }

        return pos;
    }
}