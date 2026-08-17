package sky.core.utils.player;

import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Slot;
import net.minecraft.network.play.client.CClickWindowPacket;
import sky.core.handlers.impl.InventoryHandler;
import sky.core.utils.Wrapper;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.Hand;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.misc.ChatUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Timer;
import java.util.TimerTask;

public class InventoryUtil implements Wrapper {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static int getSlot(Item item) {
        if (mc == null || mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static Slot getSlottest(Item item) {
        return mc.player.openContainer.inventorySlots.stream().filter(s -> s.getStack().getItem().equals(item)).findFirst().orElse(null);
    }

    public static void findItemAndThrow(Item item) {
        if (mc.player.getCooldownTracker().hasCooldown(item)) {
            ChatUtil.addText(item.getName().getString() + " - находиться в кулдауне", "Error");
            return;
        }

        Slot slot = getSlottest(item);
        if (slot == null) {
            ChatUtil.addText(item.getName().getString() + " - не обнаружен в инвентаре", "Error");
            return;
        }

        findItemAndThrow(slot);
    }

    public static void findItemAndThrow(Slot slot) {
        swapHand(slot, Hand.MAIN_HAND, true);
        useItem(Hand.MAIN_HAND);
        swapHand(slot, Hand.MAIN_HAND, true);
    }

    public static void swapHand(Slot slot, Hand hand, boolean packet) {
        if (slot != null) swapHand(slot.slotNumber, hand, packet);
    }

    public static void swapHand(int slotId, Hand hand, boolean packet) {
        if (slotId == -1) return;
        int button = hand.equals(Hand.MAIN_HAND) ? mc.player.inventory.currentItem : 40;

        clickSlotId(slotId, button, ClickType.SWAP, packet);
    }

    public static void useItem(Hand hand) {

        mc.player.connection.sendPacketWithoutEvent(new CPlayerTryUseItemPacket(hand));
    }

    public static void clickSlotId(int slotId, int buttonId, ClickType clickType, boolean packet) {
        clickSlotId(mc.player.openContainer.windowId, slotId, buttonId, clickType, packet);
    }

    public static void clickSlotId(int windowId, int slotId, int buttonId, ClickType clickType, boolean packet) {
        if (packet) {
            mc.player.connection.sendPacket(new CClickWindowPacket(windowId, slotId, buttonId, clickType, ItemStack.EMPTY, mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
        } else {
            mc.playerController.windowClick(windowId, slotId, buttonId, clickType, mc.player);
        }
    }

    public static void clickSlotId(int slotId, int buttonId, ClickType clickType) {
        if (mc.player == null || mc.playerController == null) return;
        mc.playerController.windowClick(0, slotId, buttonId, clickType, mc.player);
    }

    public static void moveItem(int from, int to) {
        if (from == to || from == -1 || mc.player == null || mc.playerController == null) return;
        from = from < 9 ? from + 36 : from;
        if (to == 45) {
            clickSlotId(from, 40, ClickType.SWAP);
        } else {
            clickSlotId(from, 0, ClickType.SWAP);
            clickSlotId(to, 0, ClickType.SWAP);
            clickSlotId(from, 0, ClickType.SWAP);
        }
    }
    public static void moveItemtest(int from, int to) {
        if (from == -1 || mc.player == null || mc.playerController == null) {
            return;
        }
        from = from < 9 ? from + 36 : from;
        if (from == to) {
            return;
        }
        clickSlotId(from, 0, ClickType.SWAP, false);
        clickSlotId(to, 0, ClickType.SWAP, false);
        clickSlotId(from, 0, ClickType.SWAP, false);
    }
    public static int antipoletToOffhand(Item item) {
        if (getSlot(item) == -1 || mc.player == null) {
            return -1;
        }
        for (int i = 0; i < 36; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                int originalSlot = i < 9 ? 36 + i : i;
                moveItem(originalSlot, 45);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (mc != null && mc.gameSettings != null && !mc.gameSettings.keyBindSneak.isKeyDown()) {
                            mc.gameSettings.keyBindSneak.setPressed(true);
                        }
                    }
                }, 150);

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (mc != null && mc.gameSettings != null && mc.gameSettings.keyBindSneak.isKeyDown()) {
                            mc.gameSettings.keyBindSneak.setPressed(false);
                        }
                    }
                }, 200);

                return originalSlot;
            }
        }
        return -1;
    }

    //    public void findItemAndThrow(Item item, float yaw, float pitch) {
