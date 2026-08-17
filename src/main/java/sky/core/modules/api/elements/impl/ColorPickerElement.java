package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Data;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.ByteBuffer;

import static sky.core.utils.Wrapper.mc;

@Data
public class ColorPickerElement {
    private static ColorPickerElement currentlyOpenPicker = null;
    private boolean colorPickMode = false;
    private boolean eyeDropperActive = false;
    private float x, y;
    private final ColorSetting setting;
    private float hue, saturation, brightness, alpha;
    private boolean isDraggingPicker = false;
    private boolean isDraggingHue = false;
    private boolean isDraggingAlpha = false;
    private final AnimationUtil openAnimation = new AnimationUtil(0f, 20, Easings.LINEAR);
    private final AnimationUtil copyAnimation = new AnimationUtil(0f, 6, Easings.LINEAR);
    private final AnimationUtil pasteAnimation = new AnimationUtil(0f, 6, Easings.LINEAR);
    private final AnimationUtil pipetteAnimation = new AnimationUtil(0f, 10, Easings.LINEAR);

    public ColorPickerElement(ColorSetting setting) {
        this.setting = setting;
        updateHSBFromColor();
    }

    public void setColorPickMode(boolean colorPickMode) {
        if (colorPickMode && currentlyOpenPicker != this) {
            if (currentlyOpenPicker != null) {
                currentlyOpenPicker.colorPickMode = false;
                currentlyOpenPicker.openAnimation.update(0f);
            }
            currentlyOpenPicker = this;
            updateHSBFromColor();
            openAnimation.setValue(0f);
        } else if (!colorPickMode && currentlyOpenPicker == this) {
            currentlyOpenPicker = null;
        }
        this.colorPickMode = colorPickMode;
        openAnimation.update(colorPickMode ? 1f : 0f);
        if (colorPickMode) {
            isDraggingPicker = false;
            isDraggingHue = false;
            isDraggingAlpha = false;
        }
    }

