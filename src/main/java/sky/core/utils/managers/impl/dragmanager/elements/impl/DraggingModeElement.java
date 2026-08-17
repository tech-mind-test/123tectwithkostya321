package sky.core.utils.managers.impl.dragmanager.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.adl.nativeprotect.Native;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.elements.Element;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class DraggingModeElement extends Element {
    @Getter
    private final ModeSetting setting;
    private final Map<String, AnimationUtil> colorAnimations;

    public DraggingModeElement(ModeSetting setting) {
        this.setting = setting;
        this.colorAnimations = new HashMap<>();
        for (String mode : setting.strings) {
            colorAnimations.put(mode, new AnimationUtil(mode.equals(setting.get()) ? 1f : 0f, 10f, Easings.LINEAR));
        }
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        super.render(stack, mouseX, mouseY, alpha);

        float offsetX = 0, offsetY = 0, totalHeight = 0;

        Fonts.sf_medium[12].drawString(stack, setting.getName(), getX() + 3, getY() + 5, ThemeEditor.getColor(ThemeSettings.TEXT));

        for (String text : setting.strings) {
            float boxWidth = Fonts.sf_regular[12].getWidth(text) + 4;
            float boxHeight = Fonts.sf_regular[12].getHeight() + 6;

            if (offsetX + boxWidth >= getWidth() + 20) {
                offsetX = 0;
                offsetY += boxHeight + 1;
            }

            AnimationUtil animation = colorAnimations.get(text);
            animation.update(text.equals(setting.get()) ? 1f : 0f);
            float progress = animation.getValue();

            int currentBg = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.BUTTON), ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE), progress);
            int currentText = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.TEXT), ThemeEditor.getColor(ThemeSettings.TEXT_INACTIVE), progress);

            RenderUtil.drawRoundedRectangle(getX() + offsetX + 2, getY() + 11 + offsetY, boxWidth, boxHeight - 0.5F, 1.5f, ColorUtil.applyOpacity(currentBg, text.equals(setting.get()) ? (ThemeEditor.getAlpha(ThemeSettings.BUTTON) / 255F) * alpha : (ThemeEditor.getAlpha(ThemeSettings.BUTTON_INACTIVE) / 255F) * alpha));

            float rectCenterX = getX() + offsetX + boxWidth / 2.0f;
            float rectCenterY = getY() + 11 + offsetY + boxHeight / 2.0f + 0.5F;

            float textX = rectCenterX - Fonts.sf_regular[12].getWidth(text) / 2.0f;
            float textY = rectCenterY - Fonts.sf_regular[12].getHeight() / 2.0f + 0.5f;

            Fonts.sf_regular[12].drawString(stack, text, textX + 2, textY, ColorUtil.applyOpacity(currentText, text.equals(setting.get()) ? (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha : (ThemeEditor.getAlpha(ThemeSettings.TEXT_INACTIVE) / 255F) * alpha));

            offsetX += boxWidth + 1;
            totalHeight = Math.max(totalHeight, offsetY + boxHeight);
        }

        setHeight(totalHeight + Fonts.sf_medium[12].getHeight() + 7);
    }

    @Native
    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        boolean changed = false;

        if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            float offsetX = 0, offsetY = 0;
            for (String text : setting.strings) {
                float boxWidth = Fonts.sf_regular[12].getWidth(text) + 4;
                float boxHeight = Fonts.sf_regular[12].getHeight() + 6;

                if (offsetX + boxWidth >= getWidth() + 20) {
                    offsetX = 0;
                    offsetY += boxHeight + 1;
                }

                if (MathUtil.isHovered(mouseX, mouseY, getX() + offsetX + 1, getY() + 9 + offsetY, boxWidth + 1, boxHeight + 2)) {
                    setting.set(text);
                    changed = true;
                    break;
                }
                offsetX += boxWidth + 1;
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_3 && MathUtil.isHovered(mouseX, mouseY, getX(), getY() - 1, getWidth(), getHeight())) {
            setting.set(setting.defaultVal);
            changed = true;
        }

        if (changed) {
            colorAnimations.forEach((mode, anim) -> anim.update(mode.equals(setting.get()) ? 1f : 0f));
        }
        super.mouseClicked(mouseX, mouseY, button);
    }
}
