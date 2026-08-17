package sky.core.ui.gui.themes;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import sky.core.SkyCore;
import sky.core.utils.managers.impl.ClientConfig;
import sky.core.utils.managers.impl.ThemeManager;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.modules.api.constructors.impl.StringSetting;
import sky.core.modules.api.elements.impl.ColorElement;
import sky.core.modules.api.elements.impl.StringElement;
import sky.core.ui.gui.Panel;
import sky.core.ui.gui.guihandlers.ColorPickerHandler;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.misc.ChatUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.ScissorUtil;
import sky.core.utils.render.font.Fonts;

import java.util.*;

@Setter
@Getter
public class ThemeEditor extends Panel {
    public static final EnumMap<ThemeSettings, ColorSetting> colorSettings = new EnumMap<>(ThemeSettings.class);
    public static List<StringSetting> stringSettings = new ArrayList<>();
    public static List<StringElement> stringElements = new ArrayList<>();
    private final List<ColorElement> colorPickers = new ArrayList<>();
    private final Map<String, int[]> defaultPresets = new LinkedHashMap<>();
    private final LinkedHashMap<String, int[]> customPresets = new LinkedHashMap<>();
    private final ThemeManager themeManager = new ThemeManager();
    private final float presetCircleRadius = 4.5f;
    private final float presetCircleSpacing = 1;
    private ThemePresetContextMenu contextMenu = null;
    private ThemePreset currentPreset = null;
    private ThemePreset lastPreset = null;
    private final ColorPickerHandler colorPickerHandler;
    private static final float NAME_FIELD_WIDTH = 148 / 2f;
    private static final float NAME_FIELD_HEIGHT = 22 / 2f;
    private static final float NAME_FIELD_Y_OFFSET = 30f;
    private static final float CREATE_BUTTON_X_OFFSET = 84f;
    private static final float CREATE_BUTTON_Y_OFFSET = 30f;
    private static final float PRESET_CIRCLES_BOTTOM_OFFSET = 14f;
    private static final float PICKERS_Y_OFFSET = 48f;
    private static final float PICKERS_TO_PRESETS_GAP = 8f;
    float scroll, animatedScroll;
    private final AnimationUtil scrollAnimation = new AnimationUtil(0f, 10, Easings.SINE_OUT);
    private final AnimationUtil createAnimation = new AnimationUtil(0f, 3f, Easings.LINEAR);
    private final Map<String, AnimationUtil> presetTooltipAnimations = new HashMap<>();
    float maxHeight = 0;
    private boolean draggingScrollBar;
    private float scrollDragOffset;

    public ThemeEditor() {
        this.width = 240 / 2f;
        this.height = 550 / 2f;
        this.colorPickerHandler = new ColorPickerHandler(Collections.singletonList(this));
        initThemeSettings();
        loadCustomThemes();
        loadCurrentThemeFromConfig();
    }

    private void initThemeSettings() {
        StringSetting nameSetting = new StringSetting("Название");
        stringSettings.add(nameSetting);
        stringElements.add(new StringElement(nameSetting));

        for (ThemeSettings setting : ThemeSettings.values()) {
            colorSettings.put(setting, new ColorSetting(setting.getDisplayName(), true, 0));
        }

        for (ColorSetting colorSetting : colorSettings.values()) {
            colorSetting.setOnChange(this::updateAndSavePreset);
        }

        initDefaultPresets();
        for (ThemeSettings setting : ThemeSettings.values()) {
            colorPickers.add(new ColorElement(colorSettings.get(setting)));
        }
    }

    private void updateAndSavePreset() {
        if (currentPreset != null && currentPreset.isCustom()) {
            int[] updatedColors = new int[colorSettings.size()];
            ThemeSettings[] settings = ThemeSettings.values();
            for (int i = 0; i < settings.length; i++) {
                updatedColors[i] = colorSettings.get(settings[i]).get();
            }
            String creator = "NexusUser";
            String cleanPresetName = currentPreset.getName().replace("Название: ", "");
            currentPreset = new ThemePreset(currentPreset.getName(), updatedColors, creator, true);
            customPresets.put(currentPreset.getName(), updatedColors);
            themeManager.saveTheme(cleanPresetName, currentPreset, creator);
        }
    }

