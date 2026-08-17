package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.*;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.player.MoveUtil;
import sky.core.utils.render.RenderUtil;
import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

import java.util.ArrayList;

public class Particles extends Module {
    private final ModeSetting type = new ModeSetting("Режим", "Звездочки", "Сердечки", "Снежинки", "Доллары", "Звездочки", "Тыковки", "Бубенцы", "Сияние");
    private final MultiBooleanSetting reason = new MultiBooleanSetting("Добавлять при", new BooleanSetting("Бездействии", true), new BooleanSetting("Беге", false), new BooleanSetting("Крите", false), new BooleanSetting("Падении перла", false), new BooleanSetting("Падении трезубца", false), new BooleanSetting("Падении стрелы", false), new BooleanSetting("Сносе тотема", false));
    private final SliderSetting count = new SliderSetting("Количество", 10, 2, 40, 1, () -> reason.is("Бездействии"));
    private final BooleanSetting glow = new BooleanSetting("Свечение", true);

    private final ArrayList<Particles.Particle> particles = new ArrayList<>();
    private boolean isPlayerTotem = false;

    public Particles() {
        super("Particles", "Добавляет частицы при разных условиях", Category.Visuals);
        addSettings(type, reason, count, glow);
    }

    @EventTarget
    public void onEvent(EventRender3D event) {
        particles.removeIf(particle -> particle.time.hasTimeElapsed(particle.lifeTime));
        if (particles.isEmpty()) return;

        Quaternion rotation = new Quaternion(mc.getRenderManager().info.getRotation());
        rotation.multiply(Vector3f.ZP.rotationDegrees(180f));
        Vector3f right3f = new Vector3f(1f, 0f, 0f), up3f = new Vector3f(0f, 1f, 0f), forward3f = new Vector3f(0f, 0f, 1f);
        right3f.transform(rotation);
        up3f.transform(rotation);
        forward3f.transform(rotation);

        ResourceLocation texture = new ResourceLocation("SkyCore/icons/world_render/" + getTexturePath(type.get()));

        for (Particle particle : particles) {
            particle.update();

            Vector3d toCenter = particle.position.subtract(mc.getRenderManager().info.getProjectedView());

            double halfSize = particle.size * 0.5f;
            AxisAlignedBB aabb = new AxisAlignedBB(particle.position.x - halfSize, particle.position.y - halfSize, particle.position.z - halfSize, particle.position.x + halfSize, particle.position.y + halfSize, particle.position.z + halfSize);
            if (!mc.worldRenderer.getClippinghelper().isBoundingBoxInFrustum(aabb)) continue;

            Vector3d halfRight = new Vector3d(right3f.getX(), right3f.getY(), right3f.getZ()).scale(halfSize);
            Vector3d halfUp = new Vector3d(up3f.getX(), up3f.getY(), up3f.getZ()).scale(halfSize);

            Vector3d p0 = toCenter.subtract(halfRight).subtract(halfUp);
            Vector3d p1 = toCenter.add(halfRight).subtract(halfUp);
            Vector3d p2 = toCenter.add(halfRight).add(halfUp);
            Vector3d p3 = toCenter.subtract(halfRight).add(halfUp);

            int color = (particle.color & 0x00FFFFFF) | ((int) (particle.alpha * 255) << 24);
            float p0x = (float) p0.x, p0y = (float) p0.y, p0z = (float) p0.z;
            float p1x = (float) p1.x, p1y = (float) p1.y, p1z = (float) p1.z;
            float p2x = (float) p2.x, p2y = (float) p2.y, p2z = (float) p2.z;
            float p3x = (float) p3.x, p3y = (float) p3.y, p3z = (float) p3.z;

            RenderUtil.drawImage3DQuad(texture, glow.get(), p0x, p0y, p0z, p1x, p1y, p1z, p2x, p2y, p2z, p3x, p3y, p3z, color);
            if (glow.get())
                RenderUtil.drawImage3DQuad(texture, glow.get(), p0x, p0y, p0z, p1x, p1y, p1z, p2x, p2y, p2z, p3x, p3y, p3z, color);
        }

        RenderUtil.flushImage3DBatch();
    }

