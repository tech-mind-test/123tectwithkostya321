package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.events.EventInput;
import sky.core.events.EventOnMovePost;
import sky.core.events.EventPacket;
import sky.core.events.EventPostUpdate;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.utils.player.MoveUtil;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Speed extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "RW", "RW", "Collision");

    private int ticks;
    private int groundTicks;

    public Speed() {
        super("Speed", "Позволяет бустится от колизии", Category.Movement);
        addSettings(mode);
    }

    final Map<Entity, Vector3d> previousPositions = new HashMap<>();

    @EventTarget
    public void onEvent(EventUpdate event) {
        if (!mode.is("Collision")) {
            return;
        }

        if (MoveUtil.isMoving()) {
            double scanRadius = 1.25;

            List<Entity> nearbyEntities = mc.world.getEntitiesWithinAABBExcludingEntity(
                    mc.player,
                    mc.player.getBoundingBox().grow(scanRadius)
            );

            for (Entity entity : nearbyEntities) {
                if (entity != mc.player &&
                        (entity instanceof PlayerEntity)) {

                    double distanceX = Math.abs(mc.player.getPosX() - entity.getPosX());
                    double distanceZ = Math.abs(mc.player.getPosZ() - entity.getPosZ());
                    double activationDistanceX = 2.1;
                    double activationDistanceZ = 1.3;

                    if (distanceX > activationDistanceX || distanceZ > activationDistanceZ) {
                        continue;
                    }

                    double entitySpeed = getEntitySpeed(entity);
                    double boostAmount;

                    if (entitySpeed < 5) {
                        boostAmount = 0.02;

                        List<Entity> collisionEntities = mc.world.getEntitiesWithinAABBExcludingEntity(
                                mc.player,
                                mc.player.getBoundingBox().grow(0.1)
                        );

                        int collisions = 0;
                        for (Entity collisionEntity : collisionEntities) {
                            if (collisionEntity != mc.player &&
                                    (!(collisionEntity instanceof ArmorStandEntity) || collisionEntity instanceof BoatEntity)) {
                                collisions++;
                            }
                        }


                        if (collisions > 0) {
                            double[] motion = forward(boostAmount);
                            mc.player.addVelocity(motion[0], 0.0, motion[1]);
                        }
                    } else {
                        boostAmount = 0.032;
                        double checkRadius = 1.25;

                        List<Entity> potentialCollisions = mc.world.getEntitiesWithinAABBExcludingEntity(
                                mc.player,
                                mc.player.getBoundingBox().grow(checkRadius)
                        );

                        int collisions = 0;
                        for (Entity collisionEntity : potentialCollisions) {
                            if (collisionEntity != mc.player &&
                                    (!(collisionEntity instanceof ArmorStandEntity) || collisionEntity instanceof BoatEntity)) {

                                double distToCollision = mc.player.getDistance(collisionEntity);
                                if (distToCollision <= checkRadius) {
                                    collisions++;
                                }
                            }
                        }

                        if (collisions > 0) {
                            double[] motion = forward(boostAmount);
                            mc.player.addVelocity(motion[0], 0.0, motion[1]);
                        }
                    }
                    break;
                }
            }
        }

    }

    @EventTarget
    public void onEvent(EventOnMovePost event) {
        if (!mode.is("RW") || !canUseRW()) {
            resetRWState(true);
            return;
        }

        mc.timer.setSpeed(1.9F);

        if (ticks > 3) {
            double boost = 0.03D;

            if (ticks % 2 == 0) {
                mc.player.setMotion(mc.player.getMotion().add(0.0D, 0.03D, 0.0D));
                boost = mc.player.isOnGround() ? 0.085D : 0.03D;
            }

            double yaw = Math.toRadians(MoveUtil.getDirection(false));
            double x = -Math.sin(yaw);
            double z = Math.cos(yaw);

            mc.player.setMotion(mc.player.getMotion().add(x * boost, 0.0D, z * boost));
        }

        ticks++;
    }

    @EventTarget
    public void onEvent(EventInput event) {
        if (!mode.is("RW") || mc.player == null || !MoveUtil.isMoving()) {
            groundTicks = 0;
            return;
        }

        if (mc.player.collidedVertically) {
            groundTicks++;
        } else {
            groundTicks = 0;
        }

        if (groundTicks >= 1) {
            mc.player.jump();
        }
    }

    @EventTarget
    public void onEvent(EventPostUpdate event) {
        if (!mode.is("RW")) {
            return;
        }

        if (!canUseRW()) {
            resetRWState(true);
            return;
        }

        if (ticks % 2 == 0) {
            mc.timer.setSpeed(0.3F);
            mc.player.connection.sendPacketWithoutEvent(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
        }
    }

    @EventTarget
    public void onEvent(EventPacket event) {
        if (!mode.is("RW") || mc.player == null || !event.isReceive()) {
            return;
        }

        IPacket<?> packet = event.getPacket();
        if (packet instanceof SPlayerPositionLookPacket) {
            if (ticks % 2 == 1) {
                ticks++;
            }

            mc.timer.setSpeed(1.0F);
        }
    }

    public double getEntitySpeed(Entity entity) {
        Vector3d currentPos = entity.getPositionVec();
        Vector3d previousPos = previousPositions.getOrDefault(entity, currentPos);

        double dx = currentPos.x - previousPos.x;
        double dz = currentPos.z - previousPos.z;
        double speed = Math.sqrt(dx * dx + dz * dz) * 20.0;

        previousPositions.put(entity, currentPos);

        return speed;
    }

    private double[] forward(double speed) {
        float forward = mc.player.moveForward;
        float strafe = mc.player.moveStrafing;
        float yaw = mc.player.rotationYaw;

        if (forward != 0) {
            if (strafe > 0) {
                yaw += forward > 0 ? -45 : 45;
            } else if (strafe < 0) {
                yaw += forward > 0 ? 45 : -45;
            }
            strafe = 0;
            if (forward > 0) {
                forward = 1;
            } else if (forward < 0) {
                forward = -1;
            }
        }

        double sin = Math.sin(Math.toRadians(yaw + 90));
        double cos = Math.cos(Math.toRadians(yaw + 90));
        double posX = forward * speed * cos + strafe * speed * sin;
        double posZ = forward * speed * sin - strafe * speed * cos;

        return new double[]{posX, posZ};
    }

    private boolean canUseRW() {
        return mc.player != null
                && mc.world != null
                && mc.player.connection != null
                && MoveUtil.isMoving()
                && !mc.player.isPassenger()
                && !mc.player.abilities.isFlying;
    }

    private void resetRWState(boolean resetTimer) {
        ticks = 0;
        groundTicks = 0;
        if (resetTimer && mc.timer != null) {
            mc.timer.resetSpeed();
        }
    }

    @Override
    public void onEnable() {
        resetRWState(true);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetRWState(true);
        super.onDisable();
    }

}
