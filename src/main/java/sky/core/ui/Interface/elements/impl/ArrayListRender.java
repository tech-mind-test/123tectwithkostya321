package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.screen.ChatScreen;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.Setting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class ArrayListRender implements ElementRender {

    public static final BooleanSetting showCombat = new BooleanSetting("Combat", true);
    public static final BooleanSetting showMovement = new BooleanSetting("Movement", true);
    public static final BooleanSetting showVisuals = new BooleanSetting("Visuals", true);
    public static final BooleanSetting showPlayer = new BooleanSetting("Player", true);
    public static final BooleanSetting showMisc = new BooleanSetting("Misc", true);

    private static final float ROW_GAP = -1.4f;
    private static final int ROW_PADDING = 0;

    private static final int FONT_SIZE = 13;
    /** Длительность полного цикла перелива снизу вверх (мс) */
    private static final long GRADIENT_CYCLE_MS = 4500L;
    private static final float ROW_HEIGHT = 13f;
    private static final float PADDING_X = 5f;
    private static final float LINE_WIDTH = 0.5f;
    private static final float NAME_SUFFIX_GAP = 3f;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final float SLIDE_OFFSET = 10f;
    /** Яркость тёмного конца градиента (больше = светлее) */
    private static final float GRADIENT_DARK_BRIGHTNESS = 0.62f;

    private static final List<String> PREVIEW_NAMES = Arrays.asList(
            "Auto Swap", "No Web", "Elytra Target", "Sprint"
    );
    private static final List<String> PREVIEW_SUFFIXES = Arrays.asList(
            "Предметы", "Custom", "Reallyworld", ""
    );

    private final Dragging dragging;
    private final AnimationUtil listAlphaAnimation = new AnimationUtil(0f, 10);
    private final Map<String, AnimationUtil> entryAnimations = new HashMap<>();
    private final Map<String, ArrayListEntry> entryCache = new HashMap<>();

    @Override
    public void render(EventRender2D.Post event) {
        MatrixStack ms = event.getStack();
        boolean preview = mc.currentScreen instanceof ChatScreen;

        List<ArrayListEntry> activeEntries = collectEntries();
        boolean hasActive = !activeEntries.isEmpty();
        boolean showList = hasActive || preview;

        listAlphaAnimation.update(showList ? 1.0f : 0.0f);
        float listAlpha = listAlphaAnimation.getValue();
        if (listAlpha <= 0.01f && listAlphaAnimation.isDone()) {
            dragging.setWidth(0);
            dragging.setHeight(0);
            return;
        }

        if (activeEntries.isEmpty() && preview) {
            activeEntries = buildPreviewEntries();
        }

        updateEntryAnimations(activeEntries, preview);

        List<RenderedRow> rows = buildRenderRows(activeEntries);
        if (rows.isEmpty()) {
            dragging.setWidth(0);
            dragging.setHeight(0);
            return;
        }

        rows.sort(Comparator.comparingDouble((RenderedRow r) -> -r.entry.sortWidth));

        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int darkColor = ColorUtil.darken(logoColor, GRADIENT_DARK_BRIGHTNESS);
        int bgColor = ColorUtil.rgba(0, 0, 0, 150);
        int suffixColor = TEXT_WHITE;

        float maxRowWidth = 0f;
        float contentHeight = 0f;
        for (RenderedRow row : rows) {
            float anim = row.anim * listAlpha;
            if (anim <= 0.01f) continue;
            maxRowWidth = Math.max(maxRowWidth, row.entry.rowWidth);
            contentHeight += (ROW_HEIGHT + ROW_PADDING * 2f + ROW_GAP) * anim;
        }

        if (contentHeight <= 0.01f) {
            dragging.setWidth(0);
            dragging.setHeight(0);
            return;
        }

        float rowBlurHeight = ROW_HEIGHT + ROW_PADDING * 2f;
        float listWidth = maxRowWidth + LINE_WIDTH;
        int screenWidth = mc.getMainWindow().getScaledWidth();

        float posX = dragging.getX();
        float posY = dragging.getY();

        boolean mirror = posX + listWidth * 0.5f >= screenWidth * 0.5f;

        if (mirror) {
            if (posX < 0) {
                posX = 4f;
                dragging.setX(posX);
            }
            if (posX + listWidth > screenWidth + 2f) {
                posX = Math.max(4f, screenWidth - listWidth - 4f);
                dragging.setX(posX);
            }
        } else {
            if (posX < 0 || posX + listWidth > screenWidth + 2f) {
                posX = Math.max(4f, screenWidth - listWidth - 4f);
                dragging.setX(posX);
            }
        }
        if (posY < 0) {
            posY = 8f;
            dragging.setY(posY);
        }

        float lineX = mirror ? posX + listWidth - LINE_WIDTH : posX;
        float rowYi = posY;
        int lineColor = ColorUtil.applyOpacity(logoColor, listAlpha);
        int totalHi = Math.round(contentHeight) - 1;
        int visibleRowCount = rows.size();

        RenderUtil.drawMinecraftRectangle(ms, Math.round(lineX), Math.round(rowYi) + 1f, Math.round(LINE_WIDTH), totalHi, lineColor);

        float renderY = rowYi;
        int rowIndex = 0;
        for (RenderedRow row : rows) {
            float anim = row.anim * listAlpha;
            if (anim <= 0.01f) continue;

            ArrayListEntry entry = row.entry;
            float slideOffset = (1.0f - anim) * SLIDE_OFFSET;
            float rowY = renderY;
            float drawWidth = entry.rowWidth - 2;

            float bgX = mirror
                    ? lineX - drawWidth + slideOffset
                    : lineX + LINE_WIDTH - slideOffset;
            RenderUtil.drawBlurredRoundedRectangle(bgX, rowY, drawWidth, rowBlurHeight, 0f, bgColor, anim);

            float textY = rowY + (rowBlurHeight - Fonts.sf_medium[FONT_SIZE].getHeight()) / 2f + 1f;
            float suffixWidth = entry.suffix.isEmpty()
                    ? 0f
                    : NAME_SUFFIX_GAP + Fonts.sf_medium[FONT_SIZE].getWidth(entry.suffix);
            float totalTextWidth = entry.nameWidth + suffixWidth;
            float textX = mirror
                    ? bgX + drawWidth - PADDING_X - totalTextWidth
                    : bgX + PADDING_X;

            int nameColor = getVerticalFlowColor(rowIndex, visibleRowCount, logoColor, darkColor);
            rowIndex++;
            nameColor = ColorUtil.applyOpacity(nameColor, anim);

            Fonts.sf_medium[FONT_SIZE].drawString(ms, entry.name, textX - 0.5f, textY, nameColor);

            if (!entry.suffix.isEmpty()) {
                float suffixX = textX + entry.nameWidth + NAME_SUFFIX_GAP - 0.5f;
                int suffixWithAlpha = ColorUtil.applyOpacity(suffixColor, anim);
                Fonts.sf_medium[FONT_SIZE].drawString(ms, entry.suffix, suffixX, textY, suffixWithAlpha);
            }

            renderY += (rowBlurHeight + ROW_GAP) * anim;
        }

        dragging.setWidth(listWidth);
        dragging.setHeight(contentHeight);
    }

    private void updateEntryAnimations(List<ArrayListEntry> activeEntries, boolean preview) {
        Set<String> activeIds = new HashSet<>();
        for (ArrayListEntry entry : activeEntries) {
            activeIds.add(entry.id);
            entryCache.put(entry.id, entry);
            AnimationUtil anim = entryAnimations.computeIfAbsent(entry.id, key -> new AnimationUtil(0.0f, 12));
            anim.update(1.0f);
        }

        for (String id : new ArrayList<>(entryAnimations.keySet())) {
            if (!activeIds.contains(id)) {
                entryAnimations.get(id).update(0.0f);
            }
        }

        entryAnimations.entrySet().removeIf(e ->
                !activeIds.contains(e.getKey())
                        && e.getValue().isDone()
                        && e.getValue().getValue() <= 0.01f
        );

        if (!preview) {
            entryCache.keySet().removeIf(id -> !entryAnimations.containsKey(id));
        }
    }

    private List<RenderedRow> buildRenderRows(List<ArrayListEntry> activeEntries) {
        List<RenderedRow> rows = new ArrayList<>();
        Set<String> added = new HashSet<>();

        for (ArrayListEntry entry : activeEntries) {
            AnimationUtil anim = entryAnimations.get(entry.id);
            if (anim == null) continue;
            float animVal = anim.getValue();
            if (animVal <= 0.01f && anim.isDone()) continue;
            rows.add(new RenderedRow(entry, animVal));
            added.add(entry.id);
        }

        for (Map.Entry<String, AnimationUtil> e : entryAnimations.entrySet()) {
            if (added.contains(e.getKey())) continue;
            float animVal = e.getValue().getValue();
            if (animVal <= 0.01f) continue;
            ArrayListEntry cached = entryCache.get(e.getKey());
            if (cached != null) {
                rows.add(new RenderedRow(cached, animVal));
            }
        }

        return rows;
    }

    private List<ArrayListEntry> collectEntries() {
        List<ArrayListEntry> entries = new ArrayList<>();

        for (Module module : SkyCore.getInstance().getModuleManager().getModules()) {
            if (!module.isEnabled()) continue;
            if (module instanceof Interface) continue;
            if (!isCategoryVisible(module.getCategory())) continue;

            String name = module.getName();
            String suffix = getModuleSuffix(module);
            entries.add(createEntry(name, suffix));
        }

        entries.sort(Comparator.comparingDouble((ArrayListEntry e) -> e.sortWidth).reversed());
        return entries;
    }

    private List<ArrayListEntry> buildPreviewEntries() {
        List<ArrayListEntry> entries = new ArrayList<>();
        for (int i = 0; i < PREVIEW_NAMES.size(); i++) {
            entries.add(createEntry(PREVIEW_NAMES.get(i), PREVIEW_SUFFIXES.get(i)));
        }
        entries.sort(Comparator.comparingDouble((ArrayListEntry e) -> e.sortWidth).reversed());
        return entries;
    }

    private static ArrayListEntry createEntry(String name, String suffix) {
        float nameWidth = Fonts.sf_medium[FONT_SIZE].getWidth(name);
        float suffixWidth = suffix.isEmpty() ? 0f : NAME_SUFFIX_GAP + Fonts.sf_medium[FONT_SIZE].getWidth(suffix);
        float textWidth = nameWidth + suffixWidth;
        float rowWidth = PADDING_X * 2f + textWidth;
        return new ArrayListEntry(name, name, suffix, nameWidth, rowWidth, textWidth);
    }

    private static boolean isCategoryVisible(Category category) {
        return switch (category) {
            case Combat -> showCombat.get();
            case Movement -> showMovement.get();
            case Visuals -> showVisuals.get();
            case Player -> showPlayer.get();
            case Miscellaneous -> showMisc.get();
        };
    }

    private static int getVerticalFlowColor(int rowIndex, int rowCount, int logoColor, int darkColor) {
        if (rowCount <= 1) {
            return logoColor;
        }
        float rowT = rowIndex / (float) (rowCount - 1);
        float flow = (System.currentTimeMillis() % GRADIENT_CYCLE_MS) / (float) GRADIENT_CYCLE_MS;
        float phase = (1f - rowT + flow) * (float) (Math.PI * 2);
        float blend = (float) ((Math.sin(phase) + 1.0) * 0.5);
        blend = smoothstep(blend);
        return ColorUtil.interpolate(darkColor, logoColor, blend);
    }

    private static float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private static String getModuleSuffix(Module module) {
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof ModeSetting mode && setting.isVisible()) {
                return mode.get();
            }
        }
        return "";
    }

    private static final class RenderedRow {
        private final ArrayListEntry entry;
        private final float anim;

        private RenderedRow(ArrayListEntry entry, float anim) {
            this.entry = entry;
            this.anim = anim;
        }
    }

    @Getter
    private static final class ArrayListEntry {
        private final String id;
        private final String name;
        private final String suffix;
        private final float nameWidth;
        private final float rowWidth;
        private final float sortWidth;

        private ArrayListEntry(String id, String name, String suffix, float nameWidth, float rowWidth, float sortWidth) {
            this.id = id;
            this.name = name;
            this.suffix = suffix;
            this.nameWidth = nameWidth;
            this.rowWidth = rowWidth;
            this.sortWidth = sortWidth;
        }
    }
}