    public static int getColor(ThemeSettings setting) {
        return colorSettings.get(setting).get();
    }

    public static float getAlpha(ThemeSettings setting) {
        return colorSettings.get(setting).getAlpha();
    }

    private void initDefaultPresets() {
        defaultPresets.clear();
        defaultPresets.putAll(DefaultThemePresets.getDefaultPresets());

        String presetToUse = "Название: Прозрачная";
        int[] colors = defaultPresets.get(presetToUse);
        if (colors == null && !defaultPresets.isEmpty()) {
            Map.Entry<String, int[]> first = defaultPresets.entrySet().iterator().next();
            presetToUse = first.getKey();
            colors = first.getValue();
        }
        boolean isCustom = false;

        currentPreset = new ThemePreset(presetToUse, colors, "Unknown", isCustom);
        ClientConfig.setCurrentActiveTheme(presetToUse);

        ThemeSettings[] settings = ThemeSettings.values();
        for (int i = 0; i < Math.min(colors.length, settings.length); i++) {
            colorSettings.get(settings[i]).set(colors[i]);
        }
    }

    private void renderpreset(MatrixStack stack, float mouseX, float mouseY) {
        float startY = y + height - PRESET_CIRCLES_BOTTOM_OFFSET;
        float startX = x + 6;
        int index = 0;

        index = renderPresetList(stack, mouseX, mouseY, defaultPresets, index, startX, startY);
        renderPresetList(stack, mouseX, mouseY, customPresets, index, startX, startY);
    }

    private int renderPresetList(MatrixStack stack, float mouseX, float mouseY, Map<String, int[]> presets, int startIndex, float startX, float startY) {
        for (Map.Entry<String, int[]> preset : presets.entrySet()) {
            if (startIndex >= 22) break;

            float circleX = startX + (presetCircleRadius * 2 + presetCircleSpacing) * (startIndex % 11) + 4;
            float circleY = startY + ((startIndex >= 11) ? (presetCircleRadius * 2 + presetCircleSpacing) : 0);

            boolean hovered = MathUtil.isHovered(mouseX, mouseY, circleX - presetCircleRadius, circleY - presetCircleRadius, presetCircleRadius * 2, presetCircleRadius * 2);

            float selectedCircleRadius = 2.5f;
            RenderUtil.drawRoundedRectangle(circleX - presetCircleRadius, circleY - presetCircleRadius, 10, 10, presetCircleRadius - 0.5f, preset.getValue()[ThemeSettings.LOGO.ordinal()]);

            String activeTheme = ClientConfig.getCurrentActiveTheme();
            if (preset.getKey().equals(activeTheme)) {
                RenderUtil.drawRoundedRectangle(circleX - selectedCircleRadius + 0.5f, circleY - selectedCircleRadius + 0.5f, selectedCircleRadius * 2, selectedCircleRadius * 2, selectedCircleRadius - 1, ColorUtil.getColor(0, 0, 0, 255));
            }

            AnimationUtil anim = presetTooltipAnimations.computeIfAbsent(preset.getKey(), key -> new AnimationUtil(0f, 15f, Easings.LINEAR));
            anim.update(hovered ? 1f : 0f);

            startIndex++;
        }
        return startIndex;
    }

    public float getPickersScissorTop() {
        return y + PICKERS_Y_OFFSET;
    }

    public float getPickersScissorBottom() {
        float presetsTopY = y + height - PRESET_CIRCLES_BOTTOM_OFFSET - presetCircleRadius;
        return Math.max(getPickersScissorTop(), presetsTopY - PICKERS_TO_PRESETS_GAP);
    }

    private float getScrollAreaY() {
        return y + PICKERS_Y_OFFSET;
    }

    private float getScrollAreaHeight() {
        return Math.max(0f, getPickersScissorBottom() - getScrollAreaY());
    }

