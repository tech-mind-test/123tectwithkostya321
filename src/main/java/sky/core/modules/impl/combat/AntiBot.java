package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.events.EventSwapWorld;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class AntiBot extends Module {
    public static final List<Entity> bot = new ArrayList<>();

    public AntiBot() {
        super("Anti Bot", "Определяет и метит ботов, вызванных системой античита", Category.Combat);
        addSettings(mode);

    }

    private final ModeSetting mode = new ModeSetting("Обход", "ReallyWorld", "ReallyWorld", "Matrix");

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Matrix")) {
            for (PlayerEntity playerEntity : mc.world.getPlayers()) {
                if (playerEntity.equals(mc.player)) {
                    continue;
                }
                if (!playerEntity.getUniqueID().equals(PlayerEntity.getOfflineUUID(playerEntity.getName().getString()))
                        && !bot.contains(playerEntity)) {
                    bot.add(playerEntity);
                }
            }
            return;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.equals(mc.player)) {
                continue;
            }

            boolean isBot = false;
            if (mode.is("ReallyWorld")) {
                boolean hasValidArmor = player.inventory.armorInventory.stream().allMatch(armorItem ->
                        armorItem.getItem() != Items.AIR && armorItem.isEnchantable() && !armorItem.isDamaged());
                boolean hasValidEquipment = player.getHeldItemOffhand().getItem() == Items.AIR &&
                        player.inventory.armorInventory.stream().anyMatch(armorItem ->
                                armorItem.getItem() == Items.LEATHER_BOOTS ||
                                        armorItem.getItem() == Items.LEATHER_LEGGINGS ||
                                        armorItem.getItem() == Items.LEATHER_CHESTPLATE ||
                                        armorItem.getItem() == Items.LEATHER_HELMET ||
                                        armorItem.getItem() == Items.IRON_BOOTS ||
                                        armorItem.getItem() == Items.IRON_LEGGINGS ||
                                        armorItem.getItem() == Items.IRON_CHESTPLATE ||
                                        armorItem.getItem() == Items.IRON_HELMET);
                boolean hasFullFood = player.getFoodStats().getFoodLevel() == 20;
                isBot = hasValidArmor && hasValidEquipment && hasFullFood;
            }

            if (isBot) {
                if (!bot.contains(player)) {
                    bot.add(player);
                }
            } else {
                bot.remove(player);
            }
        }
    }

    @EventTarget
    public void onEventWorldChanged(EventSwapWorld e) {
        bot.clear();
    }

    @Native
    @Override
    public void onDisable() {
        bot.clear();
        super.onDisable();
    }
}