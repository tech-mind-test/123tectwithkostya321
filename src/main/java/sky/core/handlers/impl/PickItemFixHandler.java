package sky.core.handlers.impl;

import com.darkmagician6.eventapi.EventTarget;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import lombok.Getter;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.server.SHeldItemChangePacket;
import net.minecraft.util.math.MathHelper;

import static sky.core.utils.Wrapper.mc;

public class PickItemFixHandler {
    @Getter
    public static boolean fixActive = false;
    public static int tickCounter = 0;

    public static void activateFix() {
        fixActive = true;
        tickCounter = 0;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (fixActive) {
            ++tickCounter;
            if (tickCounter >= 6) {
                fixActive = false;
                tickCounter = 0;
            }
        }
    }

    @EventTarget
    public void onPacketEvent(EventPacket e) {
        if (fixActive && mc.player != null && mc.world != null) {
            if ((e.getPacket() instanceof SHeldItemChangePacket fix) && e.isReceive()) {
                if (fix.getHeldItemHotbarIndex() != mc.player.inventory.currentItem) {
                    int newSlot = MathHelper.clamp(mc.player.inventory.currentItem >= 8 ? mc.player.inventory.currentItem - 1 : mc.player.inventory.currentItem + 1, 0, 8);
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(newSlot));
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                    e.setCancelled(true);
                }
            }
        }
    }
}
