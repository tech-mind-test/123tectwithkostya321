package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.SkyCore;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.modules.impl.movement.GrimGlide;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class AutoEat extends Module {
    private final BooleanSetting noUseWithAura = new BooleanSetting("Не использовать с AttackAura", false);
    private final SliderSetting hungerThreshold = new SliderSetting("Кол-во голода", 18, 1, 20, 0.5f);
    private boolean isEating = false;

    public AutoEat() {
        super("AutoEat", "Автоматически ест еду из рук для восстановления голода", Category.Player);
        addSettings(noUseWithAura, hungerThreshold);
    }

    @Native
    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen))) {
            stopEating();
            return;
        }

        if (GrimGlide.isGlideFlyActive()) {
            stopEating();
            return;
        }

        if (noUseWithAura.get()) {
            Module attackAura = SkyCore.getInstance().getModuleManager().getModule(AttackAura.class);
            if (attackAura != null && attackAura.isEnabled()) {
                stopEating();
                return;
            }
        }

        ItemStack offHandStack = mc.player.getHeldItemOffhand();
        ItemStack mainHandStack = mc.player.getHeldItemMainhand();
        boolean offHandFood = isEdibleFood(offHandStack);
        boolean mainHandFood = isEdibleFood(mainHandStack);

        if ((offHandFood || mainHandFood) && mc.player.getFoodStats().getFoodLevel() <= hungerThreshold.get().intValue()) {
            Hand handToUse = offHandFood ? Hand.OFF_HAND : Hand.MAIN_HAND;
            isEating = true;
            if (!mc.gameSettings.keyBindUseItem.isKeyDown()) {
                mc.playerController.processRightClick(mc.player, mc.world, handToUse);
                mc.gameSettings.keyBindUseItem.setPressed(true);
            }
        } else if (isEating) {
            stopEating();
        }
    }

    private boolean isEdibleFood(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().isFood();
    }

    private void stopEating() {
        if (isEating) {
            if (mc.player.isHandActive()) {
                mc.playerController.onStoppedUsingItem(mc.player);
            }
            mc.gameSettings.keyBindUseItem.setPressed(false);
            isEating = false;
        }
    }

    @java.lang.Override
    public void onDisable() {
        stopEating();
        super.onDisable();
    }
}