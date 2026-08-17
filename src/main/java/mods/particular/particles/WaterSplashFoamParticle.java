package mods.particular.particles;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;

public class WaterSplashFoamParticle extends WaterSplashParticle {
    public WaterSplashFoamParticle(ClientWorld world, double x, double y, double z, float width, float height, IAnimatedSprite sprites) {
        super(world, x, y, z, width, height, sprites);
        colored = false;
        particleRed = 1f;
        particleGreen = 1f;
        particleBlue = 1f;
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprites;

        public Factory(IAnimatedSprite sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle makeParticle(BasicParticleType type, net.minecraft.world.World world, double x, double y, double z,
                                   double width, double height, double unused) {
            return new WaterSplashFoamParticle((ClientWorld) world, x, y, z, (float) width, (float) height, sprites);
        }
    }
}
