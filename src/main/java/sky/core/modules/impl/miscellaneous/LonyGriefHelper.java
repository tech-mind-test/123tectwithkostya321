package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.Hand;
import sky.core.utils.component.impl.MoveComponent;
import sky.core.events.EventKey;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.player.InventoryUtil;
import sky.core.utils.player.MoveUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LonyGriefHelper extends Module {

    public BindSetting useBlazeRod = new BindSetting("Ливалка");
    public BindSetting cryingObsidianKey = new BindSetting("Трапка");
    public BindSetting clayKey = new BindSetting("Ливалка с платформой");
    public BooleanSetting autoGps = new BooleanSetting("AutoGps", true);

    BooleanSetting stop = new BooleanSetting("Полная остановка", false);
    boolean canUse = false;

    public boolean slow = false;
    private boolean progress = false;
    private final Pattern pattern = Pattern.compile("Его координаты: (-?\\d+)\\. (-?\\d+)\\. (-?\\d+)\\.");

    public LonyGriefHelper() {
        super("LonyGrief Assist", "Ассит под LonyGrief", Category.Miscellaneous);
        this.addSettings(useBlazeRod, cryingObsidianKey, clayKey, stop, autoGps);
    }

    Item swapItem;

    private void useItemAndClick(Item item) {
        if (mc.player == null) return;
        if (InventoryUtil.find(item) < 0) return;
        if (progress) return;

        swapItem = item;
        progress = true;
        allow = true;
        canUse = true;
    }

    @EventTarget
    public void onUse(EventUpdate e) {
        if (swapItem == null) return;
        itemController(swapItem);
    }

    private void itemController(Item item) {
        int slot = InventoryUtil.find(item);
        if (slot >= 0) {
            if (allow) {
                MoveComponent.stopTicks = 1;
                if (stop.get()) MoveComponent.stop = true;

                if (!MoveUtil.isMoving() || (!stop.get() && !mc.player.serverSprintState && !mc.player.isSprinting())) {
                    int hotbarSlot = mc.player.inventory.currentItem;
                    savedInvSlot = slot;
                    savedHotbarSlot = hotbarSlot;

                    mc.playerController.windowClick(0, savedInvSlot, savedHotbarSlot, ClickType.SWAP, mc.player);
                    mc.playerController.syncCurrentPlayItem();
                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                    mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));

                    delay = System.currentTimeMillis() + 100;

                    if (mc.currentScreen == null) {
                        mc.player.connection.sendPacket(new CCloseWindowPacket(mc.player.openContainer.windowId));
                    }
                    allow = false;
                }
            }
        }

        if (delay >= 0L && System.currentTimeMillis() >= delay) {
            if (savedInvSlot != -1 && savedHotbarSlot != -1) {
                MoveComponent.stopTicks = 1;
                if (stop.get()) MoveComponent.stop = true;

                if (!MoveUtil.isMoving() || (!stop.get() && !mc.player.serverSprintState && !mc.player.isSprinting())) {
                    mc.playerController.windowClick(0, savedInvSlot, savedHotbarSlot, ClickType.SWAP, mc.player);
                    mc.playerController.syncCurrentPlayItem();

                    if (mc.currentScreen == null) {
                        mc.player.connection.sendPacket(new CCloseWindowPacket(mc.player.openContainer.windowId));
                    }

                    savedInvSlot = -1;
                    savedHotbarSlot = -1;
                    delay = -1L;
                    swapItem = null;
                    progress = false;
                    canUse = false;
                }
            } else {
                delay = -1L;
                swapItem = null;
                progress = false;
                canUse = false;
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (autoGps.get() && event.isReceive() && event.getPacket() instanceof SChatPacket) {
            SChatPacket packet = (SChatPacket) event.getPacket();
            String text = packet.getChatComponent().getString();

            if (text.contains("Его координаты:")) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String x = matcher.group(1);
                    String z = matcher.group(3);
                    if (mc.player != null) {
                        mc.player.sendChatMessage(".gps " + x + " " + z);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onKey(EventKey e) {
        if (mc.currentScreen == null && mc.player != null) {
            if (canUse) return;
            boolean keyWasPressed = e.getKey() == this.useBlazeRod.get() ||
                    e.getKey() == this.cryingObsidianKey.get() ||
                    e.getKey() == this.clayKey.get();

            if (keyWasPressed) {
                this.slow = true;
            }

            if (e.isHold()) {
                if (useBlazeRod.get() == e.getKey()) {
                    useItemAndClick(Items.MAGMA_CREAM);
                }
                if (cryingObsidianKey.get() == e.getKey()) {
                    useItemAndClick(Items.CRYING_OBSIDIAN);
                }
                if (clayKey.get() == e.getKey()) {
                    useItemAndClick(Items.CLAY_BALL);
                }
            }
        }
    }

    private boolean allow;
    private long delay = -1L;
    private int savedInvSlot = -1;
    private int savedHotbarSlot = -1;

    @EventTarget
    public void onUpdate(EventUpdate e) {
        this.slow = false;
    }
}