package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.utils.managers.impl.notificationmanager.NotificationManager;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class NotificationRender implements ElementRender {
    public static BooleanSetting shield = new BooleanSetting("Ломание щита", false);
    public static BooleanSetting spec = new BooleanSetting("Просьба о наблюдении", false);
    public static BooleanSetting warps = new BooleanSetting("Пиар варпов", false);
    public static BooleanSetting module = new BooleanSetting("Состояние модулей/настроек", true);
    public static BooleanSetting lowstrength = new BooleanSetting("Низкая прочность предметов", true);
    public static BooleanSetting effects = new BooleanSetting("Закончились важные зелья", true);
    public static BooleanSetting alphabg = new BooleanSetting("Прозрачный фон", false);

    private final Dragging dragging;
    private final AnimationUtil alphaAnimation = new AnimationUtil(0.0f, 10);
    private final TimeUtil previewTimer = new TimeUtil();
    private int previewIndex = 0;
    private boolean wasChatOpen = false;

    private static final Map<Category, String> CATEGORY_ICONS = new HashMap<>();
    static {
        CATEGORY_ICONS.put(Category.Combat, "l");
        CATEGORY_ICONS.put(Category.Movement, "m");
        CATEGORY_ICONS.put(Category.Visuals, "b");
        CATEGORY_ICONS.put(Category.Player, "i");
        CATEGORY_ICONS.put(Category.Miscellaneous, "d");
    }

    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final List<String> PREVIEW_ICONS = new ArrayList<>();
    static {
        PREVIEW_ICONS.add("l"); // Combat
        PREVIEW_ICONS.add("m"); // Movement
        PREVIEW_ICONS.add("b"); // Visuals
        PREVIEW_ICONS.add("i"); // Player
        PREVIEW_ICONS.add("d"); // Miscellaneous
    }

    private static final String PREVIEW_TEXT = "Это пример уведомления";

    @Override
    public void render(EventRender2D.Post event) {
        MatrixStack ms = event.getStack();
        float posX = dragging.getX(), posY = dragging.getY();

        boolean isChatOpen = mc.currentScreen instanceof ChatScreen;
        boolean hasNotifications = NotificationManager.hasActiveNotifications();

        if (isChatOpen && !wasChatOpen) {
            previewIndex = 0;
            previewTimer.reset();
        }
        wasChatOpen = isChatOpen;

        boolean shouldShowPreview = isChatOpen && !hasNotifications;
        alphaAnimation.update(shouldShowPreview ? 1.0f : 0.0f);
        float globalAlpha = alphaAnimation.getValue();

        if (globalAlpha <= 0.01f) {
            dragging.setWidth(0);
            dragging.setHeight(0);
            return;
        }

        if (isChatOpen && previewTimer.hasTimeElapsed(1000, true)) {
            previewIndex = (previewIndex + 1) % PREVIEW_ICONS.size();
        }

        String currentIcon = PREVIEW_ICONS.get(previewIndex);
        renderPreviewCard(ms, posY, currentIcon, globalAlpha);
    }

    private void renderPreviewCard(MatrixStack ms, float y, String icon, float alpha) {
        float cardHeight = 19f;
        float cardRadius = 5f;
        float paddingX = 8f;
        float paddingY = 6f;
        float gap = 4f;
        float iconSize = 7f;

        String text = PREVIEW_TEXT;
        float textWidth = Fonts.sfregular[12].getWidth(text);
        float cardWidth = paddingX + iconSize + gap + textWidth + paddingX;

        float x = (mc.getMainWindow().getScaledWidth() - cardWidth) / 2f;

        int cardBgColor = ThemeEditor.getColor(ThemeSettings.WINDOW_BG);
        int iconColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(iconColor, 0.1f);
        int bgWithAlpha = ColorUtil.applyOpacity(bgColor, alpha);
        int textWithAlpha = ColorUtil.applyOpacity(TEXT_COLOR, alpha);
        int iconWithAlpha = ColorUtil.applyOpacity(iconColor, alpha);

        RenderUtil.drawRoundedRectangle(x, y, cardWidth, cardHeight, cardRadius, bgWithAlpha);

        float iconHeight = Fonts.skycore[13].getHeight();
        float textHeight = Fonts.sfregular[12].getHeight();
        float contentHeight = Math.max(iconHeight, textHeight);
        float contentY = y + (cardHeight - contentHeight) / 2f;

        float iconX = x + paddingX;
        float iconY = contentY + (contentHeight - iconHeight) / 2f + 1f;
        if (icon != null && !icon.isEmpty()) {
            Fonts.skycore[13].drawString(ms, icon, iconX, iconY, iconWithAlpha);
        }

        float textX = iconX + iconSize + gap;
        float textY = contentY + (contentHeight - textHeight) / 2f + 1f;
        Fonts.sfregular[12].drawString(ms, text, textX, textY, textWithAlpha);

        dragging.setWidth(cardWidth);
        dragging.setHeight(cardHeight);
    }

    public static String getCategoryIcon(Module module) {
        Category category = module.getCategory();
        return CATEGORY_ICONS.getOrDefault(category, "d");
    }

}
