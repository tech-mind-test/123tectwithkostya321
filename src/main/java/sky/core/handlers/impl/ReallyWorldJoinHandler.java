package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CClickWindowPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.network.play.server.SOpenWindowPacket;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TextFormatting;
import sky.core.events.EventUpdate;
import sky.core.events.EventPacket;
import sky.core.modules.impl.player.AutoJoiner;
import sky.core.utils.misc.ChatUtil;
import sky.core.utils.Wrapper;
import sky.core.utils.player.InventoryUtil;
import net.minecraft.scoreboard.ScoreObjective;

public class ReallyWorldJoinHandler implements Wrapper {
    private static boolean enabled = false;
    private static int targetGrief = -1;
    private static Integer manualGrief = null;
    private static int pendingArrowClicks = 0;

    public static void startRejoin() {
        if (mc.player == null) return;
        targetGrief = manualGrief != null ? manualGrief : getGrief();
        if (targetGrief <= 0) return;
        pendingArrowClicks = Math.max(0, (targetGrief - 1) / 32);
        enabled = true;
        useCompassAndOpenSelector();
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (!enabled || mc.player == null || mc.player.openContainer == null) return;
        if (mc.currentScreen instanceof ContainerScreen) {
            mc.displayGuiScreen(null);
        }

        if (pendingArrowClicks > 0) {
            for (int slot = 0; slot < mc.player.openContainer.inventorySlots.size(); slot++) {
                Slot containerSlot = mc.player.openContainer.getSlot(slot);
                if (containerSlot == null || !containerSlot.getHasStack()) continue;
                ItemStack stack = containerSlot.getStack();
                if (stack.getItem() != Items.ARROW) continue;

                mc.player.connection.sendPacket(new CClickWindowPacket(
                        mc.player.openContainer.windowId,
                        slot, 0, ClickType.PICKUP,
                        stack,
                        mc.player.openContainer.getNextTransactionID(mc.player.inventory)
                ));
                pendingArrowClicks--;
                return;
            }
        }

        for (int slot = 0; slot < mc.player.openContainer.inventorySlots.size(); slot++) {
            Slot containerSlot = mc.player.openContainer.getSlot(slot);
            if (containerSlot == null || !containerSlot.getHasStack()) continue;
            ItemStack stack = containerSlot.getStack();
            String itemName = stack.getDisplayName().getString();
            String targetName = String.format("ГРИФ #%s (1.16.5+)", targetGrief);
            if (itemName.equalsIgnoreCase(targetName)) {
                mc.player.connection.sendPacket(new CClickWindowPacket(
                        mc.player.openContainer.windowId,
                        slot, 0, ClickType.PICKUP,
                        stack,
                        mc.player.openContainer.getNextTransactionID(mc.player.inventory)
                ));
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (!enabled || !event.isReceive()) return;
        if (mc.player == null) return;

        if (event.getPacket() instanceof SChatPacket packet) {
            String message = packet.getChatComponent().getString();
            if (message.contains("Подождите несколько секунд перед повторым подключением!")) {
                event.setCancelled(true);
            } else if (message.contains("Не удалось подключиться к серверу")) {
                event.setCancelled(true);
                ChatUtil.addChatMessage(String.format("Попытка зайти на гриф: #%s: " + TextFormatting.RED + "Неудачно", targetGrief));
            } else if (message.contains("К сожалению сервер переполнен")) {
                event.setCancelled(true);
                ChatUtil.addChatMessage(String.format("Попытка зайти на гриф: #%s: " + TextFormatting.RED + "Неудачно", targetGrief));
            } else {
                useCompassAndOpenSelector();
            }
        }

        if (event.getPacket() instanceof SOpenWindowPacket packet) {
            if (packet.getTitle().getString().contains("Выбор сервера") && mc.player.openContainer != null) {
                mc.player.connection.sendPacket(new CClickWindowPacket(
                        packet.getWindowId(),
                        21, 0, ClickType.PICKUP,
                        mc.player.openContainer.getSlot(21).getStack(),
                        mc.player.openContainer.getNextTransactionID(mc.player.inventory)
                ));
                event.setCancelled(true);
            }
        }

        if (event.getPacket() instanceof SJoinGamePacket) {
            ChatUtil.addChatMessage(String.format("Попытка зайти на гриф: #%s: " + TextFormatting.GREEN + "Готово", targetGrief));
            enabled = false;
            AutoJoiner autoJoiner = (AutoJoiner) sky.core.SkyCore.getInstance().getModuleManager().getModule(AutoJoiner.class);
            if (autoJoiner != null && autoJoiner.isEnabled()) {
                autoJoiner.settoggled(false);
            }
        }
    }

    private static void useCompassAndOpenSelector() {
        int slot = InventoryUtil.getSlot(Items.COMPASS);
        if (slot == -1 || mc.player == null) return;
        mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
    }

    public static int getGrief() {
        if (mc.world == null) return -1;
        for (ScoreObjective team : mc.world.getScoreboard().getScoreObjectives()) {
            String board = team.getDisplayName().getString();
            if (board.contains("ГРИФ #")) {
                return Integer.parseInt(board.split("#")[1].replace(" ", ""));
            }
        }
        return -1;
    }

    public static void setManualGrief(int grief) {
        manualGrief = grief;
    }
    public static void clearManualGrief() {
        manualGrief = null;
    }

    public static void disable() {
        enabled = false;
        manualGrief = null;
        targetGrief = -1;
        pendingArrowClicks = 0;
    }
}