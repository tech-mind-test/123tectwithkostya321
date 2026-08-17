package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.api.constructors.impl.ClickSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.modules.api.elements.Element;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import org.lwjgl.glfw.GLFW;

public class ClickElement extends Element {
    private final ClickSetting setting;
    private float width, height;
    private final AnimationUtil hoverAnimation = new AnimationUtil(0.0f, 10, Easings.LINEAR);

    public ClickElement(ClickSetting setting) {
        this.setting = setting;
        setHeight(20);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        super.render(stack, mouseX, mouseY, alpha);

        width = 94;
        height = 16.0f;
        float toggleX = getX() + 6.5f;
        float toggleY = getY() - 2;

        boolean isHovered = MathUtil.isHovered(mouseX, mouseY, toggleX, toggleY, width, height);
        hoverAnimation.update(isHovered ? 1.0f : 0.0f);

        int interpolatedBg = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.BUTTON), ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE), hoverAnimation.getValue());

        float bgAlpha = (isHovered ? ThemeEditor.getAlpha(ThemeSettings.BUTTON) : ThemeEditor.getAlpha(ThemeSettings.BUTTON_INACTIVE)) / 255F * alpha;
        int finalBg = ColorUtil.applyOpacity(interpolatedBg, bgAlpha);
        RenderUtil.drawRoundedRectangle(toggleX - 1, toggleY, width - 3, height - 2, 2.5f, finalBg);

        String displayText = setting.getName();
        float textWidth = Fonts.sfregular[13].getWidth(displayText);

        float rectCenterX = toggleX + width / 2;

        int textX = (int) (rectCenterX - textWidth / 2) - 1;
        float textY = getY() + (getHeight() - 14.5f) - 2;

        int interpolatedText = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.TEXT), ThemeEditor.getColor(ThemeSettings.TEXT_INACTIVE), hoverAnimation.getValue());
        float txtAlpha = (isHovered ? ThemeEditor.getAlpha(ThemeSettings.TEXT) : ThemeEditor.getAlpha(ThemeSettings.TEXT_INACTIVE)) / 255F * alpha;
        int finalText = ColorUtil.applyOpacity(interpolatedText, txtAlpha);

        Fonts.sfregular[13].drawString(stack, displayText, textX, textY, finalText);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, getX() + 3, getY() - 4, width + 1, height + 2)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                setting.performAction();
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}