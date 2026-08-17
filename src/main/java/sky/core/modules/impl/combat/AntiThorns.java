package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import sky.core.events.EventPacket;
import sky.core.events.EventThornsDamage;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;

public class AntiThorns extends Module {
    private static final int THORNS_VELOCITY_SUPPRESS_TICKS = 8;
    private static final int THORNS_ENTITY_STATUS_OPCODE = 33;

    private static AntiThorns instance;

    private int suppressVelocityTicks;

    public AntiThorns() {
        super("AntiThorns", "Убирает урон и откидывание от шипов", Category.Combat);
        instance = this;
    }

    public static AntiThorns getInstance() {
        return instance;
    }

    @EventTarget
    public void onThornsDamage(EventThornsDamage event) {
        if (shouldCancelThornsDamage(event.getAttacker())) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (!event.isReceive() || mc.player == null || mc.world == null) {
            return;
        }

        if (event.getPacket() instanceof SEntityStatusPacket packet) {
            if (packet.getOpCode() == THORNS_ENTITY_STATUS_OPCODE
                    && packet.getEntity(mc.world) == mc.player
                    && isLocalPlayerElytraFlying()) {
                armVelocitySuppress();
            }
            return;
        }

        if (event.getPacket() instanceof SEntityVelocityPacket packet
                && shouldCancelVelocityPacket(packet)) {
            event.setCancelled(true);
            resetVelocitySuppress();
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        tickSuppressTimer();
    }

    public void tickSuppressTimer() {
        if (mc.player == null || !isLocalPlayerElytraFlying()) {
            resetVelocitySuppress();
            return;
        }

        if (suppressVelocityTicks > 0) {
            --suppressVelocityTicks;
        }
    }

    public boolean shouldCancelThornsDamage(Entity attacker) {
        if (!isEnabled() || mc.player == null || attacker != mc.player) {
            return false;
        }

        if (isLocalPlayerElytraFlying()) {
            armVelocitySuppress();
        }

        return true;
    }

    public boolean shouldCancelVelocityPacket(SEntityVelocityPacket packet) {
        return isVelocitySuppressActive()
                && packet != null
                && mc.player != null
                && packet.getEntityID() == mc.player.getEntityId();
    }

    public boolean isVelocitySuppressActive() {
        return suppressVelocityTicks > 0 && isLocalPlayerElytraFlying();
    }

    public int getSuppressVelocityTicks() {
        return suppressVelocityTicks;
    }

    public void armVelocitySuppress() {
        suppressVelocityTicks = THORNS_VELOCITY_SUPPRESS_TICKS;
    }

    public void resetVelocitySuppress() {
        suppressVelocityTicks = 0;
    }

    @Override
    public void onEnable() {
        resetVelocitySuppress();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetVelocitySuppress();
        super.onDisable();
    }

    private boolean isLocalPlayerElytraFlying() {
        LivingEntity player = mc.player;
        return player != null && player.isElytraFlying();
    }
}
