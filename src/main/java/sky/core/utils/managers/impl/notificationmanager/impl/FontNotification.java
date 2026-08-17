package sky.core.utils.managers.impl.notificationmanager.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.utils.managers.impl.notificationmanager.AbstractNotification;
import lombok.Getter;
import net.minecraft.util.text.StringTextComponent;
import sky.core.ui.Interface.elements.impl.NotificationRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

@Getter
public class FontNotification extends AbstractNotification {
    private final String categoryIcon;
    private final Integer color;

    private static final int TEXT_COLOR = 0xFFFFFF;

    public FontNotification(String categoryIcon, String message, int color) {
        super(new StringTextComponent(message));
        this.categoryIcon = categoryIcon;
        this.color = color;
    }

    @Override
    public void render(float x, float y, MatrixStack matrixStack) {
        initYIfNeeded(y);
        boolean expired = isExpired();
        alphaAnimation.update(expired ? 0f : 1f);
        yAnimation.update((forceExpire && expired) ? y - 2.0f : y);

        float animatedY = yAnimation.getValue();
        float animatedAlpha = alphaAnimation.getValue();
        boolean ordinaryMode = sky.core.modules.impl.visuals.Interface.isNewHud();

        if (animatedAlpha <= 0.01f) return;

        String displayMessage = message.getString();

        if (!ordinaryMode) {
            float width = Fonts.icons[16].getWidth(this.categoryIcon) + Fonts.sf_medium[12].getWidth(displayMessage) + 5.0F;
            int iconColor;
            switch (this.categoryIcon) {
                case "I":
                case "L":
                    iconColor = ColorUtil.applyOpacity(16711680, animatedAlpha);
                    break;
                case "H":
                    iconColor = ColorUtil.applyOpacity(65280, animatedAlpha);
                    break;
                case "M":
                    iconColor = ColorUtil.applyOpacity(16753920, animatedAlpha);
                    break;
                default:
                    iconColor = ColorUtil.applyOpacity(16777215, animatedAlpha);
            }

            RenderUtil.drawBlurredRoundedRectangle(x, animatedY, width + 12.5F, 14.0F, 4.0F, NotificationRender.alphabg.get() ? ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.WINDOW_BG), 0) : ThemeEditor.getColor(ThemeSettings.WINDOW_BG), animatedAlpha);
            Fonts.icons[16].drawString(matrixStack, this.categoryIcon, (int)x + 5, animatedY + Fonts.icons[16].getHeight(), iconColor);
            RenderUtil.drawMinecraftRectangle(matrixStack, (int)x + Fonts.icons[16].getWidth(this.categoryIcon) + 8.5F, animatedY + 3.5F, 0.5F, 7.0F, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.SEPARATOR), ThemeEditor.getAlpha(ThemeSettings.SEPARATOR) / 255.0F * animatedAlpha));
            Fonts.sf_medium[12].drawString(matrixStack, displayMessage, (int)x + Fonts.icons[16].getWidth(this.categoryIcon) + 12.0F, animatedY + Fonts.sf_semibold[12].getHeight() + 2.0F, ColorUtil.applyOpacity(this.color, animatedAlpha));
            return;
        }

        float cardHeight = 19f;
        float cardRadius = 5f;
        float paddingX = 8f;
        float paddingY = 6f;
        float gap = 4f;
        float iconSize = 7f;

        String prefix = "";
        String moduleName = displayMessage;
        String status = "";

        boolean hasPrefix = displayMessage.startsWith("Module ");
        boolean hasStatus = displayMessage.contains(" enabled") || displayMessage.contains(" disabled");

        if (hasPrefix) {
            prefix = "Module ";
            moduleName = displayMessage.substring(7);
        }

        if (hasStatus) {
            int enabledIdx = moduleName.indexOf(" enabled");
            int disabledIdx = moduleName.indexOf(" disabled");

            if (enabledIdx != -1) {
                String actualName = moduleName.substring(0, enabledIdx);
                status = moduleName.substring(enabledIdx);
                moduleName = actualName;
            } else if (disabledIdx != -1) {
                String actualName = moduleName.substring(0, disabledIdx);
                status = moduleName.substring(disabledIdx);
                moduleName = actualName;
            }
        }

        float prefixWidth = Fonts.sfregular[12].getWidth(prefix);
        float moduleNameWidth = Fonts.sfregular[12].getWidth(moduleName);
        float statusWidth = Fonts.sfregular[12].getWidth(status);
        float textWidth = prefixWidth + moduleNameWidth + statusWidth;
        float cardWidth = paddingX + iconSize + gap + textWidth + paddingX;

        int cardBgColor = ThemeEditor.getColor(ThemeSettings.WINDOW_BG);
        int iconColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(iconColor, 0.1f);
        int bgWithAlpha = ColorUtil.applyOpacity(bgColor, animatedAlpha);
        int whiteColor = ColorUtil.applyOpacity(TEXT_COLOR, animatedAlpha);
        int blueColor = ColorUtil.applyOpacity(iconColor, animatedAlpha);
        int iconWithAlpha = ColorUtil.applyOpacity(iconColor, animatedAlpha);

        RenderUtil.drawRoundedRectangle(x, animatedY, cardWidth, cardHeight, cardRadius, bgWithAlpha);

        float iconX = x + paddingX;
        float iconY = animatedY + paddingY + (iconSize - 7f) / 2f;
        if (categoryIcon != null && !categoryIcon.isEmpty()) {
            Fonts.skycore[13].drawString(matrixStack, categoryIcon, iconX, iconY + 2.5f, iconWithAlpha);
        }

        float textY = animatedY + paddingY + (cardHeight - paddingY * 2 - Fonts.sfregular[12].getHeight()) / 2f + 1;
        float textX = iconX + iconSize + gap;

        if (!prefix.isEmpty()) {
            Fonts.sfregular[12].drawString(matrixStack, prefix, textX, textY, whiteColor);
            textX += prefixWidth;
        }

        Fonts.sfregular[12].drawString(matrixStack, moduleName, textX, textY, blueColor);
        textX += moduleNameWidth;

        if (!status.isEmpty()) {
            Fonts.sfregular[12].drawString(matrixStack, status, textX, textY, whiteColor);
        }
    }
}
