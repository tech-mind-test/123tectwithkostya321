package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import sky.core.events.EventKey;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.misc.ServerUtil;
import sky.core.utils.player.InventoryUtil;

public class ClickPearl extends Module {
    public BindSetting clickKey = new BindSetting("Кнопка");
    public BooleanSetting legit = new BooleanSetting("Легитный", false);
    private long delay;

    public ClickPearl() {
        super("ClickPearl", "Позволяет мгновенно бросать эндер-жемчуг, нажатием на определенную кнопку", Category.Player);
        clickKey.set(-98);
        addSettings(clickKey, legit);
    }

    @EventTarget
    private void onKey(EventKey e) {
        if (mc.player == null || mc.world == null) return;
        if (e.getKey() == clickKey.get()) {
            handleClickPearl();
        }
    }

    private void handleClickPearl() {
        if (mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL) || InventoryUtil.getItemSlot(Items.ENDER_PEARL) == -1) {
            return;
        }
        if (legit.get()) {
            InventoryUtil.inventorySwapClick(Items.ENDER_PEARL, false);
            return;
        }

        if (ServerUtil.isConnectedToServer("funtime") || ServerUtil.isConnectedToServer("spooky")) {
            int hbSlot = findItem(Items.ENDER_PEARL, true);
            int invSlot = findItem(Items.ENDER_PEARL, false);
            int slot = findAndThrowItem(hbSlot, invSlot);
            if (slot > 8) {
                mc.playerController.pickItem(slot);
            }
            useItem(Hand.MAIN_HAND);
        } else {
            InventoryUtil.inventorySwapClick(Items.ENDER_PEARL, false);
            useItem(Hand.MAIN_HAND);
        }
    }

    private int findItem(Item item, boolean hotbarOnly) {
        for (int i = hotbarOnly ? 0 : 9; i < (hotbarOnly ? 9 : 36); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findAndThrowItem(int hbSlot, int invSlot) {
        if (hbSlot != -1) {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
            this.delay = System.currentTimeMillis();
            return hbSlot;
        }
        if (invSlot != -1) {
            mc.playerController.pickItem(invSlot);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            this.delay = System.currentTimeMillis();
            return invSlot;
        }
        return -1;
    }

    private void useItem(Hand hand) {
        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
    }
}
