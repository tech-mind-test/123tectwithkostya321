package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.math.TimeUtil;
import net.minecraft.inventory.container.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class

    ChestStealer extends Module {
    private final SliderSetting stealDelay = new SliderSetting("Задержка", 100.0F, 0.0F, 1000.0F, 1.0F);
    private final BooleanSetting randomize = new BooleanSetting("Рандомизация", false);
    private final TimeUtil timer = new TimeUtil();

    public ChestStealer() {
        super("ChestStealer", "Автоматически открывает сундуки и забирает из них предметы", Category.Player);
        addSettings(stealDelay, randomize);
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        Container openContainer = mc.player.openContainer;
        if (openContainer instanceof ChestContainer || openContainer instanceof HopperContainer) {
            List<Slot> slots = openContainer.inventorySlots;
            if (timer.hasTimeElapsed(stealDelay.get().longValue())) {
                findValidItemCustom(slots).ifPresent((slot) -> {
                    if (mc.player.openContainer == openContainer) {
                        mc.playerController.windowClick(openContainer.windowId, slot.slotNumber, 0, ClickType.QUICK_MOVE, mc.player);
                        timer.reset();
                    }
                });
            }
        }
    }
    
    private Optional<Slot> findValidItemCustom(List<Slot> slots) {
        int containerSlotCount = slots.size() - mc.player.inventory.mainInventory.size();
        List<Slot> containerSlots = slots.subList(0, containerSlotCount);
        List<Slot> validSlots = containerSlots.stream().filter((slot) -> !slot.getStack().isEmpty()).filter((slot) -> !mc.player.getCooldownTracker().hasCooldown(slot.getStack().getItem())).toList();
        if (validSlots.isEmpty()) {
            return Optional.empty();
        } else {
            return randomize.get() ? Optional.of(validSlots.get(ThreadLocalRandom.current().nextInt(validSlots.size()))) : validSlots.stream().findFirst();
        }
    }
}
