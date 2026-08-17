package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.item.ItemEntity;
import sky.core.events.EventSwapWorld;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import mods.particular.ParticularWaterSplash;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class ParticularWater extends Module {
    private final ParticularWaterSplash splash = new ParticularWaterSplash();
    private boolean wasInWater;
    private final Map<UUID, Boolean> itemWasInWater = new HashMap<>();
    private final Map<UUID, Queue<Float>> itemVelocities = new HashMap<>();

    public ParticularWater() {
        super("Particular", "Брызги воды при прыжке", Category.Visuals);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        wasInWater = false;
        itemWasInWater.clear();
        itemVelocities.clear();
    }

    @EventTarget
    public void onSwapWorld(EventSwapWorld event) {
        wasInWater = false;
        itemWasInWater.clear();
        itemVelocities.clear();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        ClientPlayerEntity player = mc.player;
        splash.trackVelocity(player);

        boolean inWater = player.isInWater();
        if (inWater && !wasInWater) {
            splash.trySpawnOnWaterEntry(player);
        }
        wasInWater = inWater;

        for (ItemEntity item : mc.world.getEntitiesWithinAABB(ItemEntity.class, mc.player.getBoundingBox().grow(64))) {
            UUID uuid = item.getUniqueID();
            boolean currentlyInWater = item.isInWater();
            boolean wasIn = itemWasInWater.getOrDefault(uuid, false);

            Queue<Float> velocities = itemVelocities.computeIfAbsent(uuid, k -> new LinkedList<>());
            velocities.offer(Math.abs((float) item.getMotion().y));
            if (velocities.size() > 4) {
                velocities.poll();
            }

            if (currentlyInWater && !wasIn) {
                float speed = velocities.isEmpty() ? 0f : Collections.max(velocities);
                ParticularWaterSplash.spawnEmitter(item.world, item.getPosX(), item.getPosY(), item.getPosZ(), item.getWidth() * 2f, speed);
            }

            itemWasInWater.put(uuid, currentlyInWater);
        }
    }
}
