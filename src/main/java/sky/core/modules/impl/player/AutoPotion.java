package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPickItemPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import sky.core.utils.component.impl.RotationComponent;
import sky.core.events.EventUpdate;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.utils.misc.ServerUtil;
import sky.core.utils.player.InventoryUtil;
import sky.core.utils.player.MoveUtil;

import java.util.ArrayList;
import java.util.List;

public class AutoPotion extends Module {
    private int ticksSinceLastThrow;
    private final BooleanSetting onlypvp = new BooleanSetting("Только при пвп", true);
    private final MultiBooleanSetting potions = new MultiBooleanSetting(
            "Бросать",
            new BooleanSetting("Скорость", true),
            new BooleanSetting("Сила", true),
            new BooleanSetting("Огнестойкость", true)
    );

    public AutoPotion() {
        super("Auto Potion", "Автоматически использует зелья, бросая их под себя", Category.Miscellaneous);
        addSettings(onlypvp, potions);
    }

    @java.lang.Override
    public void onEnable() {
        super.onEnable();
        ticksSinceLastThrow = 20;
    }

    @EventTarget
    private void onUpdate(EventUpdate e) {
        ticksSinceLastThrow++;
        List<Integer> slotsToThrow = new ArrayList<>();

        collectPotionSlot(slotsToThrow, potions.is("Скорость"), Effects.SPEED);
        collectPotionSlot(slotsToThrow, potions.is("Сила"), Effects.STRENGTH);
        collectPotionSlot(slotsToThrow, potions.is("Огнестойкость"), Effects.FIRE_RESISTANCE);

        if ((onlypvp.get() && !ServerUtil.isPvP()) || ticksSinceLastThrow < 20 || MoveUtil.isBlockUnder(0.5F) || slotsToThrow.isEmpty())
            return;

        RotationComponent.update(new Rotation(mc.player.rotationYaw, 90.0F), 360, 1, 10);
        if (new Rotation(mc.player).getDelta(Rotation.getReported()) > 1.0F) {
            return;
        }

        int original = mc.player.inventory.currentItem;

        for (Integer slot : slotsToThrow) {
            if (slot < 9) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            } else {
                mc.player.connection.sendPacket(new CPickItemPacket(slot));
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                mc.player.connection.sendPacket(new CPickItemPacket(slot));
            }
        }

        mc.player.connection.sendPacket(new CHeldItemChangePacket(original));
        ticksSinceLastThrow = 0;
    }

    private void collectPotionSlot(List<Integer> slotsToThrow, boolean enabled, Effect effect) {
        if (!enabled || mc.player.isPotionActive(effect)) return;
        int hotbarSlot = InventoryUtil.findPotionSlotWithEffects(true, false, true, false, effect);
        if (hotbarSlot != -1) {
            slotsToThrow.add(hotbarSlot);
            return;
        }
        int inventorySlot = InventoryUtil.findPotionSlotWithEffects(false, false, true, false, effect);
        if (inventorySlot != -1) slotsToThrow.add(inventorySlot);
    }
}