    public void render(MatrixStack stack, float mouseX, float mouseY) {
        openAnimation.update(colorPickMode ? 1f : 0f);
        if (copyAnimation.getTarget() == 1f && copyAnimation.isDone()) {
            copyAnimation.update(0f);
        } else if (!copyAnimation.isDone()) {
            copyAnimation.update(copyAnimation.getTarget());
        }
        if (pasteAnimation.getTarget() == 1f && pasteAnimation.isDone()) {
            pasteAnimation.update(0f);
        } else if (!pasteAnimation.isDone()) {
            pasteAnimation.update(pasteAnimation.getTarget());
        }
        boolean showAlpha = setting.alphaBar;
        float alphaOffset = showAlpha ? 0 : -10f;
        float PICKER_WIDTH = 93;
        float PICKER_HEIGHT = 100 + (showAlpha ? 10 : 0);

        pipetteAnimation.update(pipetteAnimation.getTarget());
        RenderUtil.scaleStart(x + PICKER_WIDTH / 2.0f, y + PICKER_HEIGHT / 2.0f, openAnimation.getValue());
        RenderUtil.drawBlurredRoundedRectangle(Math.round(x + 7), Math.round(y), PICKER_WIDTH, PICKER_HEIGHT, 4, ThemeEditor.getColor(ThemeSettings.MAIN), 1);

        RenderUtil.drawRoundedRectangleGradient(x + 11, y + 4.5f, 84, 70, 3, ColorUtil.getColor(255, 255, 255, 255), Color.BLACK.getRGB(), new Color(Color.HSBtoRGB(hue, 1, 1)).getRGB(), Color.BLACK.getRGB(), 1);
        RenderUtil.drawOutlineRectangle(Math.max(x + 13, Math.min(x + 89, x + 11 + saturation * 84)), Math.max(y + 6, Math.min(y + 68, y + 4 + (1 - brightness) * 70)), 4, 4, 1.5f, -1, 255);

        float hueX = x + 13;
        float hueY = y + 80;
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/colorpicker/img.png"), hueX, hueY, 82, 4, -1);
        RenderUtil.drawMinecraftRectangle(stack, hueX + hue * 82, hueY - 1, 1, 6, -1);

        float alphaX = x + 13;
        float alphaY = y + 80 + 10;
        if (showAlpha) {
            RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/colorpicker/opacity.png"), alphaX, alphaY, 82, 4, -1);
            RenderUtil.drawMinecraftRectangle(stack, alphaX + alpha * 82, alphaY - 1, 1, 6, -1);
        }

        float copyAnim = copyAnimation.getValue();
        int copyBg = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.BUTTON), ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE), copyAnim);
        int copyText = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.TEXT), ThemeEditor.getColor(ThemeSettings.TEXT_INACTIVE), copyAnim);
        RenderUtil.drawRoundedRectangle(x + 11, y + 96 + alphaOffset, 37, 10, 1.5f, copyBg);
        Fonts.sf_regular[12].drawString(stack, "Копировать", x + 11 + (37 - Fonts.sf_regular[12].getWidth("Копировать")) / 2f, y + 100 + alphaOffset, copyText);

        float pasteAnim = pasteAnimation.getValue();
        int pasteBg = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.BUTTON), ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE), pasteAnim);
        int pasteText = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.TEXT), ThemeEditor.getColor(ThemeSettings.TEXT_INACTIVE), pasteAnim);
        RenderUtil.drawRoundedRectangle(x + 49, y + 96 + alphaOffset, 37, 10, 1.5f, pasteBg);
        Fonts.sf_regular[12].drawString(stack, "Вставить", x + 49 + (37 - Fonts.sf_regular[12].getWidth("Вставить")) / 2f, y + 100 + alphaOffset, pasteText);

        float pipetteAnim = pipetteAnimation.getValue();
        int pipetteBg = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.BUTTON), ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE), pipetteAnim);
        int pipetteText = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.TEXT), ThemeEditor.getColor(ThemeSettings.TEXT_INACTIVE), pipetteAnim);
        RenderUtil.drawRoundedRectangle(x + 87, y + 96 + alphaOffset, 10, 10, 1.5f, pipetteBg);
        Fonts.icons[12].drawString(stack, "R", x + 89, y + 101 + alphaOffset, pipetteText);
        RenderUtil.scaleEnd();
        updateDraggingStates(mouseX, mouseY);
    }

    private void updateDraggingStates(float mouseX, float mouseY) {
        if (isDraggingPicker) {
            updateColorFromMouse(mouseX, mouseY);
        }
        if (isDraggingHue) {
            updateHueFromMouse(mouseX);
        }
        if (isDraggingAlpha) {
            updateAlphaFromMouse(mouseX);
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY) {
        boolean showAlpha = setting.alphaBar;
        float alphaOffset = showAlpha ? 0f : -10f;

        if (MathUtil.isHovered(mouseX, mouseY, x + 87, y + 96 + alphaOffset, 10, 10)) {
            eyeDropperActive = !eyeDropperActive;
            pipetteAnimation.update(eyeDropperActive ? 1f : 0f);
            return true;
        }

        if (eyeDropperActive) {
            getColorFromScreen(mouseX, mouseY);
            eyeDropperActive = false;
            pipetteAnimation.update(0f);
            return true;
        }

        if (MathUtil.isHovered(mouseX, mouseY, x + 11, y + 96 + alphaOffset, 37, 10)) {
            if (copyAnimation.isDone()) {
                copyColorToClipboard();
                copyAnimation.update(1f);
            }
            return true;
        }

        if (MathUtil.isHovered(mouseX, mouseY, x + 49, y + 96 + alphaOffset, 37, 10)) {
            if (pasteAnimation.isDone()) {
                pasteColorFromClipboard();
                pasteAnimation.update(1f);
            }
            return true;
        }

        if (MathUtil.isHovered(mouseX, mouseY, x + 11, y + 4, 84, 70)) {
            isDraggingPicker = true;
            updateColorFromMouse(mouseX, mouseY);
            return true;
        }

        if (MathUtil.isHovered(mouseX, mouseY, x + 13, y + 80, 82, 6)) {
            isDraggingHue = true;
            updateHueFromMouse(mouseX);
            return true;
        }
        if (showAlpha) {
            if (MathUtil.isHovered(mouseX, mouseY, x + 13, y + 90, 82, 6)) {
                isDraggingAlpha = true;
                updateAlphaFromMouse(mouseX);
                return true;
            }
        }

        return false;
    }

    public void mouseReleased() {
        isDraggingPicker = false;
        isDraggingHue = false;
        isDraggingAlpha = false;
        updateSettingColor();
    }

    private void updateColorFromMouse(float mouseX, float mouseY) {
        float pickerX = x + 13;
        float pickerY = y + 7;

        float clampedX = Math.max(pickerX, Math.min(mouseX, pickerX + 84));
        float clampedY = Math.max(pickerY, Math.min(mouseY, pickerY + 70));

        saturation = (clampedX - pickerX) / 84;
        brightness = 1.0f - (clampedY - pickerY) / 70;

        saturation = Math.max(0.0f, Math.min(1.0f, saturation));
        brightness = Math.max(0.0f, Math.min(1.0f, brightness));

        updateSettingColor();
    }

    private void updateHueFromMouse(float mouseX) {
        float hueX = x + 13;
        float width = 82f;

        float clamped = Math.max(hueX, Math.min(mouseX, hueX + width));
        hue = (clamped - hueX) / width;

        hue = Math.max(0.0f, Math.min(1.0f, hue));
        updateSettingColor();
    }


    private void updateAlphaFromMouse(float mouseX) {
        if (!setting.alphaBar) return;

        float alphaX = x + 13;
        float width = 82f;

        float clamped = Math.max(alphaX, Math.min(mouseX, alphaX + width));
        alpha = (clamped - alphaX) / width;

        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        updateSettingColor();
    }



    public void updateHSBFromColor() {
        Color color = new Color(setting.get(), true);
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        if (setting.alphaBar) {
            this.alpha = color.getAlpha() / 255.0f;
        } else {
            this.alpha = 1.0f;
            int rgb = setting.get() & 0xFFFFFF;
            setting.set((255 << 24) | rgb);
        }
    }

    private void pasteColorFromClipboard() {
        try {
            String clipboardText = mc.keyboardListener.getClipboardString().trim();

            if (!clipboardText.matches("^#([A-Fa-f0-9]{6,8})$")) {
                return;
            }

            parseAndSetColor(clipboardText.substring(1).toUpperCase());
            pasteAnimation.setValue(0f);
            pasteAnimation.update(1f);
        } catch (Exception ignored) {
        }
    }

    private void parseAndSetColor(String hex) {
        int r, g, b, a = 255;

        if (hex.length() == 6) {
            r = Integer.parseInt(hex.substring(0, 2), 16);
            g = Integer.parseInt(hex.substring(2, 4), 16);
            b = Integer.parseInt(hex.substring(4, 6), 16);
        } else {
            try {
                r = Integer.parseInt(hex.substring(0, 2), 16);
                g = Integer.parseInt(hex.substring(2, 4), 16);
                b = Integer.parseInt(hex.substring(4, 6), 16);
                a = Integer.parseInt(hex.substring(6, 8), 16);
            } catch (Exception e) {
                a = Integer.parseInt(hex.substring(0, 2), 16);
                r = Integer.parseInt(hex.substring(2, 4), 16);
                g = Integer.parseInt(hex.substring(4, 6), 16);
                b = Integer.parseInt(hex.substring(6, 8), 16);
            }
        }

        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        if (setting.alphaBar) {
            this.alpha = a / 255.0f;
        } else {
            this.alpha = 1.0f;
            a = 255;
        }

        setting.set((a << 24) | (new Color(r, g, b).getRGB() & 0xFFFFFF));
    }

    private void copyColorToClipboard() {
        Color color = new Color(setting.get(), true);
        String hex = String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        mc.keyboardListener.setClipboardString(hex);
        copyAnimation.setValue(0f);
        copyAnimation.update(1f);
    }

    private void getColorFromScreen(float mouseX, float mouseY) {
        Framebuffer framebuffer = mc.getFramebuffer();
        double guiScale = mc.getMainWindow().getGuiScaleFactor();

        int fbX = (int) (mouseX * guiScale);
        int fbY = framebuffer.framebufferHeight - (int) (mouseY * guiScale) - 1;

        if (fbX < 0 || fbX >= framebuffer.framebufferWidth || fbY < 0 || fbY >= framebuffer.framebufferHeight) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(3);
        GL11.glReadPixels(fbX, fbY, 1, 1, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);

        int r = buffer.get(0) & 0xFF;
        int g = buffer.get(1) & 0xFF;
        int b = buffer.get(2) & 0xFF;

        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];

        int currentAlpha = (setting.get() >> 24) & 0xFF;
        if (!setting.alphaBar) currentAlpha = 255;
        setting.set((currentAlpha << 24) | (r << 16) | (g << 8) | b);
    }

    private void updateSettingColor() {
        int alphaValue = (int) (alpha * 255);
        if (!setting.alphaBar) alphaValue = 255;
        int newColor = Color.HSBtoRGB(hue, saturation, brightness);
        setting.set((alphaValue << 24) | (newColor & 0xFFFFFF));
    }

    public void onClose() {
        updateSettingColor();
        mouseReleased();
        eyeDropperActive = false;
        setColorPickMode(false);
        copyAnimation.setValue(0f);
        pasteAnimation.setValue(0f);
        pipetteAnimation.setValue(0f);
    }
}
