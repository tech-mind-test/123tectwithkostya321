package mods.particular.particles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.MetaParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import mods.particular.ParticularParticleTypes;

public class WaterSplashEmitterParticle extends MetaParticle {
    private final float speed;
    private final float width;
    private final float height;

    private WaterSplashEmitterParticle(ClientWorld world, double x, double y, double z, float width, float fallSpeed) {
        super(world, x, y, z);
        this.particleGravity = 0;
        this.maxAge = 24;
        this.speed = Math.min(2f, fallSpeed);
        this.width = width;
        this.height = speed / 2f + width / 3f;

        world.addParticle(ParticularParticleTypes.WATER_SPLASH, true, x, y, z, width, this.height, 0);
        world.addParticle(ParticularParticleTypes.WATER_SPLASH_FOAM, true, x, y, z, width, this.height, 0);
        world.addParticle(ParticularParticleTypes.WATER_SPLASH_RING, true, x, y, z, width, 0, 0);

        if (speed > 0.5f) {
            splash(world, width, (1.5f / 8f + speed / 8f) + (width / 6f), 0.15f);
        } else {
            setExpired();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (age == 8) {
            world.addParticle(ParticularParticleTypes.WATER_SPLASH, true, posX, posY, posZ, width * 0.66f, height * 2f, 0);
            world.addParticle(ParticularParticleTypes.WATER_SPLASH_FOAM, true, posX, posY, posZ, width * 0.66f, height * 2f, 0);
            world.addParticle(ParticularParticleTypes.WATER_SPLASH_RING, true, posX, posY, posZ, width * 0.66f, 0, 0);
            splash((ClientWorld) world, width * 0.66f, (3f / 8f + speed / 8f) + (width / 6f), 0.05f);
        }

        if (world.getFluidState(new BlockPos(posX, posY, posZ)).getFluid() != Fluids.WATER) {
            setExpired();
        }
    }

    private void splash(ClientWorld world, float splashWidth, float splashSpeed, float spread) {
        for (int i = 0; i < splashWidth * 20f; ++i) {
            double xVel = triangular(spread);
            double yVel = splashSpeed * triangular(1.0, 0.25);
            double zVel = triangular(spread);
            double px = posX + (spread != 0 ? xVel / spread * splashWidth : 0);
            double py = posY + 1 / 16f;
            double pz = posZ + (spread != 0 ? zVel / spread * splashWidth : 0);
            Particle droplet = Minecraft.getInstance().particles.addParticle(
                    ParticleTypes.FALLING_WATER, px, py, pz, xVel, yVel, zVel);
            if (droplet instanceof SpriteTexturedParticle) {
                ((SpriteTexturedParticle) droplet).multiplyParticleScaleBy(0.125f);
            }
        }
    }

    private double triangular(double spread) {
        return (rand.nextDouble() + rand.nextDouble() - 1.0) * spread;
    }

    private double triangular(double mean, double deviation) {
        return mean + (rand.nextDouble() + rand.nextDouble() - 1.0) * deviation;
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {
        public Factory(net.minecraft.client.particle.IAnimatedSprite sprites) {
        }

        @Override
        public Particle makeParticle(BasicParticleType type, net.minecraft.world.World world, double x, double y, double z,
                                   double width, double speed, double unused) {
            return new WaterSplashEmitterParticle((ClientWorld) world, x, y, z, (float) width, (float) speed);
        }
    }
}
