package mods.maseffects.particles;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public class ReviveSparkParticle extends SpriteTexturedParticle {
    private static final float EFFECT_OPACITY = 1.0f;

    private LivingEntity target;
    private Vector3d targetDir = Vector3d.ZERO;

    private ReviveSparkParticle(ClientWorld world, double x, double y, double z, int entityId, IAnimatedSprite sprites) {
        super(world, x, y, z);
        this.maxAge = this.rand.nextInt(19) + 30;
        this.particleAlpha = EFFECT_OPACITY;
        this.particleScale = 0.1f;
        this.particleGravity = 0f;
        this.motionX = (this.rand.nextInt(21) - 10) / 25.0;
        this.motionY = (this.rand.nextInt(21) - 10) / 25.0;
        this.motionZ = (this.rand.nextInt(21) - 10) / 25.0;
        this.canCollide = true;
        this.selectSpriteRandomly(sprites);

        Entity entity = world.getEntityByID(entityId);
        if (entity instanceof LivingEntity living) {
            this.target = living;
        }

        if (this.rand.nextBoolean()) {
            this.setColor(0f, 1f, 0f);
        } else {
            this.setColor(1f, 1f, 0f);
        }
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.age++ >= this.maxAge) {
            this.setExpired();
            return;
        }

        if (target != null) {
            double ty = target.getPosY() + target.getHeight() * 0.5f;
            targetDir = new Vector3d(target.getPosX() - this.posX, ty - this.posY, target.getPosZ() - this.posZ);
            if (targetDir.lengthSquared() > 1.0E-6) {
                targetDir = targetDir.normalize();
            }
        }

        if (this.age < this.maxAge / 5) {
            this.move(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.98;
            this.motionY *= 0.98;
            this.motionZ *= 0.98;
        } else if (this.age >= this.maxAge / 4 && target != null) {
            Vector3d pos = new Vector3d(this.posX, this.posY, this.posZ);
            if (target.getPositionVec().distanceTo(pos) < 20.0) {
                this.particleAlpha = MathHelper.clamp(
                        1f - ((this.age - (this.maxAge / 1.5f)) / 20f),
                        0f, 1f) * EFFECT_OPACITY;

                float pull = (this.age - 15) * 0.05f;
                this.motionX = targetDir.x * pull;
                this.motionY = targetDir.y * pull;
                this.motionZ = targetDir.z * pull;
                this.move(this.motionX, this.motionY, this.motionZ);

                if (target.getPositionVec().distanceTo(pos) < 2.0) {
                    this.particleAlpha = 0f;
                    this.setExpired();
                }
            }
        }
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprites;

        public Factory(IAnimatedSprite sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle makeParticle(BasicParticleType type, net.minecraft.world.World world, double x, double y, double z,
                                   double entityId, double unused1, double unused2) {
            return new ReviveSparkParticle((ClientWorld) world, x, y, z, (int) entityId, sprites);
        }
    }
}
