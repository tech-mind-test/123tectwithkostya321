package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventInput;
import sky.core.events.EventKey;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.player.InventoryUtil;
import sky.core.utils.player.MoveUtil;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.network.play.client.CEntityActionPacket;


public class ElytraHelper extends Module {
    private final BindSetting keySwap = new BindSetting("Кнопка свапа");
    private final BooleanSetting equipchestplate = new BooleanSetting("Снимать элитры при приземлении", false, () -> keySwap.get() != -1);
    private final BindSetting fireworkKey = new BindSetting("Кнопка фейера");
    private final BooleanSetting autoFly = new BooleanSetting("Автоматически взлетать", false);
    private final BooleanSetting autoJump = new BooleanSetting("Авто прыжок", false);
    private final BooleanSetting usefirework = new BooleanSetting("Автоматически использовать феерверк", false, autoFly::get);
    private final BooleanSetting fireworkOffhand = new BooleanSetting("Фейер в левую руку", false);

    private boolean swapRequested = false;
    private boolean fireworkUsed = false;
    private boolean wasOnGround = false;
    private boolean wasInElytraFlight = false;
    private int tickswap;
    private boolean hasFiredOnStart = false;

    public ElytraHelper() {
        super("Elytra Helper", "Позволяет быстро свапать элитру или нагрудник", Category.Player);
        addSettings(keySwap, equipchestplate, fireworkKey, fireworkOffhand, autoFly, autoJump, usefirework);
    }

    @EventTarget
    public void onTick(EventUpdate event) {
        if (tickswap > 0) tickswap--;
        if (swapRequested && !MoveUtil.isMoving()) {
            executeSwap();
            swapRequested = false;
            mc.player.connection.sendPacket(new CCloseWindowPacket(mc.player.openContainer.windowId));
        }
        if (mc.player.isElytraFlying()) wasInElytraFlight = true;
        if (equipchestplate.get()) {
            if (mc.player.isOnGround() && !wasOnGround && wasInElytraFlight) {
                if (mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA) {
                    if (InventoryUtil.getChestPlateSlot() != -1) {
                        InventoryUtil.moveToArmor(InventoryUtil.getChestPlateSlot(), 6);
                    } else {
                        InventoryUtil.pickupItem(6, 0);
                        if (InventoryUtil.findEmptyInventorySlot() != -1) {
                            InventoryUtil.pickupItem(InventoryUtil.findEmptyInventorySlot(), 0);
                        }
                    }
                    wasInElytraFlight = false;
                }
            }
            wasOnGround = mc.player.isOnGround();
        }

        // Авто прыжок (как в SkyCore src new) — отдельной настройкой
        if (autoJump.get()
                && !mc.player.abilities.isFlying
                && mc.player.isOnGround()
                && mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA
                && !mc.gameSettings.keyBindJump.isKeyDown()
                && !mc.player.isInWater()
                && !mc.player.isInLava()) {
            mc.player.jump();
        }

        // Автовзлёт (как в SkyCore src new)
        if (autoFly.get()
                && !mc.player.abilities.isFlying
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && !mc.player.isOnGround()
                && !mc.player.isElytraFlying()
                && mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA
                && ElytraItem.isUsable(mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST))) {
            mc.player.startFallFlying();
            mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
            wasInElytraFlight = true;

            if (usefirework.get() && !hasFiredOnStart) {
                if (InventoryUtil.getSlot(Items.FIREWORK_ROCKET) != -1) {
                    InventoryUtil.inventorySwapClick(Items.FIREWORK_ROCKET, fireworkOffhand.get());
                    hasFiredOnStart = true;
                }
            }
        }

        // Сброс "одиночного" фейера на старте
        if (mc.player.isOnGround() || mc.player.isInWater() || mc.player.isInLava()) {
            hasFiredOnStart = false;
            fireworkUsed = false;
        }
    }

    @EventTarget
    public void onEvent(EventKey event) {
        if (!event.isHold()) return;
        if (event.getKey() == keySwap.get()) {
            boolean hasElytra = InventoryUtil.getSlot(Items.ELYTRA) != -1;
            boolean hasChestplate = InventoryUtil.getChestPlateSlot() != -1;
            if (!hasElytra && !hasChestplate) return;
            tickswap = 2;
            swapRequested = true;
        }
        if (event.getKey() == fireworkKey.get()) {
            InventoryUtil.inventorySwapClick(Items.FIREWORK_ROCKET, fireworkOffhand.get());
        }
    }

    @EventTarget
    private void onEvent(EventInput eventInput) {
        if (tickswap > 0) {
            eventInput.setForward(0);
            eventInput.setStrafe(0);
            eventInput.setSneak(false);
            eventInput.setJump(false);
        }
    }


    private void executeSwap() {
        int targetSlot;
        if (mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA) {
            targetSlot = InventoryUtil.getChestPlateSlot();
        } else {
            targetSlot = InventoryUtil.getSlot(Items.ELYTRA);
        }

        if (targetSlot == -1) return;
        InventoryUtil.moveItemtest(targetSlot, 6);
    }
}