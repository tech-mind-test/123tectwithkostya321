package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.events.EventUpdate;
import sky.core.SkyCore;
import sky.core.handlers.impl.StaffHandler;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.misc.ServerUtil;
import mods.baritone.api.api.java.baritone.api.BaritoneAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class AutoLeave extends Module {
    private final MultiBooleanSetting leave = new MultiBooleanSetting("Ливать если",
            new BooleanSetting("Мало здоровья", true),
            new BooleanSetting("Рядом игрок", true),
            new BooleanSetting("Администратор в ванише", true),
            new BooleanSetting("Лутание предметов", false));

    private final ModeSetting typeleave = new ModeSetting("Тип ухода", "Спавн",
            () -> leave.is("Мало здоровья") || leave.is("Рядом игрок") || leave.is("Администратор в ванише") || leave.is("Лутание предметов"),
            "Спавн", "Домой", "Хаб");

    private final BooleanSetting pvp = new BooleanSetting("Ливать только в конце пвп", false,
            () -> leave.is("Мало здоровья") || leave.is("Рядом игрок") || leave.is("Администратор в ванише") || leave.is("Лутание предметов"));

    private final SliderSetting hp = new SliderSetting("Здоровье", 15, 1, 20, 1, () -> leave.is("Мало здоровья"));
    private final SliderSetting radius = new SliderSetting("Радиус игрока", 10, 1, 50, 1, () -> leave.is("Рядом игрок"));

    private final SliderSetting minItems = new SliderSetting("Мин. предметов", 1, 1, 64, 1, () -> leave.is("Лутание предметов"));

    private final BooleanSetting baritone = new BooleanSetting("Отключать баритон", true,
            () -> leave.is("Мало здоровья") || leave.is("Рядом игрок") || leave.is("Администратор в ванише") || leave.is("Лутание предметов"));

    private int lastItemCount;

    public AutoLeave() {
        super("AutoLeave", "Избегает нежелательной встречи с игроками, выбранным способом, обеспечивая безопасность", Category.Player);
        addSettings(leave, typeleave, pvp, hp, radius, minItems, baritone);
    }

    @java.lang.Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            lastItemCount = getTotalItemCount();
        }
    }

    @Native
    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (leave.is("Мало здоровья") && mc.player.getHealth() <= hp.get()) {
            handleLeave();
            return;
        }

        if (leave.is("Рядом игрок")) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (SkyCore.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())) continue;
                if (mc.player.getDistance(player) <= radius.get()) {
                    handleLeave();
                    return;
                }
            }
        }

        if (leave.is("Администратор в ванише")) {
            if (!StaffHandler.getVanishedStaff().isEmpty()) {
                handleLeave();
                return;
            }
        }

        if (leave.is("Лутание предметов")) {
            int currentItemCount = getTotalItemCount();
            int pickedUp = currentItemCount - lastItemCount;

            if (pickedUp >= minItems.get()) {
                handleLeave();
                return;
            }

            lastItemCount = currentItemCount;
        }
    }

    private int getTotalItemCount() {
        int count = 0;
        for (ItemStack stack : mc.player.inventory.mainInventory) {
            if (!stack.isEmpty()) count += stack.getCount();
        }
        for (ItemStack stack : mc.player.inventory.armorInventory) {
            if (!stack.isEmpty()) count += stack.getCount();
        }
        for (ItemStack stack : mc.player.inventory.offHandInventory) {
            if (!stack.isEmpty()) count += stack.getCount();
        }
        return count;
    }

    @Native
    private void handleLeave() {
        if (pvp.get() && !ServerUtil.isPvP()) return;

        if (baritone.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            mc.player.sendChatMessage(BaritoneAPI.getSettings().prefix.value + "stop");
        }

        switch (typeleave.get()) {
            case "Спавн" -> mc.player.sendChatMessage("/spawn");
            case "Домой" -> mc.player.sendChatMessage("/home");
            case "Хаб" -> mc.player.sendChatMessage("/hub");
        }

        toggle();
    }
}