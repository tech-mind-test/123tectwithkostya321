package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventBlockDamage;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.utils.player.InventoryUtil;

public class AutoTool extends Module {
    private int lastSlot = -1;

    public AutoTool() {
        super("AutoTool", "Автоматически выбирает лучший инструмент в инвентаре для добычи блоков и атаки противника", Category.Player);
    }

    @EventTarget
    public void onBlockDamage(EventBlockDamage e) {
        if (e.getState() == EventBlockDamage.State.START) {
            this.lastSlot = mc.player.inventory.currentItem;

            int superScissorsSlot = findSuperScissorsSlot();
            String blockName = e.getBlockState().getBlock().getTranslationKey().toLowerCase();
            boolean isCobweb = blockName.contains("web") || blockName.contains("cobweb");

            int bestToolSlot;

            if (isCobweb && superScissorsSlot != -1) {
                bestToolSlot = superScissorsSlot;
            } else {
                bestToolSlot = InventoryUtil.findBestToolForBlock(e.getBlockState());
            }

            if (bestToolSlot != -1) {
                mc.player.inventory.currentItem = bestToolSlot;
            }
        } else if (lastSlot != -1) {
            mc.player.inventory.currentItem = lastSlot;
            lastSlot = -1;
        }
    }

    private int findSuperScissorsSlot() {
        for (int slot = 0; slot < 9; slot++) {
            net.minecraft.item.ItemStack stack = mc.player.inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.hasDisplayName()) {
                String displayName = stack.getDisplayName().getString();
                if (displayName.toLowerCase().contains("супер ножницы")) {
                    return slot;
                }
            }
        }
        return -1;
    }
}