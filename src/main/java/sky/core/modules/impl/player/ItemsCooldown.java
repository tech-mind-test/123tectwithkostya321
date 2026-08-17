package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventCooldownTracker;
import sky.core.events.EventRightClickItemCheck;
import sky.core.events.EventUseEnderPearl;
import sky.core.events.EventUseFinish;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.misc.ServerUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

public class ItemsCooldown extends Module {
    private static final MultiBooleanSetting items = new MultiBooleanSetting("Применять на", new BooleanSetting("Золотое яблоко", true), new BooleanSetting("Золотое зачарованное яблоко", true), new BooleanSetting("Эндер жемчуг", true), new BooleanSetting("Хорус", true));
    private static final SliderSetting gapple = new SliderSetting("Задержка золотого яблока", 4.5F, 0.5F, 15.0F, 0.05F, () -> items.getIndex(0).get());
    private static final SliderSetting encgapple = new SliderSetting("Задержка золотого зачарованного яблока", 4.5F, 0.5F, 15.0F, 0.05F, () -> items.getIndex(3).get());
    private static final SliderSetting pearl = new SliderSetting("Задержка эндер жемчуга", 14.05F, 0.5F, 15.0F, 0.05F, () -> items.getIndex(1).get());
    private static final SliderSetting chorus = new SliderSetting("Задержка хоруса", 2.3F, 0.5F, 15.0F, 0.05F, () -> items.getIndex(2).get());
    private static final BooleanSetting onlypvp = new BooleanSetting("Только при пвп", true);
    private final Map<Item, TimeUtil> cooldowns = new HashMap<>();

    public ItemsCooldown() {
        super("Items Cooldown", "Отображает оставшееся время восстановления для предметов, имеющих перезарядку", Category.Player);
        addSettings(items, gapple, pearl, chorus, encgapple, onlypvp);
    }

    @EventTarget
    public void onEvent(EventUseFinish event) {
        if (onlypvp.get() && !ServerUtil.isPvP()) return;
        Item item = event.getItemStack().getItem();
        if (ItemType.fromItem(item) != null) {
            cooldowns.put(item, new TimeUtil());
        }
    }

    @EventTarget
    public void onEvent(EventUseEnderPearl event) {
        if (onlypvp.get() && !ServerUtil.isPvP()) return;
        Item item = event.getItemStack().getItem();
        if (ItemType.fromItem(item) != null) {
            cooldowns.put(item, new TimeUtil());
        }
    }

    @EventTarget
    public void onEvent(EventCooldownTracker event) {
        if (onlypvp.get() && !ServerUtil.isPvP()) return;
        Item item = event.getItem();
        TimeUtil timer = cooldowns.get(item);
        if (timer == null) return;

        float cooldownMs = ItemType.fromItem(item).getCooldownSeconds() * 1000F;
        long elapsed = timer.getTimePassed();
        if (elapsed >= cooldownMs) {
            cooldowns.remove(item);
            return;
        }
        event.setPartialTicks(elapsed / cooldownMs);
    }

    @EventTarget
    public void onEvent(EventRightClickItemCheck event) {
        if (onlypvp.get() && !ServerUtil.isPvP()) return;
        Item item = event.getItemStack().getItem();
        ItemType type = ItemType.fromItem(item);
        if (type != null && cooldowns.containsKey(type.getItem()) && mc.player.getCooldownTracker().hasCooldown(type.getItem())) {
            event.setCancelled(true);
        }
    }

    @Getter
    @AllArgsConstructor
    private enum ItemType {
        GOLDEN_APPLE(Items.GOLDEN_APPLE, gapple.get()), CHORUS(Items.CHORUS_FRUIT, chorus.get()), PEARL(Items.ENDER_PEARL, pearl.get()), ENC_GOLDEN_APPLE(Items.ENCHANTED_GOLDEN_APPLE, encgapple.get());

        private final Item item;
        private final float cooldownSeconds;

        public static ItemType fromItem(Item item) {
            for (ItemType type : values()) {
                if (type.item == item) {
                    return type;
                }
            }
            return null;
        }
    }
}
