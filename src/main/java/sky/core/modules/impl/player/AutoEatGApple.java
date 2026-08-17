package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.impl.movement.GrimGlide;
import sky.core.modules.api.constructors.impl.SliderSetting;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoEatGApple extends Module {
    private final SliderSetting health = new SliderSetting("Здоровье", 14, 1, 20, 0.5f);
    private boolean isEating = false;

    public AutoEatGApple() {
        super("Auto Eat GApple", "Автоматически кушает золотые яблоки", Category.Player);
        addSettings(health);
    }


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
        if (mc.player.getHeldItemOffhand().getItem() == Items.GOLDEN_APPLE && mc.player.getHealth() <= health.get()) {
            isEating = true;
            if (!mc.gameSettings.keyBindUseItem.isKeyDown()) {
                mc.playerController.processRightClick(mc.player, mc.world, Hand.OFF_HAND);
                mc.gameSettings.keyBindUseItem.setPressed(true);
            }
        } else if (isEating) {
            stopEating();
        }
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

    @Override
    public void onDisable() {
        stopEating();
        super.onDisable();
    }
}