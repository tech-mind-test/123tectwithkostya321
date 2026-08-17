package mods.maseffects;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

public final class TotemParticleSpawner {
    private static final float SCALER = 2.5f;

    private TotemParticleSpawner() {
    }

    public static void spawn(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity.world == null) {
            return;
        }

        World world = entity.world;
        if (!world.isRemote) {
            return;
        }

        double x = entity.getPosX();
        double y = entity.getPosY() + entity.getHeight() * 0.5f;
        double z = entity.getPosZ();
        int entityId = entity.getEntityId();

        for (int i = 1; i < 18; i++) {
            world.addParticle(MasEffectParticleTypes.REVIVE, true, x, y, z, SCALER, entityId, 0);
        }
        for (int i = 1; i < 100; i++) {
            world.addParticle(MasEffectParticleTypes.REVIVE_SPARK, true, x, y, z, entityId, 0, 0);
        }
    }
}