    private float getThemeThumbHeight(float contentHeight) {
        return Math.max(14f, (contentHeight / maxHeight) * getScrollAreaHeight() * 0.8f);
    }

    private boolean isHoveringThemeScrollThumb(float mouseX, float mouseY) {
        float contentHeight = getHeight() - 46;
        if (maxHeight <= contentHeight) return false;

        float thumbHeight = getThemeThumbHeight(contentHeight);
        float maxThumbTravel = Math.max(0f, getScrollAreaHeight() - thumbHeight);
        float thumbPosition = maxThumbTravel <= 0f ? 0f : (-animatedScroll / (maxHeight - contentHeight)) * maxThumbTravel;
        float scrollX = x + getWidth() - 4.5f;

        return MathUtil.isHovered(mouseX, mouseY, scrollX - 4.5f, getScrollAreaY() + thumbPosition - 2f, 11f, thumbHeight + 4f);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        scrollAnimation.update(scroll);
        animatedScroll = scrollAnimation.getValue();
        if (createAnimation.getTarget() == 1f && createAnimation.isDone()) {
            createAnimation.update(0f);
        }
        if (!createAnimation.isDone()) {
            createAnimation.update(createAnimation.getTarget());
        }

        float contentHeight = getHeight() - 46;
        if (maxHeight > contentHeight) {
            scroll = MathHelper.clamp(scroll, -maxHeight + contentHeight, 0);
            animatedScroll = MathHelper.clamp(animatedScroll, -maxHeight + contentHeight, 0);
        } else {
            scroll = animatedScroll = 0;
        }

        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(logoColor, 0.1f);
        int bgWithAlpha = ColorUtil.applyOpacity(bgColor, ThemeEditor.getAlpha(ThemeSettings.LOGO) / 255f * alpha);

        RenderUtil.drawRoundedRectangle(x, y + 4, getWidth() - 8, getHeight() - 4, 10, bgWithAlpha);

        String categoryIcon = "";
        String title = "Theme Editor";

        float iconWidth = Fonts.skycore[14].getWidth(categoryIcon);
        float titleWidth = Fonts.sf_regular[19].getWidth(title);
        float headerGap = 4f;
        float totalHeaderWidth = iconWidth + headerGap + titleWidth;

        float headerStartX = getX() + (getWidth() - 8 - totalHeaderWidth) / 2f;
        float headerTextY = getY() + 12 + 1;

        int iconColor = ColorUtil.getColorWithAlpha(ThemeEditor.getColor(ThemeSettings.LOGO), (int) (ThemeEditor.getAlpha(ThemeSettings.LOGO) * alpha));
        int headerTextColor = ColorUtil.getColorWithAlpha(ThemeEditor.getColor(ThemeSettings.HEADER), (int) (ThemeEditor.getAlpha(ThemeSettings.HEADER) * alpha));

        Fonts.skycore[14].drawString(stack, categoryIcon, headerStartX - 4, headerTextY + 5f, iconColor);
        Fonts.sf_regular[19].drawString(stack, title, headerStartX + iconWidth + headerGap - 4, headerTextY + 2, headerTextColor);

        for (StringElement stringElement : stringElements) {
            stringElement.setX(x + 1);
            stringElement.setY(y + NAME_FIELD_Y_OFFSET);
            stringElement.setWidth(NAME_FIELD_WIDTH + 25f);
            stringElement.setHeight(22 / 2f);
            stringElement.render(stack, mouseX, mouseY, alpha);
        }

        float buttonX = x + CREATE_BUTTON_X_OFFSET;
        float buttonY = y + CREATE_BUTTON_Y_OFFSET;
        float buttonWidth = 58 / 2f;
        float buttonHeight = 22 / 2f;

        float createAnim = createAnimation.getValue();
        int createBg = ColorUtil.interpolate(getColor(ThemeSettings.BUTTON), getColor(ThemeSettings.BUTTON_INACTIVE), createAnim);
        RenderUtil.drawRoundedRectangle(buttonX - 5, buttonY, buttonWidth, buttonHeight, 3, ColorUtil.getColorWithAlpha(createBg, (int) (ColorUtil.alpha(createBg) * alpha)));

        float textpedik = buttonX + (buttonWidth - Fonts.sf_regular[12].getWidth("Создать")) / 2f;
        float textIgrek = buttonY + 5.5f;
        int createTextColor = ColorUtil.interpolate(getColor(ThemeSettings.TEXT), getColor(ThemeSettings.TEXT_INACTIVE), createAnim);
        Fonts.sf_regular[12].drawString(stack, "Создать", textpedik - 5, textIgrek - 1, ColorUtil.getColorWithAlpha(createTextColor, (int) (ColorUtil.alpha(createTextColor) * alpha)));

        float pickerY = y + PICKERS_Y_OFFSET;

        float pickersScissorY = getPickersScissorTop();
        float pickersScissorHeight = Math.max(0f, getPickersScissorBottom() - pickersScissorY);
        ScissorUtil.start(getX(), pickersScissorY, getWidth() - 8, pickersScissorHeight);

        for (ColorElement colorPicker : colorPickers) {
            colorPicker.setX(x + 2.5f);
            colorPicker.setY(pickerY + animatedScroll);
            colorPicker.setWidth(getWidth() - 5);

            colorPicker.render(stack, mouseX, mouseY, alpha);

            pickerY += colorPicker.getHeight() + 1.5f;
        }

        maxHeight = pickerY - (y + 14);

        ScissorUtil.end();

        renderpreset(stack, mouseX, mouseY);

        colorPickerHandler.render(stack, (int) mouseX, (int) mouseY);

        if (contextMenu != null) {
            contextMenu.render(stack);
            if (contextMenu.isClosed()) {
                contextMenu = null;
            }
        }
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        for (StringElement stringElement : stringElements) {
            stringElement.mouseClicked(mouseX, mouseY, button);
        }

        if (button == 0 && isHoveringThemeScrollThumb(mouseX, mouseY)) {
            float contentHeight = getHeight() - 46;
            float thumbHeight = getThemeThumbHeight(contentHeight);
            float maxThumbTravel = Math.max(0f, getScrollAreaHeight() - thumbHeight);
            float thumbPosition = maxThumbTravel <= 0f ? 0f : (-animatedScroll / (maxHeight - contentHeight)) * maxThumbTravel;
            draggingScrollBar = true;
            scrollDragOffset = mouseY - (getScrollAreaY() + thumbPosition);
            return;
        }

        if (contextMenu != null) {
            if (button == 0) {
                float menuX = contextMenu.getX();
                float menuY = contextMenu.getY() + 4;
                float menuW = contextMenu.getWidth();
                float menuH = contextMenu.getHeight();

                if (MathUtil.isHovered(mouseX, mouseY, menuX, menuY, menuW, menuH)) {
                    String presetName = getPresetNameByIndex(contextMenu.getPresetIndex());
                    if (presetName != null && customPresets.containsKey(presetName)) {
                        String cleanPresetName = presetName.replace("Название: ", "");
                        themeManager.deleteTheme(cleanPresetName);
                        customPresets.remove(presetName);
                    }
                }
                contextMenu.close();
                return;
            }
        }

        if (MathUtil.isHovered(mouseX, mouseY, x + CREATE_BUTTON_X_OFFSET, y + CREATE_BUTTON_Y_OFFSET + 2f, 58 / 2f, 22 / 2f) && button == 0) {
            if (createAnimation.isAlive()) {
                return;
            }
            String themeName = stringSettings.get(0).get();
            if (themeName == null || themeName.trim().isEmpty()) {
                ChatUtil.addText("Введите название темы!");
                return;
            }
            createAnimation.setValue(0f);
            createAnimation.update(1f);
            saveCustomTheme();
            return;
        }

        int index = 0;
        List<Map.Entry<String, int[]>> allPresets = new ArrayList<>();
        allPresets.addAll(defaultPresets.entrySet());
        allPresets.addAll(customPresets.entrySet());

        for (Map.Entry<String, int[]> preset : allPresets) {
            if (index >= 22) break;

            float circleX = x + 6 + (presetCircleRadius * 2 + presetCircleSpacing) * (index % 11) + 4;
            float circleY = y + height - PRESET_CIRCLES_BOTTOM_OFFSET + ((index >= 11) ? (presetCircleRadius * 2 + presetCircleSpacing) : 0);

            if (MathUtil.isHovered(mouseX, mouseY, circleX - presetCircleRadius, circleY - presetCircleRadius, presetCircleRadius * 2, presetCircleRadius * 2)) {
                boolean isCustom = customPresets.containsKey(preset.getKey());
                if (button == 0) {
                    applyPreset(preset.getValue(), preset.getKey(), isCustom);
                    if (contextMenu != null) {
                        contextMenu.close();
                    }
                } else if (button == 1 && isCustom) {
                    contextMenu = new ThemePresetContextMenu(circleX + 2, circleY - presetCircleRadius - 1, 52, 14, index);
                }
                return;
            }

            index++;
        }
    }

