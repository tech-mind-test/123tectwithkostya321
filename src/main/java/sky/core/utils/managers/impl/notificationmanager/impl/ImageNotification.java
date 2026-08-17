package sky.core.utils.managers.impl.notificationmanager.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import sky.core.utils.managers.impl.notificationmanager.AbstractNotification;
import sky.core.ui.Interface.elements.impl.NotificationRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

import static sky.core.utils.Wrapper.mc;

public class ImageNotification extends AbstractNotification {
    private final ResourceLocation image;
    private final Integer overrideColor;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    public ImageNotification(ResourceLocation image, ITextComponent message) {
        super(message);
        this.image = image;
        this.overrideColor = null;
    }

    public ImageNotification(ResourceLocation image, ITextComponent message, int overrideColor) {
        super(message);
        this.image = image;
        this.overrideColor = overrideColor;
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
            RenderUtil.drawBlurredRoundedRectangle(x, animatedY, Fonts.sf_semibold[12].getWidth(this.message.getString()) + 24.0F, 13.0F, 4F, NotificationRender.alphabg.get() ? ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.WINDOW_BG), 0) : ThemeEditor.getColor(ThemeSettings.WINDOW_BG), animatedAlpha);
            int offsetX = (int)(x + 4.0F);
            int offsetY = (int)(animatedY + 2.5F);
            if (this.overrideColor != null && this.image.getPath().contains("potion")) {
                int colored = ColorUtil.applyOpacity(this.overrideColor, animatedAlpha);
                RenderUtil.drawPotionLiquid(colored, (float)offsetX, (float)offsetY, 8.0F, 8.0F);
            }

            RenderUtil.drawImage2D(this.image, (float)offsetX, (float)offsetY, 8.0F, 8.0F, ColorUtil.applyOpacity(-1, animatedAlpha));
            RenderUtil.drawMinecraftRectangle(matrixStack, (float)((int)x + 16), animatedY + 3.0F, 0.5F, 7.0F, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.SEPARATOR), ThemeEditor.getAlpha(ThemeSettings.SEPARATOR) / 255.0F * animatedAlpha));
            Fonts.sf_semibold[12].drawText(matrixStack, this.message, (double)((float)((int)x) + 20.0F), (double)(animatedY + Fonts.sf_semibold[12].getHeight() + 1.5F), ColorUtil.applyOpacity(-1, 1.0F * animatedAlpha));
            return;
        }

        float cardHeight = 19f;
        float cardRadius = 5f;
        float paddingX = 8f;
        float paddingY = 6f;
        float gap = 4f;
        float iconSize = 10f;

        String text = message.getString();
        float textWidth = Fonts.sfregular[12].getWidth(text);
        float cardWidth = paddingX + iconSize + gap + textWidth + paddingX;

        float centeredX = (mc.getMainWindow().getScaledWidth() - cardWidth) / 2f;

        int cardBgColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(cardBgColor, 0.1f);
        int bgWithAlpha = ColorUtil.applyOpacity(bgColor, animatedAlpha);
        int textWithAlpha = ColorUtil.applyOpacity(TEXT_COLOR, animatedAlpha);

        RenderUtil.drawRoundedRectangle(centeredX, animatedY, cardWidth, cardHeight, cardRadius, bgWithAlpha);

        float textHeight = Fonts.sfregular[12].getHeight();
        float contentHeight = Math.max(iconSize, textHeight);
        float contentY = animatedY + (cardHeight - contentHeight) / 2f;

        float iconX = centeredX + paddingX;
        float iconY = contentY + (contentHeight - iconSize) / 2f;

        if (overrideColor != null && image.getPath().contains("potion")) {
            int colored = ColorUtil.applyOpacity(overrideColor, animatedAlpha);
            RenderUtil.drawPotionLiquid(colored, (int) iconX, (int) iconY, (int) iconSize, (int) iconSize);
        }
        RenderUtil.drawImage2D(image, (int) iconX, (int) iconY - 0.5f, (int) iconSize, (int) iconSize, ColorUtil.applyOpacity(0xFFFFFFFF, animatedAlpha));

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
        } else if (overrideColor != null && text.contains("зачарован:") && text.endsWith("●")) {
            String beforeDot = text.substring(0, text.length() - 1);
            Fonts.sfregular[12].drawString(matrixStack, beforeDot, textX, textY, textWithAlpha);
            float beforeDotWidth = Fonts.sfregular[12].getWidth(beforeDot);
            int dotColor = ColorUtil.applyOpacity(overrideColor, animatedAlpha);
            Fonts.sfregular[12].drawString(matrixStack, "●", textX + beforeDotWidth, textY, dotColor);
        } else {
            Fonts.sfregular[12].drawString(matrixStack, text, textX, textY, textWithAlpha);
        }
    }
}
