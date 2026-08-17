package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.item.UseAction;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import sky.core.events.EventNoSlow;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;


public class NoSlow extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "ReallyWorld", "ReallyWorld", "Spooky", "BravoHvH");
    private final SliderSetting vanillaSpeed = new SliderSetting("Скорость", 0.6F, 0.1F, 1F, 0.05F, () -> mode.is("BravoHvH"));
    public NoSlow() {
        super("No Slow", "Убирает замедление при использовании предмета", Category.Movement);
        addSettings(mode, vanillaSpeed);
    }

    public static int ticks = 0;
    private static int cycleCounter = 0;

    @EventTarget
    public void onNoSlow(EventNoSlow event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isElytraFlying()) return;
        if (!mc.player.isHandActive()) return;
        switch (mode.get()) {
            case "BravoHvH" -> handleBravo(event);
            case "ReallyWorld" -> handleReallyWorld(event);
            case "Spooky" -> handleSpooky(event);
            default -> {
            }
        }
    }

    @EventTarget
    public void onEvent(EventUpdate event) {
        if ((mode.is("ReallyWorld") || mode.is("Spooky")) && mc.player != null && !mc.player.isElytraFlying()) {
            if (mc.player.isHandActive()) {
                ticks++;
            } else {
                ticks = 0;
                cycleCounter = 0;
            }
        }
    }

    private void handleReallyWorld(EventNoSlow eventNoSlow) {
        int[] thresholds = {2, 3, 3};
        int threshold = thresholds[cycleCounter % 3];
        if (ticks >= threshold) {
            eventNoSlow.setCancelled(true);
            ticks = 0;
            cycleCounter++;
        }
    }

    private void handleSpooky(EventNoSlow eventNoSlow) {
        int[] thresholds = {2, 2};
        int threshold = thresholds[cycleCounter % 2];
        if (ticks >= threshold) {
            eventNoSlow.setCancelled(true);
            ticks = 0;
            cycleCounter++;
        }
    }

    private void handleBravo(EventNoSlow eventNoSlow) {
        eventNoSlow.setCancelled(true);
        double vx = mc.player.getMotion().x * vanillaSpeed.get();
        double vz = mc.player.getMotion().z * vanillaSpeed.get();
        mc.player.setMotion(vx, mc.player.getMotion().y, vz);
    }

    private void handleGrimOld(EventNoSlow eventNoSlow) {
        boolean isBlockingOrEatingOffhand =
                (mc.player.getHeldItemOffhand().getUseAction() == UseAction.BLOCK
                        || mc.player.getHeldItemOffhand().getUseAction() == UseAction.EAT
                        || mc.player.getHeldItemOffhand().getUseAction() == UseAction.DRINK)
                        && mc.player.getActiveHand() == Hand.MAIN_HAND;

        if (isBlockingOrEatingOffhand) return;

        if (mc.player.getActiveHand() == Hand.MAIN_HAND) {
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
            eventNoSlow.setCancelled(true);
            return;
        }

        eventNoSlow.setCancelled(true);
        sendItemChangePacket();
    }

    private void sendItemChangePacket() {

        boolean isMoving = mc.player.moveForward != 0.0F || mc.player.moveStrafing != 0.0F;
        if (!isMoving) return;

        int nextSlot = (mc.player.inventory.currentItem % 8) + 1;
        mc.player.connection.sendPacket(new CHeldItemChangePacket(nextSlot));
        mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
    }
}