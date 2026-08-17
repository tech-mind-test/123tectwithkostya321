package mods.particular.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class WaterSplashRingParticle extends SpriteTexturedParticle {
    private final IAnimatedSprite sprites;
    private final float width;

    public WaterSplashRingParticle(ClientWorld world, double x, double y, double z, float width, IAnimatedSprite sprites) {
        super(world, x, y, z);
        this.particleGravity = 0;
        this.maxAge = 18;
        this.width = width;
        this.sprites = sprites;
        selectSpriteWithAge(sprites);
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        selectSpriteWithAge(sprites);
        if (world.getFluidState(new BlockPos(posX, posY, posZ)).getFluid() != Fluids.WATER) {
            setExpired();
        }
    }

    @Override
    public void renderParticle(IVertexBuilder buffer, ActiveRenderInfo camera, float partialTicks) {
        Vector3d cam = camera.getProjectedView();
        float px = (float) (MathHelper.lerp(partialTicks, prevPosX, posX) - cam.x);
        float py = (float) (MathHelper.lerp(partialTicks, prevPosY, posY) - cam.y);
        float pz = (float) (MathHelper.lerp(partialTicks, prevPosZ, posZ) - cam.z);

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-1.0F, 0.0F, -1.0F),
                new Vector3f(-1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, -1.0F)
        };

        float ageDelta = MathHelper.lerp(partialTicks, age - 1, age);
        float progress = ageDelta / maxAge;
        float scale = width * (0.8f + 0.2f * progress);

        for (Vector3f corner : corners) {
            corner.mul(scale);
            corner.add(px, py, pz);
        }

        int light = getBrightnessForRender(partialTicks);
        float minU = getMinU();
        float maxU = getMaxU();
        float minV = getMinV();
        float maxV = getMaxV();

        buffer.pos(corners[0].getX(), corners[0].getY(), corners[0].getZ()).tex(maxU, maxV).color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(light).endVertex();
        buffer.pos(corners[1].getX(), corners[1].getY(), corners[1].getZ()).tex(maxU, minV).color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(light).endVertex();
        buffer.pos(corners[2].getX(), corners[2].getY(), corners[2].getZ()).tex(minU, minV).color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(light).endVertex();
        buffer.pos(corners[3].getX(), corners[3].getY(), corners[3].getZ()).tex(minU, maxV).color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(light).endVertex();
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprites;

        public Factory(IAnimatedSprite sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle makeParticle(BasicParticleType type, net.minecraft.world.World world, double x, double y, double z,
                                   double width, double unused1, double unused2) {
            return new WaterSplashRingParticle((ClientWorld) world, x, y, z, (float) width, sprites);
        }
    }
}
