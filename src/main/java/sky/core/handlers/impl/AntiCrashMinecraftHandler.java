package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventPacket;
import net.minecraft.network.play.server.*;

import static sky.core.utils.Wrapper.mc;

public class AntiCrashMinecraftHandler {

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.world == null) return;
        Object p = e.getPacket();
        if (p instanceof SExplosionPacket exp) {
            if (Math.abs(exp.getX()) > 1e9 || Math.abs(exp.getY()) > 1e9 || Math.abs(exp.getZ()) > 1e9
                    || Math.abs(exp.getStrength()) > 1e9) {
                e.setCancelled(true);
            }
        } else if (p instanceof SSpawnParticlePacket part) {
            if (Math.abs(part.getXCoordinate()) > 1e9 || Math.abs(part.getYCoordinate()) > 1e9
                    || Math.abs(part.getZCoordinate()) > 1e9 || Math.abs(part.getXOffset()) > 1e9
                    || Math.abs(part.getYOffset()) > 1e9 || Math.abs(part.getZOffset()) > 1e9
                    || Math.abs(part.getParticleSpeed()) > 1e9) {

                e.setCancelled(true);
            }
        } else if (p instanceof SPlayerPositionLookPacket pos) {
            if (Math.abs(pos.getX()) > 1e9 || Math.abs(pos.getY()) > 1e9 || Math.abs(pos.getZ()) > 1e9
                    || Math.abs(pos.getYaw()) > 1e9 || Math.abs(pos.getPitch()) > 1e9) {
                e.setCancelled(true);
            }
        } else if (p instanceof SChangeGameStatePacket change) {
            int id = change.func_241776_b_().field_241778_b_; // 1
            if (id == 5) { // 1
                e.setCancelled(true);
            }
        } else if (p instanceof SEntityTeleportPacket entity) {
            if (Math.abs(entity.getX()) > 1e6 || Math.abs(entity.getY()) > 1e6 || Math.abs(entity.getZ()) > 1e6) {
                e.setCancelled(true);
            }
        }
    }
} // 1
