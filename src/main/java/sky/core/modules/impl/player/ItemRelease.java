package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.TridentItem;

public class ItemRelease extends Module {
    private final MultiBooleanSetting items = new MultiBooleanSetting("Предметы", new BooleanSetting("Трезубец", false), new BooleanSetting("Лук", false), new BooleanSetting("Арбалет", false));
    private final SliderSetting delay = new SliderSetting("Сила выстрела", 3.0f, 1.0f, 20, 0.5f, () -> items.is("Лук"));

    public ItemRelease() {
        super("ItemRelease", "Автоматически выпускает предмет, когда он полностью натянут", Category.Player);
        addSettings(items, delay);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (items.is("Лук")) {
            if (mc.player.inventory.getCurrentItem().getItem() instanceof BowItem && mc.player.isHandActive() && mc.player.getItemInUseMaxCount() >= delay.get()) {
                mc.playerController.onStoppedUsingItem(mc.player);
            }
        }

        if (items.is("Трезубец")) {
            if (mc.player.inventory.getCurrentItem().getItem() instanceof TridentItem && mc.player.isHandActive() && mc.player.getItemInUseMaxCount() >= 10) {
                mc.playerController.onStoppedUsingItem(mc.player);
            }
        }

        if (items.is("Арбалет")) {
            if (mc.player.inventory.getCurrentItem().getItem() instanceof CrossbowItem) {
                if (mc.player.isHandActive() && mc.player.getItemInUseMaxCount() >= CrossbowItem.getChargeTime(mc.player.inventory.getCurrentItem())) {
                    mc.playerController.onStoppedUsingItem(mc.player);
                }
            }
        }
    }
}