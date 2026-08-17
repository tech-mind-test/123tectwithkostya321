package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.SkyCore;
import sky.core.events.EventDeath;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.impl.combat.AttackAura;


public class AutoRespawn extends Module {
    private final BooleanSetting disableAura = new BooleanSetting("Отключать AttackAura", true);

    public AutoRespawn() {
        super("AutoRespawn", "Автоматически возраждает вас после смерти", Category.Player);
        addSettings(disableAura);
    }

    @Native
    @EventTarget
    public void onUpdate(EventDeath event) {
        if (disableAura.get()) {
            if (SkyCore.getInstance().getModuleManager().getModule(AttackAura.class).isEnabled()) {
                SkyCore.getInstance().getModuleManager().getModule(AttackAura.class).toggle();
            }
        }
        mc.player.respawnPlayer();
        mc.displayGuiScreen(null);
    }
}