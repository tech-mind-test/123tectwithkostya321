package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.SkullItem;
import sky.core.events.EventUpdate;
import sky.core.utils.Wrapper;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.misc.ServerUtil;

public class HolyWorldJoinHandler implements Wrapper {

    private enum Phase {
        IDLE, SENT_HUB, USED_COMPASS, CLICKED_GRIEF_SURVIVAL
    }

    private static final TimeUtil timer = new TimeUtil();
    private static HolyWorldJoinHandler.Phase phase = HolyWorldJoinHandler.Phase.IDLE;
    private static int targetGrief = -1;

    public static void startRejoin() {
        if (mc.player == null) return;
        targetGrief = getGrief();
        if (targetGrief <= 0) return;
        phase = HolyWorldJoinHandler.Phase.SENT_HUB;
        mc.player.sendChatMessage("/hub");
        timer.reset();
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (phase == HolyWorldJoinHandler.Phase.IDLE) return;
        if (!ServerUtil.isConnectedToServer("holyworld")) reset();


        if (phase == HolyWorldJoinHandler.Phase.SENT_HUB && timer.hasTimeElapsed(200)) {
            useCompass();
            phase = HolyWorldJoinHandler.Phase.USED_COMPASS;
            timer.reset();
            return;
        }

        if (phase == HolyWorldJoinHandler.Phase.USED_COMPASS && !(mc.currentScreen instanceof ContainerScreen)) {
            if (timer.hasTimeElapsed(500)) {
                useCompass();
                timer.reset();
            }
            return;
        }

        if (!(mc.currentScreen instanceof ContainerScreen container)) return;

        for (int i = 0; i < container.getContainer().inventorySlots.size(); i++) {
            Slot slot = container.getContainer().inventorySlots.get(i);
            if (slot.getStack().isEmpty()) continue;
            if (phase == HolyWorldJoinHandler.Phase.USED_COMPASS) {
                if (targetGrief <= 16 && slot.getStack().getDisplayName().getString().contains("СолоЛайт")) {
                    phase = Phase.CLICKED_GRIEF_SURVIVAL;
                    clickSlot(i);
                    break;
                }
                if (targetGrief > 16 && targetGrief <= 37 && slot.getStack().getDisplayName().getString().contains("ДуоЛайт")) {
                    phase = Phase.CLICKED_GRIEF_SURVIVAL;
                    clickSlot(i);
                    break;
                }
                if (targetGrief > 37 && targetGrief <= 53 && slot.getStack().getDisplayName().getString().contains("ТриоЛайт")) {
                    phase = Phase.CLICKED_GRIEF_SURVIVAL;
                    clickSlot(i);
                    break;
                }
                if (targetGrief > 53 && targetGrief <= 69 && slot.getStack().getDisplayName().getString().contains("КланЛайт")) {
                    phase = Phase.CLICKED_GRIEF_SURVIVAL;
                    clickSlot(i);
                    break;
                }
            }

            if (phase == HolyWorldJoinHandler.Phase.CLICKED_GRIEF_SURVIVAL && slot.getStack().getTag().toString().contains("#" + targetGrief) && slot.getStack().getItem() instanceof SkullItem && timer.hasTimeElapsed(100)) {
                clickSlot(i);
                reset();
                break;
            }
        }
    }

    private void useCompass() {
        mc.player.sendChatMessage("/lite");
    }

    private void clickSlot(int slotIndex) {
        mc.playerController.windowClick(mc.player.openContainer.windowId, slotIndex, 0, ClickType.PICKUP, mc.player);
        timer.reset();
    }

    public static int getGrief() {
        if (mc.world == null) return -1;

        String headerText = mc.ingameGUI.getTabList().getHeader().getString();

        if (headerText == null || headerText.isEmpty()) return -1;

        String displayName = headerText.toLowerCase();
        int prefix = displayName.indexOf("лайт");
        if (prefix == -1) return -1;
        try {
            String numberStr = displayName.substring(prefix + "лайт".length()).replaceAll("\\D", "");
            if (!numberStr.isEmpty()) {
                return Integer.parseInt(numberStr);
            }
        } catch (NumberFormatException ignored) {
        }

        return -1;
    }

    private static void reset() {
        phase = HolyWorldJoinHandler.Phase.IDLE;
        targetGrief = -1;
        timer.reset();
    }
}
