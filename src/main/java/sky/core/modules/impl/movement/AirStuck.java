package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.network.play.client.CPlayerPacket;
import sky.core.events.EventMotion;
import sky.core.events.EventPacket;
import sky.core.events.EventSwapWorld;
import sky.core.events.EventTravel;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;

public class AirStuck extends Module {

    private final BooleanSetting changeAuraDistance = new BooleanSetting("Изменять дистанцию киллауры", false);
    private final SliderSetting auraDistance = new SliderSetting("Дистанция киллауры", 3.5f, 2.0f, 6.0f, 0.1f, changeAuraDistance::get);
    private final BooleanSetting catchMoment = new BooleanSetting("Ловить момент", true);

    private double peakY = Double.NaN;
    private boolean frozen;

    public AirStuck() {
        super("Air Stuck", "Можно зависнуть в воздухе", Category.Movement);
        addSettings(changeAuraDistance, auraDistance, catchMoment);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (catchMoment.get()) {
            peakY = mc.player != null && !mc.player.isOnGround() ? mc.player.getPosY() : Double.NaN;
            frozen = false;
        } else {
            frozen = true;
        }
    }

    @Override
    public void onDisable() {
        frozen = false;
        super.onDisable();
    }

    public float getKillAuraDistance() {
        if (isEnabled() && changeAuraDistance.get()) {
            return auraDistance.get();
        }
        return -1.0f;
    }

    public boolean isFrozen() {
        return isEnabled() && frozen;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || !catchMoment.get()) {
            return;
        }

        double currentY = mc.player.getPosY();
        if (mc.player.isOnGround()) {
            peakY = Double.NaN;
            frozen = false;
        } else if (!frozen) {
            if (Double.isNaN(peakY)) {
                peakY = currentY;
            } else if (currentY > peakY) {
                peakY = currentY;
            } else if (currentY < peakY) {
                frozen = true;
            }
        }
    }

    @EventTarget
    public void onSwapWorld(EventSwapWorld event) {
        toggle();
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || !frozen) {
            return;
        }
        if (event.getPacket() instanceof CPlayerPacket) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (!frozen) {
            return;
        }
        event.setY(0.0);
        event.setCancelled(true);
    }

    @EventTarget
    public void onTravel(EventTravel event) {
        if (frozen && event.getEntity() == mc.player) {
            event.setCancelled(true);
        }
    }
}
