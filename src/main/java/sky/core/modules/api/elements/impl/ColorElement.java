package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.modules.api.elements.Element;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

@Getter
public class ColorElement extends Element {
    private final ColorSetting setting;
    private final ColorPickerElement colorPicker;

    public ColorElement(ColorSetting setting) {
        this.setting = setting;
        this.colorPicker = new ColorPickerElement(setting);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        super.render(stack, mouseX, mouseY, alpha);


        Fonts.sf_regular[14].drawScrolledString(stack, setting.getName(), getX() + 5, getY() + 2, 80, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TEXT), (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha), MathUtil.isHovered(mouseX, mouseY, getX() + 6, getY(), 80, Fonts.sf_medium[14].getHeight() + 2), getScrollState());
        float selectedCircleRadius = 4f;
        RenderUtil.drawRoundedRectangle(getX() + getWidth() - 21, getY() - 0.5f, selectedCircleRadius * 2 + 1, selectedCircleRadius * 2 + 1, selectedCircleRadius, ColorUtil.applyOpacity(setting.get(), setting.getAlpha() * alpha / 255));

        setHeight(15f);
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && MathUtil.isHovered(mouseX, mouseY, getX(), getY() - 1, getWidth(), getHeight() - 7)) {
            colorPicker.setColorPickMode(!colorPicker.isColorPickMode());
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_3 && MathUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.set(setting.defaultVal);
            colorPicker.updateHSBFromColor();
        } else if (colorPicker.isColorPickMode()) {
            colorPicker.mouseClicked(mouseX, mouseY);
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        if (colorPicker.isColorPickMode()) {
            colorPicker.mouseReleased();
        }
    }
}