    @EventTarget
    public void onEvent(EventMotion event) {
        if (reason.is("Беге") && MoveUtil.isMoving()) {
            double speed = Math.sqrt(mc.player.motion.x * mc.player.motion.x + mc.player.motion.z * mc.player.motion.z);
            Vector3d direction;
            if (speed < 0.01) direction = mc.player.getLookVec().scale(-1);
            else if (mc.player.isElytraFlying()) direction = mc.player.motion.normalize().scale(-1);
            else direction = new Vector3d(-mc.player.motion.x / speed, 0, -mc.player.motion.z / speed);

            double distanceBehind = (mc.player.isElytraFlying() ? 1.2 : 0.5) + (speed > 0.1 ? speed * 1.5 : 0);
            double offsetX = MathUtil.random(-0.35f, 0.35f);
            double offsetZ = MathUtil.random(-0.35f, 0.35f);
            long life = (long) MathUtil.random(1500, 2000);

            double posX = mc.player.getPosX() + direction.x * distanceBehind + offsetX;
            double posY = mc.player.isElytraFlying() ? mc.player.getPosY() + mc.player.getHeight() / 2.0 + direction.y * distanceBehind + MathUtil.random(-0.35f, 0.35f) : mc.player.getPosY() + MathUtil.random(0.2f, mc.player.getHeight() + 0.1f);
            double posZ = mc.player.getPosZ() + direction.z * distanceBehind + offsetZ;

            double halfSize = 0.5f;
            AxisAlignedBB aabb = new AxisAlignedBB(posX - halfSize, posY - halfSize, posZ - halfSize, posX + halfSize, posY + halfSize, posZ + halfSize);
            if (!mc.worldRenderer.getClippinghelper().isBoundingBoxInFrustum(aabb)) return;

            Vector3d velocity = direction.scale(0.075).add(new Vector3d(MathUtil.random(-0.01f, 0.01f), MathUtil.random(-0.05f, 0.01f), MathUtil.random(-0.01f, 0.01f))).scale(0.1f);
            addParticles(posX, posY, posZ, velocity, ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), 0.3f, life, 3, 0.000005f);

        }
        for (Entity entity : mc.world.getAllEntities()) {
            if (reason.is("Падении перла") && entity instanceof EnderPearlEntity pearl) {
                if (!pearl.isOnGround()) {
                    createParticles(pearl.getPositionVec());
                }
            }
            if (reason.is("Падении трезубца") && entity instanceof TridentEntity trident) {
                if (isProjectileFlying(trident)) {
                    createParticles(trident.getPositionVec());
                }
            }
            if (reason.is("Падении стрелы") && entity instanceof ArrowEntity arrow) {
                if (isProjectileFlying(arrow)) {
                    createParticles(arrow.getPositionVec());
                }
            }
        }
    }

    private boolean isProjectileFlying(Entity projectile) {
        if (projectile.isOnGround() || projectile.getMotion().lengthSquared() <= 0.0001) return false;

        Vector3d pos = projectile.getPositionVec();
        Vector3d motion = projectile.getMotion().normalize().scale(0.5);

        BlockPos currentPos = new BlockPos(pos);
        BlockPos frontPos = new BlockPos(pos.add(motion));

        return mc.world.getBlockState(currentPos).isAir() && mc.world.getBlockState(frontPos).isAir();
    }

    private void createParticles(Vector3d position) {
        double halfSize = 0.5f;
        AxisAlignedBB aabb = new AxisAlignedBB(position.x - halfSize, position.y - halfSize, position.z - halfSize, position.x + halfSize, position.y + halfSize, position.z + halfSize);
        if (!mc.worldRenderer.getClippinghelper().isBoundingBoxInFrustum(aabb)) return;

        for (int i = 0; i < 2; i++) {
            double distance = 0f;
            double angle = Math.toRadians(MathUtil.random(0, 360));
            double cosAngle = Math.cos(angle);
            double sinAngle = Math.sin(angle);

            double dx = cosAngle * distance;
            double dz = sinAngle * distance;
            double dy = MathUtil.random(0.1f, 0.35f);

            Vector3d particlePos = new Vector3d(position.x + dx, position.y + dy, position.z + dz);

            long life = (long) MathUtil.random(2000, 2500);
            float speedMin = MathUtil.random(0.015f, 0.0375f);
            float speedMax = MathUtil.random(0.05f, 0.075f);
            double speedFinal = MathUtil.random(speedMin, speedMax);
            double speedFinalY = speedFinal * 0.4;

            double angleVel = Math.toRadians(MathUtil.random(0, 360));
            double cosVel = Math.cos(angleVel);
            double sinVel = Math.sin(angleVel);

            double velX = cosVel * speedFinal;
            double velZ = sinVel * speedFinal;
            double velY = MathUtil.random((float) -speedFinalY, (float) speedFinalY);

            addParticles(particlePos.x, particlePos.y, particlePos.z, new Vector3d(velX, velY, velZ), ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), 0.3f, life, 2, 0.00005f);
        }
    }

    @EventTarget
    public void onEvent(EventUpdate event) {
        if (!reason.is("Бездействии")) return;
        Vector3d base = new Vector3d(mc.player.getPosX(), mc.player.getPosY() + mc.player.getHeight() / 2.0, mc.player.getPosZ());

        particles.ensureCapacity(particles.size() + count.get().intValue());
        for (int i = 0; i < count.get().intValue(); i++) {
            double distance = MathUtil.random(7, 35);
            double angle = Math.toRadians(MathUtil.random(0, 360));
            double height = MathUtil.random(-7, 25);

            Vector3d offset = new Vector3d(Math.cos(angle) * distance, height, Math.sin(angle) * distance);
            Vector3d spawnPos = base.add(offset);

            double halfSize = 0.5f;
            AxisAlignedBB aabb = new AxisAlignedBB(spawnPos.x - halfSize, spawnPos.y - halfSize, spawnPos.z - halfSize, spawnPos.x + halfSize, spawnPos.y + halfSize, spawnPos.z + halfSize);
            if (!mc.worldRenderer.getClippinghelper().isBoundingBoxInFrustum(aabb)) continue;

            Vector3d originalPosition = base.add(offset);

            long life = (long) MathUtil.random(1500, 2000);
            double speed = Math.random() < 0.8 ? MathUtil.random(0.015f, 0.03f) : 0.125f;
            double phi = Math.toRadians(MathUtil.random(0, 360));
            float smooth = 3;

            Vector3d velocity = new Vector3d(Math.cos(phi) * speed, MathUtil.random((float) (-speed * 0.1f), (float) (speed * 0.1f)), Math.sin(phi) * speed);
            addParticles(originalPosition.x, originalPosition.y, originalPosition.z, velocity, ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), 0.4f, life, smooth, 0.000005f);
        }
    }


    @EventTarget
    public void onEvent(EventCriticalHit event) {
        if (!reason.is("Крите") || event.getTarget() == null) return;

        particles.ensureCapacity(particles.size() + 35);
        for (int i = 0; i < 35; i++) {
            double targetX = event.getTarget().getPosX() + MathUtil.random(-0.4f, 0.4f);
            double targetY = event.getTarget().getPosY() + MathUtil.random(-0.5f, event.getTarget().getHeight() + 0.4f);
            double targetZ = event.getTarget().getPosZ() + MathUtil.random(-0.4f, 0.4f);

            double halfSize = 0.5f;
            AxisAlignedBB aabb = new AxisAlignedBB(targetX - halfSize, targetY - halfSize, targetZ - halfSize, targetX + halfSize, targetY + halfSize, targetZ + halfSize);
            if (!mc.worldRenderer.getClippinghelper().isBoundingBoxInFrustum(aabb)) continue;

            float baseMx = MathUtil.random(-0.8f, 0.8f) * 2.0f;
            float baseMy = MathUtil.random(-0.25f, 1.4f);
            float baseMz = MathUtil.random(-0.8f, 0.8f) * 2.0f;

            float smooth = 0.5f;
            long life = (long) MathUtil.random(1000, 1250);

            Vector3d velocity = new Vector3d(baseMx * 0.07f, baseMy * 0.07f, baseMz * 0.07f);
            addParticles(targetX, targetY, targetZ, velocity, ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), 0.3f, life, smooth, 0.0005f);
        }
    }

    @EventTarget
    public void onEvent(EventPopTotem event) {
        if (!reason.is("Сносе тотема")) return;
        isPlayerTotem = (event.getEntity() == mc.player);
    }

    @EventTarget
    public void onEvent(EventTotemParticle event) {
        if (!reason.is("Сносе тотема") || isPlayerTotem) return;

        double x = event.getX();
        double y = event.getY();
        double z = event.getZ();

        double halfSize = 0.5f;
        AxisAlignedBB aabb = new AxisAlignedBB(x - halfSize, y - halfSize, z - halfSize, x + halfSize, y + halfSize, z + halfSize);
        if (!mc.worldRenderer.getClippinghelper().isBoundingBoxInFrustum(aabb)) return;

        int color = Math.random() < 0.7 ? 0xFF00FF00 : 0xFFFFFF00;
        long life = (long) MathUtil.random(1500, 2000);

        Vector3d adjustedVelocity = new Vector3d(event.getXSpeed() * 0.06, event.getYSpeed() * 0.06, event.getZSpeed() * 0.06);
        addParticles(x, y, z, adjustedVelocity, color, 0.3f, life, 0.6f, 0.0002);

        event.setCancelled(true);
    }

    @EventTarget
    public void onEvent(EventSwapWorld event) {
        particles.clear();
    }

    @java.lang.Override
    public void onDisable() {
        particles.clear();
        super.onDisable();
    }

    private static String getTexturePath(String displayName) {
        return switch (displayName) {
            case "Сердечки" -> "heart.png";
            case "Доллары" -> "dollar.png";
            case "Снежинки" -> "snowflake.png";
            case "Тыковки" -> "pumpkin.png";
            case "Бубенцы" -> "firepart2.png";
            case "Сияние" -> "sparkle.png";
            default -> "star.png";
        };
    }

    private void addParticles(double x, double y, double z, Vector3d velocity, int color, float size, long lifeTime, float smooth, double gravity) {
        Vector3d safePos = Particle.checkCollision(x, y, z, size);
        if (safePos != null)
            particles.add(new Particle(safePos.x, safePos.y, safePos.z, velocity, color, size, lifeTime, smooth, gravity));
    }

    @Getter
    public static class Particle {
        Vector3d position, velocity;
        int color;
        float size, alpha = 1.0f, smoothFactor;
        long lifeTime;
        TimeUtil time = new TimeUtil();

        public Particle(double x, double y, double z, Vector3d velocity, int color, float size, long lifeTime, float smooth, double gravity) {
            position = new Vector3d(x, y, z);
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.lifeTime = lifeTime;
            time.reset();
            lastUpdateNs = System.nanoTime();
            smoothFactor = smooth;
            this.gravity = gravity;
        }

        public void update() {
            long now = System.nanoTime();
            double delta = (now - lastUpdateNs) / 1_000_000_000.0;
            lastUpdateNs = now;

            float progress = Math.min(1.0f, (float) time.getTimePassed() / lifeTime);
            double factor = Math.pow(1.0 - progress, smoothFactor), scale = delta * 60;

            double vx = velocity.x, vy = velocity.y, vz = velocity.z;
            double px = position.x, py = position.y, pz = position.z;

            double newX = px + vx * factor * scale;
            if (checkCollision(newX, py, pz, size) == null) {
                vx *= -0.8;
                newX = px;
            }

            double newY = py + vy * factor * scale;
            if (checkCollision(newX, newY, pz, size) == null) {
                vy *= -1.5;
                newY = py;
            }

            double newZ = pz + vz * factor * scale;
            if (checkCollision(newX, newY, newZ, size) == null) {
                vz *= -0.8;
                newZ = pz;
            }

            position = new Vector3d(newX, newY, newZ);
            velocity = new Vector3d(vx * 0.9999, vy * 0.9999 - gravity, vz * 0.9999);
            alpha = 1.0f - progress;
        }

        private static Vector3d checkCollision(double x, double y, double z, float size) {
            double half = size * 0.5;
            int minX = MathHelper.floor(x - half), maxX = MathHelper.floor(x + half), minY = MathHelper.floor(y - half), maxY = MathHelper.floor(y + half), minZ = MathHelper.floor(z - half), maxZ = MathHelper.floor(z + half);
            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int bx = minX; bx <= maxX; bx++)
                for (int by = minY; by <= maxY; by++)
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        BlockState state = mc.world.getBlockState(pos.setPos(bx, by, bz));
                        if (!state.isAir()) return null;
                    }
            return new Vector3d(x, y, z);
        }

        private long lastUpdateNs;
        private final double gravity;
    }
}