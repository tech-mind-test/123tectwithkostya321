package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ShulkerBoxScreen;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.network.play.client.CClickWindowPacket;
import org.lwjgl.glfw.GLFW;
import sky.core.events.*;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.modules.api.constructors.impl.TextSetting;
import sky.core.modules.impl.visuals.ShulkerPreview;
import sky.core.ui.Interface.elements.impl.HotBarRender;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.misc.ChatUtil;
import sky.core.utils.misc.ServerUtil;
import sky.core.utils.player.InventoryUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class ItemHelper extends Module {
    private final BindSetting chorus = new BindSetting("Хорус");
    private final BindSetting golden_apple = new BindSetting("Золотое яблоко");
    private final BindSetting enchant_golden_apple = new BindSetting("Чарка");
    private final BindSetting trident = new BindSetting("Трезубец");
    private final BindSetting exp = new BindSetting("Пузырек опыта");
    private final BindSetting shield = new BindSetting("Арбалет");
    private final BindSetting instant_health = new BindSetting("Лук");

    private final TextSetting health = new TextSetting("Здоровье");

    private final BooleanSetting show_instant_health = new BooleanSetting("Подсвечивать зелье исцеления", false);
    private final ColorSetting color_instant_health = new ColorSetting("Цвет подсветки зелья исцеления", true, ColorUtil.hex("#FF2AB9"), show_instant_health::get);
    private final BooleanSetting show_enchant_golden_apple = new BooleanSetting("Подсвечивать чарки", false);
    private final ColorSetting color_enchant_golden_apple = new ColorSetting("Цвет подсветки чарки", true, ColorUtil.hex("#FFAC93"), show_enchant_golden_apple::get);
    private final BooleanSetting show_golden_apple = new BooleanSetting("Подсвечивать золотые яблоки", false);
    private final ColorSetting color_golden_apple = new ColorSetting("Цвет подсветки золотого яблока", true, ColorUtil.hex("#E7EB56"), show_golden_apple::get);

    private final TextSetting other = new TextSetting("Остальное");
    private final BooleanSetting tes1 = new BooleanSetting("Открывать шалкер в кд", false);

    private final BooleanSetting decreaseCooldown = new BooleanSetting("Уменьшать задержку на предметы", false);
    private final BooleanSetting show_new_items = new BooleanSetting("Подсвечивать только что поднятые предметы", false);
    private final BooleanSetting show_nbt = new BooleanSetting("Отображать nbt предметов", false);

    private final AnimationUtil anim = new AnimationUtil(0.0f, 4);
    private int oldSlot = -1;

    public ItemHelper() {
        super("Item Helper", "э?", Category.Miscellaneous);
        addSettings(shield, instant_health, health, show_instant_health, color_instant_health, show_enchant_golden_apple, color_enchant_golden_apple, show_golden_apple, color_golden_apple, other, decreaseCooldown, show_nbt, tes1);
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (tes1.get()) {
            if (mc.currentScreen instanceof ShulkerBoxScreen && e.isSend() && e.getPacket() instanceof CClickWindowPacket packet && ServerUtil.isPvP()) {
                if (packet.getClickType() == ClickType.QUICK_MOVE) {
                    mc.player.connection.sendPacket(new CClickWindowPacket(mc.player.openContainer.windowId, packet.getSlotId(), InventoryUtil.getSlot(Items.AIR), ClickType.SWAP, ItemStack.EMPTY, mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;

        int bind = instant_health.get();
        boolean isBindingPressed = bind >= 0 && GLFW.glfwGetKey(mc.getMainWindow().getHandle(), bind) == GLFW.GLFW_PRESS;

        if (isBindingPressed) {
            int bowSlot = InventoryUtil.getSlot(Items.BOW);
            if (bowSlot == -1) return;

            if (oldSlot == -1) {
                oldSlot = mc.player.inventory.currentItem;
            }

            if (mc.player.inventory.currentItem != bowSlot) {
                if (bowSlot < 9) {
                    mc.player.inventory.currentItem = bowSlot;
                } else {
                    InventoryUtil.useItemLegit(this, Items.BOW);
                }
            }

            mc.gameSettings.keyBindUseItem.setPressed(true);

            if (mc.player.getItemInUseMaxCount() >= 20) {
                performShot();
            }

        } else {
            if (oldSlot != -1) {
                if (mc.player.getItemInUseMaxCount() > 0) {
                    performShot();
                } else {
                    stopDrawing();
                }
            }
        }
    }

    private void performShot() {
        if (mc.player == null) return;

        mc.playerController.onStoppedUsingItem(mc.player);

        stopDrawing();
    }

    private void stopDrawing() {
        mc.gameSettings.keyBindUseItem.setPressed(false);
        if (oldSlot != -1) {
            mc.player.inventory.currentItem = oldSlot;
            oldSlot = -1;
        }
    }

    @EventTarget
    public void onEvent(EventCooldown event) {
        Item item = event.getItem();
        ItemStack itemStack = new ItemStack(item);
        if (decreaseCooldown.get() && itemStack.isFood()) {
            int reduction = item.getFood().isFastEating() ? 16 : 32;
            int originalTicks = event.getTicks();

            if (originalTicks > reduction && itemStack.getItem() != Items.DRIED_KELP) {
                event.setTicks(originalTicks - reduction);
                IFormattableTextComponent message = new StringTextComponent("Задержка на ").append(new StringTextComponent(item.getName().getString())).append(new StringTextComponent(" уменьшена на ~" + (reduction / 20.0) + " секунды")).setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY));
                ChatUtil.addText(message);
            }
        }
    }

    @EventTarget
    public void onEvent(EventContainerRender.Pre event) {
        if (mc.currentScreen instanceof CreativeScreen || !(event.getContainer() instanceof PlayerContainer)) return;

        for (Slot slot : event.getContainer().inventorySlots) {
            if (slot == null || !slot.getHasStack()) continue;
            renderItemHighlight(event.getStack(), slot.getStack(), event.getGuiLeft() + slot.xPos, event.getGuiTop() + slot.yPos);
        }
    }

    @EventTarget
    public void onEvent(EventHotbarRender event) {
        if (mc.currentScreen instanceof CreativeScreen) return;

        int screenWidth = mc.getMainWindow().getScaledWidth();
        int screenHeight = mc.getMainWindow().getScaledHeight();
        int baseY = HotBarRender.shouldReplace()
                ? HotBarRender.getItemY(screenHeight)
                : screenHeight - 16 - 3;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.mainInventory.get(i);
            if (stack.isEmpty()) continue;
            int slotX = HotBarRender.shouldReplace()
                    ? HotBarRender.getSlotX(screenWidth, i)
                    : screenWidth / 2 - 90 + i * 20 + 2;
            renderItemHighlight(event.getStack(), stack, slotX, baseY);
        }
    }

    private void renderItemHighlight(MatrixStack matrixStack, ItemStack stack, float x, float y) {
        int color = getItemHighlightColor(stack);
        if (color != 0) {
            float blinkFactor = (float) (Math.sin(System.currentTimeMillis() / 100) * 0.4 + 0.6f);
            int alpha = (int) (150 * blinkFactor);

            RenderUtil.drawMinecraftRectangle(matrixStack, x, y, 16, 16, ColorUtil.getColorWithAlpha(color, alpha));
        }
    }

    private int getItemHighlightColor(ItemStack stack) {
        if (show_enchant_golden_apple.get() && stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            return color_enchant_golden_apple.get();
        }
        if (show_golden_apple.get() && stack.getItem() == Items.GOLDEN_APPLE) {
            return color_golden_apple.get();
        }
        if (show_instant_health.get() && InventoryUtil.stackHasAnyEffect(stack, true, false, false, Effects.INSTANT_HEALTH)) {
            return color_instant_health.get();
        }
        return 0;
    }

    @EventTarget
    public void onEvent(EventRenderTooltip event) {
        if (ShulkerPreview.shouldShowPreview(event.stack)) return;
        if (!show_nbt.get() || !event.stack.hasTag()) return;
        event.setCancelled(true);

        List<ITextComponent> tooltip = mc.currentScreen.getTooltipFromItem(event.stack);
        tooltip.add(StringTextComponent.EMPTY);
        addTag(tooltip, event.stack.getTag(), 0);

        mc.currentScreen.func_243308_b(event.matrixStack, tooltip, event.mouseX, event.mouseY);
    }

    private void addTag(List<ITextComponent> tooltip, CompoundNBT tag, int depth) {
        String indent = "  ".repeat(depth);
        for (String key : tag.keySet()) {
            INBT base = tag.get(key);
            String type = getNbtType(base);
            tooltip.add(new StringTextComponent(indent + "- " + type + ": " + key).mergeStyle(TextFormatting.DARK_GRAY));
        }
    }

    private String getNbtType(INBT nbt) {
        if (nbt instanceof ByteNBT) return "byte";
        if (nbt instanceof ShortNBT) return "short";
        if (nbt instanceof IntNBT) return "int";
        if (nbt instanceof LongNBT) return "long";
        if (nbt instanceof FloatNBT) return "float";
        if (nbt instanceof DoubleNBT) return "double";
        if (nbt instanceof StringNBT) return "string";
        if (nbt instanceof ByteArrayNBT) return "byte[]";
        if (nbt instanceof IntArrayNBT) return "int[]";
        if (nbt instanceof LongArrayNBT) return "long[]";
        if (nbt instanceof ListNBT) return "list";
        if (nbt instanceof CompoundNBT) return "compound";
        return "unknown";
    }
}