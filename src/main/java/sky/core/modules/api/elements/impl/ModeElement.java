package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
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
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class ModeElement extends Element {
    private final ModeSetting setting;
    private final Map<String, AnimationUtil> colorAnimations;

    private static final int ACTIVE_TEXT_COLOR = ColorUtil.rgb(0, 0, 0);
    private static final int INACTIVE_TEXT_COLOR = ColorUtil.rgb(187, 187, 187);

    private static final int INACTIVE_BG_COLOR = ColorUtil.rgb(80, 80, 80);
    private static final float INACTIVE_BG_OPACITY = 0.35f;

    public ModeElement(ModeSetting setting) {
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

        int headerColor = ColorUtil.applyOpacity(
                ThemeEditor.getColor(ThemeSettings.TEXT),
                (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha
        );

        Fonts.sfregular[14].drawString(stack, setting.getName(), getX() + 6, getY() + 2, headerColor);

        String currentMode = setting.get();
        float modeTextX = getX() + getWidth() - 4.5f - Fonts.sfregular[14].getWidth(currentMode);

        for (String text : setting.strings) {
            float boxWidth = Fonts.sfregular[12].getWidth(text) + 5;
            float boxHeight = Fonts.sfregular[12].getHeight() + 6;

            if (offsetX + boxWidth >= getWidth() - 8) {
                offsetX = 0;
                offsetY += boxHeight + 2;
            }

            AnimationUtil animation = colorAnimations.get(text);
            animation.update(text.equals(setting.get()) ? 1f : 0f);

            float state = clamp01(animation.getValue());
            float offFactor = 1f - state;

            float boxX = getX() + 5.5f + offsetX;
            float boxY = getY() + 10 + offsetY - 0.5f;
            float boxW = boxWidth + 2;
            float boxH = boxHeight + 1.0f;
            float radius = 2.5f;

            int activeBgColor = ThemeEditor.getColor(ThemeSettings.BUTTON);
            float activeBgAlpha = ThemeEditor.getAlpha(ThemeSettings.BUTTON) / 255F;

            int bgColor = lerpColor(INACTIVE_BG_COLOR, activeBgColor, state);
            float bgAlpha = lerp(INACTIVE_BG_OPACITY, activeBgAlpha, state) * alpha;

            RenderUtil.drawRoundedRectangle(boxX, boxY, boxW, boxH, radius,
                    ColorUtil.applyOpacity(bgColor, bgAlpha)
            );

            float rectCenterX = getX() + 5 + offsetX + boxWidth / 2;
            float rectCenterY = getY() + 10 + offsetY + boxHeight / 2;

            float textX = rectCenterX - Fonts.sfregular[12].getWidth(text) / 2;
            float textY = rectCenterY - Fonts.sfregular[12].getHeight() / 2 + 1;

            int textColor = lerpColor(INACTIVE_TEXT_COLOR, ACTIVE_TEXT_COLOR, state);

            Fonts.sfregular[12].drawString(stack, text, textX + 1.5f, textY,
                    ColorUtil.applyOpacity(textColor, alpha)
            );

            offsetX += boxWidth + 3;
            totalHeight = Math.max(totalHeight, offsetY + boxHeight);
        }

        setHeight(9.5f + totalHeight + Fonts.sfregular[14].getHeight() + 2.5f);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            float offsetX = 0, offsetY = 0;

            for (String text : setting.strings) {
                float boxWidth = Fonts.sfregular[12].getWidth(text) + 5;
                float boxHeight = Fonts.sfregular[12].getHeight() + 6;

                if (offsetX + boxWidth >= getWidth() - 8) {
                    offsetX = 0;
                    offsetY += boxHeight + 2;
                }

                if (MathUtil.isHovered(mouseX, mouseY, getX() + 5.5f + offsetX, getY() + 10 + offsetY - 0.5f, boxWidth + 1, boxHeight + 0.5f)) {
                    boolean anyAlive = false;
                    for (AnimationUtil anim : colorAnimations.values()) {
                        if (anim.isAlive() && anim.getValue() != anim.getTarget()) {
                            anyAlive = true;
                            break;
                        }
                    }
                    if (anyAlive) {
                        break;
                    }
                    setting.set(text);
                    for (String mode : setting.strings) {
                        colorAnimations.get(mode).update(mode.equals(setting.get()) ? 1f : 0f);
                    }
                    break;
                }

                offsetX += boxWidth + 3;
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_3 && MathUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            boolean anyAlive = false;
            for (AnimationUtil anim : colorAnimations.values()) {
                if (anim.isAlive() && anim.getValue() != anim.getTarget()) {
                    anyAlive = true;
                    break;
                }
            }
            if (!anyAlive) {
                setting.set(setting.defaultVal);
                for (String mode : setting.strings) {
                    colorAnimations.get(mode).update(mode.equals(setting.get()) ? 1f : 0f);
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * clamp01(progress);
    }

    private static int lerpColor(int from, int to, float progress) {
        progress = clamp01(progress);
        int fr = (from >> 16) & 0xFF, fg = (from >> 8) & 0xFF, fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF, tg = (to >> 8) & 0xFF, tb = to & 0xFF;
        int r = (int) (fr + (tr - fr) * progress);
        int g = (int) (fg + (tg - fg) * progress);
        int b = (int) (fb + (tb - fb) * progress);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
