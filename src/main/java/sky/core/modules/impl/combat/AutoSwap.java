package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.text.ITextComponent;
import sky.core.events.EventKey;
import sky.core.events.EventUpdate;
import sky.core.utils.managers.impl.notificationmanager.NotificationManager;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.player.InventoryUtil;

import java.util.List;

public class AutoSwap extends Module {
    private final BooleanSetting handMode = new BooleanSetting("Рука", true);
    private final BooleanSetting headMode = new BooleanSetting("Голова", false);
    private final BindSetting swapKey = new BindSetting("Свап (рука)", handMode::get);
    private final ModeSetting itemType = new ModeSetting("Свапать с", "Щит", handMode::get, "Щит", "Геплы", "Тотем", "Шар ");
    private final ModeSetting swapType = new ModeSetting("Свапать на", "Геплы", handMode::get, "Геплы", "Щит", "Тотем", "Шар ");
    private final BindSetting swapKeyHead = new BindSetting("Свап (голова)", headMode::get);
    private final ModeSetting itemTypeHead = new ModeSetting("Свапать c", "Шар ", headMode::get, "Шар ", "Шлем");
    private final ModeSetting swapTypeHead = new ModeSetting("Свапать нa", "Шлем", headMode::get, "Шлем", "Шар ");

    private final BooleanSetting totemSwap = new BooleanSetting("Свап топора на меч", true);
    private final BooleanSetting notif = new BooleanSetting("Отображать свап предметов", true);

    private final TimeUtil stopWatch = new TimeUtil();
    private final TimeUtil attackSpeedTimer = new TimeUtil();

    private boolean hadAttackSpeedRecently = false;
    private int storedAxeSlot = -1;
    private ItemStack lastOffhandStack = ItemStack.EMPTY;
    private boolean lastAttackSpeedResult = false;
    private boolean isSwapping = false;

    public AutoSwap() {
        super("AutoSwap", "У этого модуля нет описания!", Category.Combat);
        addSettings(handMode, headMode, swapKey, itemType, swapType, swapKeyHead, itemTypeHead, swapTypeHead, totemSwap, notif);
    }

