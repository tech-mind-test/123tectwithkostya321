package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.SkyCore;
import sky.core.modules.api.constructors.impl.BooleanSetting;
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
import lombok.Setter;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.glfw.GLFW;

@Getter
@Setter
public class BooleanElement extends Element {
    private final BooleanSetting setting;
    private final AnimationUtil stateAnimation = new AnimationUtil(0f, 8f, Easings.QUAD_IN_OUT);
    private boolean bind;

    public BooleanElement(BooleanSetting setting) {
        this.setting = setting;
        stateAnimation.setValue(setting.get() ? 1f : 0f);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        super.render(stack, mouseX, mouseY, alpha);

        Fonts.sf_regular[14].drawScrolledString(stack, setting.getName(), getX() + 5, getY() + 2, 80, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TEXT), (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha), MathUtil.isHovered(mouseX, mouseY, getX() + 6, getY(), 80, Fonts.sf_regular[14].getHeight() + 2), getScrollState());

        setHeight(15);

        float toggleX = getX() + getWidth() - 16;
        float toggleY = getY() + getHeight() - 10;

        stateAnimation.update(setting.get() ? 1f : 0f);
        int currentBg = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.BUTTON), ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE), alpha);
        int currentText = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.TEXT), ThemeEditor.getColor(ThemeSettings.TEXT_INACTIVE), alpha);

        RenderUtil.drawOutlineRectangle(toggleX + 0.5f, toggleY - 6, 10, 10, 2.5f, currentBg, 34f * alpha);

        float scale = stateAnimation.getValue() < 0.5f ? 1 - stateAnimation.getValue() * 2 : (stateAnimation.getValue() - 0.5f) * 2;
        float activeAlpha = ThemeEditor.getAlpha(ThemeSettings.INDICATOR) / 255F * alpha;
        float inactiveAlpha = ThemeEditor.getAlpha(ThemeSettings.INDICATOR_INACTIVE) / 255F * alpha;

        RenderUtil.scaleStart(toggleX + 7, toggleY - 1, scale);
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/gui/check.png"), toggleX + 2.5f, toggleY - 4, 6, 6, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.INDICATOR), activeAlpha * stateAnimation.getValue()));
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/gui/xmark.png"), toggleX + 3.5f, toggleY - 3, 4, 4, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.INDICATOR_INACTIVE), inactiveAlpha * (1 - stateAnimation.getValue())));
        RenderUtil.scaleEnd();
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        float toggleX = getX() + getWidth() - 16;
        float toggleY = getY() + (getHeight() - 10) - 7;

        if (MathUtil.isHovered(mouseX, mouseY, toggleX + 0.5f, toggleY + 0.5f, 10, 10)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                if (stateAnimation.isAlive()) {
                    if (stateAnimation.getValue() != stateAnimation.getTarget()) {
                        return;
                    }
                }
                setting.set(!setting.get());
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
                SkyCore.getInstance().getDropDown().getBindingPanelManager().openForBoolean(this);
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_3 && MathUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth() + 10, getHeight())) {
            if (stateAnimation.isAlive()) {
                if (stateAnimation.getValue() != stateAnimation.getTarget()) {
                    return;
                }
            }
            setting.set(setting.defaultVal);
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
