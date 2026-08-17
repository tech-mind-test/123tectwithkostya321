package mods.particular;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public final class ParticularWaterSplash {
    private final Queue<Float> velocities = new LinkedList<>();

    public void trackVelocity(ClientPlayerEntity player) {
        velocities.offer(Math.abs((float) player.getMotion().y));
        if (velocities.size() > 4) {
            velocities.poll();
        }
    }

    public void trySpawnOnWaterEntry(ClientPlayerEntity player) {
        World world = player.world;
        if (world == null || !world.isRemote) {
            return;
        }

        float baseY = MathHelper.floor(player.getPosY());
        boolean foundSurface = false;
        FluidState prevState = Fluids.EMPTY.getDefaultState();

        for (int i = 0; i < 5; ++i) {
            FluidState nextState = world.getFluidState(player.getPosition().add(0, i, 0));
            if (prevState.getFluid() == Fluids.WATER && nextState.getFluid() == Fluids.EMPTY) {
                baseY += i - 1;
                foundSurface = true;
                break;
            }
            prevState = nextState;
        }

        if (!foundSurface) {
            return;
        }

        float speed = velocities.isEmpty() ? 0f : Collections.max(velocities);
        float surfaceY = baseY + prevState.getActualHeight(world, new BlockPos(player.getPosX(), baseY, player.getPosZ()));

        spawnEmitter(world, player.getPosX(), surfaceY, player.getPosZ(), player.getWidth(), speed);
    }

    public static void spawnEmitter(World world, double x, double y, double z, float width, float speed) {
        IParticleData type = ParticularParticleTypes.WATER_SPLASH_EMITTER;
        world.addParticle(type, true, x, y, z, width, speed, 0.0);
    }
}
