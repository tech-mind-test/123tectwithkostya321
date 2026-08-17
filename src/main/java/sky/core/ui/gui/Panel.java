package sky.core.ui.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;
import sky.core.SkyCore;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.elements.IBuilder;
import sky.core.modules.api.elements.ModuleElement;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.ScissorUtil;
import sky.core.utils.render.font.Fonts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
public class Panel implements IBuilder {
    private Category category;
    protected float x;
    protected float y;
    float maxHeight = 0;
    protected float width = 240 / 2f;
    protected float height = 550 / 2f;
    private List<ModuleElement> modules = new ArrayList<>();
    private final List<ModuleElement> allModules = new ArrayList<>();
    private AnimationUtil scrollAnimation = new AnimationUtil(0f, 7.5f, Easings.QUAD_OUT);
    private float scroll;

    public Panel() {
    }

    private void initializePanel(Category category) {
        this.category = category;
        for (Module module : SkyCore.getInstance().getModuleManager().getModules()) {
            if (module.getCategory() == category) {
                ModuleElement component = new ModuleElement(module);
                component.setPanel(this);
                allModules.add(component);
            }
        }
        allModules.sort(Comparator.comparing(moduleElement -> moduleElement.getModule().getName(), String.CASE_INSENSITIVE_ORDER));
        modules.addAll(allModules);
    }

    public Panel(Category category) {
        initializePanel(category);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        scrollAnimation.update(scroll);
        float animatedScroll = scrollAnimation.getValue();

        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(logoColor, 0.1f);
        int bgWithAlpha = ColorUtil.applyOpacity(bgColor, ThemeEditor.getAlpha(ThemeSettings.LOGO) / 255f * alpha);

        RenderUtil.drawRoundedRectangle(x, y + 4, width - 8, height - 4, 10, bgWithAlpha);

        drawHeader(stack, alpha);
        drawContent(stack, mouseX, mouseY, alpha, animatedScroll);
    }

    private void drawHeader(MatrixStack stack, float alpha) {
        final String categoryName = category.name().equals("Misc") ? "Misc" : category.name();
        String categoryIcon = getCategoryIcon();

        float iconWidth = Fonts.skycore[14].getWidth(categoryIcon);
        float textWidth = Fonts.sf_regular[19].getWidth(categoryName);
        float gap = 4f;
        float totalWidth = iconWidth + gap + textWidth;

        float startX = getX() + (getWidth() - 8 - totalWidth) / 2f;
        float textY = getY() + 12 + 1;

        int iconColor = ColorUtil.getColorWithAlpha(ThemeEditor.getColor(ThemeSettings.LOGO), (int) (ThemeEditor.getAlpha(ThemeSettings.LOGO) * alpha));
        int textColor = ColorUtil.getColorWithAlpha(ThemeEditor.getColor(ThemeSettings.HEADER), (int) (ThemeEditor.getAlpha(ThemeSettings.HEADER) * alpha));

        Fonts.skycore[14].drawString(stack, categoryIcon, startX - 4, textY + 5f, iconColor);
        Fonts.sf_regular[19].drawString(stack, categoryName, startX + iconWidth + gap - 4, textY + 2, textColor);
    }

    private String getCategoryIcon() {
        return switch (category) {
            case Combat -> "l";
            case Movement -> "m";
            case Visuals -> "b";
            case Player -> "i";
            case Miscellaneous -> "j";
            default -> "d";
        };
    }

    private void drawContent(MatrixStack stack, float mouseX, float mouseY, float alpha, float animatedScroll) {
        float contentHeight = getHeight() - 34.5f;

        if (maxHeight > contentHeight) {
            scroll = MathHelper.clamp(scroll, -maxHeight + contentHeight, 0);
            animatedScroll = MathHelper.clamp(animatedScroll, -maxHeight + contentHeight, 0);
        } else {
            scroll = animatedScroll = 0;
        }

        float visibleTop = getY() + 28;
        float visibleBottom = visibleTop + height - 28;

        ScissorUtil.start(getX(), getY() + 28, getWidth(), getHeight() - 28.66f);

        float offset = 0;
        for (ModuleElement element : modules) {
            element.setX(getX() + 8);
            element.setY(Math.round(getY() + 30f + offset + animatedScroll));
            element.setWidth(getWidth() - 16);
            element.setHeight(34 / 2f);

            if (element.getAnimation().getValue() > 0) {
                float componentOffset = (float) element.getElements().stream().mapToDouble(sub -> sub.getHeight() * sub.getVisibilityAnimation()).sum();
                float animatedHeight = (34 / 2f) - 1 + componentOffset * element.getAnimation().getValue();
                element.setHeight(animatedHeight);
            }

            float moduleTop = element.getY();
            float moduleBottom = moduleTop + element.getHeight();
            if (moduleBottom > visibleTop && moduleTop < visibleBottom) {
                element.render(stack, mouseX, mouseY, alpha);
            }

            offset += element.getHeight() + 4;
        }
        maxHeight = offset;
        ScissorUtil.end();
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, getX(), getY() + 28, getWidth(), getHeight() - 35)) {
            for (ModuleElement element : modules) {
                float moduleTop = element.getY();
                float moduleBottom = moduleTop + element.getHeight();
                if (moduleBottom > getY() + 28 && moduleTop < getY() + 28 + getHeight() - 35) {
                    if (MathUtil.isHovered(mouseX, mouseY, element.getX(), element.getY(), element.getWidth(), element.getHeight())) {
                        element.mouseClicked(mouseX, mouseY, button);
                    }
                }
            }
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        for (ModuleElement element : modules) {
            element.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (MathUtil.isHovered((float) mouseX, (float) mouseY, getX(), getY() + 28, getWidth(), getHeight() - 35)) {
            float visibleTop = getY() + 28;
            float visibleBottom = visibleTop + getHeight() - 28;
            for (ModuleElement element : modules) {
                float moduleTop = element.getY();
                float moduleBottom = moduleTop + element.getHeight();
                if (moduleBottom > visibleTop && moduleTop < visibleBottom) {
                    if (element.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleElement element : modules) {
            element.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for (ModuleElement element : modules) {
            element.charTyped(codePoint, modifiers);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for (ModuleElement element : modules) {
            if (element.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        if (MathUtil.isHovered((float) mouseX, (float) mouseY, getX(), getY(), getWidth(), getHeight())) {
            float contentHeight = getHeight() - 34.5f;
            boolean canScroll = maxHeight > contentHeight;
            float previousScroll = scroll;

            setScroll((float) (getScroll() + (delta * 20)));
            scroll = MathHelper.clamp(scroll, -maxHeight + contentHeight, 0);

            return canScroll && scroll != previousScroll;
        }
        return false;
    }
}