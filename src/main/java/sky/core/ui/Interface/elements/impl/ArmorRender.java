package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import sky.core.events.EventRender2D;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

public class ArmorRender implements ElementRender {

    private static final int ARMOR_SLOT_COUNT = 4;
    private static final float BAR_GAP_FROM_HOTBAR = 10f;

    @Override
    public void render(EventRender2D.Post event) {
        boolean hasArmor = false;
        for (ItemStack itemStack : mc.player.getArmorInventoryList()) {
            if (!itemStack.isEmpty()) {
                hasArmor = true;
                break;
            }
        }

        if (!hasArmor) {
            return;
        }

        MatrixStack matrixStack = event.getStack();
        int screenWidth = mc.getMainWindow().getScaledWidth();
        int screenHeight = mc.getMainWindow().getScaledHeight();
        int centerX = screenWidth / 2;

        boolean isRightHand = mc.player.getPrimaryHand().opposite() == HandSide.RIGHT && !mc.player.getHeldItemOffhand().isEmpty();
        int handOffset = isRightHand ? 29 : 0;

        int barY = HotBarRender.shouldReplace()
                ? HotBarRender.getBarY(screenHeight)
                : screenHeight - 22;
        int itemY = HotBarRender.shouldReplace()
                ? HotBarRender.getItemY(screenHeight) - 2
                : screenHeight - 16 - 3;

        float barX = centerX + 91 + BAR_GAP_FROM_HOTBAR + handOffset;
        float barWidth = ARMOR_SLOT_COUNT * HotBarRender.SLOT_WIDTH;

        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(logoColor, 0.1f);
        int separatorColor = ColorUtil.applyOpacity(
                ThemeEditor.getColor(ThemeSettings.SEPARATOR),
                ThemeEditor.getAlpha(ThemeSettings.SEPARATOR) / 255f * 0.2f
        );
        int textColor = ColorUtil.rgba(255, 255, 255, 180);

        RenderUtil.drawRoundedRectangle(barX, barY, barWidth, HotBarRender.HOTBAR_HEIGHT, 6, bgColor);

        for (int i = 1; i < ARMOR_SLOT_COUNT; i++) {
            float sepX = barX + i * HotBarRender.SLOT_WIDTH;
            RenderUtil.drawMinecraftRectangle(matrixStack, sepX, barY + 0.5f, 0.75f, 21f, separatorColor);
        }

        RenderSystem.enableRescaleNormal();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        IngameGui ingameGui = mc.ingameGUI;
        int slot = 0;
        for (ItemStack stack : mc.player.getArmorInventoryList()) {
            float cellX = barX + slot * HotBarRender.SLOT_WIDTH;
            int itemX = Math.round(cellX + HotBarRender.SLOT_ITEM_PADDING);
            HotBarRender.renderHotbarItem(ingameGui, matrixStack, itemX, itemY, event.getPartialTicks(), mc.player, stack);

            if (!stack.isEmpty() && stack.getCount() > 1) {
                String count = String.valueOf(stack.getCount());
                float countWidth = Fonts.sf_regular[10].getWidth(count);
                float countX = cellX + (HotBarRender.SLOT_WIDTH - countWidth) / 2f;
                float countY = barY + HotBarRender.HOTBAR_HEIGHT - Fonts.sf_regular[10].getHeight() - 1f;
                Fonts.sf_regular[10].drawString(matrixStack, count, countX + 7, countY, textColor);
            }

            slot++;
        }

        RenderSystem.disableRescaleNormal();
        RenderSystem.disableBlend();
    }
}