//        if (mc.player.getCooldownTracker().hasCooldown(item)) {
//            ChatUtil.addTextWithError(item.getName().getString() + " - в кд");
//            return;
//        }
//
//        Slot slot = InvUtil.getSlot(item);
//        if (slot == null) {
//            ChatUtil.addTextWithError(item.getName().getString() + " - нету");
//            return;
//        }
//        if (MoveUtil.isMoving()) MoveComponent.stopTicks = 1;
//        MoveComponent.stop = true;
//        if (!MoveUtil.isMoving() || (!stop.get() && !mc.player.serverSprintState && !mc.player.isSprinting())) {
//
//            scheduler.schedule(() -> {
//                findItemAndThrows(slot, yaw, pitch, false);
//            }, 1, TimeUnit.MILLISECONDS);
//        }
//    }
    public static boolean isOnHotBar(int index) {
        return index >= 36 && index <= 44;
    }

    public static void swapItem(int from, int to) {
        if (from == to) return;

        from = from < 9 ? from + 36 : from;

        mc.playerController.windowClick(mc.player.openContainer.windowId, from, 0, ClickType.SWAP, mc.player);
        mc.playerController.windowClick(mc.player.openContainer.windowId, to, 0, ClickType.SWAP, mc.player);
        mc.playerController.windowClick(mc.player.openContainer.windowId, from, 0, ClickType.SWAP, mc.player);
    }

    public static int findBestSlotInHotBar() {
        int emptySlot = findEmptySlot();
        return emptySlot != -1 ? emptySlot : findNonSwordSlot();
    }

    public static boolean haveHotBar(Item item) {
        for (int i = 0; i < 9; ++i) {
            mc.player.inventory.getStackInSlot(i);
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                return true;
            }
        }

        return false;
    }

    public static int find(Item item) {
        int slot = -1;

        for (ItemStack stack : mc.player.getArmorInventoryList()) {
            if (stack.getItem() == item)
                return -2;
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == item) {
                slot = i;
                break;
            }
        }

        if (slot < 9 && slot != -1) {
            slot += 36;
        }

        return slot;
    }

    private static int findNonSwordSlot() {
        for (int i = 0; i < 9; ++i) {
            if (!(mc.player.inventory.getStackInSlot(i).getItem() instanceof SwordItem) && !(mc.player.inventory.getStackInSlot(i).getItem() instanceof ElytraItem) && mc.player.inventory.currentItem != i) {
                return i;
            }
        }

        return -1;
    }

    public static void switchItem(Item item, int to, boolean back, int timeDelayBack) {
        if (InventoryHelper.getItemIndex(item) == -1) {
            return;
        }

        int formativeSlot = InventoryUtil.getItemSlot(item);

        mc.playerController.windowClick(0, formativeSlot, to, ClickType.SWAP, mc.player);

        if (back) {
            TimeUtil.addTask(timeDelayBack, () -> {
                mc.playerController.windowClick(0, formativeSlot, to, ClickType.SWAP, mc.player);
            });
        }
    }

    public static int getItemSlot(Item input) {
        int slot = -1;

        for (int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == input) {
                slot = i;
                break;
            }
        }

        if (slot < 9 && slot != -1) {
            slot += 36;
        }

        return slot;
    }

    public static void pickItem(int from, int to) {
        if (from == to) return;

        from = from < 9 ? from + 36 : from;

        pickupItem(from, 0);
        pickupItem(to, 0);
        pickupItem(from, 0);
    }

    public static int getAxe(boolean hotBar) {
        int startSlot = hotBar ? 0 : 9;
        int endSlot = hotBar ? 9 : 36;

        for (int i = startSlot; i < endSlot; ++i) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (itemStack.getItem() instanceof AxeItem) {
                return i;
            }
        }

        return -1;
    }

    public static int findAxeSlot() {
        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    public static void pickupItem(int slot, int button) {
        if (mc.player == null || mc.playerController == null) return;
        mc.playerController.windowClick(0, slot, button, ClickType.PICKUP, mc.player);
    }

    public static int getChestPlateSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < mc.player.inventory.mainInventory.size(); i++) {
            ItemStack itemStack = mc.player.inventory.mainInventory.get(i);
            if (!itemStack.isEmpty() && itemStack.getItem() instanceof ArmorItem && ((ArmorItem) itemStack.getItem()).getEquipmentSlot() == EquipmentSlotType.CHEST) {
                return i;
            }
        }
        return -1;
    }

    public static void moveToArmor(int fromSlot, int armorSlot) {
        if (mc.player == null || mc.playerController == null) return;
        int invFrom = fromSlot < 9 ? fromSlot + 36 : fromSlot;

        mc.playerController.windowClick(0, invFrom, 40, ClickType.SWAP, mc.player);
        mc.playerController.windowClick(0, armorSlot, 40, ClickType.SWAP, mc.player);
        mc.playerController.windowClick(0, invFrom, 40, ClickType.SWAP, mc.player);
    }

    public static int getItemCount(Item item) {
        if (mc.player == null) return 0;
        int count = 0;
        for (ItemStack stack : mc.player.inventory.mainInventory) {
            if (!stack.isEmpty() && stack.getItem().equals(item)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    public static int findBestToolForBlock(BlockState blockState) {
        if (mc.player == null) return -1;

        int bestSlot = -1;
        float bestSpeed = 1.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ToolItem) {
                float speed = mc.player.calculateItemDigSpeed(stack, blockState);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    private static int findEmptySlot() {
        if (mc == null || mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    public static void inventorySwapClick(Item item) {
        inventorySwapClick(item, true);
    }

    public static void inventorySwapClick(Item item, boolean offhandWhileEating) {
        if (mc.player == null || mc.playerController == null) return;
        int slot = getSlot(item);
        if (slot == -1) return;

        int currentSlot = mc.player.inventory.currentItem;
        boolean isInHotbar = slot < 9;

        if (isInHotbar && slot == currentSlot) {
            useItemInHand(offhandWhileEating);
        } else if (isInHotbar) {
            swapAndUseItem(slot, currentSlot, offhandWhileEating);
        } else {
            int hotbarSlot = currentSlot % 8 + 1;
            mc.playerController.windowClick(0, slot, hotbarSlot, ClickType.SWAP, mc.player);
            swapAndUseItem(hotbarSlot, currentSlot, offhandWhileEating);
            mc.playerController.windowClick(0, slot, hotbarSlot, ClickType.SWAP, mc.player);
        }
    }

    private static void useItemInHand(boolean offhandWhileEating) {
        if (mc.player == null) return;
        if (offhandWhileEating && mc.player.isHandActive() && !mc.player.isBlocking()) {
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
        } else {
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
        }
    }

    private static void swapAndUseItem(int targetSlot, int currentSlot, boolean offhandWhileEating) {
        if (mc.player == null) return;
        if (offhandWhileEating && mc.player.isHandActive() && !mc.player.isBlocking()) {
            swapItem(targetSlot, 45);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
            swapItem(targetSlot, 45);
        } else {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(targetSlot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            mc.player.connection.sendPacket(new CHeldItemChangePacket(currentSlot));
        }
    }

    public static int getBestFoodSlot() {
        if (mc == null || mc.player == null) return -1;
        int best = -1, max = 0;
        for (int i = 0; i < 45; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem().isFood() && s.getCount() > max) {
                best = i;
                max = s.getCount();
            }
        }
        return best;
    }

    public static int findEnchantedTotemSlot() {
        if (mc == null || mc.player == null) return -1;
        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == Items.TOTEM_OF_UNDYING && !EnchantmentHelper.getEnchantments(stack).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public static boolean stackHasAnyEffect(ItemStack stack, boolean includeRegular, boolean includeSplash, boolean includeLingering, Effect... effects) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        boolean typeOk = (includeRegular && item == Items.POTION) || (includeSplash && item == Items.SPLASH_POTION) || (includeLingering && item == Items.LINGERING_POTION);
        if (!typeOk) return false;

        for (EffectInstance instance : PotionUtils.getEffectsFromStack(stack)) {
            for (Effect effect : effects) {
                if (instance.getPotion() == effect) return true;
            }
        }
        return false;
    }

    public static int findPotionSlotWithEffects(boolean preferHotbar, boolean includeRegular, boolean includeSplash, boolean includeLingering, Effect... effects) {
        if (mc.player == null) return -1;

        if (preferHotbar) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = mc.player.inventory.getStackInSlot(i);
                if (stackHasAnyEffect(s, includeRegular, includeSplash, includeLingering, effects)) return i;
            }
        } else {
            for (int i = 0; i < 9; i++) {
                ItemStack s = mc.player.inventory.getStackInSlot(i);
                if (stackHasAnyEffect(s, includeRegular, includeSplash, includeLingering, effects)) return i;
            }
            for (int i = 9; i < 36; i++) {
                ItemStack s = mc.player.inventory.getStackInSlot(i);
                if (stackHasAnyEffect(s, includeRegular, includeSplash, includeLingering, effects)) return i;
            }
        }
        return -1;
    }

    public static int findEmptyInventorySlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    public static void useItemLegit(Object object, Item item) {
        int slot = getSlot(item);
        if (slot == -1) return;
        if (slot < 9) {
            InventoryHandler.startWithLegit(object, slot);
        } else {
            InventoryHandler.startFromInventory(object, slot);
        }
    }
}