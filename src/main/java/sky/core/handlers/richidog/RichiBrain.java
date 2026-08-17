package sky.core.handlers.richidog;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.events.EventUpdate;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.utils.animations.advanced.InfinityAnimation;
import sky.core.utils.timer.Timer;

import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class RichiBrain {
    static final Minecraft mc = Minecraft.getInstance();

    Vector3d pos;
    Vector3d motion = Vector3d.ZERO;
    float direction = random(0, 360);
    float yaw;
    float body;
    int speed = 50;

    final InfinityAnimation x = new InfinityAnimation();
    final InfinityAnimation y = new InfinityAnimation();
    final InfinityAnimation z = new InfinityAnimation();

    final InfinityAnimation bodyAnim = new InfinityAnimation();
    final InfinityAnimation yawAnim = new InfinityAnimation();
    final InfinityAnimation pitchAnim = new InfinityAnimation();

    boolean lay;
    final Timer staying = new Timer();

    public float prevLimbSwingAmount;
    public float limbSwingAmount;
    public float limbSwing;

    @Setter
    private PlayerEntity entity;

    public boolean isLay() {
        return lay;
    }

    public void reset() {
        pos = null;
        motion = Vector3d.ZERO;
        direction = random(0, 360);
        yaw = 0.0F;
        body = 0.0F;
        lay = false;
        prevLimbSwingAmount = 0.0F;
        limbSwingAmount = 0.0F;
        limbSwing = 0.0F;
    }

    public void onUpdate(EventUpdate event) {
        if (entity == null || mc.world == null) return;

        Vector3d playerPos = entity.getPositionVec();

        if (pos == null || pos.distanceTo(playerPos) > 10) {
            pos = playerPos;
            x.animate((float) pos.x, 1);
            y.animate((float) pos.y, 1);
            z.animate((float) pos.z, 1);
        }

        motion = motion.add(0, -0.2f, 0);

        Vector3d newPos = pos.add(motion);

        if (isBlockSolid(newPos.x, newPos.y, newPos.z)) {
            int blockY = (int) newPos.y;
            double correctedY = blockY + 1 + 0.1;
            newPos = new Vector3d(newPos.x, correctedY, newPos.z);
            motion = new Vector3d(motion.x, 0, motion.z);
        }

        motion = new Vector3d(motion.x, 0, motion.z);

        LivingEntity target = AttackAura.target;
        if (target != null && entity instanceof ClientPlayerEntity) {
            if (isBlockSolid(newPos.x, newPos.y - 0.1f, newPos.z)) {
                motion = new Vector3d(motion.x, 0.62f, motion.z);
            }

            AxisAlignedBB box = new AxisAlignedBB(
                    getPos().subtract(0.4, 0, 0.4),
                    getPos().add(0.4, 0.4, 0.4));
            AxisAlignedBB targetbox = target.getBoundingBox().grow(-0.1f, 0, -0.1f);

            motion = motion.add(target.getPositionVec().subtract(newPos).normalize());

            if (box.maxX > targetbox.minX
                    && box.maxY > targetbox.minY
                    && box.maxZ > targetbox.minZ
                    && box.minX < targetbox.maxX
                    && box.minY < targetbox.maxY
                    && box.minZ < targetbox.maxZ) {
                motion = motion.mul(-1, 1, -1);
            }
        } else {
            if (newPos.distanceTo(playerPos) > 2) {
                motion = motion.add(playerPos.subtract(newPos).normalize());
            }
        }

        handleRotation();

        pos = newPos;

        if (pos.distanceTo(playerPos) < 0.1f) {
            direction = random(0, 360);
            double xMot = -Math.sin(Math.toRadians(direction)) * 0.1;
            double zMot = Math.cos(Math.toRadians(direction)) * 0.1;
            motion = motion.add(xMot, 0, zMot);
        }

        motion = motion.scale(0.5);

        speed = 150;
        x.animate((float) pos.x, speed);
        y.animate((float) pos.y, speed);
        z.animate((float) pos.z, speed);

        limbTick();

        if (Math.abs(pos.x - x.get()) > 0.1f || Math.abs(pos.z - z.get()) > 0.1f) {
            staying.reset();
        }

        lay = staying.getElapsedTime() >= 1000;
    }

    private static boolean isBlockSolid(double x, double y, double z) {
        if (mc.world == null) return false;
        BlockPos bp = new BlockPos(x, y, z);
        return !mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty();
    }

    private void handleRotation() {
        if (motion.x != 0 || motion.z != 0) {
            double angle = Math.atan2(motion.z, motion.x);
            yaw = (float) Math.toDegrees(angle) - 90;
            yaw %= 360;
            if (yaw < 0) yaw += 360;
        }

        Vector3d eye = entity.getPositionVec().add(0, entity.getEyeHeight(), 0);
        Vector2f rotation = rotationToward(getPos(), eye);

        LivingEntity target = AttackAura.target;
        if (target != null && entity instanceof ClientPlayerEntity) {
            rotation = rotationToward(getPos(), target.getPositionVec());
        }

        float gradus = lay ? 200 : 150;
        float gradus1 = lay ? 100 : 50;
        if (rotation.x - yaw < -gradus || rotation.x - yaw > gradus) {
            yaw = rotation.x;
        }

        float shortestYawPath = (float) (((((yaw - body) % 360) + 540) % 360) - 180);

        if (!lay) {
            bodyAnim.animate(body + shortestYawPath, 150);
        }
        yawAnim.animate(MathHelper.clamp(rotation.x - yaw, -gradus1, gradus1), 150);
        pitchAnim.animate(rotation.y, 150);

        body += shortestYawPath;
    }

    private static Vector2f rotationToward(Vector3d from, Vector3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double h = MathHelper.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * (180 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, h) * (180 / Math.PI)));
        return new Vector2f(yaw, pitch);
    }

    public void limbTick() {
        prevLimbSwingAmount = limbSwingAmount;
        double d0 = x.get() - pos.x;
        double d1 = 0.0D;
        double d2 = z.get() - pos.z;
        float f = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2) * 4.0F;

        if (f > 1.0F) {
            f = 1.0F;
        }

        limbSwingAmount += (f - limbSwingAmount) * 0.4F;
        limbSwing += limbSwingAmount;
    }

    public float getBody() {
        return bodyAnim.get();
    }

    public float getYaw() {
        return yawAnim.get();
    }

    public float getPitch() {
        return pitchAnim.get();
    }

    public Vector3d getPos() {
        return new Vector3d(x.get(), y.get(), z.get());
    }

    private static float random(float min, float max) {
        return (float) (ThreadLocalRandom.current().nextDouble() * (max - min) + min);
    }
}
