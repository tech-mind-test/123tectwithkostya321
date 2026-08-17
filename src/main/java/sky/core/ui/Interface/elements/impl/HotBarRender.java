package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.settings.AttackIndicatorStatus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import other.bot.Bot;
import other.bot.BotManager;
import sky.core.SkyCore;
import sky.core.events.EventHotbarRender;
import com.darkmagician6.eventapi.EventManager;
import sky.core.modules.Module;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.Wrapper;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import net.optifine.CustomItems;

public final class HotBarRender implements Wrapper {

    public static final int HOTBAR_WIDTH = 182;
    public static final int HOTBAR_HEIGHT = 22;
    public static final int SLOT_WIDTH = 20;
    public static final int SLOT_ITEM_PADDING = 2;
    public static final int GAP_ABOVE_VANILLA = 2;
    private static final float ITEM_SCALE = 0.72f;
    private static final float BORDER_RADIUS = 6;
    private static final float SELECTION_RADIUS = 6f;
    private static final float SEPARATOR_WIDTH = 0.75f;
    private static final float SEPARATOR_HEIGHT = 21f;
    private static final int SLOT_LABEL_FONT_SIZE = 13;
    private static final AnimationUtil selectionSlotAnim = new AnimationUtil(0f, 14f, Easings.CUBIC_OUT);

    private HotBarRender() {
    }

    public static boolean shouldReplace() {
        try {
            Module module = SkyCore.getInstance().getModuleManager().getModule(sky.core.modules.impl.visuals.Interface.class);
            return module != null && module.isEnabled()
                    && sky.core.modules.impl.visuals.Interface.elements.is("Хотбар") != null
                    && sky.core.modules.impl.visuals.Interface.elements.is("Хотбар");
        } catch (Exception ignored) {
            return false;
        }
    }

    public static int getBarY(int screenHeight) {
        return screenHeight - 22 - GAP_ABOVE_VANILLA;
    }

    public static int getItemY(int screenHeight) {
        int barY = getBarY(screenHeight);
        float itemSize = 16f * ITEM_SCALE;
        return barY + Math.round((HOTBAR_HEIGHT - itemSize) / 2f);
    }

    public static float getSlotCellX(int screenWidth, float slot) {
        return screenWidth / 2f - 91f + slot * SLOT_WIDTH;
    }

    public static int getSlotX(int screenWidth, int slot) {
        return Math.round(getSlotCellX(screenWidth, slot) + SLOT_ITEM_PADDING);
    }

    private static float getSelectionWidth(float slot) {
        return slot >= 7.5f ? SLOT_WIDTH + 2f : SLOT_WIDTH + 1f;
    }

    private static int getSelectionCornerSlot(float animatedSlot, int targetSlot) {
        if (selectionSlotAnim.isAlive()) {
            return MathHelper.clamp(Math.round(animatedSlot), 0, 8);
        }
        return targetSlot;
    }

    private static Vector4f getSelectionCornerRadius(int slot) {
        if (slot == 0) {
            return new Vector4f(SELECTION_RADIUS, SELECTION_RADIUS, 0f, 0f);
        }
        if (slot == 8) {
            return new Vector4f(0f, 0f, SELECTION_RADIUS, SELECTION_RADIUS);
        }
        return new Vector4f(0f, 0f, 0f, 0f);
    }

