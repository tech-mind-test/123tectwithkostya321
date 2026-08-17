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
import net.minecraft.world.biome.BiomeColors;

public class WaterSplashParticle extends SpriteTexturedParticle {
    protected final IAnimatedSprite sprites;
    protected final float width;
    protected final float height;
    protected final int waterColor;
    protected final float unit;
    protected boolean colored = true;

    protected WaterSplashParticle(ClientWorld world, double x, double y, double z, float width, float height, IAnimatedSprite sprites) {
        super(world, x, y, z);
        this.particleGravity = 0;
        this.maxAge = 18;
        this.width = width;
        this.height = height;
        this.sprites = sprites;
        selectSpriteWithAge(sprites);
        this.waterColor = BiomeColors.getWaterColor(world, new BlockPos(x, y, z));
        this.unit = 2f / 256f;
        this.particleRed = ((waterColor >> 16) & 0xFF) / 255f;
        this.particleGreen = ((waterColor >> 8) & 0xFF) / 255f;
        this.particleBlue = (waterColor & 0xFF) / 255f;
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

        float minU = getMinU() + unit;
        float maxU = getMaxU() - unit;
        float minV = getMinV();
        float maxV = getMaxV();
        int light = getBrightnessForRender(partialTicks);
        int color = colored ? waterColor : 0xFFFFFFFF;
        int a = (int) (particleAlpha * 255) << 24;
        int rgba = (color & 0x00FFFFFF) | a;

        renderSide(buffer, corners, 0, 1, height, minU, maxU, minV, maxV, light, rgba);
        renderSide(buffer, corners, 1, 2, height, minU, maxU, minV, maxV, light, rgba);
        renderSide(buffer, corners, 2, 3, height, minU, maxU, minV, maxV, light, rgba);
        renderSide(buffer, corners, 3, 0, height, minU, maxU, minV, maxV, light, rgba);
    }

    private void renderSide(IVertexBuilder buffer, Vector3f[] corners, int a, int b, float h,
                            float minU, float maxU, float minV, float maxV, int light, int rgba) {
        float r = ((rgba >> 16) & 0xFF) / 255f;
        float g = ((rgba >> 8) & 0xFF) / 255f;
        float bl = (rgba & 0xFF) / 255f;
        float al = ((rgba >> 24) & 0xFF) / 255f;

        buffer.pos(corners[a].getX(), corners[a].getY(), corners[a].getZ()).tex(minU, maxV).color(r, g, bl, al).lightmap(light).endVertex();
        buffer.pos(corners[b].getX(), corners[b].getY(), corners[b].getZ()).tex(maxU, maxV).color(r, g, bl, al).lightmap(light).endVertex();
        buffer.pos(corners[b].getX(), corners[b].getY() + h, corners[b].getZ()).tex(maxU, minV).color(r, g, bl, al).lightmap(light).endVertex();
        buffer.pos(corners[a].getX(), corners[a].getY() + h, corners[a].getZ()).tex(minU, minV).color(r, g, bl, al).lightmap(light).endVertex();

        buffer.pos(corners[b].getX(), corners[b].getY(), corners[b].getZ()).tex(maxU, maxV).color(r, g, bl, al).lightmap(light).endVertex();
        buffer.pos(corners[a].getX(), corners[a].getY(), corners[a].getZ()).tex(minU, maxV).color(r, g, bl, al).lightmap(light).endVertex();
        buffer.pos(corners[a].getX(), corners[a].getY() + h, corners[a].getZ()).tex(minU, minV).color(r, g, bl, al).lightmap(light).endVertex();
        buffer.pos(corners[b].getX(), corners[b].getY() + h, corners[b].getZ()).tex(maxU, minV).color(r, g, bl, al).lightmap(light).endVertex();
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprites;

        public Factory(IAnimatedSprite sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle makeParticle(BasicParticleType type, net.minecraft.world.World world, double x, double y, double z,
                                   double width, double height, double unused) {
            return new WaterSplashParticle((ClientWorld) world, x, y, z, (float) width, (float) height, sprites);
        }
    }
}
