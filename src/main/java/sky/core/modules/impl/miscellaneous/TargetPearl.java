package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector2f;
import sky.core.SkyCore;
import sky.core.events.EventInput;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.utils.component.RotationAccess;
import sky.core.utils.component.impl.RotationComponent;
import sky.core.utils.player.InventoryUtil;
import sky.core.utils.player.MoveUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TargetPearl extends Module {

    /** Выше AttackAura (макс. ~6) и сброса ауры (30), чтобы не дублировать look-пакеты. */
    private static final int ROTATION_PRIORITY = 50;
    private static final float ROTATION_SPEED = 360f;
    private static final float ROTATION_EPSILON = 4f;

    private final BooleanSetting onlyTarget = new BooleanSetting("Только за таргетом", false);
    private final SliderSetting distance = new SliderSetting("Мин дистанция", 10, 8, 20, 1f);

    private EnderPearlEntity targetPearl = null;
    private Vector3d cachedLanding = null;
    private long lastPearlScan = 0;
    private long lastThrowTime = 0;
    private long rotationHoldUntil = 0;
    private boolean isThrowing = false;
    private Vector2f server = null;

    private boolean awaitingThrow = false;
    private float pendingYaw;
    private float pendingPitch;
    private int rotationWaitTicks = 0;

    public Entity rememberedTarget = null;
    private long rememberedTargetTime = 0;

    private final Map<Integer, Integer> pearlOwnerById = new HashMap<>();
    private final Map<Integer, Integer> recentThrowers = new HashMap<>();

    private static final long TARGET_MEMORY_DURATION = 10000L;
    private static final int PEARL_SCAN_INTERVAL = 35;
    private static final int THROW_COOLDOWN = 2000;
    private static final int MAX_SIM_TICKS = 160;
    private static final double MAX_DISTANCE_FROM_TARGET = 1.5;
    private static final double MAX_ACCEPTABLE_ERROR = 2.0;

    public TargetPearl() {
        super("TargetPearl", "Кидает перл точно за перлом врага (через всё)", Category.Combat);
        addSettings(onlyTarget, distance);
    }

    public static boolean isRotationActive() {
        TargetPearl module = (TargetPearl) SkyCore.getInstance().getModuleManager().getModule(TargetPearl.class);
        return module != null && module.isManagingRotation();
    }

    public boolean isManagingRotation() {
        if (!isEnabled()) {
            return false;
        }
        return awaitingThrow
                || isThrowing
                || (rotationHoldUntil > 0L && System.currentTimeMillis() < rotationHoldUntil);
    }

    private boolean check() {
        return isEnabled() && isThrowing && server != null;
    }

    @EventTarget
    public void onEvent(EventInput event) {
        if (event == null || mc.player == null || mc.world == null || mc.player.isElytraFlying()) {
            return;
        }

        if (rotationHoldUntil > 0L && System.currentTimeMillis() >= rotationHoldUntil) {
            isThrowing = false;
            server = null;
            rotationHoldUntil = 0L;
        }

        if (check()) {
            MoveUtil.fixMovement(event, server.x);
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (!isEnabled() || event == null || !event.isReceive() || mc.world == null) {
            return;
        }

        Object packet = event.getPacket();
        int tick = mc.player != null ? mc.player.ticksExisted : 0;

        if (packet instanceof SAnimateHandPacket animate) {
            if (animate.getAnimationType() == 0) {
                recentThrowers.put(animate.getEntityID(), tick + 12);
            }
            return;
        }

        if (packet instanceof SSpawnObjectPacket spawn && spawn.getType() == EntityType.ENDER_PEARL) {
            int ownerId = spawn.getData();
            if (ownerId != 0) {
                pearlOwnerById.put(spawn.getEntityID(), ownerId);
                return;
            }

            for (Map.Entry<Integer, Integer> entry : recentThrowers.entrySet()) {
                if (entry.getValue() >= tick) {
                    pearlOwnerById.put(spawn.getEntityID(), entry.getKey());
                    break;
                }
            }

            Entity nearest = findNearestThrower(spawn.getX(), spawn.getY(), spawn.getZ(), 4.5D);
            if (nearest != null) {
                pearlOwnerById.put(spawn.getEntityID(), nearest.getEntityId());
            }
        }
    }

    @EventTarget
    public void onEvent(EventUpdate event) {
        if (event == null || mc.player == null || mc.world == null || mc.player.isElytraFlying()) {
            return;
        }

        if (awaitingThrow) {
            processPendingThrow();
            return;
        }

        tryThrow();
    }

    private void processPendingThrow() {
        if (!canThrow() || cachedLanding == null) {
            cancelPendingThrow();
            return;
        }

        if (!applyThrowRotation(pendingYaw, pendingPitch)) {
            rotationWaitTicks++;
            if (rotationWaitTicks < 10) {
                return;
            }
        }
        rotationWaitTicks = 0;

        if (!mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL)
                && InventoryUtil.getItemSlot(Items.ENDER_PEARL) != -1) {
            InventoryUtil.findItemAndThrow(Items.ENDER_PEARL);
            lastThrowTime = System.currentTimeMillis();
        }

        awaitingThrow = false;
        rotationHoldUntil = System.currentTimeMillis() + 50L;
    }

    private void cancelPendingThrow() {
        awaitingThrow = false;
        isThrowing = false;
        server = null;
    }

    private void tryThrow() {
        if (System.currentTimeMillis() - lastThrowTime < THROW_COOLDOWN) {
            return;
        }
        if (!canThrow()) {
            return;
        }

        updateTargetPearl();
        if (cachedLanding == null) {
            return;
        }

        float[] rot = calculateYawPitch(cachedLanding);
        if (rot == null) {
            rot = findAnyWorkingRotation(cachedLanding);
            if (rot == null) {
                return;
            }
        }

        Vector3d predicted = simulateTrajectory(rot[0], rot[1]);
        if (predicted != null) {
            double distToTarget = cachedLanding.distanceTo(predicted);
            Vector3d eye = mc.player.getEyePosition(1.0f);
            double distToCached = eye.distanceTo(cachedLanding);
            double heightDiff = cachedLanding.y - eye.y;
            boolean isHighTarget = heightDiff > 5.0;

            double maxAllowedError = distToCached > 60
                    ? MAX_DISTANCE_FROM_TARGET * 2.0
                    : MAX_DISTANCE_FROM_TARGET * 1.5;

            if (isHighTarget) {
                maxAllowedError = MAX_DISTANCE_FROM_TARGET * 3.0;
            }

            if (hasObstacleBetween(eye, cachedLanding)) {
                maxAllowedError = isHighTarget
                        ? MAX_DISTANCE_FROM_TARGET * 4.0
                        : MAX_DISTANCE_FROM_TARGET * 2.5;
            }

            if (distToTarget > maxAllowedError) {
                rot = findAnyWorkingRotation(cachedLanding);
                if (rot == null) {
                    return;
                }
            }
        }

        pendingYaw = rot[0];
        pendingPitch = rot[1];
        isThrowing = true;
        server = new Vector2f(pendingYaw, pendingPitch);
        awaitingThrow = true;
        applyThrowRotation(pendingYaw, pendingPitch);
    }

    private boolean canThrow() {
        return !mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL);
    }

    private void updateTargetPearl() {
        cleanupPearlTracking();

        long now = System.currentTimeMillis();
        if (now - lastPearlScan < PEARL_SCAN_INTERVAL) {
            if (targetPearl != null && targetPearl.isAlive() && cachedLanding != null) {
                return;
            }
            if (now - lastPearlScan < PEARL_SCAN_INTERVAL / 2) {
                return;
            }
        }

        lastPearlScan = now;
        cachedLanding = null;
        targetPearl = findBestPearl();

        if (targetPearl != null && targetPearl.isAlive()) {
            cachedLanding = predictLanding(targetPearl);
            if (cachedLanding != null && !isValidLanding(cachedLanding)) {
                cachedLanding = null;
            }
        }
    }

    private boolean isValidLanding(Vector3d landing) {
        BlockPos pos = new BlockPos(landing);
        for (int i = 1; i <= 4; i++) {
            if (!mc.world.getBlockState(pos.down(i)).getCollisionShape(mc.world, pos.down(i)).isEmpty()) {
                return true;
            }
        }
        return landing.y <= 1.0D;
    }

    private void cleanupPearlTracking() {
        if (mc.player == null) {
            return;
        }
        int tick = mc.player.ticksExisted;
        recentThrowers.entrySet().removeIf(entry -> entry.getValue() < tick);
        pearlOwnerById.entrySet().removeIf(entry -> mc.world.getEntityByID(entry.getKey()) == null);
    }

    private LivingEntity resolveAuraTarget() {
        long currentTime = System.currentTimeMillis();
        LivingEntity currentAuraTarget = AttackAura.getTarget();
        if (currentAuraTarget == null) {
            currentAuraTarget = RotationAccess.target;
        }

        if (currentAuraTarget != null && currentAuraTarget.isAlive() && !currentAuraTarget.removed) {
            rememberedTarget = currentAuraTarget;
            rememberedTargetTime = currentTime;
            return currentAuraTarget;
        }

        if (rememberedTarget != null) {
            if (!rememberedTarget.isAlive() || rememberedTarget.removed) {
                rememberedTarget = null;
                rememberedTargetTime = 0;
                return null;
            }
            if (currentTime - rememberedTargetTime < TARGET_MEMORY_DURATION) {
                return (LivingEntity) rememberedTarget;
            }
            rememberedTarget = null;
            rememberedTargetTime = 0;
        }

        return null;
    }

    private Entity getPearlShooter(EnderPearlEntity pearl) {
        assignPearlOwner(pearl);

        Entity shooter = pearl.getShooter();
        if (shooter != null && shooter.isAlive()) {
            return shooter;
        }

        shooter = pearl.func_234616_v_();
        if (shooter != null) {
            return shooter;
        }

        Integer mappedOwner = pearlOwnerById.get(pearl.getEntityId());
        if (mappedOwner != null && mc.world != null) {
            return mc.world.getEntityByID(mappedOwner);
        }

        int ownerId = pearl.getOwnerEntityId();
        if (ownerId != 0 && mc.world != null) {
            return mc.world.getEntityByID(ownerId);
        }

        return null;
    }

    private void assignPearlOwner(EnderPearlEntity pearl) {
        if (pearl == null || mc.world == null) {
            return;
        }

        Entity knownOwner = getPearlShooterWithoutAssign(pearl);
        if (knownOwner != null) {
            if (pearl.getShooter() == null) {
                pearl.setShooter(knownOwner);
            }
            return;
        }

        if (pearl.ticksExisted > 8) {
            return;
        }

        Entity nearest = findNearestThrower(pearl.getPosX(), pearl.getPosY(), pearl.getPosZ(), 4.0D);
        if (nearest != null) {
            pearl.setShooter(nearest);
            pearlOwnerById.put(pearl.getEntityId(), nearest.getEntityId());
        }
    }

    private Entity getPearlShooterWithoutAssign(EnderPearlEntity pearl) {
        Entity shooter = pearl.getShooter();
        if (shooter != null && shooter.isAlive()) {
            return shooter;
        }
        shooter = pearl.func_234616_v_();
        if (shooter != null) {
            return shooter;
        }
        Integer mappedOwner = pearlOwnerById.get(pearl.getEntityId());
        if (mappedOwner != null) {
            return mc.world.getEntityByID(mappedOwner);
        }
        int ownerId = pearl.getOwnerEntityId();
        if (ownerId != 0) {
            return mc.world.getEntityByID(ownerId);
        }
        return null;
    }

    private Entity findNearestThrower(double x, double y, double z, double maxDistance) {
        PlayerEntity nearest = null;
        double nearestDist = maxDistance * maxDistance;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {
                continue;
            }
            double eyeDist = player.getEyePosition(1.0F).squareDistanceTo(x, y, z);
            double bodyDist = player.getPositionVec().squareDistanceTo(x, y, z);
            double dist = Math.min(eyeDist, bodyDist);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    private boolean isPearlFromEntity(EnderPearlEntity pearl, Entity entity) {
        if (pearl == null || entity == null) {
            return false;
        }

        Entity shooter = getPearlShooter(pearl);
        if (shooter != null) {
            return isSameEntity(shooter, entity);
        }

        int ownerId = pearl.getOwnerEntityId();
        if (ownerId != 0 && ownerId == entity.getEntityId()) {
            return true;
        }

        UUID ownerUuid = pearl.getOwnerUuid();
        return ownerUuid != null && ownerUuid.equals(entity.getUniqueID());
    }

    private boolean isSameEntity(Entity a, Entity b) {
        if (a == null || b == null) {
            return false;
        }
        return a == b
                || a.getEntityId() == b.getEntityId()
                || a.getUniqueID().equals(b.getUniqueID());
    }

    private EnderPearlEntity findBestPearl() {
        List<Entity> entities = mc.world.getEntitiesWithinAABBExcludingEntity(
                mc.player, mc.player.getBoundingBox().grow(130));

        EnderPearlEntity best = null;
        double bestDist = Double.MAX_VALUE;
        double minDist = distance.get();

        LivingEntity targetToUse = onlyTarget.get() ? resolveAuraTarget() : null;
        if (onlyTarget.get() && targetToUse == null) {
            return null;
        }

        for (Entity e : entities) {
            if (!(e instanceof EnderPearlEntity pearl) || !pearl.isAlive()) {
                continue;
            }

            assignPearlOwner(pearl);
            Entity shooter = getPearlShooter(pearl);
            if (shooter != null && isSameEntity(shooter, mc.player)) {
                continue;
            }

            if (onlyTarget.get() && !isPearlFromEntity(pearl, targetToUse)) {
                continue;
            }

            Vector3d landing = predictLanding(pearl);
            if (landing == null) {
                continue;
            }

            double dist = mc.player.getPositionVec().distanceTo(landing);
            if (dist >= minDist && dist <= 120 && dist < bestDist) {
                best = pearl;
                bestDist = dist;
            }
        }
        return best;
    }

    private Vector3d predictLanding(EnderPearlEntity pearl) {
        Vector3d pos = pearl.getPositionVec();
        Vector3d vel = pearl.getMotion();

        for (int i = 0; i < MAX_SIM_TICKS; i++) {
            Vector3d next = pos.add(vel);
            vel = vel.scale(0.99).subtract(0, 0.03, 0);

            if (next.y <= 0) {
                return snapToBlockCenter(next);
            }

            BlockPos bp = new BlockPos(next);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return snapToBlockCenter(next);
            }
            pos = next;
        }
        return null;
    }

    private Vector3d snapToBlockCenter(Vector3d vec) {
        return new Vector3d(
                MathHelper.floor(vec.x) + 0.5,
                MathHelper.floor(vec.y),
                MathHelper.floor(vec.z) + 0.5
        );
    }

    private boolean hasObstacleBetween(Vector3d start, Vector3d end) {
        Vector3d direction = end.subtract(start);
        double distanceToTarget = direction.length();
        Vector3d normalized = direction.normalize();

        int steps = (int) (distanceToTarget / 0.5) + 1;
        for (int i = 1; i < steps; i++) {
            Vector3d checkPos = start.add(normalized.scale(i * 0.5));
            BlockPos bp = new BlockPos(checkPos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private float[] calculateYawPitch(Vector3d target) {
        Vector3d eye = mc.player.getEyePosition(1.0f);
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;

        double dist = eye.distanceTo(target);
        double heightDiff = target.y - eye.y;
        boolean isHighTarget = heightDiff > 5.0;

        float maxPitch = isHighTarget ? 89.0f : 85.0f;
        float minPitch = dist > 60 ? -50.0f : -30.0f;
        if (isHighTarget) {
            minPitch = -80.0f;
        }
        float step = dist > 60 ? 0.5f : 0.42f;
        if (isHighTarget) {
            step = 0.3f;
        }

        float bestPitch = 0;
        int bestTicks = Integer.MAX_VALUE;
        double bestError = Double.MAX_VALUE;
        double maxError = isHighTarget ? MAX_ACCEPTABLE_ERROR * 2.0 : MAX_ACCEPTABLE_ERROR;

        for (float pitch = maxPitch; pitch >= minPitch; pitch -= step) {
            SimulationResult res = simulateWithTicks(yaw, pitch, target);
            if (res != null && res.error <= maxError) {
                if (res.ticks < bestTicks || (res.ticks == bestTicks && res.error < bestError)) {
                    bestTicks = res.ticks;
                    bestPitch = pitch;
                    bestError = res.error;
                }
            }
        }

        if (bestTicks != Integer.MAX_VALUE) {
            return new float[]{yaw, MathHelper.clamp(bestPitch, -90f, 90f)};
        }
        return null;
    }

    private float[] findAnyWorkingRotation(Vector3d target) {
        Vector3d eye = mc.player.getEyePosition(1.0f);
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float baseYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;

        double dist = eye.distanceTo(target);
        double heightDiff = target.y - eye.y;
        boolean isHighTarget = heightDiff > 5.0;

        float maxPitch = isHighTarget ? 89.0f : 85.0f;
        float minPitch = dist > 60 ? -70.0f : -50.0f;
        if (isHighTarget) {
            minPitch = -85.0f;
        }
        float step = dist > 60 ? 1.2f : 1.5f;
        if (isHighTarget) {
            step = 1.0f;
        }
        float yawRange = dist > 60 ? 30.0f : 20.0f;
        if (isHighTarget) {
            yawRange = 40.0f;
        }
        float yawStep = dist > 60 ? 3.0f : 2.5f;
        if (isHighTarget) {
            yawStep = 2.0f;
        }

        double baseMaxError = dist > 60 ? MAX_DISTANCE_FROM_TARGET * 2.0 : MAX_DISTANCE_FROM_TARGET * 1.5;
        if (isHighTarget) {
            baseMaxError = MAX_DISTANCE_FROM_TARGET * 3.0;
        }

        for (float pitch = maxPitch; pitch >= minPitch; pitch -= step) {
            Vector3d landing = simulateTrajectory(baseYaw, pitch);
            if (landing != null && target.distanceTo(landing) <= baseMaxError) {
                return new float[]{baseYaw, MathHelper.clamp(pitch, -90f, 90f)};
            }
        }

        for (float yawOffset = -yawRange; yawOffset <= yawRange; yawOffset += yawStep) {
            if (yawOffset == 0) {
                continue;
            }
            float yaw = baseYaw + yawOffset;
            for (float pitch = maxPitch; pitch >= minPitch; pitch -= step) {
                Vector3d landing = simulateTrajectory(yaw, pitch);
                if (landing != null && target.distanceTo(landing) <= baseMaxError) {
                    return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
                }
            }
        }

        if (isHighTarget) {
            for (float pitch = 89.0f; pitch >= 75.0f; pitch -= 1.5f) {
                for (float yawOffset = -yawRange; yawOffset <= yawRange; yawOffset += yawStep) {
                    float yaw = baseYaw + yawOffset;
                    Vector3d landing = simulateTrajectory(yaw, pitch);
                    if (landing != null && target.distanceTo(landing) <= baseMaxError * 1.5) {
                        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
                    }
                }
            }
        }

        for (float pitch = maxPitch; pitch >= 70.0f; pitch -= 2.0f) {
            for (float yawOffset = -yawRange; yawOffset <= yawRange; yawOffset += yawStep * 2) {
                float yaw = baseYaw + yawOffset;
                Vector3d landing = simulateTrajectory(yaw, pitch);
                double maxError = isHighTarget ? MAX_DISTANCE_FROM_TARGET * 4.0 : MAX_DISTANCE_FROM_TARGET * 2.5;
                if (landing != null && target.distanceTo(landing) <= maxError) {
                    return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
                }
            }
        }

        return null;
    }

    private static class SimulationResult {
        final double error;
        final int ticks;

        SimulationResult(double error, int ticks) {
            this.error = error;
            this.ticks = ticks;
        }
    }

    private SimulationResult simulateWithTicks(float yaw, float pitch, Vector3d target) {
        Vector3d pos = getThrowPos(yaw, pitch);
        Vector3d motion = getThrowMotion(yaw, pitch);

        for (int tick = 0; tick < MAX_SIM_TICKS; tick++) {
            pos = pos.add(motion);
            motion = motion.scale(0.99).subtract(0, 0.03, 0);

            if (pos.y <= 0) {
                return new SimulationResult(snapToBlockCenter(pos).distanceTo(target), tick + 1);
            }

            BlockPos bp = new BlockPos(pos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return new SimulationResult(snapToBlockCenter(pos).distanceTo(target), tick + 1);
            }
        }
        return null;
    }

    private Vector3d simulateTrajectory(float yaw, float pitch) {
        Vector3d pos = getThrowPos(yaw, pitch);
        Vector3d motion = getThrowMotion(yaw, pitch);

        for (int i = 0; i < MAX_SIM_TICKS; i++) {
            pos = pos.add(motion);
            motion = motion.scale(0.99).subtract(0, 0.03, 0);

            if (pos.y <= 0) {
                return snapToBlockCenter(pos);
            }

            BlockPos bp = new BlockPos(pos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return snapToBlockCenter(pos);
            }
        }
        return null;
    }

    private Vector3d getThrowPos(float yaw, float pitch) {
        float yr = (float) Math.toRadians(yaw);
        double x = mc.player.getPosX() - MathHelper.cos(yr) * 0.16;
        double y = mc.player.getPosY() + mc.player.getEyeHeight() - 0.1;
        double z = mc.player.getPosZ() - MathHelper.sin(yr) * 0.16;
        return new Vector3d(x, y, z);
    }

    private Vector3d getThrowMotion(float yaw, float pitch) {
        double v = 1.5;
        float yr = (float) Math.toRadians(yaw);
        float pr = (float) Math.toRadians(pitch);
        double vx = -MathHelper.sin(yr) * MathHelper.cos(pr) * v;
        double vy = -MathHelper.sin(pr) * v;
        double vz = MathHelper.cos(yr) * MathHelper.cos(pr) * v;
        vy += mc.player.getMotion().y;
        return new Vector3d(vx, vy, vz);
    }

    /** Silent rotation: серверный угол без рывка камеры от первого лица. */
    private boolean applyThrowRotation(float yaw, float pitch) {
        float clampedPitch = MathHelper.clamp(pitch, -90f, 90f);
        Rotation target = new Rotation(yaw, clampedPitch);

        RotationComponent.update(
                target,
                ROTATION_SPEED,
                ROTATION_SPEED,
                ROTATION_SPEED,
                ROTATION_SPEED,
                8,
                ROTATION_PRIORITY,
                false
        );

        return new Rotation(mc.player).getDelta(target) <= ROTATION_EPSILON;
    }

    @Override
    public void onDisable() {
        awaitingThrow = false;
        isThrowing = false;
        targetPearl = null;
        cachedLanding = null;
        server = null;
        rotationHoldUntil = 0;
        rememberedTarget = null;
        rememberedTargetTime = 0;
        rotationWaitTicks = 0;
        pearlOwnerById.clear();
        recentThrowers.clear();
        RotationComponent.getInstance().stopRotation();
        super.onDisable();
    }
}
