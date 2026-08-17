package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import lombok.Getter;
import net.minecraft.block.Blocks;
import net.minecraft.potion.Effects;
import sky.core.SkyCore;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.utils.player.MoveUtil;


public class Sprint extends Module {
    @Getter
    private final ModeSetting mode = new ModeSetting("Мод", "Обычный", "Обычный", "Пакетный");
    @Getter
    private final BooleanSetting weatetr = new BooleanSetting("Отжимать спринт в воде", false);

    public Sprint() {
        super("Sprint", "Автоматически активирует режим бега, когда игрок начинает двигаться", Category.Movement);
        addSettings(mode, weatetr);
    }

    @EventTarget
    public void onUpdateganodn(EventUpdate eventUpdate) {
        if (mc.player == null || mc.world == null) return;
        if (mode.is("Обычный")) {
            AttackAura aura = SkyCore.getInstance().getModuleManager().attackAura;
            boolean reset = aura.isEnabled() && aura.getTarget() != null && aura.getOnlycrit().get();
            if (reset) {
                if (sprint()) {
                    mc.gameSettings.keyBindSprint.setPressed(true);
                } else {
                    mc.gameSettings.keyBindSprint.setPressed(false);
                    mc.player.setSprinting(false);
                }
            } else {
                mc.gameSettings.keyBindSprint.setPressed(true);
            }
        }
    }


    public boolean sprint() {
        return canSprint();
    }

    public boolean RAGE() {
        return !mc.player.isSneaking() && !mc.player.collidedHorizontally && mc.player.movementInput.moveForward > 0.0F && !mc.player.isCrouching() && !mc.player.isPotionActive(Effects.SLOWNESS) && !mc.player.isPotionActive(Effects.BLINDNESS) && !mc.player.isVisuallySwimming() && !mc.player.isHandActive();
    }

    public boolean canSprint() {
        boolean flag = mc.player.getBlockState().isIn(Blocks.COBWEB) || mc.player.abilities.isFlying || MoveUtil.isInLiquid() || mc.player.isRidingHorse() || (mc.player.fallDistance <= 0.0F && mc.player.isOnGround());
        return flag;
    }

    @Override
    public void onDisable() {
        mc.gameSettings.keyBindSprint.setPressed(false);
    }
}
