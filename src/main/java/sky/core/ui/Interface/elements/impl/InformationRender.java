//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import sky.core.events.EventRender2D;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

public class InformationRender implements ElementRender {
    public static BooleanSetting fps = new BooleanSetting("Фпс", true);
    public static BooleanSetting coords = new BooleanSetting("Координаты", true);
    public static BooleanSetting bps = new BooleanSetting("БПС", false);
    private final Dragging dragging;

    public InformationRender(Dragging dragging) {
        this.dragging = dragging;
    }

    public void render(EventRender2D.Post event) {
        if (Interface.isNewHud()) {
            Minecraft mc = Minecraft.getInstance();
            float posX = this.dragging.getX();
            float posY = this.dragging.getY();
            MatrixStack ms = event.getStack();
            String coordsIcon = "k";
            String fpsIcon = "i";
            String bpsIcon = "l";
            double playerX = Minecraft.player != null ? Minecraft.player.getPosX() : (double)0.0F;
            double playerY = Minecraft.player != null ? Minecraft.player.getPosY() : (double)0.0F;
            double playerZ = Minecraft.player != null ? Minecraft.player.getPosZ() : (double)0.0F;
            Locale var10000 = Locale.US;
            Object[] var10002 = new Object[]{playerX};
            String xText = "X " + String.format(var10000, "%.0f", var10002);
            var10000 = Locale.US;
            var10002 = new Object[]{playerY};
            String yText = "Y " + String.format(var10000, "%.0f", var10002);
            var10000 = Locale.US;
            var10002 = new Object[]{playerZ};
            String zText = "Z " + String.format(var10000, "%.0f", var10002);
            int fpsValue = Minecraft.debugFPS;
            String fpsValueText = String.valueOf(fpsValue);
            String fpsLabel = "fps";
            double bpsVal = (double)0.0F;
            if (Minecraft.player != null) {
                double dx = Minecraft.player.getPosX() - Minecraft.player.prevPosX;
                double dy = Minecraft.player.getPosY() - Minecraft.player.prevPosY;
                double dz = Minecraft.player.getPosZ() - Minecraft.player.prevPosZ;
                bpsVal = Math.sqrt(dx * dx + dy * dy + dz * dz) * (double)20.0F;
            }

            String bpsValueText = String.format(Locale.ENGLISH, "%.1f", bpsVal);
            String bpsLabel = "bps";
            int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
            int bgColor = ColorUtil.darken(logoColor, 0.1F);
            int textColor = -1;
            int labelColor = ColorUtil.rgb(170, 170, 170);
            int dotColor = ColorUtil.applyOpacity(textColor, 0.15F);
            int separatorColor = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.SEPARATOR), ThemeEditor.getAlpha(ThemeSettings.SEPARATOR) / 255.0F * 0.2F);
            int coordLabelColor = ColorUtil.rgb(140, 140, 140);
            float totalHeight = 22.0F;
            float padding = 6.0F;
            float gap = 6.0F;
            float smallGap = 2.0F;
            float dotGap = 3.0F;
            float borderRadius = 6.0F;
            float iconSize = 7.0F;
            float dotSize = 3.0F;
            float separatorWidth = 0.75F;
            float separatorHeight = 21.0F;
            float coordGap = 3.0F;
            boolean showCoords = (Boolean)coords.get();
            boolean showFps = (Boolean)fps.get();
            boolean showBps = (Boolean)bps.get();
            if (showCoords || showFps || showBps) {
                float totalWidth = padding - 3.0F;
                boolean hasPrev = false;
                if (showCoords) {
                    totalWidth += iconSize + smallGap + Fonts.sfregular[13].getWidth(xText) + coordGap + Fonts.sfregular[13].getWidth(yText) + coordGap + Fonts.sfregular[13].getWidth(zText);
                    hasPrev = true;
                }

                if (showFps) {
                    if (hasPrev) {
                        totalWidth += gap + separatorWidth + gap;
                    }

                    totalWidth += iconSize + smallGap + Fonts.sfregular[13].getWidth(fpsValueText) + smallGap + Fonts.sfregular[11].getWidth(fpsLabel);
                    hasPrev = true;
                }

                if (showBps) {
                    if (hasPrev) {
                        totalWidth += dotGap + dotSize + dotGap;
                    }

                    totalWidth += iconSize + smallGap + Fonts.sfregular[13].getWidth(bpsValueText) + smallGap + Fonts.sfregular[11].getWidth(bpsLabel);
                }

                totalWidth += padding;
                RenderUtil.drawRoundedRectangle(posX, posY, totalWidth, totalHeight, borderRadius, bgColor);
                float fontSize = 6.0F;
                float currentX = posX + padding;
                float textY = posY + (totalHeight - fontSize) / 2.0F + 1.5F;
                float labelY = textY + 1.0F;
                float iconY = posY + (totalHeight - iconSize) / 2.0F + 2.0F;
                hasPrev = false;
                if (showCoords) {
                    Fonts.divine_icons[14].drawString(ms, coordsIcon, (double)currentX, (double)(iconY + 0.5F), logoColor);
                    currentX += iconSize + smallGap;
                    Fonts.sfregular[11].drawString(ms, "x", (double)(currentX + 1.5F), (double)labelY, coordLabelColor);
                    currentX += Fonts.sfregular[11].getWidth("x") + 1.0F;
                    Fonts.sfregular[13].drawString(ms, String.format(Locale.US, "%.0f", playerX), (double)(currentX + 1.5F), (double)textY, textColor);
                    currentX += Fonts.sfregular[13].getWidth(String.format(Locale.US, "%.0f", playerX)) + coordGap;
                    Fonts.sfregular[11].drawString(ms, "y", (double)(currentX + 1.5F), (double)labelY, coordLabelColor);
                    currentX += Fonts.sfregular[11].getWidth("y") + 1.0F;
                    Fonts.sfregular[13].drawString(ms, String.format(Locale.US, "%.0f", playerY), (double)(currentX + 1.5F), (double)textY, textColor);
                    currentX += Fonts.sfregular[13].getWidth(String.format(Locale.US, "%.0f", playerY)) + coordGap;
                    Fonts.sfregular[11].drawString(ms, "z", (double)(currentX + 1.5F), (double)labelY, coordLabelColor);
                    currentX += Fonts.sfregular[11].getWidth("z") + 1.0F;
                    Fonts.sfregular[13].drawString(ms, String.format(Locale.US, "%.0f", playerZ), (double)(currentX + 1.5F), (double)textY, textColor);
                    currentX += Fonts.sfregular[13].getWidth(String.format(Locale.US, "%.0f", playerZ));
                    hasPrev = true;
                }

                if (showFps) {
                    if (hasPrev) {
                        currentX += gap;
                        RenderUtil.drawMinecraftRectangle(ms, currentX + 1.5F, posY + 0.5F, separatorWidth, separatorHeight, separatorColor);
                        currentX += separatorWidth + gap;
                    }

                    Fonts.divine_icons[14].drawString(ms, fpsIcon, (double)(currentX + 1.5F), (double)(iconY + 0.5F), logoColor);
                    currentX += iconSize + smallGap;
                    Fonts.sfregular[13].drawString(ms, fpsValueText, (double)(currentX + 1.5F), (double)textY, textColor);
                    currentX += Fonts.sfregular[13].getWidth(fpsValueText) + smallGap;
                    Fonts.sfregular[11].drawString(ms, fpsLabel, (double)(currentX + 1.5F), (double)labelY, labelColor);
                    currentX += Fonts.sfregular[11].getWidth(fpsLabel);
                    hasPrev = true;
                }

                if (showBps) {
                    if (hasPrev) {
                        currentX += dotGap;
                        RenderUtil.drawRoundedRectangle(currentX + 0.5F, posY + totalHeight / 2.0F - dotSize / 2.0F, dotSize, dotSize, 0.5F, dotColor);
                        currentX += dotSize + dotGap;
                    }

                    Fonts.divine_icons[14].drawString(ms, bpsIcon, (double)(currentX + 1.5F), (double)(iconY + 0.5F), logoColor);
                    currentX += iconSize + smallGap;
                    Fonts.sfregular[13].drawString(ms, bpsValueText, (double)(currentX + 1.5F), (double)textY, textColor);
                    currentX += Fonts.sfregular[13].getWidth(bpsValueText) + smallGap;
                    Fonts.sfregular[11].drawString(ms, bpsLabel, (double)(currentX + 1.5F), (double)labelY, labelColor);
                    float var72 = currentX + Fonts.sfregular[11].getWidth(bpsLabel);
                }

                this.dragging.setWidth(totalWidth);
                this.dragging.setHeight(totalHeight);
            }
        }
    }
}
