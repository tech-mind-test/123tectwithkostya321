package sky.core.utils.player;

import net.minecraft.util.math.vector.Vector3d;
import sky.core.SkyCore;
import sky.core.events.EventInput;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.utils.Wrapper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;

public class MoveUtil implements Wrapper {

    public static boolean isMoving() {
        return mc.player.movementInput.moveStrafe != 0.0 || mc.player.movementInput.moveForward != 0.0;
    }
    public static boolean isInLiquid() {
        return mc.player.isInWater() || mc.player.isInLava();
    }
    public static double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }
    public static boolean isMovingTargetEntity() {
        return SkyCore.getInstance().getModuleManager().attackAura.isEnabled() && AttackAura.getTarget() != null && AttackAura.getBpsTarget() > 0.0f;
    }
    public static void moveToPosition(EventInput event, Vector3d position, float currentYaw) {
        double deltaX = position.x - mc.player.getPosX();
        double deltaZ = position.z - mc.player.getPosZ();

        double angleToTarget = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90F;
        angleToTarget = MathHelper.wrapDegrees(angleToTarget);

        float bestForward = 0F;
        float bestStrafe = 0F;
        float minDifference = Float.MAX_VALUE;

        for (float forward = -1F; forward <= 1F; forward += 1F) {
            for (float strafe = -1F; strafe <= 1F; strafe += 1F) {
                if (forward == 0F && strafe == 0F) {
                    continue;
                }

                double moveAngle = Math.toDegrees(direction(currentYaw, forward, strafe));
                double difference = Math.abs(angleToTarget - MathHelper.wrapDegrees(moveAngle));

                if (difference < minDifference) {
                    minDifference = (float) difference;
                    bestForward = forward;
                    bestStrafe = strafe;
                }
            }
        }

        event.setForward(bestForward);
        event.setStrafe(bestStrafe);
    }
    public static void fixMovementAura(EventInput event, float yaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(
                mc.player.isElytraFlying() ? mc.player.rotationYaw : yaw, forward, strafe)));

        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        float closestForward = 0.0F;
        float closestStrafe = 0.0F;
        float closestDifference = Float.MAX_VALUE;
        for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward += 1.0F) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe += 1.0F) {
                if (predictedStrafe == 0.0F && predictedForward == 0.0F) {
                    continue;
                }
                double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(
                        direction(mc.player.rotationYaw, predictedForward, predictedStrafe)));
                double difference = Math.abs(angle - predictedAngle);
                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }
        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }

    public static void fixMovement(EventInput event, final float yaw) {
        final float forward = event.getForward();
        final float strafe = event.getStrafe();

        final double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.isElytraFlying() ? mc.player.rotationYaw : yaw, forward, strafe)));

        if (forward == 0 && strafe == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;
        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;
                final double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.rotationYaw, predictedForward, predictedStrafe)));
                final double difference = Math.abs(angle - predictedAngle);
                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }

    public static void setMotion(final double speed) {
        if (!isMoving())
            return;

        final double yaw = getDirection(true);
        mc.player.setMotion(-Math.sin(yaw) * speed, mc.player.motion.y, Math.cos(yaw) * speed);
    }

    public static double getDirection(final boolean toRadians) {
        float rotationYaw = mc.player.rotationYaw;
        if (mc.player.moveForward < 0F)
            rotationYaw += 180F;
        float forward = 1F;
        if (mc.player.moveForward < 0F)
            forward = -0.5F;
        else if (mc.player.moveForward > 0F)
            forward = 0.5F;

        if (mc.player.moveStrafing > 0F)
            rotationYaw -= 90F * forward;
        if (mc.player.moveStrafing < 0F)
            rotationYaw += 90F * forward;

        return toRadians ? Math.toRadians(rotationYaw) : rotationYaw;
    }


    public static boolean isBlockUnder(float under) {
        if (mc.player.getPosY() < 0.0) {
            return false;
        } else {
            AxisAlignedBB aab = mc.player.getBoundingBox().offset(0.0, -under, 0.0);
            return mc.world.getCollisionShapes(mc.player, aab).toList().isEmpty();
        }
    }
}