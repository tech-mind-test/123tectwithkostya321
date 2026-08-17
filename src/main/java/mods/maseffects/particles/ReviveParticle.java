package mods.maseffects.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import mods.maseffects.MasEffectMath;

public class ReviveParticle extends SpriteTexturedParticle {
    private static final float EFFECT_OPACITY = 1.0f;

    private final IAnimatedSprite sprites;
    private final float scaler;
    private final float rotX;
    private final float rotZ;
    private float rotY;
    private LivingEntity target;
    private Quaternion rotation = new Quaternion(0f, -0.7f, 0.7f, 0f);

    private ReviveParticle(ClientWorld world, double x, double y, double z, float scaler, int entityId, IAnimatedSprite sprites) {
        super(world, x, y, z);
        this.maxAge = 20;
        this.particleAlpha = 0f;
        this.particleScale = 0.2f;
        this.scaler = scaler;
        this.particleGravity = 0f;
        this.rotX = this.rand.nextInt(360);
        this.rotY = this.rand.nextInt(360);
        this.rotZ = this.rand.nextInt(360);
        this.sprites = sprites;

        Entity entity = world.getEntityByID(entityId);
        if (entity instanceof LivingEntity living) {
            this.target = living;
        }

        if (this.rand.nextBoolean()) {
            this.setColor(1f, 1f, 0f);
        } else {
            this.setColor(0f, 1f, 0f);
        }
        this.selectSpriteWithAge(sprites);
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

        this.selectSpriteWithAge(sprites);

        if (target != null) {
            this.setPosition(target.getPosX(), target.getPosY() + target.getHeight() * 0.5f, target.getPosZ());
        }

        this.rotY += 20f;
        this.rotation = MasEffectMath.euler(0f, 0f, this.rotY);
        Quaternion offset = MasEffectMath.euler(this.rotZ, this.rotX, -this.rotZ);
        offset.multiply(this.rotation);
        this.rotation = offset;

        this.particleScale = (this.particleAlpha / EFFECT_OPACITY) * this.scaler;
        float progress = (float) this.age / this.maxAge;
        this.particleAlpha = MathHelper.clamp(
                (float) (Math.sqrt(Math.sin(progress * Math.PI)) / 1.2f),
                0f, 1f) * EFFECT_OPACITY;
    }

    @Override
    public void renderParticle(IVertexBuilder buffer, ActiveRenderInfo camera, float partialTicks) {
        Vector3d cam = camera.getProjectedView();
        float px = (float) (MathHelper.lerp(partialTicks, this.prevPosX, this.posX) - cam.x);
        float py = (float) (MathHelper.lerp(partialTicks, this.prevPosY, this.posY) - cam.y);
        float pz = (float) (MathHelper.lerp(partialTicks, this.prevPosZ, this.posZ) - cam.z);

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-1f, -1f, 0f),
                new Vector3f(-1f, 1f, 0f),
                new Vector3f(1f, 1f, 0f),
                new Vector3f(1f, -1f, 0f)
        };

        float scale = this.getScale(partialTicks);
        for (Vector3f corner : corners) {
            corner.transform(rotation);
            corner.mul(scale);
            corner.add(px, py, pz);
        }

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();
        int light = this.getBrightnessForRender(partialTicks);

        buffer.pos(corners[0].getX(), corners[0].getY(), corners[0].getZ()).tex(maxU, maxV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(light).endVertex();
        buffer.pos(corners[1].getX(), corners[1].getY(), corners[1].getZ()).tex(maxU, minV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(light).endVertex();
        buffer.pos(corners[2].getX(), corners[2].getY(), corners[2].getZ()).tex(minU, minV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(light).endVertex();
        buffer.pos(corners[3].getX(), corners[3].getY(), corners[3].getZ()).tex(minU, maxV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(light).endVertex();

        buffer.pos(corners[3].getX(), corners[3].getY(), corners[3].getZ()).tex(minU, maxV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(light).endVertex();
        buffer.pos(corners[2].getX(), corners[2].getY(), corners[2].getZ()).tex(minU, minV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(light).endVertex();
        buffer.pos(corners[1].getX(), corners[1].getY(), corners[1].getZ()).tex(maxU, minV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(light).endVertex();
        buffer.pos(corners[0].getX(), corners[0].getY(), corners[0].getZ()).tex(maxU, maxV)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(light).endVertex();
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprites;

        public Factory(IAnimatedSprite sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle makeParticle(BasicParticleType type, net.minecraft.world.World world, double x, double y, double z,
                                   double scaler, double entityId, double unused) {
            return new ReviveParticle((ClientWorld) world, x, y, z, (float) scaler, (int) entityId, sprites);
        }
    }
}
