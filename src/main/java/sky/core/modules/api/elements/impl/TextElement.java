package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.api.constructors.impl.TextSetting;
import sky.core.modules.api.elements.Element;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.font.Fonts;

public class TextElement extends Element {
    private final TextSetting setting;

    public TextElement(TextSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(MatrixStack matrixStack, float mouseX, float mouseY, float alpha) {
        super.render(matrixStack, mouseX, mouseY, alpha);
        float xPos = getX() + (getWidth() - Fonts.sf_semibold[17].getWidth(setting.getName())) / 2f;

        Fonts.sfregular[17].drawString(matrixStack, setting.getName(), xPos, getY() + 4, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TEXT), (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha));

        setHeight(11 + Fonts.sf_regular[14].getHeight());
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}