package sky.core.utils.component.impl;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import sky.core.handlers.impl.Rotation;
import sky.core.utils.Wrapper;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class RotationUtil implements Wrapper {

    public Vector2f calculate(final Vector3d fromVec, final Vector3d toVec) {
        final double TO_DEGREES = 180.0F / Math.PI;
        final Vector3d diff = toVec.subtract(fromVec);
        final double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (MathHelper.atan2(diff.z, diff.x) * TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(MathHelper.atan2(diff.y, distance) * TO_DEGREES));
        return new Vector2f(yaw, pitch);
    }
    public static float[] calculateDelta(float currentYaw, float currentPitch, float targetYaw, float targetPitch) {
        float deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);

        float deltaPitch = targetPitch - currentPitch;

        return new float[]{deltaYaw, deltaPitch};
    }
    public static Rotation calculateDelta(Rotation current, Rotation target) {
        float deltaYaw = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());

        float deltaPitch = target.getPitch() - current.getPitch();

        return new Rotation(deltaYaw, deltaPitch);
    }
    public Vector2f calculate(final org.joml.Vector3d fromVec, final org.joml.Vector3d toVec) {
        final double TO_DEGREES = 180.0F / Math.PI;
        final org.joml.Vector3d diff = toVec.sub(fromVec);
        final double distance = Math.hypot(diff.x(), diff.z());
        float yaw = (float) (MathHelper.atan2(diff.z(), diff.x()) * TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(MathHelper.atan2(diff.y(), distance) * TO_DEGREES));
        return new Vector2f(yaw, pitch);
    }

    public Vector2f calculate(final Entity entity) {
        return calculate(entity.getPositionVec().add(0, entity.getEyeHeight(), 0));
    }

    public Vector2f calculate(final Vector3d toVec) {
        return calculate(mc.player.getPositionVec().add(0, mc.player.getEyeHeight(), 0), toVec);
    }

    public Vector2f calculate(final Vector3d toVec, final Direction dir) {
        double x = toVec.x+ 0.5D;
        double y = toVec.y + 0.5D;
        double z = toVec.z + 0.5D;

        x += (double) dir.getDirectionVec().getX() * 0.5D;
        y += (double) dir.getDirectionVec().getY() * 0.5D;
        z += (double) dir.getDirectionVec().getZ() * 0.5D;
        return calculate(new Vector3d(x, y, z));
    }

    public static float getAngleDifference(float dir, float yaw) {
        float f = Math.abs(yaw - dir) % 360.0f;
        return f > 180.0f ? 360.0f - f : f;
    }

    public static Vector3d getEyesPos(Entity entity) {
        return entity.getPositionVec().add(0, entity.getEyeHeight(entity.getPose()), 0);
    }

    public static float[] calculateAngle(Vector3d to) {
        return calculateAngle(getEyesPos(mc.player), to);
    }

    public static float[] calculateAngle(Vector3d from, Vector3d to) {
        double difX = to.x - from.x;
        double difY = (to.y - from.y) * -1.0;
        double difZ = to.z - from.z;
        double dist = MathHelper.sqrt((float) (difX * difX + difZ * difZ));
        float yD = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
        float pD = (float) MathHelper.clamp(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist))), -90f, 90f);
        return new float[]{yD, pD};
    }

    public Vector3f getDirectionVector(float yaw, float pitch) {
        float yawRadians = (float) Math.toRadians(yaw);
        float pitchRadians = (float) Math.toRadians(pitch);

        float x = -MathHelper.cos(pitchRadians) * MathHelper.sin(yawRadians);
        float y = -MathHelper.sin(pitchRadians);
        float z = MathHelper.cos(pitchRadians) * MathHelper.cos(yawRadians);

        return new Vector3f(x, y, z);
    }

    private static final double SAFETY_OFFSET = 0.22; // 5 см внутрь хитбокса

    public static Vector3d findPerfectInternalAimPoint(Entity target, double maxDistance) {
        Vector3d playerEyes = mc.player.getEyePosition(1.0f);

        AxisAlignedBB targetBB = getShrunkHitbox(target.getBoundingBox());

        Vector3d closestPoint = new Vector3d(
                MathHelper.clamp(playerEyes.x, targetBB.minX, targetBB.maxX),
                MathHelper.clamp(playerEyes.y, targetBB.minY, targetBB.maxY),
                MathHelper.clamp(playerEyes.z, targetBB.minZ, targetBB.maxZ)
        );

        if (isStrictlyInside(closestPoint, targetBB)) {
            closestPoint = projectToInnerSurface(playerEyes, targetBB);
        }

        double distance = playerEyes.distanceTo(closestPoint);
        if (distance > maxDistance) {
            Vector3d direction = closestPoint.subtract(playerEyes).normalize();
            closestPoint = playerEyes.add(direction.scale(maxDistance));
            closestPoint = clampToInnerHitbox(closestPoint, targetBB);
        }

        return closestPoint;
    }

    private static AxisAlignedBB getShrunkHitbox(AxisAlignedBB original) {
        return new AxisAlignedBB(
                original.minX + SAFETY_OFFSET,
                original.minY + SAFETY_OFFSET,
                original.minZ + SAFETY_OFFSET,
                original.maxX - SAFETY_OFFSET,
                original.maxY - SAFETY_OFFSET,
                original.maxZ - SAFETY_OFFSET
        );
    }

    private static boolean isStrictlyInside(Vector3d point, AxisAlignedBB box) {
        return point.x > box.minX && point.x < box.maxX &&
                point.y > box.minY && point.y < box.maxY &&
                point.z > box.minZ && point.z < box.maxZ;
    }

    private static Vector3d projectToInnerSurface(Vector3d from, AxisAlignedBB box) {
        double[] distances = {
                from.x - box.minX, box.maxX - from.x,
                from.y - box.minY, box.maxY - from.y,
                from.z - box.minZ, box.maxZ - from.z
        };

        int closestFace = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distances[i] < distances[closestFace]) {
                closestFace = i;
            }
        }

        double x = from.x;
        double y = from.y;
        double z = from.z;

        switch (closestFace) {
            case 0:
                x = box.minX;
                break;
            case 1:
                x = box.maxX;
                break;
            case 2:
                y = box.minY;
                break;
            case 3:
                y = box.maxY;
                break;
            case 4:
                z = box.minZ;
                break;
            case 5:
                z = box.maxZ;
                break;
        }

        return new Vector3d(x, y, z);
    }

    private static Vector3d clampToInnerHitbox(Vector3d point, AxisAlignedBB box) {
        double x = MathHelper.clamp(point.x, box.minX, box.maxX);
        double y = MathHelper.clamp(point.y, box.minY, box.maxY);
        double z = MathHelper.clamp(point.z, box.minZ, box.maxZ);

        return new Vector3d(x, y, z);
    }

    private static boolean isPointInside(Vector3d point, AxisAlignedBB box) {
        return point.x > box.minX && point.x < box.maxX &&
                point.y > box.minY && point.y < box.maxY &&
                point.z > box.minZ && point.z < box.maxZ;
    }

    private static Vector3d findClosestSurfacePoint(Vector3d from, AxisAlignedBB box) {
        double[] distances = {
                from.x - box.minX, box.maxX - from.x,
                from.y - box.minY, box.maxY - from.y,
                from.z - box.minZ, box.maxZ - from.z
        };

        int minIndex = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distances[i] < distances[minIndex]) {
                minIndex = i;
            }
        }

        double x = from.x;
        double y = from.y;
        double z = from.z;

        switch (minIndex) {
            case 0:
                x = box.minX;
                break;
            case 1:
                x = box.maxX;
                break;
            case 2:
                y = box.minY;
                break;
            case 3:
                y = box.maxY;
                break;
            case 4:
                z = box.minZ;
                break;
            case 5:
                z = box.maxZ;
                break;
        }

        return new Vector3d(x, y, z);
    }

    private static Vector3d snapToSurface(Vector3d point, AxisAlignedBB box) {
        double x = MathHelper.clamp(point.x, box.minX, box.maxX);
        double y = MathHelper.clamp(point.y, box.minY, box.maxY);
        double z = MathHelper.clamp(point.z, box.minZ, box.maxZ);

        double dx = Math.min(Math.abs(x - box.minX), Math.abs(x - box.maxX));
        double dy = Math.min(Math.abs(y - box.minY), Math.abs(y - box.maxY));
        double dz = Math.min(Math.abs(z - box.minZ), Math.abs(z - box.maxZ));

        if (dx <= dy && dx <= dz) {
            x = (Math.abs(x - box.minX) < Math.abs(x - box.maxX)) ? box.minX : box.maxX;
        } else if (dy <= dx && dy <= dz) {
            y = (Math.abs(y - box.minY) < Math.abs(y - box.maxY)) ? box.minY : box.maxY;
        } else {
            z = (Math.abs(z - box.minZ) < Math.abs(z - box.maxZ)) ? box.minZ : box.maxZ;
        }

        return new Vector3d(x, y, z);
    }

    private static Vector3d ensureOnSurface(Vector3d point, AxisAlignedBB box) {
        final double EPSILON = 1e-5;

        boolean onX = Math.abs(point.x - box.minX) < EPSILON || Math.abs(point.x - box.maxX) < EPSILON;
        boolean onY = Math.abs(point.y - box.minY) < EPSILON || Math.abs(point.y - box.maxY) < EPSILON;
        boolean onZ = Math.abs(point.z - box.minZ) < EPSILON || Math.abs(point.z - box.maxZ) < EPSILON;

        if (!(onX || onY || onZ)) {
            return snapToSurface(point, box);
        }

        return point;
    }

    private static Vector3d rayTraceToBoxSurface(Vector3d start, Vector3d direction, AxisAlignedBB box) {
        double tMin = Double.NEGATIVE_INFINITY;
        double tMax = Double.POSITIVE_INFINITY;

        // Проверяем пересечение с каждой парой плоскостей
        for (int i = 0; i < 3; i++) {
            double min = i == 0 ? box.minX : (i == 1 ? box.minY : box.minZ);
            double max = i == 0 ? box.maxX : (i == 1 ? box.maxY : box.maxZ);
            double origin = i == 0 ? start.x : (i == 1 ? start.y : start.z);
            double dir = i == 0 ? direction.x : (i == 1 ? direction.y : direction.z);

            if (Math.abs(dir) < 1E-6) {
                if (origin < min || origin > max) return null; // Нет пересечения
                continue;
            }

            double t1 = (min - origin) / dir;
            double t2 = (max - origin) / dir;

            if (t1 > t2) {
                double temp = t1;
                t1 = t2;
                t2 = temp;
            }

            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);

            if (tMin > tMax) return null; // Нет пересечения
        }

        // Возвращаем точку пересечения
        return start.add(direction.scale(tMin));
    }

    private static Vector3d correctToBoxSurface(Vector3d point, AxisAlignedBB box) {
        double x = MathHelper.clamp(point.x, box.minX, box.maxX);
        double y = MathHelper.clamp(point.y, box.minY, box.maxY);
        double z = MathHelper.clamp(point.z, box.minZ, box.maxZ);

        double[] distances = {
                Math.abs(x - box.minX), Math.abs(x - box.maxX),
                Math.abs(y - box.minY), Math.abs(y - box.maxY),
                Math.abs(z - box.minZ), Math.abs(z - box.maxZ)
        };

        int closestFace = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distances[i] < distances[closestFace]) {
                closestFace = i;
            }
        }

        switch (closestFace) {
            case 0:
                x = box.minX;
                break;
            case 1:
                x = box.maxX;
                break;
            case 2:
                y = box.minY;
                break;
            case 3:
                y = box.maxY;
                break;
            case 4:
                z = box.minZ;
                break;
            case 5:
                z = box.maxZ;
                break;
        }

        return new Vector3d(x, y, z);
    }

    public Vector2f getClosestAimAngles(Entity target) {
        Vector3d eyePos = mc.player.getEyePosition(1.0f);
        AxisAlignedBB bb = target.getBoundingBox();

        double minX = bb.minX, minY = bb.minY, minZ = bb.minZ;
        double maxX = bb.maxX, maxY = bb.maxY, maxZ = bb.maxZ;
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        List<Vector3d> points = new ArrayList<>();
        points.add(new Vector3d(minX, minY, minZ));
        points.add(new Vector3d(minX, minY, maxZ));
        points.add(new Vector3d(minX, maxY, minZ));
        points.add(new Vector3d(minX, maxY, maxZ));
        points.add(new Vector3d(maxX, minY, minZ));
        points.add(new Vector3d(maxX, minY, maxZ));
        points.add(new Vector3d(maxX, maxY, minZ));
        points.add(new Vector3d(maxX, maxY, maxZ));
        points.add(new Vector3d(midX, midY, minZ));
        points.add(new Vector3d(midX, midY, maxZ));
        points.add(new Vector3d(midX, minY, midZ));
        points.add(new Vector3d(midX, maxY, midZ));
        points.add(new Vector3d(minX, midY, midZ));
        points.add(new Vector3d(maxX, midY, midZ));
        float bestFov = Float.MAX_VALUE;
        Vector3d bestPoint = null;
        float currentYaw = mc.player.rotationYaw;
        float currentPitch = mc.player.rotationPitch;

        for (Vector3d point : points) {
            Vector2f aimAngles = calculate(point);
            float fov = calculateFov(currentYaw, currentPitch, aimAngles.x, aimAngles.y);
            if (fov < bestFov) {
                bestFov = fov;
                bestPoint = point;
            }
        }

        if (bestPoint != null) {
            return calculate(bestPoint);
        } else {
            return calculate(new Vector3d(midX, midY, midZ));
        }
    }


    public Vector2f calculateLimitedAim(Entity target, double maxDistance) {
        Vector3d aimPoint = findPerfectInternalAimPoint(target, maxDistance);
        return calculate(aimPoint);
    }

    public float calculateFov(float cameraYaw, float cameraPitch, float targetYaw, float targetPitch) {
        Vector3f cameraDirection = getDirectionVector(cameraYaw, cameraPitch);
        Vector3f targetDirection = getDirectionVector(targetYaw, targetPitch);

        float dotProduct = cameraDirection.dot(targetDirection);
        dotProduct = MathHelper.clamp(dotProduct, -1.0f, 1.0f);

        float angleRadians = (float) Math.acos(dotProduct);

        return (float) Math.toDegrees(angleRadians);
    }
}
