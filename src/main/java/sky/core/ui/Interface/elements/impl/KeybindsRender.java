package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.util.InputMappings;
import org.lwjgl.glfw.GLFW;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.Setting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.misc.KeyMapper;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class KeybindsRender implements ElementRender {

    private enum ModuleCategory {
        Combat("l"),
        Movement("m"),
        Render("b"),
        Player("i"),
        Util("j");

        private final String icon;

        ModuleCategory(String icon) {
            this.icon = icon;
        }

        public String getIcon() {
            return icon;
        }
    }

    private final Dragging dragging;
    private final AnimationUtil alphaAnimation = new AnimationUtil(0.0f, 10);
    private final AnimationUtil ordinaryAlphaAnimation = new AnimationUtil(0.0f, 10);
    private final AnimationUtil slideAnimation = new AnimationUtil(-20.0f, 10);
    private final AnimationUtil widthAnimation = new AnimationUtil(90.0f, 10);
    private final AnimationUtil heightAnimation = new AnimationUtil(62.5f, 10);
    private final Map<String, AnimationUtil> entryAnimations = new HashMap<>();
    private final Map<KeybindItem, AnimationUtil[]> itemAnimations = new HashMap<>();
    private final Map<String, AnimationUtil> ordinaryEntryAnimations = new HashMap<>();
    public static BooleanSetting alphabg = new BooleanSetting("Прозрачный фон", false);

    @Override
    public void render(EventRender2D.Post event) {
        if (Interface.isOldHud()) {
            renderDefault(event);
        } else {
            renderNursultan(event);
        }
    }

    private void renderDefault(EventRender2D.Post event) {
        MatrixStack ms = event.getStack();
        float posX = dragging.getX(), posY = dragging.getY();
        List<OrdinaryEntry> entries = collectOrdinaryEntries();
        boolean hasActive = entries.stream().anyMatch(OrdinaryEntry::isActive);
        boolean show = mc.currentScreen instanceof ChatScreen || hasActive;

        ordinaryAlphaAnimation.update(show ? 1.0f : 0.0f);
        float globalAlpha = ordinaryAlphaAnimation.getValue();
        if (globalAlpha <= 0.01f) {
            dragging.setWidth(65f);
            dragging.setHeight(18.5f);
            return;
        }

        float headerHeight = 12.3f;
        float itemHeight = 9.6f;
        float maxCombinedWidth = 0.0f;
        List<String> currentIds = new ArrayList<>();
        for (OrdinaryEntry entry : entries) {
            currentIds.add(entry.id);
            AnimationUtil anim = ordinaryEntryAnimations.computeIfAbsent(entry.id, key -> new AnimationUtil(0.0f, 10));
            anim.update(entry.active ? 1.0f : 0.0f);
            if (anim.getValue() > 0.01f || entry.active) {
                String shortBind = "[" + toShortBind(entry.bind) + "]";
                float combined = Fonts.sf_medium[14].getWidth(entry.name) + Fonts.sf_medium[14].getWidth(shortBind) + 22f;
                maxCombinedWidth = Math.max(maxCombinedWidth, combined);
            }
        }
        ordinaryEntryAnimations.entrySet().removeIf(e -> !currentIds.contains(e.getKey()) && e.getValue().isDone() && e.getValue().getValue() <= 0.01f);

        float width = Math.max(65f, maxCombinedWidth);
        float contentHeight = 0.0f;
        for (OrdinaryEntry entry : entries) {
            AnimationUtil anim = ordinaryEntryAnimations.get(entry.id);
            if (anim != null) {
                contentHeight += itemHeight * anim.getValue();
            }
        }
        float windowHeight = headerHeight + (contentHeight <= 0.01f ? 6.0f : contentHeight + 6.7f);

        int textColor = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.WINDOW_TEXT), ThemeEditor.getAlpha(ThemeSettings.WINDOW_TEXT) / 255f * globalAlpha);
        int iconColor = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.LOGO), ThemeEditor.getAlpha(ThemeSettings.LOGO) / 255f * globalAlpha);

        float topOffset = 3.8f;
        RenderUtil.drawBlurredRoundedRectangle(posX, posY + topOffset, width, windowHeight - topOffset, 4.9f,
                alphabg.get() ? ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.WINDOW_BG), 0) : ThemeEditor.getColor(ThemeSettings.WINDOW_BG), globalAlpha);

        if (width > 35f) {
            float iconX = posX + 54f + (width - 65f);
            Fonts.lox[13].drawString(ms, "d", iconX + 0.5f, posY + 10.0f, iconColor);
            Fonts.sf_medium[15].drawString(ms, "Keybinds", posX + 4.0f, posY + 9.0f, textColor);
        }

        float renderY = posY + headerHeight + 3;
        for (OrdinaryEntry entry : entries) {
            AnimationUtil itemAnim = ordinaryEntryAnimations.get(entry.id);
            if (itemAnim == null) continue;
            float animVal = itemAnim.getValue();
            if (animVal <= 0.01f) continue;

            float slideOffset = (1.0f - animVal) * 10.0f;
            int itemTextColor = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.WINDOW_TEXT), ThemeEditor.getAlpha(ThemeSettings.WINDOW_TEXT) / 255f * animVal * globalAlpha);
            int itemBindColor = ColorUtil.rgba(180, 180, 180, 255);

            Fonts.sf_medium[14].drawString(ms, entry.name, posX + 4.0f - slideOffset, renderY + 4.05f, itemTextColor);

            String shortBind = "[" + toShortBind(entry.bind) + "]";
            float bindWidth = Fonts.sf_medium[14].getWidth(shortBind);
            float bindX = posX + width - bindWidth - 3.0f + slideOffset;
            if (width > bindWidth + 12f) {
                Fonts.sf_medium[14].drawString(ms, shortBind, bindX - 0.5f, renderY + 4.05f, itemBindColor);
            }

            renderY += itemHeight  * animVal;
        }

        dragging.setHeight(windowHeight);
        dragging.setWidth(width);
    }

    private void renderNursultan(EventRender2D.Post event) {
        MatrixStack ms = event.getStack();
        float posX = dragging.getX(), posY = dragging.getY();
        List<ModuleKeybindEntry> entries = collectModuleKeybinds();
        boolean hasActive = entries.stream().anyMatch(ModuleKeybindEntry::isActive);
        boolean show = mc.currentScreen instanceof ChatScreen || hasActive;

        alphaAnimation.update(show ? 1.0f : 0.0f);
        float globalAlpha = alphaAnimation.getValue();
        if (globalAlpha <= 0.01f) {
            dragging.setWidth(90f);
            dragging.setHeight(25f);
            return;
        }

        float totalWidth = 180.0f / 2f;
        float paddingX = 12.0f / 2f;
        float paddingY = 15.0f / 2f;
        float gap = 15.0f / 2f;
        float itemGap = 12.0f / 2f;
        float borderRadius = 5.5f;
        float iconSize = 12.0f / 2f;
        float headerGap = 6.0f / 2f;
        float itemHeight = 14.0f / 2f;

        float maxItemWidth = 0;
        List<String> currentIds = new ArrayList<>();
        float headerHeight = 12 + gap;
        float contentHeight = 0;

        for (ModuleKeybindEntry entry : entries) {
            currentIds.add(entry.name + entry.bind);
            AnimationUtil anim = entryAnimations.computeIfAbsent(
                    entry.name + entry.bind,
                    key -> new AnimationUtil(0.0f, 10)
            );
            anim.update(entry.isActive() ? 1.0f : 0.0f);

            float animVal = anim.getValue();
            if (animVal > 0.01f || entry.isActive()) {
                float nameWidth = Fonts.sf_medium[12].getWidth(entry.name);
                float bindWidth = Fonts.sf_medium[12].getWidth(toShortBind(entry.bind));
                float itemWidth = iconSize + headerGap + nameWidth + itemGap + bindWidth;
                maxItemWidth = Math.max(maxItemWidth, itemWidth);
                contentHeight += (itemHeight + itemGap) * animVal;
            }
        }
        entryAnimations.entrySet().removeIf(e ->
                !currentIds.contains(e.getKey()) &&
                        e.getValue().isDone() &&
                        e.getValue().getValue() <= 0.01f
        );

        float targetWidth = Math.max(totalWidth, maxItemWidth + paddingX * 2);
        float animatedHeight = paddingY + headerHeight + contentHeight + paddingY - 10;

        widthAnimation.update(targetWidth);
        float animatedWidth = widthAnimation.getValue();

        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(logoColor, 0.1f);
        int iconColor = logoColor;
        int textColor = 0xFFFFFFFF;
        int bindColor = logoColor;

        RenderUtil.drawRoundedRectangle(posX, posY, animatedWidth, animatedHeight, borderRadius, ColorUtil.applyOpacity(bgColor, (int)(255 * globalAlpha)));

        float currentY = posY + paddingY;
        float currentX = posX + paddingX;

        Fonts.skycore[14].drawString(ms, "c", currentX, currentY + 2.9f, ColorUtil.applyOpacity(iconColor, (int)(255 * globalAlpha)));
        currentX += iconSize + headerGap;
        Fonts.sfregular[13].drawString(ms, "Keybinds", currentX + 1, currentY + 2.5f, ColorUtil.applyOpacity(textColor, (int)(255 * globalAlpha)));

        currentY += headerHeight;

        for (ModuleKeybindEntry entry : entries) {
            AnimationUtil itemAnim = entryAnimations.get(entry.name + entry.bind);
            if (itemAnim == null) continue;
            float animVal = itemAnim.getValue();
            if (animVal <= 0.01f) continue;

            currentX = posX + paddingX;
            float slideOffset = (1.0f - animVal) * 10.0f;

            Fonts.skycore[13].drawString(ms, entry.categoryIcon, currentX, currentY - 1 - slideOffset, ColorUtil.applyOpacity(iconColor, (int)(255 * animVal * globalAlpha)));
            currentX += iconSize + itemGap;

            Fonts.sfregular[13].drawString(ms, entry.name, currentX, currentY - 2 - slideOffset, ColorUtil.applyOpacity(textColor, (int)(255 * animVal * globalAlpha)));
            float nameWidth = Fonts.sfregular[13].getWidth(entry.name);
            currentX += nameWidth;

            String shortBind = toShortBind(entry.bind);
            float bindWidth = Fonts.sfregular[13].getWidth(shortBind);
            float bindX = posX + animatedWidth - paddingX - bindWidth;
            Fonts.sfregular[13].drawString(ms, shortBind, bindX + slideOffset, currentY - 2 - slideOffset, ColorUtil.applyOpacity(bindColor, (int)(255 * animVal * globalAlpha)));

            currentY += (itemHeight + itemGap) * animVal;
        }

        dragging.setWidth(animatedWidth);
        dragging.setHeight(animatedHeight);
    }

    private String toShortBind(int bind) {
        String bindStr = InputMappings.getInputByCode(bind, GLFW.GLFW_KEY_UNKNOWN)
                .getTranslationKey()
                .replace("key.keyboard.", "")
                .replace("key.mouse.", "M")
                .replace("MOUSE", "M")
                .replace("CONTROL", "C")
                .replace("LEFT", "L")
                .replace("RIGHT", "R")
                .replace("_", "");
        if (bindStr.equals("semicolon")) bindStr = ";";
        if (bindStr.equals("apostrophe")) bindStr = "'";
        if (bindStr.equals("3")) bindStr = "M3";
        if (bindStr.equals("4")) bindStr = "M4";
        if (bindStr.length() > 4) bindStr = bindStr.substring(0, 4);
        return bindStr.toUpperCase();
    }

    private List<OrdinaryEntry> collectOrdinaryEntries() {
        List<OrdinaryEntry> entries = new ArrayList<>();
        for (Module module : SkyCore.getInstance().getModuleManager().getModules()) {
            if (module.getBind() != -100) {
                entries.add(new OrdinaryEntry("module::" + module.getName(), module.getName(), module.getBind(), module.isEnabled()));
            }
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof BooleanSetting booleanSetting && booleanSetting.getBind() != -100) {
                    entries.add(new OrdinaryEntry("setting::" + module.getName() + "::" + booleanSetting.getName(), booleanSetting.getName(), booleanSetting.getBind(), module.isEnabled() && booleanSetting.get()));
                }
            }
        }
        return entries;
    }

    private List<KeybindItem> getActiveItems() {
        List<KeybindItem> activeItems = new ArrayList<>();
        for (Module module : SkyCore.getInstance().getModuleManager().getModules()) {
            if (module.getBind() != -100 && module.isEnabled() && module.isKeybindvisible()) {
                activeItems.add(new KeybindItem(module.getName(), module.getBind()));
            }
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof BooleanSetting booleanSetting && booleanSetting.getBind() != -100 && booleanSetting.get() && booleanSetting.isKeybindvisible()) {
                    activeItems.add(new KeybindItem(booleanSetting.getName(), booleanSetting.getBind()));
                }
            }
        }
        return activeItems;
    }

    private float[] calculateDimensions(List<KeybindItem> activeItems) {
        float maxNameWidth = 0, maxBindWidth = 0;
        for (KeybindItem item : activeItems) {
            maxNameWidth = Math.max(maxNameWidth, Fonts.sf_medium[12].getWidth(item.getName()));
            maxBindWidth = Math.max(maxBindWidth, Fonts.sf_medium[12].getWidth(KeyMapper.getKey(item.getBind())));
        }
        float width = Math.max(60, maxNameWidth + maxBindWidth + 18.5f);
        return new float[]{width, maxBindWidth};
    }

    @Getter
    private static class KeybindItem {
        private final String name;
        private final int bind;

        public KeybindItem(String name, int bind) {
            this.name = name;
            this.bind = bind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeybindItem that = (KeybindItem) o;
            return bind == that.bind && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + bind;
        }
    }

    private static class ModuleKeybindEntry {
        private final String name;
        private final int bind;
        private final String categoryIcon;
        private final boolean active;

        private ModuleKeybindEntry(String name, int bind, String categoryIcon, boolean active) {
            this.name = name;
            this.bind = bind;
            this.categoryIcon = categoryIcon;
            this.active = active;
        }

        private boolean isActive() {
            return active;
        }
    }

    private List<ModuleKeybindEntry> collectModuleKeybinds() {
        List<ModuleKeybindEntry> entries = new ArrayList<>();
        for (Module module : SkyCore.getInstance().getModuleManager().getModules()) {
            if (module.getBind() != -100) {
                String categoryIcon = getCategoryIcon(module);
                entries.add(new ModuleKeybindEntry(module.getName(), module.getBind(), categoryIcon, module.isEnabled()));
            }
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof BooleanSetting booleanSetting && booleanSetting.getBind() != -100) {
                    String categoryIcon = getCategoryIcon(module);
                    entries.add(new ModuleKeybindEntry(booleanSetting.getName(), booleanSetting.getBind(), categoryIcon, module.isEnabled() && booleanSetting.get()));
                }
            }
        }
        return entries;
    }

    private String getCategoryIcon(Module module) {
        String categoryName = String.valueOf(module.getCategory());
        if (categoryName != null) {
            switch (categoryName.toLowerCase()) {
                case "combat": return ModuleCategory.Combat.getIcon();
                case "movement": return ModuleCategory.Movement.getIcon();
                case "render": return ModuleCategory.Render.getIcon();
                case "player": return ModuleCategory.Player.getIcon();
                case "util": return ModuleCategory.Util.getIcon();
                default: return "d";
            }
        }
        return "d";
    }

    private static class OrdinaryEntry {
        private final String id;
        private final String name;
        private final int bind;
        private final boolean active;

        private OrdinaryEntry(String id, String name, int bind, boolean active) {
            this.id = id;
            this.name = name;
            this.bind = bind;
            this.active = active;
        }

        private boolean isActive() {
            return active;
        }
    }
}