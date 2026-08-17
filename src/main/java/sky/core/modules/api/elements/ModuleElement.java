package sky.core.modules.api.elements;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.Setting;
import sky.core.modules.api.constructors.impl.*;
import sky.core.modules.api.elements.impl.*;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.ScissorUtil;
import sky.core.utils.render.font.Fonts;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ModuleElement extends Element {
    private final Module module;
    private final AnimationUtil animation = new AnimationUtil(0.0f, 12, Easings.CIRC_IN_OUT);
    private final AnimationUtil alphaAnimation = new AnimationUtil(0.0f, 12);
    public boolean open;
    private boolean bind;
    private final ObjectArrayList<Element> elements = new ObjectArrayList<>();

    public ModuleElement(Module module) {
        this.module = module;
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting booleanSetting) {
                elements.add(new BooleanElement(booleanSetting));
            }
            if (setting instanceof SliderSetting sliderSetting) {
                elements.add(new SliderElement(sliderSetting));
            }
            if (setting instanceof BindSetting bindSetting) {
                elements.add(new BindElement(bindSetting));
            }
            if (setting instanceof ModeSetting modeSetting) {
                elements.add(new ModeElement(modeSetting));
            }
            if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
                elements.add(new MultiBooleanElement(multiBooleanSetting));
            }
            if (setting instanceof ColorSetting colorSetting) {
                elements.add(new ColorElement(colorSetting));
            }
            if (setting instanceof StringSetting stringSetting) {
                elements.add(new StringElement(stringSetting));
            }
            if (setting instanceof TextSetting textSetting) {
                elements.add(new TextElement(textSetting));
            }
            if (setting instanceof ButtonSetting buttonSetting) {
                elements.add(new ButtonElement(buttonSetting, buttonSetting.getTextOn(), buttonSetting.getTextOff()));
            }
            if (setting instanceof ClickSetting clickSetting) {
                elements.add(new ClickElement(clickSetting));
            }
        }
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        RenderUtil.beginNoLiquidGlass();
        try {
            module.getAnimation().update(module.isEnabled() ? 1f : 0f);
            float enabledState = module.getAnimation().getValue();

            RenderUtil.drawRoundedRectangle(getX() - 3, getY() - 0.2f, getWidth() - 1, getHeight() + 2f, 5, ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.FIELD), ThemeEditor.getColor(ThemeSettings.FIELD_INACTIVE), enabledState * alpha));
            float textX = getX() + 2;
            int textY = (int) (getY() + (18 - Fonts.sfregular[16].getHeight()) / 2);

            int disabledTextColor = ThemeEditor.getColor(ThemeSettings.TEXT);
            int enabledTextColor = ColorUtil.darken(ThemeEditor.getColor(ThemeSettings.TEXT), 0.65f);
            int textColor = ColorUtil.interpolate(disabledTextColor, enabledTextColor, enabledState);

            Fonts.sf_regular[15].drawString(stack, module.getName(), textX + 1, textY + 1.5f, ColorUtil.getColorWithAlpha(textColor, (int) (ThemeEditor.getAlpha(ThemeSettings.TEXT) * alpha)));

            if (elements.stream().anyMatch(Element::isVisible)) {
                int enabledIconColor = ColorUtil.rgb(255, 255, 255);
                int disabledIconColor = ColorUtil.rgb(170, 170, 170);
                int iconColor = ColorUtil.interpolate(enabledIconColor, disabledIconColor, enabledState);

                Fonts.mototanya[12].drawString(stack, "i", getX() + getWidth() - 15, textY + 3f, ColorUtil.getColorWithAlpha(iconColor, (int) (alpha * 255)));
            }

            drawComponents(stack, mouseX, mouseY, alpha);
            super.render(stack, mouseX, mouseY, alpha);
        } finally {
            RenderUtil.endNoLiquidGlass();
        }
    }

    private boolean isModuleHeaderHovered(float mouseX, float mouseY) {
        return MathUtil.isHovered(mouseX, mouseY, getX(), getY() + 1f, getWidth() - 4f, 16f);
    }

    public void drawComponents(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        animation.update(open ? 1.0f : 0.0f);
        alphaAnimation.update(open ? 1.0f : 0.0f);

        if (animation.getValue() > 0.01) {
            ScissorUtil.start(getX(), getY(), getWidth(), getHeight());
            float baseYOffset = -8 * (1 - animation.getValue());
            float baseY = getY() + 18 + baseYOffset;
            float currentYOffset = 0;

            for (Element element : elements) {
                float visibleValue = element.getVisibilityAnimation();

                if (visibleValue > 0) {
                    float offsetY = -8 * (1.0f - visibleValue);
                    float targetY = baseY + currentYOffset + offsetY;
                    float currentY = baseY + (targetY - baseY) * animation.getValue();

                    element.setX(Math.round(getX()) - 3f);
                    element.setY(Math.round(currentY));
                    element.setWidth(getWidth());

                    float alphaFade = alphaAnimation.getValue() * visibleValue * alpha;
                    element.render(stack, mouseX, mouseY, alphaFade);

                    currentYOffset += element.getHeight() * visibleValue;
                }
            }
            ScissorUtil.end();
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Element element : elements) {
            if (element.isVisible()) element.keyPressed(keyCode, scanCode, modifiers);
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for (Element element : elements) {
            if (element.isVisible()) element.charTyped(codePoint, modifiers);
        }
        super.charTyped(codePoint, modifiers);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        if (isInScissorZone(mouseX, mouseY) && isModuleHeaderHovered(mouseX, mouseY)) {
            if (button == 0) {
                module.toggle();
            } else if (button == 1) {
                if (elements.stream().anyMatch(Element::isVisible)) {
                    open = !open;
                }
            } else if (button == 2) {
                bind = !bind;
            }
        }

        if (open && animation.getValue() > 0.99f) {
            if (isInScissorZone(mouseX, mouseY)) {
                for (int i = elements.size() - 1; i >= 0; i--) {
                    Element element = elements.get(i);
                    if (!element.isVisible()) continue;
                    if (element.getVisibilityAnimation() < 0.99f) continue;
                    if (element.isHovered(mouseX, mouseY)) {
                        element.mouseClicked(mouseX, mouseY, button);
                        break;
                    }
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (open) {
            for (Element element : elements) {
                if (element.isVisible() && element.mouseScrolled(mouseX, mouseY, delta)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (open) {
            for (Element element : elements) {
                if (element.isVisible() && element.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInScissorZone(float mouseX, float mouseY) {
        if (getPanel() == null) return false;
        return MathUtil.isHovered(mouseX, mouseY, getPanel().getX(), getPanel().getY() + 28, getPanel().getWidth(), getPanel().getHeight() - 35);
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        for (Element element : elements) {
            element.mouseReleased(mouseX, mouseY, button);
        }
        super.mouseReleased(mouseX, mouseY, button);
    }
}
