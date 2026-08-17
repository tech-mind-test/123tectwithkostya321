package sky.core.utils.managers.impl.dragmanager.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.adl.nativeprotect.Native;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.elements.Element;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

public class DraggingBooleanElement extends Element {
    @Getter
    private final BooleanSetting setting;
    private final AnimationUtil animation;

    public DraggingBooleanElement(BooleanSetting setting) {
        this.setting = setting;
        this.animation = new AnimationUtil(setting.get() ? 1f : 0f, 10f, Easings.LINEAR);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        animation.update(setting.get() ? 1f : 0f);
        super.render(stack, mouseX, mouseY, alpha);

        Fonts.sf_regular[12].drawString(stack, setting.getName(), getX() + 3, getY() + 4.5f, ThemeEditor.getColor(ThemeSettings.TEXT));

        float progress = animation.getValue();

        int boxColor = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.BUTTON), ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE), progress);
        int textColor = ColorUtil.interpolate(-1, ColorUtil.getColor(255, 255, 255, 175), progress);

        RenderUtil.drawRoundedRectangle(getX() + getWidth() - 18, getY() + 2, 15, 8, 3, boxColor);
        float offsetX = progress * 7f;
        RenderUtil.drawRoundedRectangle(getX() + getWidth() - 17 + offsetX, getY() + 3, 6, 6, 2, textColor);

        setHeight(Math.max(10, Fonts.sf_regular[12].getHeight()));
    }

    @Native
    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        boolean inBounds = mouseX >= getX() + getWidth() - 18 && mouseX <= getX() + getWidth() - 4
                && mouseY >= getY() + 1.5f && mouseY <= getY() + 9.5f;

        if (inBounds) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                setting.set(!setting.get());
                animation.update(setting.get() ? 1f : 0f);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                setting.set(setting.defaultVal);
                animation.update(setting.defaultVal ? 1f : 0f);
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}