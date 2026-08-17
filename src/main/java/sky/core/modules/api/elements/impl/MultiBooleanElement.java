package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import org.lwjgl.glfw.GLFW;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.elements.Element;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

import java.util.HashMap;
import java.util.Map;

public class MultiBooleanElement extends Element {
    private final MultiBooleanSetting setting;
    private final Map<BooleanSetting, AnimationUtil> colorAnimations;

    private static final int ACTIVE_TEXT_COLOR = ColorUtil.rgb(0, 0, 0);
    private static final int INACTIVE_TEXT_COLOR = ColorUtil.rgb(187, 187, 187);

    private static final int INACTIVE_BG_COLOR = ColorUtil.rgb(80, 80, 80);
    private static final float INACTIVE_BG_OPACITY = 0.35f;

    public MultiBooleanElement(MultiBooleanSetting setting) {
        this.setting = setting;
        this.colorAnimations = new HashMap<>();
        for (BooleanSetting option : setting.get()) {
            colorAnimations.put(option, new AnimationUtil(option.get() ? 1f : 0f, 10f, Easings.LINEAR));
        }
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        super.render(stack, mouseX, mouseY, alpha);

        float offsetX = 0;
        float offsetY = 0;
        float totalHeight = 0;

        int headerColor = ColorUtil.applyOpacity(
                ThemeEditor.getColor(ThemeSettings.TEXT),
                (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha
        );

        Fonts.sf_regular[14].drawString(stack, setting.getName(), getX() + 6, getY() + 2, headerColor);

        String countText = (int) setting.get().stream().filter(BooleanSetting::get).count() + "/" + setting.get().size();
        float counterX = getX() + getWidth() - 4.5f - Fonts.sfregular[14].getWidth(countText);
        Fonts.sf_regular[14].drawString(stack, countText, counterX, getY() + 2, headerColor);

        float lineLimit = getWidth() - 8f;
        float maxTextWidth = Math.max(0f, lineLimit - 7f);

        for (BooleanSetting option : setting.get()) {
            colorAnimations.computeIfAbsent(option, o -> new AnimationUtil(o.get() ? 1f : 0f, 10f, Easings.LINEAR));

            String optionText = fitTextToWidth(option.getName(), maxTextWidth);

            float textWidth = Fonts.sfregular[12].getWidth(optionText);
            float boxWidth = textWidth + 5;
            float boxHeight = Fonts.sfregular[12].getHeight() + 6;

            float boxW = boxWidth + 2;
            float boxH = boxHeight + 1;

            if (offsetX > 0 && offsetX + boxW >= lineLimit) {
                offsetX = 0;
                offsetY += boxHeight + 2;
            }

            AnimationUtil animation = colorAnimations.get(option);
            animation.update(option.get() ? 1f : 0f);

            float state = clamp01(animation.getValue());
            float offFactor = 1f - state;

            float boxX = getX() + 5.5f + offsetX;
            float boxY = getY() + 10 + offsetY - 0.5f;
            float radius = 2.5f;

            int activeBgColor = ThemeEditor.getColor(ThemeSettings.BUTTON);
            float activeBgAlpha = ThemeEditor.getAlpha(ThemeSettings.BUTTON) / 255F;

            int currentBgColor = lerpColor(INACTIVE_BG_COLOR, activeBgColor, state);
            float currentBgAlpha = lerp(INACTIVE_BG_OPACITY, activeBgAlpha, state) * alpha;

            RenderUtil.drawRoundedRectangle(
                    boxX, boxY, boxW, boxH, radius,
                    ColorUtil.applyOpacity(currentBgColor, currentBgAlpha)
            );

            float rectCenterX = getX() + 5 + offsetX + boxWidth / 2f;
            float rectCenterY = getY() + 10 + offsetY + boxHeight / 2f;

            float textX = rectCenterX - textWidth / 2f;
            float textY = rectCenterY - Fonts.sfregular[12].getHeight() / 2f + 1f;

            int currentTextColor = lerpColor(INACTIVE_TEXT_COLOR, ACTIVE_TEXT_COLOR, state);

            Fonts.sfregular[12].drawString(
                    stack, optionText,
                    textX + 1.5f, textY,
                    ColorUtil.applyOpacity(currentTextColor, alpha)
            );

            offsetX += boxWidth + 3;
            totalHeight = Math.max(totalHeight, offsetY + boxHeight);
        }

        setHeight(9.5f + totalHeight + Fonts.sfregular[14].getHeight() + 2.5f);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            float offsetX = 0;
            float offsetY = 0;

            float lineLimit = getWidth() - 8f;
            float maxTextWidth = Math.max(0f, lineLimit - 7f);

            for (BooleanSetting option : setting.get()) {
                colorAnimations.computeIfAbsent(option, o -> new AnimationUtil(o.get() ? 1f : 0f, 10f, Easings.LINEAR));

                String optionText = fitTextToWidth(option.getName(), maxTextWidth);

                float textWidth = Fonts.sfregular[12].getWidth(optionText);
                float boxWidth = textWidth + 5;
                float boxHeight = Fonts.sfregular[12].getHeight() + 6;

                float boxW = boxWidth + 2;
                float boxH = boxHeight + 1;

                if (offsetX > 0 && offsetX + boxW >= lineLimit) {
                    offsetX = 0;
                    offsetY += boxHeight + 2;
                }

                float boxX = getX() + 5.5f + offsetX;
                float boxY = getY() + 10 + offsetY - 0.5f;

                if (MathUtil.isHovered(mouseX, mouseY, boxX, boxY, boxW, boxH)) {
                    AnimationUtil anim = colorAnimations.get(option);
                    if (anim != null && anim.isAlive() && anim.getValue() != anim.getTarget()) {
                        break;
                    }
                    option.set(!option.get());
                    colorAnimations.get(option).update(option.get() ? 1f : 0f);
                    break;
                }

                offsetX += boxWidth + 3;
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_3 && MathUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            for (BooleanSetting option : setting.get()) {
                colorAnimations.computeIfAbsent(option, o -> new AnimationUtil(o.get() ? 1f : 0f, 10f, Easings.LINEAR));

                AnimationUtil anim = colorAnimations.get(option);
                if (anim != null && anim.isAlive() && anim.getValue() != anim.getTarget()) {
                    continue;
                }
                option.set(option.defaultVal);
                colorAnimations.get(option).update(option.defaultVal ? 1f : 0f);
            }
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }

    private String fitTextToWidth(String text, float maxWidth) {
        if (maxWidth <= 0) return "";

        if (Fonts.sfregular[12].getWidth(text) <= maxWidth) {
            return text;
        }

        String dots = "...";
        float dotsWidth = Fonts.sfregular[12].getWidth(dots);

        if (dotsWidth >= maxWidth) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = builder.toString() + text.charAt(i);
            if (Fonts.sfregular[12].getWidth(next) + dotsWidth > maxWidth) {
                break;
            }
            builder.append(text.charAt(i));
        }

        return builder + dots;
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
