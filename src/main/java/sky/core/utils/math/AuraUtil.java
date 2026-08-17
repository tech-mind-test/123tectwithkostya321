package sky.core.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.joml.Vector4f;
import sky.core.handlers.impl.Rotation;

import static sky.core.utils.Wrapper.mc;

@UtilityClass
public class AuraUtil {

    public static Vector3d getClosestVec(Vector3d vec, AxisAlignedBB AABB) {
        return new Vector3d(MathHelper.clamp(vec.getX(), AABB.minX, AABB.maxX), MathHelper.clamp(vec.getY(), AABB.minY, AABB.maxY), MathHelper.clamp(vec.getZ(), AABB.minZ, AABB.maxZ));
    }

    public static Vector3d getClosestVec(Vector3d vec, Entity entity) {
        return getClosestVec(vec, entity.getBoundingBox());
    }

    public static Vector3d getClosestVec(Entity entity) {
        Vector3d eyePosVec = mc.player.getEyePosition(mc.getRenderPartialTicks());
        return getClosestVec(eyePosVec, entity).subtract(eyePosVec);
    }
    public Vector3d calculateCakeElytraVector(LivingEntity target, boolean targetAir, float predictV) {
        double yExpand = MathHelper.clamp(mc.player.getPosY() + (double) mc.player.getEyeHeight() - target.getPosY(), 0.0, target.getHeight() / 1.5f);
        Vector3d vectorPredict = target.getPositionVec().add(0, yExpand, 0).subtract(mc.player.getEyePosition(1.0f)).add(target.getForward().normalize().scale(predictV));
        Vector3d vectorDefault = target.getPositionVec().add(0, yExpand, 0).subtract(mc.player.getEyePosition(1.0f));
        return targetAir ? vectorPredict : vectorDefault;
    }
    public static Vector3d getVector(Entity entity) {
        Vector3d eye = mc.player.getEyePosition(1.0F);
        Vector3d raw = new Vector3d(entity.getPositionVec().x, MathHelper.lerp(MathHelper.clamp(eye.distanceTo(entity.getPositionVec().add(0, entity.getEyeHeight(), 0)) / 3.0, 0.0, 1.0), entity.getBoundingBox().minY, MathHelper.clamp(eye.y, entity.getBoundingBox().minY, entity.getBoundingBox().maxY)), entity.getPositionVec().z).subtract(eye);
        double distance = mc.player.getDistanceEyePos(entity);
        return raw.normalize().scale(distance);
    }
    public double getStrictDistance(Entity entity) {
        return getClosestVec(entity).length();
    }

    public double calculateFOVFromCamera(LivingEntity target) {
        Vector4f rotation = calculateRotationFromCamera(target);
        float yawDelta = rotation.z;
        float pitchDelta = rotation.w;

        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }
    public Vector4f calculateRotationFromCamera(LivingEntity target) {
        Vector3d vec = getClosestTargetPoint(target).subtract(mc.player.getEyePosition(mc.getRenderPartialTicks()));

        float rawYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90F);
        float rawPitch = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)))));
        float yawDelta = MathHelper.wrapDegrees(rawYaw - Rotation.cameraYaw());
        float pitchDelta = rawPitch - Rotation.cameraPitch();

        return new Vector4f(rawYaw, rawPitch, yawDelta, pitchDelta);
    }
    public Vector3d getClosestTargetPoint(Entity entity) {
        return getClosestTargetPoint(mc.player.getEyePosition(mc.getRenderPartialTicks()), entity, Math.min(entity.getWidth(), entity.getHeight()) / 4F);
    }
    public Vector3d getClosestTargetPoint(Vector3d vec, Entity entity, float point) {
        if (entity == null) {
            return Vector3d.ZERO;
        }

        AxisAlignedBB boundingBox = entity.getBoundingBox().grow(-point);
        Vector3d center = boundingBox.getCenter();
        Vector3d closestPoint = Vector3d.ZERO;
        double closestDistance = Double.MAX_VALUE;

        for (double offsetX = 0; offsetX <= (boundingBox.maxX - boundingBox.minX) / 2; offsetX += 0.1) {
            for (double offsetY = 0; offsetY <= (boundingBox.maxY - boundingBox.minY) / 2; offsetY += 0.1) {
                for (double offsetZ = 0; offsetZ <= (boundingBox.maxZ - boundingBox.minZ) / 2; offsetZ += 0.1) {
                    for (int signX : new int[]{-1, 1}) {
                        for (int signY : new int[]{-1, 1}) {
                            for (int signZ : new int[]{-1, 1}) {
                                double x = center.x + signX * offsetX;
                                double y = center.y + signY * offsetY;
                                double z = center.z + signZ * offsetZ;



//                                if (result instanceof EntityRayTraceResult entityTrace && entityTrace.getEntity().equals(entity)) {
//                                    double distance = vec.distanceTo(potentialPoint);
//                                    if (distance < closestDistance) {
//                                        closestDistance = distance;
//                                        closestPoint = potentialPoint;
//                                    }
//                                }
                            }
                        }
                    }
                }
            }
        }

        if (!closestPoint.equals(Vector3d.ZERO)) {
            return closestPoint;
        }

        double closestX = MathHelper.clamp(vec.x, boundingBox.minX, boundingBox.maxX);
        double closestY = MathHelper.clamp(vec.y, boundingBox.minY, boundingBox.maxY);
        double closestZ = MathHelper.clamp(vec.z, boundingBox.minZ, boundingBox.maxZ);

        return new Vector3d(closestX, closestY, closestZ);
    }
    public static boolean isJumpBlockedByCeiling() {
        boolean blockAboveHead = isBlockAboveHead();
        boolean cannotMoveUp = !canMoveUp();
        if (!mc.player.isInWater() && !mc.player.isOnLadder() && !mc.player.abilities.isFlying) {
            return mc.player.isOnGround() && blockAboveHead && cannotMoveUp;
        } else {
            return false;
        }
    }

    public static boolean isBlockAboveHead() {
        double upHeight = !mc.player.isOnGround() ? (double) 1.5F : (double) 2.5F;
        AxisAlignedBB collisionBox = createBoundingBox(upHeight);
        return mc.world.getCollisionShapes(mc.player, collisionBox).iterator().hasNext();
    }

    public static boolean canMoveUp() {
        boolean isCrouchingAndJumping = mc.player.getPose() == Pose.CROUCHING && mc.gameSettings.keyBindJump.isKeyDown();
        double upHeight = !isCrouchingAndJumping ? (double) mc.player.getHeight() + 0.2 : (double) mc.player.getHeight() + 0.05;
        AxisAlignedBB upMoveBox = createBoundingBox(upHeight);
        return !mc.world.getCollisionShapes(mc.player, upMoveBox).iterator().hasNext();
    }

    private static AxisAlignedBB createBoundingBox(double heightOffset) {
        return new AxisAlignedBB(mc.player.getPosX() - 0.3, mc.player.getPosY() + (double) mc.player.getHeight(), mc.player.getPosZ() - 0.3, mc.player.getPosX() + 0.3, mc.player.getPosY() + heightOffset, mc.player.getPosZ() + 0.3);
    }
}