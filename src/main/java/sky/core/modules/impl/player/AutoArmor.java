package sky.core.modules.impl.player;


import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.math.TimeUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AutoArmor extends Module {

    private final SliderSetting delay = new SliderSetting("Задержка", 150.0F, 0.0F, 1000.0F, 5.0F);
    private final TimeUtil timer = new TimeUtil();

    public AutoArmor() {
        super("AutoArmor", "Автоматически одевает лучшую броню, обеспечивая максимальную защиту", Category.Player);
        addSettings(delay);
    }

    @EventTarget
    private void onUpdate(EventUpdate e) {
        ArmorItem armorItem;
        int i;

        if (mc.currentScreen != null) {
            return;
        }

        if (!timer.hasTimeElapsed(delay.get().longValue())) {
            return;
        }
        int[] bestIndexes = new int[]{-1, -1, -1, -1};
        int[] bestValues = new int[4];
        for (i = 0; i < 4; ++i) {
            ItemStack currentArmor = mc.player.inventory.armorItemInSlot(i);
            if (!this.isValidArmor(currentArmor)) continue;
            armorItem = (ArmorItem) currentArmor.getItem();
            bestValues[i] = this.getArmorValue(armorItem, currentArmor);
        }
        for (i = 0; i < 36; ++i) {
            Item item;
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack == null || stack.isEmpty() || !((item = stack.getItem()) instanceof ArmorItem)) continue;
            armorItem = (ArmorItem) item;
            int slotIndex = armorItem.getSlot().getIndex();
            int value = this.getArmorValue(armorItem, stack);
            if (value <= bestValues[slotIndex]) continue;
            bestIndexes[slotIndex] = i;
            bestValues[slotIndex] = value;
        }
        List<Integer> slots = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(slots);
        for (int slot : slots) {
            ItemStack currentArmor = mc.player.inventory.armorItemInSlot(slot);
            int bestIndex = bestIndexes[slot];
            if (bestIndex == -1 || (this.isValidArmor(currentArmor) && mc.player.inventory.getFirstEmptyStack() == -1)) {
                continue;
            }
            if (bestIndex < 9) {
                bestIndex += 36;
            }
            if (this.isValidArmor(currentArmor)) {
                mc.playerController.windowClick(0, 8 - slot, 0, ClickType.QUICK_MOVE, mc.player);
            }
            mc.playerController.windowClick(0, bestIndex, 0, ClickType.QUICK_MOVE, mc.player);
            timer.reset();
            break;
        }
    }

    private int getArmorValue(ArmorItem armor, ItemStack stack) {
        int protection = EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack);
        IArmorMaterial material = armor.getArmorMaterial();
        int damageReduction = material.getDamageReductionAmount(armor.getSlot());
        float toughness = armor.getToughness();
        return (int) (((float) (damageReduction * 20 + protection * 12) + toughness * 2.0f + (float) (damageReduction * 5)) / 8.0f);
    }

    private boolean isValidArmor(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ArmorItem;
    }
}