    private String getPresetNameByIndex(int index) {
        List<Map.Entry<String, int[]>> allPresets = new ArrayList<>();
        allPresets.addAll(defaultPresets.entrySet());
        allPresets.addAll(customPresets.entrySet());
        if (index >= 0 && index < allPresets.size()) {
            return allPresets.get(index).getKey();
        }
        return null;
    }

    private void saveCustomTheme() {
        if (defaultPresets.size() + customPresets.size() >= 22) {
            return;
        }

        String themeName = stringSettings.get(0).get();
        if (themeName == null || themeName.trim().isEmpty()) {
            return;
        }

        String fullThemeName = "Название: " + themeName;
        if (defaultPresets.containsKey(fullThemeName) || customPresets.containsKey(fullThemeName)) {
            return;
        }

        String creator = "SkyCore";

        int[] colors = new int[ThemeSettings.values().length];
        ThemeSettings[] settings = ThemeSettings.values();
        for (int i = 0; i < settings.length; i++) {
            colors[i] = colorSettings.get(settings[i]).get();
        }

        lastPreset = currentPreset;
        currentPreset = new ThemePreset(fullThemeName, colors, creator, true);
        themeManager.saveTheme(themeName, currentPreset, creator);
        customPresets.put(fullThemeName, colors);

        ClientConfig.setCurrentActiveTheme(fullThemeName);

        ClientConfig clientConfig = SkyCore.getInstance().getClientConfig();
        if (clientConfig != null) {
            clientConfig.onThemeCreated(fullThemeName);
        }

        stringSettings.get(0).setValue("");
        stringElements.get(0).clearText();
    }

