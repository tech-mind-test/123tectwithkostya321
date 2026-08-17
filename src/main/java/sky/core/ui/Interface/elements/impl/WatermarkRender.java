package sky.core.ui.Interface.elements.impl;

import com.adl.nativeprotect.User;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

import java.util.Locale;

public class WatermarkRender implements ElementRender {

    public static BooleanSetting alphabg = new BooleanSetting("Прозрачный фон", false);

    private final Dragging dragging;

    public WatermarkRender(Dragging dragging) {
        this.dragging = dragging;
    }

    @Override
    public void render(EventRender2D.Post event) {
        if (Interface.isOldHud()) {
            renderClassic(event);
            return;
        }

        float posX = dragging.getX();
        float posY = dragging.getY();
        MatrixStack ms = event.getStack();

        String titleText = "SkyCore";
        String icon = "a";
        String nameText = User.getInstance().profile("username");
        String iconName = "b";
        String ipIcon = "g";
        String ip;
        try {
            boolean isMultiplayer = Minecraft.getInstance().getCurrentServerData() != null;
            ip = isMultiplayer ? Minecraft.getInstance().getCurrentServerData().serverIP : "localhost";
        } catch (Exception e) {
            ip = "localhost";
        }

        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(logoColor, 0.1f);
        int iconColor = logoColor;
        int textColor = 0xFFFFFFFF;
        int separatorColor = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.SEPARATOR), ThemeEditor.getAlpha(ThemeSettings.SEPARATOR) / 255f * 0.2f);

        float skyCoreWidth = Fonts.sf_medium[13].getWidth(titleText);
        float userWidth = Fonts.sf_medium[13].getWidth(nameText);
        float ipWidth = Fonts.sf_medium[13].getWidth(ip);

        float totalHeight = 44.0f / 2f;
        float padding = 12.0f / 2f;
        float gap = 12.0f / 2f;
        float innerGap = 8.0f / 2f;
        float smallGap = 4.0f / 2f;
        float borderRadius = 6;
        float iconSize = 14.0f / 2f;
        float fontSize = 12.0f / 2f;
        float separatorWidth = 0.75f;
        float separatorHeight = 21f;

        float leftSectionWidth = iconSize + innerGap + skyCoreWidth;
        float rightSectionWidth = iconSize + smallGap + userWidth + gap + gap + iconSize + smallGap + ipWidth;
        float totalWidth = padding + leftSectionWidth + gap + separatorWidth + gap + rightSectionWidth + padding;

        RenderUtil.drawRoundedRectangle(posX, posY, totalWidth, totalHeight, borderRadius, bgColor);

        float currentX = posX + padding;
        float textY = posY + (totalHeight - fontSize) / 2f + 1.5f;

        Fonts.skycore[14].drawString(ms, icon, currentX, posY + (totalHeight - iconSize) / 2f + 2, iconColor);

        currentX += iconSize + innerGap;

        Fonts.sfregular[13].drawString(ms, titleText, currentX, textY, textColor);

        currentX += skyCoreWidth + gap;

        RenderUtil.drawMinecraftRectangle(ms, currentX, posY + 0.5f, separatorWidth, separatorHeight, separatorColor);

        currentX += separatorWidth + gap;

        Fonts.skycore[14].drawString(ms, iconName, currentX, posY + (totalHeight - iconSize) / 2f + 2.5f, iconColor);

        currentX += iconSize + smallGap;

        Fonts.sfregular[13].drawString(ms, nameText, currentX, textY, textColor);

        currentX += userWidth + gap;

        RenderUtil.drawRoundedRectangle(currentX - 2.5f, posY + totalHeight / 2f - 1f, 3, 3, 0.5f, ColorUtil.applyOpacity(textColor, 0.10f));

        currentX += gap;
        Fonts.skycore[14].drawString(ms, ipIcon, currentX, posY + (totalHeight - iconSize) / 2f + 2.5f, iconColor);

        currentX += iconSize + smallGap;

        Fonts.sfregular[13].drawString(ms, ip, currentX, textY, textColor);

        dragging.setWidth(totalWidth);
        dragging.setHeight(totalHeight);
    }

    private void renderClassic(EventRender2D.Post event) {
        MatrixStack ms = event.getStack();
        float x = dragging.getX();
        float y = dragging.getY();
        final int mainSize = 14;
        final int labelSize = 13;
        final int iconSize = 14;

        String user = User.getInstance().profile("username");
        String fpsValue = String.valueOf(Minecraft.debugFPS);
        String pingValue;
        try {
            boolean isMultiplayer = Minecraft.getInstance().getCurrentServerData() != null;
            int pingVal = isMultiplayer ? Minecraft.getInstance().getConnection().getPlayerInfo(Minecraft.getInstance().player.getUniqueID()).getResponseTime() : 0;
            pingValue = String.valueOf(pingVal);
        } catch (Exception e) {
            pingValue = "0";
        }
        String tpsValue = "20";
        String xValue = String.format(Locale.ENGLISH, "%.0f", Minecraft.getInstance().player.getPosX());
        String yValue = String.format(Locale.ENGLISH, "%.0f", Minecraft.getInstance().player.getPosY());
        String zValue = String.format(Locale.ENGLISH, "%.0f", Minecraft.getInstance().player.getPosZ());
        double dx = Minecraft.getInstance().player.getPosX() - Minecraft.getInstance().player.prevPosX;
        double dy = Minecraft.getInstance().player.getPosY() - Minecraft.getInstance().player.prevPosY;
        double dz = Minecraft.getInstance().player.getPosZ() - Minecraft.getInstance().player.prevPosZ;
        String bpsValue = String.format(Locale.ENGLISH, "%.1f", Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0);

        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = alphabg.get() ? ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.WINDOW_BG), 0) : ThemeEditor.getColor(ThemeSettings.WINDOW_BG);
        int whiteColor = 0xFFFFFFFF;
        int textColor = ColorUtil.applyOpacity(whiteColor, 180);
        int dotColor = ColorUtil.hex("#9A9A9A");

        float textFpsWidth = Fonts.sf_medium[labelSize].getWidth("fps");
        float textMsWidth = Fonts.sf_medium[labelSize].getWidth("ms");
        float textTpsWidth = Fonts.sf_medium[labelSize].getWidth("tps");
        float textXWidth = Fonts.sf_medium[labelSize].getWidth("x");
        float textYWidth = Fonts.sf_medium[labelSize].getWidth("y");
        float textZWidth = Fonts.sf_medium[labelSize].getWidth("z");
        float textBpsWidth = Fonts.sf_medium[labelSize].getWidth("bps");

        float userWidth = Fonts.sf_medium[mainSize].getWidth(user);
        float iconAWidth = Fonts.skycore[iconSize].getWidth("a");
        float iconBWidth = Fonts.skycore[iconSize].getWidth("b");
        float iconFWidth = Fonts.skycore[iconSize].getWidth("f");
        float iconGWidth = Fonts.skycore[iconSize].getWidth("g");
        float iconHWidth = Fonts.skycore[iconSize].getWidth("h");
        float iconIWidth = Fonts.skycore[iconSize].getWidth("i");
        float iconJWidth = Fonts.skycore[iconSize].getWidth("j");
        float skycoreTextWidth = Fonts.sf_medium[mainSize].getWidth("SkyCore");
        float fpsWidth = Fonts.sf_medium[mainSize].getWidth(fpsValue);
        float pingWidth = Fonts.sf_medium[mainSize].getWidth(pingValue);
        float tpsWidth = Fonts.sf_medium[mainSize].getWidth(tpsValue);
        float xWidth = Fonts.sf_medium[mainSize].getWidth(xValue);
        float yWidth = Fonts.sf_medium[mainSize].getWidth(yValue);
        float zWidth = Fonts.sf_medium[mainSize].getWidth(zValue);
        float bpsWidth = Fonts.sf_medium[mainSize].getWidth(bpsValue);

        float paddingLeft = 7f;
        float spacingAfterIconA = 4f;
        float spacingAfterText = 3f;
        float spacingAfterDot = 3f;
        float spacingAfterLabel = 1.5f;
        float spacingAfterValue = 3f;
        float paddingRight = 6f;
        float tpsTextGap = 1.2f;
        float rowHeight = 19f;
        float roundRadius = 6.5f;
        float gap = 3f;

        float topW = paddingLeft + iconAWidth + spacingAfterIconA + skycoreTextWidth
                + spacingAfterText + 3f + spacingAfterDot
                + iconBWidth + spacingAfterText + userWidth
                + spacingAfterText + 3f + spacingAfterDot
                + iconFWidth + spacingAfterText + fpsWidth + textFpsWidth
                + spacingAfterText + 3f + spacingAfterDot
                + iconGWidth + spacingAfterText + pingWidth + textMsWidth
                + spacingAfterText + 3f + spacingAfterDot
                + iconHWidth + spacingAfterText + tpsWidth + tpsTextGap + textTpsWidth
                + paddingRight;

        float bottomW = paddingLeft + iconJWidth + spacingAfterText
                + textXWidth + spacingAfterLabel + xWidth + spacingAfterValue
                + textYWidth + spacingAfterLabel + yWidth + spacingAfterValue
                + textZWidth + spacingAfterLabel + zWidth
                + spacingAfterValue + 3f + spacingAfterDot
                + iconIWidth + spacingAfterText + bpsWidth + textBpsWidth
                + paddingRight;

        float width = Math.max(topW, bottomW);
        dragging.setWidth(width);
        dragging.setHeight(rowHeight * 2 + gap);

        RenderUtil.drawBlurredRoundedRectangle(x, y, topW, rowHeight, roundRadius, bgColor, 1);
        RenderUtil.drawBlurredRoundedRectangle(x, y + rowHeight + gap, bottomW, rowHeight, roundRadius, bgColor, 1);

        float localX = x + paddingLeft;
        float dotY = y + (rowHeight / 2f);
        float textYPos = y + (rowHeight - Fonts.sf_semibold[mainSize].getHeight()) / 2f + 0.2f;
        float labelY = y + (rowHeight - Fonts.sf_semibold[labelSize].getHeight()) / 2f + 0.25f;
        float iconY = y + (rowHeight - Fonts.skycore[iconSize].getHeight()) / 2f + 0.35f;

        Fonts.skycore[iconSize].drawString(ms, "a", localX, iconY, logoColor);
        localX += iconAWidth + spacingAfterIconA;
        Fonts.sf_semibold[mainSize].drawString(ms, "SkyCore", localX, textYPos, whiteColor);
        localX += skycoreTextWidth + spacingAfterText;
        RenderUtil.drawCircle(localX + 1.7f, dotY + 0.6f, 0f, 360f, 1.2f, 0.8f, true, dotColor);
        localX += 3f + spacingAfterDot;

        Fonts.skycore[iconSize].drawString(ms, "b", localX, iconY, logoColor);
        localX += iconBWidth + spacingAfterText;
        Fonts.sf_semibold[mainSize].drawString(ms, user, localX, textYPos, whiteColor);
        localX += userWidth + spacingAfterText;
        RenderUtil.drawCircle(localX + 1.5f, dotY + 0.6f, 0f, 360f, 1.2f, 0.8f, true, dotColor);
        localX += 3f + spacingAfterDot;

        Fonts.skycore[iconSize].drawString(ms, "f", localX, iconY, logoColor);
        localX += iconFWidth + spacingAfterText;
        Fonts.sf_semibold[mainSize].drawString(ms, fpsValue, localX, textYPos, whiteColor);
        localX += fpsWidth;
        Fonts.sf_semibold[labelSize].drawString(ms, "fps", localX - 0.1f, labelY + 0.35f, textColor);
        localX += textFpsWidth + spacingAfterText;
        RenderUtil.drawCircle(localX + 1.7f, dotY + 0.6f, 0f, 360f, 1.2f, 0.8f, true, dotColor);
        localX += 3f + spacingAfterDot;

        Fonts.skycore[iconSize].drawString(ms, "g", localX, iconY, logoColor);
        localX += iconGWidth + spacingAfterText;
        Fonts.sf_semibold[mainSize].drawString(ms, pingValue, localX, textYPos, whiteColor);
        localX += pingWidth;
        Fonts.sf_semibold[labelSize].drawString(ms, "ms", localX - 0.1f, labelY + 0.35f, textColor);
        localX += textMsWidth + spacingAfterText;
        RenderUtil.drawCircle(localX + 1.7f, dotY + 0.6f, 0f, 360f, 1.2f, 0.8f, true, dotColor);
        localX += 3f + spacingAfterDot;

        Fonts.skycore[iconSize].drawString(ms, "h", localX, iconY, logoColor);
        localX += iconHWidth + spacingAfterText;
        Fonts.sf_semibold[mainSize].drawString(ms, tpsValue, localX, textYPos, whiteColor);
        localX += tpsWidth;
        Fonts.sf_semibold[labelSize].drawString(ms, "tps", localX + tpsTextGap, labelY + 0.35f, textColor);
        localX += textTpsWidth + spacingAfterText;

        float by = y + rowHeight + gap;
        localX = x + paddingLeft;
        dotY = by + (rowHeight / 2f);
        textYPos = by + (rowHeight - Fonts.sf_semibold[mainSize].getHeight()) / 2f + 0.2f;
        labelY = by + (rowHeight - Fonts.sf_semibold[labelSize].getHeight()) / 2f + 0.25f;
        iconY = by + (rowHeight - Fonts.skycore[iconSize].getHeight()) / 2f + 0.35f;

        Fonts.skycore[iconSize].drawString(ms, "j", localX, iconY, logoColor);
        localX += iconJWidth + spacingAfterText;
        Fonts.sf_semibold[labelSize].drawString(ms, "x", localX, labelY, textColor);
        localX += textXWidth + spacingAfterLabel;
        Fonts.sf_semibold[mainSize].drawString(ms, xValue, localX, textYPos, whiteColor);
        localX += xWidth + spacingAfterValue;
        Fonts.sf_semibold[labelSize].drawString(ms, "y", localX - 0.9f, labelY, textColor);
        localX += textYWidth + spacingAfterLabel;
        Fonts.sf_semibold[mainSize].drawString(ms, yValue, localX - 1f, textYPos, whiteColor);
        localX += yWidth + spacingAfterValue;
        Fonts.sf_semibold[labelSize].drawString(ms, "z", localX - 1.8f, labelY, textColor);
        localX += textZWidth + spacingAfterLabel;
        Fonts.sf_semibold[mainSize].drawString(ms, zValue, localX - 2f, textYPos, whiteColor);
        localX += zWidth + spacingAfterValue;
        RenderUtil.drawCircle(localX - 0.1f, dotY + 0.6f, 0f, 360f, 1.2f, 0.8f, true, dotColor);
        localX += 3f + spacingAfterDot;
        Fonts.skycore[iconSize].drawString(ms, "i", localX - 2f, iconY, logoColor);
        localX += iconIWidth + spacingAfterText;
        Fonts.sf_semibold[mainSize].drawString(ms, bpsValue, localX - 2f, textYPos, whiteColor);
        localX += bpsWidth;
        Fonts.sf_semibold[labelSize].drawString(ms, "bps", localX - 1.8f, labelY + 0.35f, textColor);
    }
}