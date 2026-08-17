package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventDropItem;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.utils.misc.ChatUtil;
import sky.core.utils.misc.ServerUtil;
import net.minecraft.item.Items;
import net.minecraft.util.text.TextFormatting;

public class LockSlot extends Module {

    private final BooleanSetting workInPVP = new BooleanSetting("Только при пвп", true);

    public final MultiBooleanSetting slots = new MultiBooleanSetting("Выберите слоты",
            new BooleanSetting("1", false),
            new BooleanSetting("2", false),
            new BooleanSetting("3", false),
            new BooleanSetting("4", false),
            new BooleanSetting("5", false),
            new BooleanSetting("6", false),
            new BooleanSetting("7", false),
            new BooleanSetting("8", false),
            new BooleanSetting("9", false));


    public LockSlot() {
        super("Lock Slot", "Блокирует возможность выкинуть предет в указанных слотах", Category.Miscellaneous);
        addSettings(workInPVP, slots);
    }

    @EventTarget
    public void onDropItem(EventDropItem e) {
        if (mc.player == null || mc.player.inventory == null || mc.player.getHeldItemMainhand().getItem() == Items.AIR) return;
        if (workInPVP.get() && !ServerUtil.isPvP()) return;

        int currentSlot = e.getCurrentSlot();
        BooleanSetting setting = slots.getIndex(currentSlot);

        if (setting.get()) {
            e.setCancelled(true);
            ChatUtil.addText(TextFormatting.GRAY + "Выброс предмет из слота " + (currentSlot + 1) + " был заблокирован");
        }
    }
}
