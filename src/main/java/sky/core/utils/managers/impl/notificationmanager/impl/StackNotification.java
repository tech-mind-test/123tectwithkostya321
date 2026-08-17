package sky.core.utils.managers.impl.notificationmanager.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import sky.core.utils.managers.impl.notificationmanager.AbstractNotification;
import sky.core.ui.Interface.elements.impl.NotificationRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

import static sky.core.utils.Wrapper.mc;

public class StackNotification extends AbstractNotification {
    private final ItemStack stack;
    private final boolean autoSwapStyle;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    public StackNotification(ItemStack stack, ITextComponent message) {
        this(stack, message, false);
    }

    public StackNotification(ItemStack stack, ITextComponent message, boolean autoSwapStyle) {
        super(message);
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        this.autoSwapStyle = autoSwapStyle;
    }

    public boolean isAutoSwapStyle() {
        return autoSwapStyle;
    }

    @Override
    public void render(float x, float y, MatrixStack matrixStack) {
        initYIfNeeded(y);
        boolean expired = isExpired();
        alphaAnimation.update(expired ? 0f : 1f);
        yAnimation.update((forceExpire && expired) ? y - 4.0f : y);
        float animatedY = yAnimation.getValue();
        float animatedAlpha = alphaAnimation.getValue();
        boolean ordinaryMode = sky.core.modules.impl.visuals.Interface.isNewHud();

        if (animatedAlpha <= 0.01f) return;

        if (!ordinaryMode) {
            RenderUtil.drawBlurredRoundedRectangle(x, animatedY, Fonts.sf_medium[12].getWidth(this.message.getString()) + 24.0F, 13.0F, 4F, NotificationRender.alphabg.get() ? ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.WINDOW_BG), 0) : ThemeEditor.getColor(ThemeSettings.WINDOW_BG), animatedAlpha);
            int offsetX = (int)(x + 4.0F);
            int offsetY = (int)(animatedY + 2.5F);
            RenderUtil.drawStack(this.stack, (float)offsetX, (float)offsetY, 0.5F, animatedAlpha);
            RenderUtil.drawMinecraftRectangle(matrixStack, (float)((int)x + 16), animatedY + 3.0F, 0.5F, 7.0F, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.SEPARATOR), ThemeEditor.getAlpha(ThemeSettings.SEPARATOR) / 255.0F * animatedAlpha));
            Fonts.sf_medium[12].drawText(matrixStack, this.message, (double)((float)((int)x) + 20.0F), (double)(animatedY + Fonts.sf_medium[12].getHeight() + 1.5F), ColorUtil.applyOpacity(-1, animatedAlpha));
            return;
        }

        float cardHeight = 19f;
        float cardRadius = 5f;
        float paddingX = autoSwapStyle ? 6f : 8f;
        float paddingY = 6f;
        float gap = 4f;
        float iconSize = 10f;

        String text = message.getString();
        float textWidth = Fonts.sfregular[12].getWidth(text);
        float contentWidth = paddingX + iconSize + gap + textWidth + paddingX;
        float cardWidth = contentWidth;

        int cardBgColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(cardBgColor, 0.1f);
        int bgWithAlpha = ColorUtil.applyOpacity(bgColor, animatedAlpha);
        int textWithAlpha = ColorUtil.applyOpacity(TEXT_COLOR, animatedAlpha);

        float drawX = autoSwapStyle
                ? (mc.getMainWindow().getScaledWidth() - cardWidth) / 2f
                : x;
        RenderUtil.drawRoundedRectangle(drawX, animatedY, cardWidth, cardHeight, cardRadius, bgWithAlpha);

        float textHeight = Fonts.sfregular[12].getHeight();
        float contentHeight = Math.max(iconSize, textHeight);
        float contentY = animatedY + (cardHeight - contentHeight) / 2f;

        float iconX = drawX + paddingX;
        float iconY = contentY + (contentHeight - iconSize) / 2f;
        RenderUtil.drawStack(stack, (int) iconX, (int) iconY, 0.625f, animatedAlpha);

        float textX = iconX + iconSize + gap;
        float textY = contentY + (contentHeight - textHeight) / 2f + 1f;

        int logoColor = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.LOGO), animatedAlpha);

        int firstQuote = text.indexOf('\'');
        int secondQuote = text.indexOf('\'', firstQuote + 1);

        if (firstQuote != -1 && secondQuote != -1 && firstQuote < secondQuote) {
            String beforeQuote = text.substring(0, firstQuote);
            String quotedText = text.substring(firstQuote + 1, secondQuote);
            String afterQuote = text.substring(secondQuote + 1);

            float beforeWidth = Fonts.sfregular[12].getWidth(beforeQuote);
            float quotedWidth = Fonts.sfregular[12].getWidth(quotedText);

            if (!beforeQuote.isEmpty()) {
                Fonts.sfregular[12].drawString(matrixStack, beforeQuote, textX, textY, textWithAlpha);
            }

            Fonts.sfregular[12].drawString(matrixStack, quotedText, textX + beforeWidth, textY, logoColor);

            if (!afterQuote.isEmpty()) {
                Fonts.sfregular[12].drawString(matrixStack, afterQuote, textX + beforeWidth + quotedWidth, textY, textWithAlpha);
            }
        } else {
            Fonts.sfregular[12].drawString(matrixStack, text, textX, textY, textWithAlpha);
        }
    }
}
