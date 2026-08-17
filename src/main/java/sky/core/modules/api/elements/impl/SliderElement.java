package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Setter;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.api.elements.Element;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

@Setter
public class SliderElement extends Element {
    private final SliderSetting setting;
    private boolean drag;
    private float anim;

    public SliderElement(SliderSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        super.render(stack, mouseX, mouseY, alpha);
        setHeight(24);

        String valueStr = setting.get() == Math.floor(setting.get()) ? String.valueOf((int)(float) setting.get()) : String.valueOf(setting.get());
        float valueBoxH = 20 / 2f;
        float valuePad = 3f;
        float valueWidth = Fonts.sfregular[12].getWidth(valueStr);
        float valueBoxW = Math.max(7.5f, valueWidth + valuePad * 2f - 2);
        float valueX = getX() + getWidth() - valueBoxW - 4;

        float textWidth = getWidth() - valueBoxW - 15.5f;

        Fonts.sfregular[14].drawScrolledString(stack, setting.getName(), getX() + 5, getY() + 2, textWidth, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TEXT), (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha), MathUtil.isHovered(mouseX, mouseY, getX() + 6, getY(), textWidth, Fonts.sf_regular[14].getHeight() + 2), getScrollState());

        float targetWidth = (getWidth() - 15) * (setting.get() - setting.min) / (setting.max - setting.min);
        anim = MathUtil.fast(anim, targetWidth, 20);

        RenderUtil.drawRoundedRectangle(valueX, getY() + (getHeight() - valueBoxH) / 2f - 8, valueBoxW, valueBoxH, 2f, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.SLIDER_WINDOW), (ThemeEditor.getAlpha(ThemeSettings.SLIDER_WINDOW) / 255F) * alpha));

        float textX = valueX + (valueBoxW - valueWidth) * 0.5f;

        Fonts.sfregular[12].drawString(stack, valueStr, textX + 0.2f, getY() + 3f, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TEXT), (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha));

        RenderUtil.drawOutlineRectangle(getX() + 5, getY() + 12.5f, getWidth() - 10.5f, 3, 1.5f,
                ThemeEditor.getColor(ThemeSettings.OUTLINE), ThemeEditor.getAlpha(ThemeSettings.OUTLINE) * 0.42f * alpha);
        RenderUtil.drawRoundedRectangle(getX() + 5, getY() + 12, anim + 2, 4, 1.5f, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.SLIDER), (ThemeEditor.getAlpha(ThemeSettings.SLIDER) / 255F) * alpha));
        RenderUtil.drawRoundedRectangle(getX() + 4.5f + anim, getY() + 10.5f, 7, 7, 2.5f, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.SLIDER_CIRCLE), (ThemeEditor.getAlpha(ThemeSettings.SLIDER_CIRCLE) / 255F) * alpha));
    }


    private void applyMouseX(float mouseX) {
        float width = getWidth() - 17;
        float startCenter = getX() + 8.5f;
        float ratio = MathHelper.clamp((mouseX - startCenter) / width, 0.0f, 1.0f);
        float value = setting.min + ratio * (setting.max - setting.min);
        setting.set(MathHelper.clamp(MathUtil.round(value, setting.increment), setting.min, setting.max));
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && MathUtil.isHovered(mouseX, mouseY, getX() + 5, getY() + 10, getWidth() - 10.5f, 6)) {
            drag = true;
            applyMouseX(mouseX);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_3 && MathUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.set(setting.defaultVal);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            drag = false;
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            drag = false;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && drag) {
            applyMouseX((float) mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (Screen.hasControlDown() && isHovered((float) mouseX, (float) mouseY)) {
            float step = setting.increment;
            float rawValue = setting.get() + ((float) delta > 0 ? step : -step);
            float roundedValue = MathUtil.round(rawValue, step);
            float clampedValue = MathHelper.clamp(roundedValue, setting.min, setting.max);
            setting.set(clampedValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