    @EventTarget
    public void onEvent(EventKey event) {
        if (mc.player == null || mc.world == null || !event.isHold() || isSwapping || !stopWatch.isReached(300L)) {
            return;
        }

        int key = event.getKey();
        if (handMode.get() && swapKey.get() >= 0 && key == swapKey.get()) {
            handleHandSwap();
        }

        if (headMode.get() && swapKeyHead.get() >= 0 && key == swapKeyHead.get()) {
            handleHelmetSwap();
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        handleAttackSpeedLogic();
    }

    private void handleHandSwap() {
        isSwapping = true;

        ItemStack offhandStack = mc.player.getHeldItemOffhand();
        Item selectedItem = getItemByType(itemType.get());
        Item swapItem = getItemByType(swapType.get());

        if (offhandStack.getItem() == selectedItem) {
            int slot = getInventorySlot(swapItem);
            if (slot >= 0) {
                int invSlot = slot < 9 ? slot + 36 : slot;
                InventoryUtil.swapHand(invSlot, net.minecraft.util.Hand.OFF_HAND, false);
                notifySwap(mc.player.getHeldItemOffhand());
                stopWatch.reset();
            }
        } else {
            int slot = getInventorySlot(selectedItem);
            if (slot >= 0) {
                int invSlot = slot < 9 ? slot + 36 : slot;
                InventoryUtil.swapHand(invSlot, net.minecraft.util.Hand.OFF_HAND, false);
                notifySwap(mc.player.getHeldItemOffhand());
                stopWatch.reset();
            }
        }

        isSwapping = false;
    }

    private void handleHelmetSwap() {
        isSwapping = true;

        ItemStack helmetStack = mc.player.inventory.armorInventory.get(3);
        Item selectedItem = getItemByType(itemTypeHead.get());
        Item swapItem = getItemByType(swapTypeHead.get());

        if (helmetStack.getItem() == selectedItem) {
            int slot = getInventorySlot(swapItem);
            if (slot >= 0) {
                InventoryUtil.moveItemtest(slot, 5);
                notifySwap(mc.player.inventory.armorInventory.get(3));
                stopWatch.reset();
            }
        } else {
            int slot = getInventorySlot(selectedItem);
            if (slot >= 0) {
                InventoryUtil.moveItemtest(slot, 5);
                notifySwap(mc.player.inventory.armorInventory.get(3));
                stopWatch.reset();
            }
        }

        isSwapping = false;
    }

    private void handleAttackSpeedLogic() {
        ItemStack offhandItemStack = mc.player.getHeldItemOffhand();
        if (!ItemStack.areItemStacksEqual(offhandItemStack, lastOffhandStack)) {
            lastAttackSpeedResult = hasAttackSpeedDescription(offhandItemStack);
            lastOffhandStack = offhandItemStack.copy();
        }

        if (lastAttackSpeedResult) {
            hadAttackSpeedRecently = true;
            attackSpeedTimer.reset();
        } else if (attackSpeedTimer.isReached(500L)) {
            hadAttackSpeedRecently = false;
        }

        if (!totemSwap.get() || !stopWatch.isReached(300L)) {
            return;
        }

        ItemStack mainHand = mc.player.getHeldItemMainhand();
        if (mainHand.getItem() instanceof SwordItem && offhandItemStack.getItem() == Items.PLAYER_HEAD && lastAttackSpeedResult) {
            int axeSlot = getAxeSlot();
            if (axeSlot != -1) {
                mc.player.inventory.currentItem = axeSlot;
                stopWatch.reset();
            }
        }

        boolean hasAxeInHotbar = getAxeSlot() != -1;
        boolean hasSwordInHotbar = getSwordSlot() != -1;

        if (hasAxeInHotbar && hasSwordInHotbar) {
            if (!lastAttackSpeedResult && hadAttackSpeedRecently && mainHand.getItem() instanceof AxeItem) {
                storedAxeSlot = mc.player.inventory.currentItem;
                int swordSlot = getSwordSlot();
                if (swordSlot != -1) {
                    mc.player.inventory.currentItem = swordSlot;
                    stopWatch.reset();
                }
            } else if (lastAttackSpeedResult && mainHand.getItem() instanceof SwordItem && storedAxeSlot != -1) {
                int axeSlot = getAxeSlot();
                if (axeSlot != -1) {
                    mc.player.inventory.currentItem = axeSlot;
                    storedAxeSlot = -1;
                    stopWatch.reset();
                }
            }
        }
    }

    private void notifySwap(ItemStack stack) {
        if (notif.get() && !stack.isEmpty()) {
            String itemName = stack.getDisplayName().getString();
            String[] words = itemName.split(" ");
            if (words.length > 1) {
                String lastWord = words[words.length - 1];
                String beforeLast = String.join(" ", java.util.Arrays.copyOf(words, words.length - 1));
                NotificationManager.addNotification(stack, new net.minecraft.util.text.StringTextComponent(beforeLast + " " + lastWord + ""), true);
            } else {
                NotificationManager.addNotification(stack, new net.minecraft.util.text.StringTextComponent("" + itemName + ""), true);
            }
        }
    }

    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Щит" -> Items.SHIELD;
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Геплы" -> Items.GOLDEN_APPLE;
            case "Шар " -> Items.PLAYER_HEAD;
            case "Шлем" -> Items.NETHERITE_HELMET;
            default -> Items.AIR;
        };
    }

    private int getInventorySlot(Item item) {
        if (item == Items.AIR) return -1;
        for (int i = 0; i < 36; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int getSwordSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() instanceof SwordItem) {
                return i;
            }
        }
        return -1;
    }

    private int getAxeSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasAttackSpeedDescription(ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        if (itemStack.hasDisplayName() && itemStack.getDisplayName().getString().contains("Кобры")) return true;

        List<ITextComponent> tooltip = itemStack.getTooltip(mc.player, ITooltipFlag.TooltipFlags.NORMAL);
        for (ITextComponent component : tooltip) {
            String text = component.getString();
            if (text.contains("Скорости Атаки") || text.contains("Скорость атаки") || text.contains("Attack Speed")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        storedAxeSlot = -1;
        hadAttackSpeedRecently = false;
        isSwapping = false;
        lastOffhandStack = ItemStack.EMPTY;
        super.onDisable();
    }
}