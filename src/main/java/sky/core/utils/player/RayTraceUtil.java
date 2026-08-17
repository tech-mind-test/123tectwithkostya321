package sky.core.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.function.Predicate;

import static sky.core.utils.Wrapper.mc;

@UtilityClass
public class RayTraceUtil {


    /**
     * @param checkBlocks if true — не считать попадание, если блок ближе цели по лучу
     */
    public static boolean rayTraceEntity(float yaw, float pitch, double distance, Entity entity, boolean checkBlocks) {
        if (mc.player == null || mc.world == null || entity == null) {
            return false;
        }

        Vector3d start = mc.player.getEyePosition(mc.getRenderPartialTicks());
        Vector3d look = mc.player.getVectorForRotation(pitch, yaw);
        Vector3d end = start.add(look.x * distance, look.y * distance, look.z * distance);

        AxisAlignedBB box = entity.getBoundingBox().grow(entity.getCollisionBorderSize());
        Optional<Vector3d> entityHit = box.rayTrace(start, end);

        if (!box.contains(start) && entityHit.isEmpty()) {
            return false;
        }

        if (!checkBlocks) {
            return true;
        }

        Vector3d entityHitVec = box.contains(start) ? start : entityHit.get();
        double entityDist = start.distanceTo(entityHitVec);

        RayTraceContext blockCtx = new RayTraceContext(
                start, end,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                mc.player);
        BlockRayTraceResult blockResult = mc.world.rayTraceBlocks(blockCtx);
        if (blockResult.getType() == RayTraceResult.Type.MISS) {
            return true;
        }

        return start.distanceTo(blockResult.getHitVec()) >= entityDist - 0.05D;
    }
    public static boolean rayTraceSingleEntityWithCustomBox(float yaw, float pitch, double distance, Entity entity, float offsetValue) {
        Vector3d eyeVec = mc.player.getEyePosition(mc.getRenderPartialTicks());
        Vector3d lookVec = getVectorForRotation(pitch, yaw);
        Vector3d endVec = eyeVec.add(lookVec.scale(distance));

        AxisAlignedBB entityBox = entity.getBoundingBox().expand(offsetValue, 0, offsetValue);
        return entityBox.contains(eyeVec) || entityBox.rayTrace(eyeVec, endVec).isPresent();
    }
    public Entity getMouseOver(Entity target,
                               float yaw,
                               float pitch,
                               double distance) {
        RayTraceResult objectMouseOver;
        Entity entity = mc.getRenderViewEntity();

        if (entity != null && mc.world != null) {
            objectMouseOver = null;
            boolean flag = distance > 3;

            Vector3d startVec = entity.getEyePosition(1);
            Vector3d directionVec = getVectorForRotation(pitch, yaw);
            Vector3d endVec = startVec.add(
                    directionVec.x * distance,
                    directionVec.y * distance,
                    directionVec.z * distance
            );

            AxisAlignedBB axisalignedbb = target.getBoundingBox().grow(target.getCollisionBorderSize());

            EntityRayTraceResult entityraytraceresult = rayTraceEntities(entity,
                    startVec,
                    endVec,
                    axisalignedbb,
                    (p_lambda$getMouseOver$0_0_) ->
                            !p_lambda$getMouseOver$0_0_.isSpectator()
                                    && p_lambda$getMouseOver$0_0_.canBeCollidedWith(), distance
            );

            if (entityraytraceresult != null) {
                if (flag && startVec.distanceTo(startVec) > distance) {
                    objectMouseOver = BlockRayTraceResult.createMiss(startVec, null, new BlockPos(startVec));
                }
                if ((distance < distance || objectMouseOver == null)) {
                    objectMouseOver = entityraytraceresult;
                }
            }
            if (objectMouseOver == null) {
                return null;
            }
            try {
                return ((EntityRayTraceResult) objectMouseOver).getEntity();
            } catch (ClassCastException e) {
                return null;
            }
        }
        return null;
    }

    public EntityRayTraceResult rayTraceEntities(Entity shooter,
                                                 Vector3d startVec,
                                                 Vector3d endVec,
                                                 AxisAlignedBB boundingBox,
                                                 Predicate<Entity> filter,
                                                 double distance) {
        World world = shooter.world;
        double closestDistance = distance;
        Entity entity = null;
        Vector3d closestHitVec = null;

        for (Entity entity1 : world.getEntitiesInAABBexcluding(shooter, boundingBox, filter)) {
            AxisAlignedBB axisalignedbb = entity1.getBoundingBox().grow((double) entity1.getCollisionBorderSize());
            Optional<Vector3d> optional = axisalignedbb.rayTrace(startVec, endVec);

            if (axisalignedbb.contains(startVec)) {
                if (closestDistance >= 0.0D) {
                    entity = entity1;
                    closestHitVec = startVec;
                    closestDistance = 0.0D;
                }
            } else if (optional.isPresent()) {
                Vector3d vector3d1 = optional.get();
                double d3 = startVec.distanceTo(optional.get());

                if (d3 < closestDistance || closestDistance == 0.0D) {
                    boolean flag1 = false;

                    if (!flag1 && entity1.getLowestRidingEntity() == shooter.getLowestRidingEntity()) {
                        if (closestDistance == 0.0D) {
                            entity = entity1;
                            closestHitVec = vector3d1;
                        }
                    } else {
                        entity = entity1;
                        closestHitVec = vector3d1;
                        closestDistance = d3;
                    }
                }
            }
        }

        return entity == null ? null : new EntityRayTraceResult(entity, closestHitVec);
    }

    public static boolean rayTraceSingleEntity(float yaw, float pitch, double distance, Entity entity) {
        Vector3d eyeVec = mc.player.getEyePosition(1.0F);
        Vector3d lookVec = mc.player.getVectorForRotation(pitch, yaw);
        Vector3d extendedVec = eyeVec.add(lookVec.scale(distance));
        AxisAlignedBB AABB = entity.getBoundingBox();
        return AABB.contains(eyeVec) || AABB.rayTrace(eyeVec, extendedVec).isPresent();
    }

    public static boolean rayTraceSmallHitBox(float yaw, float pitch, double distance, Entity entity) {
        Vector3d eyeVec = mc.player.getEyePosition(1.0F);
        Vector3d lookVec = mc.player.getVectorForRotation(pitch, yaw);
        Vector3d extendedVec = eyeVec.add(lookVec.scale(distance));

        AxisAlignedBB originalBB = entity.getBoundingBox();

        float value = 1.6f;

        double centerX = (originalBB.minX + originalBB.maxX) / 2.0;
        double centerY = (originalBB.minY + originalBB.maxY) / 2.0;
        double centerZ = (originalBB.minZ + originalBB.maxZ) / 2.0;

        double halfX = (originalBB.maxX - originalBB.minX) / 2.0 / value;
        double halfY = (originalBB.maxY - originalBB.minY) / 2.0 / 1.2f;
        double halfZ = (originalBB.maxZ - originalBB.minZ) / 2.0 / value;

        AxisAlignedBB smallBB = new AxisAlignedBB(
                centerX - halfX, centerY - halfY, centerZ - halfZ,
                centerX + halfX, centerY + halfY, centerZ + halfZ
        );

        return smallBB.contains(eyeVec) || smallBB.rayTrace(eyeVec, extendedVec).isPresent();
    }

    public static Vector3d getVectorForRotation(float pitch, float yaw) {
        return mc.player.getVectorForRotation(pitch, yaw);
    }
}
