package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventCancelHotbar;
import sky.core.events.EventInput;
import sky.core.events.EventUpdate;
import sky.core.utils.Wrapper;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.util.Hand;

public class InventoryHandler implements Wrapper {

    private static final int DEFAULT_DELAY = 1;

    private enum ActionState {IDLE, WAITING, READY}

    private enum InventoryStage {SWAP_TO_HOTBAR, RIGHT_CLICK, SWAP_BACK}

    private static ActionState actionState = ActionState.IDLE;
    private static int waitTicks, rightClickTicks, originalSlot = -1;
    private static boolean pendingRightClick, pendingInventoryTransfer;
    private static int inventoryTicks, pendingInventorySlotId = -1, pendingHotbarIndex = -1;
    private static InventoryStage inventoryStage = InventoryStage.SWAP_TO_HOTBAR;
    private static Object ownerToken = null;

    @EventTarget
    public void onEvent(EventInput e) {
        if (isBusy()) {
            e.setForward(0);
            e.setStrafe(0);
            e.setJump(false);
        }
    }

    @EventTarget
    public void onEvent(EventCancelHotbar e) {
        if (isBusy()) e.setCancelled(true);
    }

    @EventTarget
    public void onEvent(EventUpdate e) {
        if (actionState == ActionState.WAITING && --waitTicks <= 0) actionState = ActionState.READY;
        handleRightClick();
        handleInventoryTransfer();
    }

    private void handleRightClick() {
        if (!pendingRightClick || --rightClickTicks > 0) return;
        click();
        if (originalSlot >= 0) {
            mc.playerController.syncCurrentPlayItem();
            mc.player.inventory.currentItem = originalSlot;
        }
        resetRightClick();
    }

    private void handleInventoryTransfer() {
        if (!pendingInventoryTransfer || mc.player == null || mc.playerController == null || --inventoryTicks > 0)
            return;
        switch (inventoryStage) {
            case SWAP_TO_HOTBAR -> {
                pendingHotbarIndex = mc.player.inventory.currentItem;
                mc.playerController.windowClick(0, pendingInventorySlotId, pendingHotbarIndex, ClickType.SWAP, mc.player);
                inventoryStage = InventoryStage.RIGHT_CLICK;
                mc.player.closeScreen();
                resetInventoryTimer();
            }
            case RIGHT_CLICK -> {
                click();
                inventoryStage = InventoryStage.SWAP_BACK;
                resetInventoryTimer();
            }
            case SWAP_BACK -> {
                mc.playerController.windowClick(0, pendingInventorySlotId, pendingHotbarIndex, ClickType.SWAP, mc.player);
                mc.player.closeScreen();
                resetInventoryTransfer();
            }
        }
    }

    private static void click() {
        mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
        mc.player.swingArm(Hand.MAIN_HAND);
    }

    private static boolean isBusy() {
        return actionState != ActionState.IDLE || pendingInventoryTransfer;
    }

    public static void startWithoutAction(Object owner) {
        if (ownerToken != null && ownerToken != owner) return;
        ownerToken = owner;
        actionState = ActionState.WAITING;
        waitTicks = DEFAULT_DELAY;
    }

    public static boolean isReadyForAction(Object owner) {
        return actionState == ActionState.READY && ownerToken == owner;
    }

    public static void finishAction(Object owner) {
        if (ownerToken != owner) return;
        actionState = ActionState.IDLE;
        waitTicks = 0;
        ownerToken = null;
        if (mc.player != null) mc.player.closeScreen();
    }

    public static void startWithLegit(Object owner, int slot) {
        if (mc.player == null || mc.playerController == null) return;
        if (ownerToken != null && ownerToken != owner) return;
        ownerToken = owner;
        originalSlot = mc.player.inventory.currentItem;
        mc.playerController.syncCurrentPlayItem();
        mc.player.inventory.currentItem = slot;
        pendingRightClick = true;
        rightClickTicks = DEFAULT_DELAY;
    }

    public static void startFromInventory(Object owner, int slotId) {
        if (mc.player == null || mc.playerController == null) return;
        if (ownerToken != null && ownerToken != owner) return;
        ownerToken = owner;
        pendingInventoryTransfer = true;
        inventoryStage = InventoryStage.SWAP_TO_HOTBAR;
        resetInventoryTimer();
        pendingInventorySlotId = slotId;
    }

    private static void resetRightClick() {
        pendingRightClick = false;
        rightClickTicks = 0;
        originalSlot = -1;
        clearOwnerIfIdle();
    }

    private static void resetInventoryTimer() {
        inventoryTicks = DEFAULT_DELAY;
    }

    private static void resetInventoryTransfer() {
        pendingInventoryTransfer = false;
        inventoryStage = InventoryStage.SWAP_TO_HOTBAR;
        inventoryTicks = 0;
        pendingInventorySlotId = pendingHotbarIndex = -1;
        clearOwnerIfIdle();
    }

    private static void clearOwnerIfIdle() {
        if (!pendingRightClick && !pendingInventoryTransfer && actionState == ActionState.IDLE) {
            ownerToken = null;
        }
    }
}