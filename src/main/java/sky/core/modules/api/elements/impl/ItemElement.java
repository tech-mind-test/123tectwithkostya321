package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.api.constructors.impl.ItemSetting;
import sky.core.modules.api.elements.Element;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.Wrapper;
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

@Setter
@Getter
public class ItemElement extends Element implements Wrapper {
    private final ItemSetting setting;
    private final AnimationUtil stateAnimation = new AnimationUtil(0f, 8f, Easings.QUAD_IN_OUT);
    private final AnimationUtil backgroundAnimation = new AnimationUtil(0f, 8f, Easings.QUAD_IN_OUT);
    private boolean selected = false;

    public ItemElement(ItemSetting setting) {
        this.setting = setting;
        stateAnimation.setValue(setting.get() ? 1f : 0f);
        backgroundAnimation.setValue(0f);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        super.render(stack, mouseX, mouseY, alpha);

        backgroundAnimation.update(selected ? 1f : 0f);
        int bgColor = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.FIELD), ThemeEditor.getColor(ThemeSettings.FIELD_INACTIVE), backgroundAnimation.getValue());
        RenderUtil.drawRoundedRectangle(getX(), getY(), 108, 15, 2.5f, bgColor);

        RenderUtil.drawStack(setting.getItemStack(), getX() + 3, getY() + 3.5f, 0.5f);
        Fonts.sf_regular[14].drawScrolledString(stack, setting.getName(), getX() + 13, getY() + 6, 80, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TEXT), (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha), MathUtil.isHovered(mouseX, mouseY, getX() + 13, getY(), 80, Fonts.sf_regular[14].getHeight() + 2), getScrollState());

        RenderUtil.drawRoundedRectangle(getX() + 96, getY() + 3, 9, 9, 2.5f,
                ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE), (ThemeEditor.getAlpha(ThemeSettings.BUTTON_INACTIVE) / 255F) * 0.55f * alpha));

        stateAnimation.update(setting.get() ? 1f : 0f);
        float iconX = getX() + 98.5f;
        float iconY = getY() + 5.5f;
        float scale = stateAnimation.getValue() < 0.5f ? 1 - stateAnimation.getValue() * 2 : (stateAnimation.getValue() - 0.5f) * 2;
        float activeAlpha = (ThemeEditor.getAlpha(ThemeSettings.INDICATOR) / 255F) * stateAnimation.getValue();
        float inactiveAlpha = (ThemeEditor.getAlpha(ThemeSettings.INDICATOR_INACTIVE) / 255F) * (1 - stateAnimation.getValue());
        RenderUtil.scaleStart(iconX + 3f, iconY + 3f, scale);
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/gui/check.png"), iconX - 1, iconY - 1, 6, 6, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.INDICATOR), activeAlpha));
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/gui/xmark.png"), iconX, iconY, 4, 4, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.INDICATOR_INACTIVE), inactiveAlpha));
        RenderUtil.scaleEnd();

        setHeight(20);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && MathUtil.isHovered(mouseX, mouseY, getX() + 96, getY() + 3, 9, 9)) {
            if (stateAnimation.isAlive() && stateAnimation.getValue() != stateAnimation.getTarget()) {
                return;
            }
            setting.set(!setting.get());
        }
        super.mouseClicked(mouseX, mouseY, button);
    }
}
