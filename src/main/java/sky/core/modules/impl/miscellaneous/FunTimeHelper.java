package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import sky.core.utils.component.impl.MoveComponent;
import sky.core.events.EventKey;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.utils.player.InventoryUtil;
import sky.core.utils.player.MoveUtil;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FunTimeHelper extends Module {
    public BindSetting useDezor = new BindSetting("Дезориентация");
    public BindSetting useTrap = new BindSetting("Трапка");
    public BindSetting usePil = new BindSetting("Явная пыль");
    public BindSetting useSmerch = new BindSetting("Огненный смерч");
    public BindSetting usePlast = new BindSetting("Пласт");
    public BindSetting useAura = new BindSetting("Божья аура");
    public BindSetting useSnow = new BindSetting("Снежок");

    boolean canUse = false;
    public boolean slow = false;
    private boolean progress = false;
    private boolean allow;
    private int savedInvSlot = -1;
    private int savedHotbarSlot = -1;
    private long delay = -1L;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public FunTimeHelper() {
        super("FuntimeHelper", "Помощник для сервера Funtime", Category.Miscellaneous);
        addSettings(useDezor, useTrap, usePil, useSmerch, usePlast, useAura, useSnow);
    }


    Item swapItem;

    private void useItemAndClick(Item item, long delay) {
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

            if (allow && InventoryUtil.haveHotBar(Items.ENDER_PEARL)) {

                int hotbarSlot = slot - 36;

                mc.player.connection.sendPacket(new CHeldItemChangePacket(hotbarSlot));
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));

                delay = System.currentTimeMillis() + 150;
                allow = false;

            } else if (allow) {

                MoveComponent.stopTicks = 1;
                MoveComponent.stop = true;

                if (!MoveUtil.isMoving()) {

                    int hotbarSlot = mc.player.inventory.currentItem % 8 + 1;

                    savedInvSlot = slot;
                    savedHotbarSlot = hotbarSlot;

                    mc.playerController.windowClick(
                            0,
                            savedInvSlot,
                            savedHotbarSlot,
                            ClickType.SWAP,
                            mc.player
                    );

                    mc.player.connection.sendPacket(new CHeldItemChangePacket(hotbarSlot));
                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                    mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));

                    delay = System.currentTimeMillis() + 200;

                    if (mc.currentScreen == null) {
                        mc.player.connection.sendPacket(new CCloseWindowPacket());
                    }

                    allow = false;
                }
            }
        }

        if (delay >= 0L && System.currentTimeMillis() >= delay) {

            if (savedInvSlot != -1 && savedHotbarSlot != -1) {

                MoveComponent.stopTicks = 1;
                MoveComponent.stop = true;

                if (!MoveUtil.isMoving()) {

                    mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));

                    mc.playerController.windowClick(
                            0,
                            savedInvSlot,
                            savedHotbarSlot,
                            ClickType.SWAP,
                            mc.player
                    );

                    if (mc.currentScreen == null) {
                        mc.player.connection.sendPacket(new CCloseWindowPacket());
                    }

                    savedInvSlot = -1;
                    savedHotbarSlot = -1;
                    delay = -1L;
                    swapItem = null;
                    progress = false;
                    canUse = false;
                }

            } else {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                delay = -1L;
                swapItem = null;
                progress = false;
                canUse = false;
            }
        }


    }

    @EventTarget
    public void onKey(EventKey e) {
        if (mc.currentScreen == null && mc.player != null) {
            if (canUse) return;
            boolean keyWasPressed = useDezor.get() == e.getKey() ||
                    useTrap.get() == e.getKey() ||
                    usePil.get() == e.getKey() ||
                    useSmerch.get() == e.getKey() ||
                    usePlast.get() == e.getKey() ||
                    useAura.get() == e.getKey() ||
                    useSnow.get() == e.getKey();

            if (keyWasPressed) {
                this.slow = true;
            }

            long sleep = 0;
            if (e.isHold()) {
                if (useDezor.get() == e.getKey()) {
                    useItemAndClick(Items.ENDER_EYE, sleep);
                }

                if (useTrap.get() == e.getKey()) {
                    useItemAndClick(Items.NETHERITE_SCRAP, sleep);
                }

                if (usePil.get() == e.getKey()) {
                    useItemAndClick(Items.SUGAR, sleep);
                }

                if (useSmerch.get() == e.getKey()) {
                    useItemAndClick(Items.FIRE_CHARGE, sleep);
                }

                if (usePlast.get() == e.getKey()) {
                    useItemAndClick(Items.DRIED_KELP, sleep);
                }

                if (this.useAura.get() == e.getKey()) {
                    useItemAndClick(Items.PHANTOM_MEMBRANE, sleep);
                }

                if (useSnow.get() == e.getKey()) {
                    useItemAndClick(Items.SNOWBALL, sleep);
                }
            }
        }
    }


    @EventTarget
    public void onUpdate(EventUpdate e) {

        this.slow = false;
    }


}
