package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SOpenWindowPacket;
import net.minecraft.util.Hand;
import sky.core.events.EventInput;
import sky.core.events.EventKey;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.misc.ChatUtil;
import sky.core.utils.misc.ServerUtil;
import sky.core.utils.player.InventoryUtil;
import sky.core.utils.player.MoveUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ReallyWorldHelper extends Module {

    private final BooleanSetting close = new BooleanSetting("Закрывать меню", false);
    private final BooleanSetting filter = new BooleanSetting("Фильтр чата", false);
    private final BindSetting antiPoletKey = new BindSetting("Анти Полет");
    private final BooleanSetting bindTrap = new BooleanSetting("Бинд на трапку", false);
    private final BindSetting bindTrapKey = new BindSetting("Кнопка трапки", bindTrap::get);
    private final BooleanSetting bindTrapLegit = new BooleanSetting("Легитный", false, bindTrap::get);
    private final BooleanSetting grimBypass = new BooleanSetting("Обход Grim", true);
    private boolean swapRequested = false;
    private int tickSwap = 0;
    private int queuedReturnSlot = -1;
    private int returnDelayTicks = 0;
    private boolean returnSwapRequested = false;

    private final Set<String> bannedWords = new HashSet<>(Arrays.asList(
            "акриен(а|у|ом|е|чик)?", "рич(а|у|ом|ей|е)?", "ньюкод(ом|а|у|ами|ик|е)?",
            "экспенсив(ом|а|у|ами|е)?", "импакт(ом|а|у|ами|ик|е)?", "экселлент(ом|а|у|ами|ик|е)?",
            "экселент(ом|а|у|ами|ик)?", "катлаван(ом|а|у|ами|чик)?", "катлован(ом|а|у|ами|чик)?",
            "целестиал(ом|а|у|ами|е)?", "целк(ой|а|у|ами|очка|е)?", "матикс(ом|а|у|ами|е)?",
            "инерти(я|ей|ю|ями|е)?", "эксп(а|ой|ою|у|уличка|е)?", "флюгер(ом|а|у|ами)?",
            "рикер(а|у|ом|очек)?", "фанпе(й|ю|я|ем|е|йчик)?", "вексайд(ом|а|у|ами|ик|е)?",
            "нурсултан(а|у|е|ом|чик)?", "нурик(а|у|ом|е)?", "нурлан(а|у|ом|чик|е)?",
            "векс(ом|у|а|ами|ик|е)?", "релейк(ом|у|а|ами|е)?", "арбуз(ом|а|у|ами|ик|е)?",
            "вилд(ом|у|а|ами|ик|е)?", "фантайм(е|а|у)?", "холик(е|а|у)?",
            "холиворлд(а|у|е)?", "рокстар(ом|а|у|ами|чик|е)?", "рогалик(а|у|ом|е)?",
            "тандерхак(ом|у|и|ами|а|е)?", "ликвидбаунс(а|у|ами|е)?",
            "expensive", "celestial", "newcode", "arbuz", "akrien", "nursultan", "relake", "wild", "wurst", "catlovan",
            "excellent", "rockstar", "catlavan", "impact", "matix", "inertia", "wex", "wexside", "nurik", "nurlan",
            "rich", "funpay", "fluger", "riker", "funtime", "holyworld", "wwe", "hvh", "rogalik", "thunderhack",
            "liquidbounce"
    ));


    public ReallyWorldHelper() {
        super("Really World Helper", "У данного модуля нет описания!", Category.Miscellaneous);
        bindTrapKey.set(-98);
        addSettings(close, filter, antiPoletKey, bindTrap, bindTrapKey, bindTrapLegit, grimBypass);
    }


    @EventTarget
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof SOpenWindowPacket packet && close.get()) {
            if (packet.getTitle().getString().contains("ꈁꀀꈂꌁꈂꀁ§0ꈃꄀ") && mc.player.ticksExisted < 100) {
                mc.player.connection.sendPacket(new CCloseWindowPacket(packet.getWindowId()));
                e.setCancelled(true);
            }
        }
        if (e.getPacket() instanceof CChatMessagePacket && filter.get()) {
            String message = ((CChatMessagePacket) e.getPacket()).getMessage().toLowerCase();
            boolean banwords = false;
            for (String pattern : bannedWords) {
                if (message.matches(".*" + pattern + ".*")) {
                    banwords = true;
                    break;
                }
            }
            if (banwords) {
                e.setCancelled(true);
                ChatUtil.addText("В вашем сообщении было найдено запретное слово, отправка сообщения отменена!");
            }
        }
    }

    @EventTarget
    private void onKey(EventKey event) {
        if (event == null || !event.isHold() || mc.player == null) return;
        if (event.getKey() == antiPoletKey.get()) {
            tickSwap = 2;
            swapRequested = true;
        }
        if (bindTrap.get() && event.getKey() == bindTrapKey.get()) {
            handleBindTrap();
        }
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.player.connection == null) return;

        if (tickSwap > 0) {
            tickSwap--;
        }

        if (returnDelayTicks > 0) {
            returnDelayTicks--;
            if (returnDelayTicks == 0 && queuedReturnSlot != -1) {
                returnSwapRequested = true;
                tickSwap = 2;
            }
        }

        if (swapRequested && (!grimBypass.get() || !MoveUtil.isMoving())) {
            executeAntiPolet();
            swapRequested = false;
        }

        if (returnSwapRequested && queuedReturnSlot != -1 && (!grimBypass.get() || !MoveUtil.isMoving())) {
            InventoryUtil.moveItem(45, queuedReturnSlot);
            returnSwapRequested = false;
            queuedReturnSlot = -1;
        }
    }

    @EventTarget
    private void onInput(EventInput eventInput) {
        if (eventInput == null) return;
        if (grimBypass.get() && (tickSwap > 0 || returnSwapRequested)) {
            eventInput.setForward(0);
            eventInput.setStrafe(0);
            eventInput.setSneak(false);
            eventInput.setJump(false);
        }
    }

    private void executeAntiPolet() {
        if (InventoryUtil.getSlot(Items.FIREWORK_STAR) == -1) {
            ChatUtil.addText("Нема анти полета падла!");
            return;
        }
        int slot = InventoryUtil.antipoletToOffhand(Items.FIREWORK_STAR);
        if (slot == -1) {
            return;
        }
        queuedReturnSlot = slot;
        returnDelayTicks = 6;
    }

    private void handleBindTrap() {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.getCooldownTracker().hasCooldown(Items.HEART_OF_THE_SEA) || InventoryUtil.getItemSlot(Items.HEART_OF_THE_SEA) == -1) {
            return;
        }
        if (bindTrapLegit.get()) {
            InventoryUtil.inventorySwapClick(Items.HEART_OF_THE_SEA);
            return;
        }

        if (ServerUtil.isConnectedToServer("funtime") || ServerUtil.isConnectedToServer("spooky")) {
            int hbSlot = findItem(Items.HEART_OF_THE_SEA, true);
            int invSlot = findItem(Items.HEART_OF_THE_SEA, false);
            int slot = findAndThrowItem(hbSlot, invSlot);
            if (slot > 8) {
                mc.playerController.pickItem(slot);
            }
            useItem(Hand.MAIN_HAND);
        } else {
            InventoryUtil.inventorySwapClick(Items.HEART_OF_THE_SEA);
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
            return hbSlot;
        }
        if (invSlot != -1) {
            mc.playerController.pickItem(invSlot);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            return invSlot;
        }
        return -1;
    }

    private void useItem(Hand hand) {
        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
    }
}