    private void applyPreset(int[] colors, String presetName, boolean isCustom) {
        lastPreset = currentPreset;
        ThemeSettings[] settings = ThemeSettings.values();
        for (int i = 0; i < Math.min(colors.length, settings.length); i++) {
            ColorSetting colorSetting = colorSettings.get(settings[i]);
            Runnable originalCallback = colorSetting.getOnChange();
            colorSetting.setOnChange(null);
            colorSetting.set(colors[i]);
            colorSetting.setOnChange(originalCallback);
        }
        currentPreset = new ThemePreset(presetName, colors, "SkyCore", isCustom);

        ClientConfig.setCurrentActiveTheme(presetName);

        colorPickerHandler.closeAllPickers();
    }

    private void loadCustomThemes() {
        themeManager.getThemeNames().forEach(name -> {
            ThemeManager.Theme theme = themeManager.loadTheme(name);
            if (theme != null && theme.getPresetName() != null && theme.getPresetColors() != null) {
                customPresets.put(theme.getPresetName(), theme.getPresetColors());
            }
        });
    }

    private void loadCurrentThemeFromConfig() {
        ClientConfig clientConfig = SkyCore.getInstance().getClientConfig();
        if (clientConfig != null) {
            String savedTheme = clientConfig.getData().getSelectedTheme();
            if (savedTheme != null && !savedTheme.trim().isEmpty()) {
                int[] colors = defaultPresets.get(savedTheme);
                if (colors != null) {
                    applyPreset(colors, savedTheme, false);
                } else {
                    colors = customPresets.get(savedTheme);
                    if (colors != null) {
                        applyPreset(colors, savedTheme, true);
                    }
                }
            }
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        if (button == 0) {
            draggingScrollBar = false;
        }
        for (ColorElement colorPicker : colorPickers) {
            if (colorPicker.getColorPicker().isColorPickMode()) {
                colorPicker.getColorPicker().mouseReleased();
            }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingScrollBar) {
            float contentHeight = getHeight() - 46;
            if (maxHeight <= contentHeight) {
                scroll = 0f;
                return true;
            }

            float thumbHeight = getThemeThumbHeight(contentHeight);
            float maxThumbTravel = Math.max(0f, getScrollAreaHeight() - thumbHeight);
            if (maxThumbTravel <= 0f) {
                scroll = 0f;
                return true;
            }

            float thumbTop = MathHelper.clamp((float) mouseY - getScrollAreaY() - scrollDragOffset, 0f, maxThumbTravel);
            float progress = thumbTop / maxThumbTravel;
            scroll = -progress * Math.max(0f, maxHeight - contentHeight);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (MathUtil.isHovered((float) mouseX, (float) mouseY, getX(), getY(), getWidth(), getHeight())) {
            float contentHeight = getHeight() - 46;
            boolean canScroll = maxHeight > contentHeight;
            float previousScroll = scroll;
            setScroll((float) (getScroll() + (delta * 10)));

            scroll = MathHelper.clamp(scroll, -maxHeight + contentHeight, 0);

            if (canScroll && scroll != previousScroll) {
                colorPickerHandler.closeAllPickers();
            }
            return scroll != previousScroll;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (StringElement stringElement : stringElements) {
            stringElement.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for (StringElement stringElement : stringElements) {
            stringElement.charTyped(codePoint, modifiers);
        }
    }

    @Getter
    private static class ThemePresetContextMenu {
        private final float x, y, width, height;
        private final int presetIndex;
        private final AnimationUtil scaleAnimation;
        private boolean isClosing;

        public ThemePresetContextMenu(float x, float y, float width, float height, int presetIndex) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.presetIndex = presetIndex;
            this.scaleAnimation = new AnimationUtil(0, 1.1f, Easings.LINEAR);
            this.isClosing = false;
        }

        public void render(MatrixStack stack) {
            scaleAnimation.setSpeed(10);
            scaleAnimation.update(isClosing ? 0.0f : 1.0f);

            int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
            int bgColor = ColorUtil.darken(logoColor, 0.1f);

            RenderUtil.scaleStart(x + width / 2.0f, y + 4 + height / 2.0f, scaleAnimation.getValue());
            RenderUtil.drawRoundedRectangle(x, y + 4, width, height, 5, bgColor);

            float textX = x + 13;

            RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/gui/bin.png"), x + 3f, y + 7.5f, 8, 8, getColor(ThemeSettings.TEXT));
            Fonts.sf_regular[12].drawString(stack, "Удалить", textX - 0.5f, y + 10.5f, getColor(ThemeSettings.TEXT));

            RenderUtil.scaleEnd();
        }

        public void close() {
            if (!isClosing) {
                isClosing = true;
                scaleAnimation.update(0.0f);
            }
        }

        public boolean isClosed() {
            return isClosing && scaleAnimation.isDone();
        }
    }

    @Getter
    public static class ThemePreset {
        private final String name;
        private final int[] colors;
        private final String creator;
        private final boolean isCustom;

        public ThemePreset(String name, int[] colors, String creator, boolean isCustom) {
            this.name = name;
            this.colors = colors;
            this.creator = creator != null ? creator : "Unknown";
            this.isCustom = isCustom;
        }
    }

    public void clearContextMenu() {
        this.contextMenu = null;
    }
}