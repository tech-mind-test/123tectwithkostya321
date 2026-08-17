package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.SkyCore;
import sky.core.events.EventEntityHitBox;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.impl.movement.ElytraTarget;
import net.minecraft.entity.LivingEntity;

public class HitBox extends Module {
    public static final SliderSetting size = new SliderSetting("Размер", 0.2f, 0F, 1, 0.05F);
    private final BooleanSetting showHitBox = new BooleanSetting("Отображать Размер", true);

    public HitBox() {
        super("HitBox", "Изменяет размер хитбокса", Category.Combat);
        addSettings(size, showHitBox);
    }

    @EventTarget
    public void onUpdate(EventEntityHitBox event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        ElytraTarget elytraTarget = SkyCore.getInstance().getModuleManager().getElytraTarget();
        AttackAura attackAura = SkyCore.getInstance().getModuleManager().getAttackAura();
        if (shouldKeepVanillaHitbox(elytraTarget, attackAura, (LivingEntity) event.getEntity())) {
            return;
        }

        event.setSize(size.get());
    }

    public boolean shouldShowHitBox() {
        return isEnabled() && showHitBox.get();
    }

    private boolean shouldKeepVanillaHitbox(ElytraTarget elytraTarget, AttackAura attackAura, LivingEntity entity) {
        if (elytraTarget == null || attackAura == null || !elytraTarget.isEnabled() || !elytraTarget.isPedikPredict()) {
            return false;
        }
        if (mc.player == null || !mc.player.isElytraFlying() || !entity.isElytraFlying()) {
            return false;
        }
        return entity == attackAura.getTarget();
    }
}