    public static void render(MatrixStack matrixStack, float partialTicks) {
        ClientPlayerEntity player = mc.player;
        Bot bot = null;

        for (Bot b : BotManager.allBots) {
            if (mc.renderViewEntity == b.connection.bot) {
                bot = b;
                break;
            }
        }

        if (bot != null) {
            if (bot.connection.bot == null) {
                return;
            }
        } else if (player == null) {
            return;
        }

        PlayerEntity renderPlayer = bot != null ? bot.connection.bot : player;
        int screenWidth = mc.getMainWindow().getScaledWidth();
        int screenHeight = mc.getMainWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        int barX = centerX - 91;
        int barY = getBarY(screenHeight);
        int itemY = getItemY(screenHeight) - 2;

        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(logoColor, 0.1f);
        int separatorColor = ColorUtil.applyOpacity(
                ThemeEditor.getColor(ThemeSettings.SEPARATOR),
                ThemeEditor.getAlpha(ThemeSettings.SEPARATOR) / 255f * 0.2f
        );
        int textColor = ColorUtil.rgba(255, 255, 255, 180);
        int selectedSlot = bot != null ? bot.connection.bot.inventory.currentItem : player.inventory.currentItem;

        RenderUtil.beginInterfaceRounding();
        RenderUtil.beginHudLiquidGlass();
        try {
            RenderUtil.drawRoundedRectangle(barX, barY, HOTBAR_WIDTH, HOTBAR_HEIGHT, BORDER_RADIUS, bgColor);

            for (int i = 1; i < 9; i++) {
                float sepX = barX + i * SLOT_WIDTH;
                RenderUtil.drawMinecraftRectangle(matrixStack, sepX, barY + 0.5f, SEPARATOR_WIDTH, SEPARATOR_HEIGHT, separatorColor);
            }

            int slotLabelColor = ColorUtil.rgba(255, 255, 255, 30);
            for (int slot = 0; slot < 9; slot++) {
                ItemStack slotStack = bot != null
                        ? bot.connection.bot.inventory.mainInventory.get(slot)
                        : player.inventory.mainInventory.get(slot);
                if (!slotStack.isEmpty()) {
                    continue;
                }

                String label = String.valueOf(slot + 1);
                float cellX = getSlotCellX(screenWidth, slot);
                float labelWidth = Fonts.sf_regular[18].getWidth(label);
                float labelHeight = Fonts.sf_regular[18].getHeight();
                float labelX = cellX + (SLOT_WIDTH - labelWidth) / 2f;
                float labelY = barY + (HOTBAR_HEIGHT - labelHeight) / 2f;
                Fonts.sf_regular[18].drawString(matrixStack, label, labelX + 0.5f, labelY, slotLabelColor);
            }

            selectionSlotAnim.update(selectedSlot);
            float animatedSlot = selectionSlotAnim.getValue();
            float selectedCellX = getSlotCellX(screenWidth, animatedSlot);
            float selectionWidth = getSelectionWidth(animatedSlot);
            int cornerSlot = getSelectionCornerSlot(animatedSlot, selectedSlot);
            RenderUtil.drawRoundedRectangle(
                    selectedCellX,
                    barY,
                    selectionWidth,
                    HOTBAR_HEIGHT,
                    getSelectionCornerRadius(cornerSlot),
                    ColorUtil.applyOpacity(logoColor, 0.18f)
            );
        } finally {
            RenderUtil.endHudLiquidGlass();
            RenderUtil.endInterfaceRounding();
        }

        RenderSystem.enableRescaleNormal();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        IngameGui ingameGui = mc.ingameGUI;
        EventManager.call(new EventHotbarRender(matrixStack, partialTicks));

        CustomItems.setRenderOffHand(false);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = bot != null
                    ? bot.connection.bot.inventory.mainInventory.get(slot)
                    : player.inventory.mainInventory.get(slot);
            int slotX = getSlotX(screenWidth, slot);
            renderHotbarItem(ingameGui, matrixStack, slotX, itemY, partialTicks, renderPlayer, stack);

            if (!stack.isEmpty() && stack.getCount() > 1) {
                String count = String.valueOf(stack.getCount());
                float countWidth = Fonts.sf_regular[10].getWidth(count);
                float itemSize = 16f * ITEM_SCALE;
                float cellX = getSlotCellX(screenWidth, slot);
                float countX = (int) cellX + (SLOT_WIDTH - countWidth) / 2f;
                float countY = barY + HOTBAR_HEIGHT - Fonts.sf_regular[10].getHeight() - 1f;
                Fonts.sf_regular[10].drawString(matrixStack, count, countX + 6, countY, textColor);
            }
        }

        ItemStack offhand = bot != null ? bot.connection.bot.getHeldItemOffhand() : player.getHeldItemOffhand();
        HandSide handSide = bot != null ? bot.connection.bot.getPrimaryHand().opposite() : player.getPrimaryHand().opposite();

        if (!offhand.isEmpty()) {
            CustomItems.setRenderOffHand(true);
            if (handSide == HandSide.LEFT) {
                renderHotbarItem(ingameGui, matrixStack, centerX - 91 - 26, itemY, partialTicks, renderPlayer, offhand);
            } else {
                renderHotbarItem(ingameGui, matrixStack, centerX + 91 + 10, itemY, partialTicks, renderPlayer, offhand);
            }
            CustomItems.setRenderOffHand(false);
        }

        if (mc.gameSettings.attackIndicator == AttackIndicatorStatus.HOTBAR) {
            float attackStrength = bot != null
                    ? bot.connection.bot.getCooledAttackStrength(0.0F)
                    : player.getCooledAttackStrength(0.0F);

            if (attackStrength < 1.0F) {
                int indicatorY = itemY + 4;
                int indicatorX = centerX + 91 + 6;

                if (handSide == HandSide.RIGHT) {
                    indicatorX = centerX - 91 - 22;
                }

                mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
                int fillHeight = (int) (attackStrength * 19.0F);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                ingameGui.blit(matrixStack, indicatorX, indicatorY, 0, 94, 18, 18);
                ingameGui.blit(matrixStack, indicatorX, indicatorY + 18 - fillHeight, 18, 112 - fillHeight, 18, fillHeight);
            }
        }

        RenderSystem.disableRescaleNormal();
        RenderSystem.disableBlend();
    }

    public static void renderHotbarItem(
            IngameGui ingameGui,
            MatrixStack matrixStack,
            int x,
            int y,
            float partialTicks,
            PlayerEntity player,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return;
        }

        float animation = (float) stack.getAnimationsToGo() - partialTicks;
        float centerX = x + 10f;
        float centerY = y + 8f;

        RenderSystem.pushMatrix();
        RenderSystem.translatef(centerX, centerY, 0.0F);

        if (animation > 0.0F) {
            float popScale = 1.0F + animation / 5.0F;
            RenderSystem.scalef(1.0F / popScale, (popScale + 1.0F) / 2.0F, 1.0F);
        }

        RenderSystem.scalef(ITEM_SCALE, ITEM_SCALE, 1.0F);
        RenderSystem.translatef(-centerX, -centerY, 0.0F);
        ingameGui.itemRenderer.renderItemAndEffectIntoGUI(player, stack, x, y);
        String overlayText = stack.getCount() > 1 ? "" : null;
        ingameGui.itemRenderer.renderItemOverlayIntoGUI(mc.fontRenderer, stack, x, y, overlayText);
        RenderSystem.popMatrix();
    }
}
