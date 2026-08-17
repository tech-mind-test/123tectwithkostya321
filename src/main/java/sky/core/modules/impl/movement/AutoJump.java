package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.SkyCore;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.impl.combat.AttackAura;
import net.minecraft.potion.Effects;


public class AutoJump extends Module {
    public static MultiBooleanSetting jump = new MultiBooleanSetting("Прыгать если", new BooleanSetting("Активна Attack Aura", false), new BooleanSetting("Активно зелье замедления", true));

    public AutoJump() {
        super("Auto Jump", "Автоматически прыгает", Category.Movement);
        addSettings(jump);
    }

    @Native
    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!mc.player.isOnGround()) {
            return;
        }

        boolean shouldJump = false;

        if (jump.is("Активна Attack Aura")) {
            AttackAura aura = SkyCore.getInstance().getModuleManager().attackAura;
            if (aura.isEnabled() && aura.getTarget() != null) {
                shouldJump = true;
            }
        }

        if (jump.is("Активно зелье замедления")) {
            if (mc.player.isPotionActive(Effects.SLOWNESS)) {
                shouldJump = true;
            }
        }

        if (shouldJump) {
            mc.player.jump();
        }
    }
}
