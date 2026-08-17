package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.item.minecart.TNTMinecartEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import sky.core.utils.component.impl.MoveComponent;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.player.MoveUtil;

import java.util.List;
public class  AutoTotem extends Module {
    private static final int SWAP_COOLDOWN_TICKS = 5;
    private final SliderSetting health = new SliderSetting("Уровень здоровья", 4, 1, 20, 0.5F);
    private final SliderSetting elytraHealth = new SliderSetting("Здоровье на элитре", 8, 1, 20, 0.5F);
    private final BooleanSetting swapBack = new BooleanSetting("Возвращать предмет", true);
    private final BooleanSetting noBallSwitch = new BooleanSetting("Игнорировать проверки, если в руке шар", false);
    private final BooleanSetting saveEnchanted = new BooleanSetting("Сохранять зачарованный", true);
    private final BooleanSetting ignoreDragonidTotem = new BooleanSetting("Не брать тотем Драконида", true);
    private final BooleanSetting stop = new BooleanSetting("Синхронизировать с GuiMove", false);
    private final MultiBooleanSetting mode = new MultiBooleanSetting("Проверки на", new BooleanSetting("Золотые сердца", true), new BooleanSetting("Кристаллы", true), new BooleanSetting("Падение", true), new BooleanSetting("Динамит", true), new BooleanSetting("Трезубец", false));
    private final SliderSetting distancecrystall = new SliderSetting("Дистанция проверки на кристалл", 6, 1, 6, 1, () -> mode.is("Кристаллы"));
    private int nonEnchantedTotems, oldItem = -1, ticks;
    private ItemStack oldOffhandStack = ItemStack.EMPTY;
    private boolean totemLocked;
    int slotToSwap = -1;
    public AutoTotem() {
        super("Auto Totem", "У этого модуля нет описания!", Category.Combat);
        addSettings(health, elytraHealth, swapBack, noBallSwitch, saveEnchanted, ignoreDragonidTotem, stop, mode, distancecrystall);
    }
    @EventTarget
    private void totemSwap(EventUpdate e) {
        if (slotToSwap != -1) doSwap(slotToSwap);
    }
    @EventTarget
    private void onUpdate(EventUpdate event) {
        ticks++;
        Slot slot = findTotemSlotAndUpdateCount();
        if (isTotemLockActive() && !hasTotemInHand() && slot != null && ticks >= SWAP_COOLDOWN_TICKS && !isSwapBlockedByBall()) {
            doSwap(slot.slotNumber);
            return;
        }
        if (shouldReturnSavedItem(slot)) {
            int returnSlot = findReturnSlot();
            if (returnSlot != -1) {
                doSwap(returnSlot);
                return;
            }
        }
        if (ticks >= SWAP_COOLDOWN_TICKS) {
            if (!isSwapBlockedByBall() && !hasTotemInHand()) {
                if (mode.is("Кристаллы")) {
                    if (!mc.world.getEntitiesWithinAABB(EnderCrystalEntity.class, mc.player.getBoundingBox().grow(distancecrystall.get())).isEmpty() && slot != null) {
                        doSwap(slot.slotNumber);
                        return;
                    }
                }
                if (mode.is("Динамит")) {
                    if (!mc.world.getEntitiesWithinAABB(TNTEntity.class, mc.player.getBoundingBox().grow(8)).isEmpty() && slot != null) {
                        doSwap(slot.slotNumber);
                        return;
                    }
                    if (!mc.world.getEntitiesWithinAABB(TNTMinecartEntity.class, mc.player.getBoundingBox().grow(8)).isEmpty() && slot != null) {
                        doSwap(slot.slotNumber);
                        return;
                    }
                }
                if (mode.is("Трезубец")) {
                    List<TridentEntity> nearbyTridents = mc.world.getEntitiesWithinAABB(
                            TridentEntity.class,
                            mc.player.getBoundingBox().grow(10)
                    );
                    boolean isAnyTridentMoving = nearbyTridents.stream()
                            .anyMatch(trident ->
                                    trident.getPosX() != trident.prevPosX ||
                                            trident.getPosY() != trident.prevPosY ||
                                            trident.getPosZ() != trident.prevPosZ
                            );
                    if (isAnyTridentMoving) {
                        doSwap(slot.slotNumber);
                        return;
                    }
                }
            }
            if (needTotem()) {
                if (slot != null && !hasTotemInHand()) {
                    doSwap(slot.slotNumber);
                }
            } else if (canSwapBack()) {
                int returnSlot = findReturnSlot();
                if (returnSlot == -1) {
                    oldItem = -1;
                    oldOffhandStack = ItemStack.EMPTY;
                    slotToSwap = -1;
                    return;
                }
                if (mode.is("Кристаллы") && !mc.world.getEntitiesWithinAABB(EnderCrystalEntity.class, mc.player.getBoundingBox().grow(distancecrystall.get())).isEmpty()) {
                    slotToSwap = -1;
                    return;
                }
                if (mode.is("Динамит") && (!mc.world.getEntitiesWithinAABB(TNTMinecartEntity.class, mc.player.getBoundingBox().grow(8)).isEmpty() || !mc.world.getEntitiesWithinAABB(TNTEntity.class, mc.player.getBoundingBox().grow(8)).isEmpty())) {
                    slotToSwap = -1;
                    return;
                }
                if (mode.is("Трезубец")) {
                    List<TridentEntity> nearbyTridents = mc.world.getEntitiesWithinAABB(
                            TridentEntity.class,
                            mc.player.getBoundingBox().grow(10)
                    );
                    boolean isAnyTridentMoving = nearbyTridents.stream()
                            .anyMatch(trident ->
                                    trident.getPosX() != trident.prevPosX ||
                                            trident.getPosY() != trident.prevPosY ||
                                            trident.getPosZ() != trident.prevPosZ
                            );
                    if (isAnyTridentMoving) {
                        slotToSwap = -1;
                        return;
                    }
                }
                doSwap(returnSlot);
            }
        }
    }
    @EventTarget
    private void onPacket(EventPacket event) {
        if (!event.isReceive()) {
            return;
        }
        if (event.getPacket() instanceof SSpawnObjectPacket) {
            SSpawnObjectPacket packet = (SSpawnObjectPacket) event.getPacket();
            if (packet.getType() != EntityType.END_CRYSTAL) {
                return;
            }
            if (!mode.is("Кристаллы")) {
                return;
            }
            if (isSwapBlockedByBall()) {
                return;
            }
            if (mc.player.getDistance(new EnderCrystalEntity(mc.world, packet.getX(), packet.getY(), packet.getZ())) > distancecrystall.get() || hasTotemInHand()) {
                return;
            }
            Slot crystalSlot = findTotemSlotAndUpdateCount();
            if (crystalSlot == null) {
                return;
            }
            doSwap(crystalSlot.slotNumber);
        }
    }
    private boolean hasTotemInHand() {
        for (Hand hand : Hand.values()) {
            ItemStack stack = mc.player.getHeldItem(hand);
            if (isUsableTotem(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsableTotem(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.TOTEM_OF_UNDYING || isDragonidTotem(stack)) {
            return false;
        }
        return !saveEnchanted.get() || !stack.isEnchanted() || nonEnchantedTotems <= 0;
    }

    private boolean isDragonidTotem(ItemStack stack) {
        if (!ignoreDragonidTotem.get() || stack.isEmpty() || stack.getItem() != Items.TOTEM_OF_UNDYING) {
            return false;
        }
        return stack.getDisplayName().getString().contains("Тотем Драконида");
    }
    private boolean needTotem() {
        boolean blockByBall = isSwapBlockedByBall();
        if (!blockByBall && (mode.is("Падение") && !mc.player.isInWater() && !mc.player.isElytraFlying() && mc.player.fallDistance > 10)) {
            return true;
        }
        return getEffectiveHealth() <= getTotemHealthThreshold();
    }

    private boolean shouldReturnSavedItem(Slot totemSlot) {
        if (!swapBack.get() || oldItem == -1) {
            return false;
        }
        if (isSameReturnItem(mc.player.getHeldItemOffhand(), oldOffhandStack)) {
            oldItem = -1;
            oldOffhandStack = ItemStack.EMPTY;
            totemLocked = false;
            return false;
        }
        if (isTotemLockActive()) {
            return false;
        }
        return totemSlot == null && !isUsableTotem(mc.player.getHeldItemOffhand());
    }

    private boolean canSwapBack() {
        if (!totemLocked || oldItem == -1 || !swapBack.get()) {
            return false;
        }
        if (isCriticalHalfHeart()) {
            return false;
        }
        return getEffectiveHealth() >= getSwapBackHealthThreshold();
    }

    private boolean isTotemLockActive() {
        return totemLocked && getEffectiveHealth() < getSwapBackHealthThreshold();
    }

    private float getEffectiveHealth() {
        float currentHealth = mc.player.getHealth();
        if (mode.is("Золотые сердца") && !mc.player.isElytraFlying() && mc.player.isPotionActive(Effects.ABSORPTION)) {
            currentHealth += mc.player.getAbsorptionAmount();
        }
        return currentHealth;
    }

    private float getTotemHealthThreshold() {
        return mc.player.isElytraFlying() ? elytraHealth.get() : health.get();
    }

    private float getSwapBackHealthThreshold() {
        return getTotemHealthThreshold() + 2.0F;
    }

    private boolean isCriticalHalfHeart() {
        return mc.player.getHealth() <= 1.0F;
    }

    private int findReturnSlot() {
        if (isValidSwapSlot(oldItem) && isSameReturnItem(mc.player.openContainer.getSlot(oldItem).getStack(), oldOffhandStack)) {
            return oldItem;
        }
        if (oldOffhandStack.isEmpty()) {
            return isValidSwapSlot(oldItem) ? oldItem : -1;
        }
        for (Slot slot : mc.player.openContainer.inventorySlots) {
            if (slot.slotNumber == 45) {
                continue;
            }
            if (isSameReturnItem(slot.getStack(), oldOffhandStack)) {
                oldItem = slot.slotNumber;
                return slot.slotNumber;
            }
        }
        return -1;
    }

    private boolean isSameReturnItem(ItemStack current, ItemStack expected) {
        if (current.isEmpty() || expected.isEmpty()) {
            return false;
        }
        return current.getItem() == expected.getItem() && ItemStack.areItemStackTagsEqual(current, expected);
    }
    private Slot findTotemSlotAndUpdateCount() {
        nonEnchantedTotems = (int) mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.getStack().getItem() == Items.TOTEM_OF_UNDYING && !isDragonidTotem(s.getStack()) && !s.getStack().isEnchanted())
                .count();
        return mc.player.openContainer.inventorySlots.stream()
                .filter(s -> s.getStack().getItem() == Items.TOTEM_OF_UNDYING && !isDragonidTotem(s.getStack())
                        && (!saveEnchanted.get() || !s.getStack().isEnchanted() || nonEnchantedTotems <= 0))
                .findFirst()
                .orElse(null);
    }

    private void doSwap(int fromSlot) {
        if (!isValidSwapSlot(fromSlot)) {
            slotToSwap = -1;
            return;
        }

        ItemStack fromStack = mc.player.openContainer.getSlot(fromSlot).getStack();
        boolean swappingTotemToOffhand = fromStack.getItem() == Items.TOTEM_OF_UNDYING;
        boolean swappingSavedItemBack = oldItem != -1 && isSameReturnItem(fromStack, oldOffhandStack);

        if (MoveUtil.isMoving()) MoveComponent.stopTicks = 3;
        if (stop.get()) MoveComponent.stop = true;

        if (!MoveUtil.isMoving() || (!stop.get() && !mc.player.isSprinting())) {
            if (oldItem != -1 && oldItem == fromSlot) {
                oldItem = -1;
                oldOffhandStack = ItemStack.EMPTY;
                totemLocked = false;
            } else if (!mc.player.getHeldItemOffhand().isEmpty() && oldItem == -1) {
                oldItem = fromSlot;
                oldOffhandStack = mc.player.getHeldItemOffhand().copy();
            }
            mc.playerController.windowClick(mc.player.openContainer.windowId, fromSlot, 40, ClickType.SWAP, mc.player);
            if (swappingTotemToOffhand) {
                totemLocked = true;
            } else if (swappingSavedItemBack) {
                totemLocked = false;
            }
            slotToSwap = -1;
            ticks = 0;
        } else {
            slotToSwap = fromSlot;
        }
    }

    private boolean isValidSwapSlot(int slot) {
        return slot >= 0 && slot < mc.player.openContainer.inventorySlots.size();
    }
    private boolean isSwapBlockedByBall() {
        return noBallSwitch.get()
                && mc.player.getHeldItemOffhand().getItem() == Items.PLAYER_HEAD
                && !(mode.is("Падение") && mc.player.fallDistance > 5);
    }
    @java.lang.Override
    public void onDisable() {
        oldItem = -1;
        oldOffhandStack = ItemStack.EMPTY;
        totemLocked = false;
        ticks = 0;
        super.onDisable();
    }
}