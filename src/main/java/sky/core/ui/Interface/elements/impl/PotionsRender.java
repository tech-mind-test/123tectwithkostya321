package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.texture.PotionSpriteUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.EffectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class PotionsRender implements ElementRender {
    private final Dragging dragging;
    private final AnimationUtil alphaAnimation = new AnimationUtil(0.0f, 10);
    private final AnimationUtil ordinaryAlphaAnimation = new AnimationUtil(0.0f, 10);
    private float ordinaryAnimatedWidth = 40f;
    private final Map<Effect, AnimationUtil> ordinaryEffectAnimations = new HashMap<>();
    private final Map<PotionItem, AnimationUtil[]> itemAnimations = new HashMap<>();
    private final Map<PotionItem, String> lastDurationTexts = new HashMap<>();
    private final Map<PotionItem, Integer> lastDurationTicks = new HashMap<>();
    private static final int DURATION_HIDE_TICKS = 60 * 60 * 20;
    public static BooleanSetting alphabg = new BooleanSetting("Прозрачный фон", false);
    public static BooleanSetting badeffects = new BooleanSetting("Отображать плохие эффекты", true);

    private int currentEffectIndex = 0;
    private long lastEffectSwitchTime = 0;
    private static final long EFFECT_SWITCH_INTERVAL = 1000;

    @Override
    public void render(EventRender2D.Post event) {
        if (Interface.isNewHud()) {
            renderOrdinary(event);
        } else {
            renderNursultan(event);
        }
    }

    private static final Map<String, String> EFFECT_TRANSLATIONS = new HashMap<>();
    static {
        EFFECT_TRANSLATIONS.put("effect.minecraft.speed", "Скорость");
        EFFECT_TRANSLATIONS.put("effect.minecraft.slowness", "Медлительность");
        EFFECT_TRANSLATIONS.put("effect.minecraft.haste", "Спешка");
        EFFECT_TRANSLATIONS.put("effect.minecraft.mining_fatigue", "Усталость");
        EFFECT_TRANSLATIONS.put("effect.minecraft.strength", "Сила");
        EFFECT_TRANSLATIONS.put("effect.minecraft.instant_health", "Моментальное лечение");
        EFFECT_TRANSLATIONS.put("effect.minecraft.instant_damage", "Моментальный урон");
        EFFECT_TRANSLATIONS.put("effect.minecraft.jump_boost", "Прыгучесть");
        EFFECT_TRANSLATIONS.put("effect.minecraft.nausea", "Тошнота");
        EFFECT_TRANSLATIONS.put("effect.minecraft.regeneration", "Регенерация");
        EFFECT_TRANSLATIONS.put("effect.minecraft.resistance", "Сопротивление");
        EFFECT_TRANSLATIONS.put("effect.minecraft.fire_resistance", "Огнестойкость");
        EFFECT_TRANSLATIONS.put("effect.minecraft.water_breathing", "Подводное дыхание");
        EFFECT_TRANSLATIONS.put("effect.minecraft.invisibility", "Невидимость");
        EFFECT_TRANSLATIONS.put("effect.minecraft.blindness", "Слепота");
        EFFECT_TRANSLATIONS.put("effect.minecraft.night_vision", "Ночное зрение");
        EFFECT_TRANSLATIONS.put("effect.minecraft.hunger", "Голод");
        EFFECT_TRANSLATIONS.put("effect.minecraft.weakness", "Слабость");
        EFFECT_TRANSLATIONS.put("effect.minecraft.poison", "Отравление");
        EFFECT_TRANSLATIONS.put("effect.minecraft.wither", "Иссушение");
        EFFECT_TRANSLATIONS.put("effect.minecraft.health_boost", "Прилив здоровья");
        EFFECT_TRANSLATIONS.put("effect.minecraft.absorption", "Поглощение");
        EFFECT_TRANSLATIONS.put("effect.minecraft.saturation", "Насыщение");
        EFFECT_TRANSLATIONS.put("effect.minecraft.glowing", "Свечение");
        EFFECT_TRANSLATIONS.put("effect.minecraft.levitation", "Левитация");
        EFFECT_TRANSLATIONS.put("effect.minecraft.luck", "Удача");
        EFFECT_TRANSLATIONS.put("effect.minecraft.unluck", "Невезение");
        EFFECT_TRANSLATIONS.put("effect.minecraft.slow_falling", "Плавное падение");
        EFFECT_TRANSLATIONS.put("effect.minecraft.conduit_power", "Мощь источника");
        EFFECT_TRANSLATIONS.put("effect.minecraft.dolphins_grace", "Грация дельфина");
        EFFECT_TRANSLATIONS.put("effect.minecraft.bad_omen", "Дурное знамение");
        EFFECT_TRANSLATIONS.put("effect.minecraft.hero_of_the_village", "Герой деревни");
    }

    private static final Map<String, Integer> EFFECT_ICON_COLORS = new HashMap<>();
    static {
        EFFECT_ICON_COLORS.put("effect.minecraft.speed", 0x7CA8FF);        // Голубой
        EFFECT_ICON_COLORS.put("effect.minecraft.slowness", 0x8A8A8A);   // Серый
        EFFECT_ICON_COLORS.put("effect.minecraft.haste", 0xD9C043);      // Желтый
        EFFECT_ICON_COLORS.put("effect.minecraft.mining_fatigue", 0x4A4A4A); // Темно-серый
        EFFECT_ICON_COLORS.put("effect.minecraft.strength", 0xFF6B6B);   // Красный
        EFFECT_ICON_COLORS.put("effect.minecraft.instant_health", 0xFF8B8B); // Розовый
        EFFECT_ICON_COLORS.put("effect.minecraft.instant_damage", 0x5C2E2E); // Темно-красный
        EFFECT_ICON_COLORS.put("effect.minecraft.jump_boost", 0x9FFF7C);  // Светло-зеленый
        EFFECT_ICON_COLORS.put("effect.minecraft.nausea", 0x55A855);      // Зеленый
        EFFECT_ICON_COLORS.put("effect.minecraft.regeneration", 0xFF6B9D); // Розово-красный
        EFFECT_ICON_COLORS.put("effect.minecraft.resistance", 0x8A8A8A);  // Серый
        EFFECT_ICON_COLORS.put("effect.minecraft.fire_resistance", 0xFF8B3D); // Оранжевый
        EFFECT_ICON_COLORS.put("effect.minecraft.water_breathing", 0x7CA8FF); // Голубой
        EFFECT_ICON_COLORS.put("effect.minecraft.invisibility", 0xF0F0F0); // Почти белый
        EFFECT_ICON_COLORS.put("effect.minecraft.blindness", 0x1A1A1A);   // Черный
        EFFECT_ICON_COLORS.put("effect.minecraft.night_vision", 0x5C2E8C); // Фиолетовый
        EFFECT_ICON_COLORS.put("effect.minecraft.hunger", 0x5C3A2E);      // Коричневый
        EFFECT_ICON_COLORS.put("effect.minecraft.weakness", 0x8A6B5C);    // Серо-коричневый
        EFFECT_ICON_COLORS.put("effect.minecraft.poison", 0x4A8A2E);     // Темно-зеленый
        EFFECT_ICON_COLORS.put("effect.minecraft.wither", 0x2A2A2A);    // Черно-серый
        EFFECT_ICON_COLORS.put("effect.minecraft.health_boost", 0xFF5555); // Красно-розовый
        EFFECT_ICON_COLORS.put("effect.minecraft.absorption", 0xFFD9A3); // Персиковый
        EFFECT_ICON_COLORS.put("effect.minecraft.saturation", 0xD9A36B); // Оранжево-коричневый
        EFFECT_ICON_COLORS.put("effect.minecraft.glowing", 0xFFFF8B);     // Желто-белый
        EFFECT_ICON_COLORS.put("effect.minecraft.levitation", 0xA3A3A3);  // Светло-серый
        EFFECT_ICON_COLORS.put("effect.minecraft.luck", 0x55D955);       // Зеленый
        EFFECT_ICON_COLORS.put("effect.minecraft.unluck", 0x8A4444);     // Темно-красный
        EFFECT_ICON_COLORS.put("effect.minecraft.slow_falling", 0xA3D9FF); // Светло-голубой
        EFFECT_ICON_COLORS.put("effect.minecraft.conduit_power", 0x5C8AA3); // Сине-голубой
        EFFECT_ICON_COLORS.put("effect.minecraft.dolphins_grace", 0x7CD9FF); // Ярко-голубой
        EFFECT_ICON_COLORS.put("effect.minecraft.bad_omen", 0x2A1A1A);     // Темный
        EFFECT_ICON_COLORS.put("effect.minecraft.hero_of_the_village", 0xFF8B3D); // Оранжевый
    }

    private String getEffectNameTranslated(Effect effect, int amplifier) {
        String key = effect.getName();
        String name = EFFECT_TRANSLATIONS.getOrDefault(key, I18n.format(key));
        if (amplifier > 0) {
            name += " " + (amplifier + 1);
        }
        return name;
    }

    private void renderOrdinary(EventRender2D.Post event) {
        MatrixStack ms = event.getStack();
        float posX = dragging.getX(), posY = dragging.getY();
        List<EffectInstance> activeEffects = new ArrayList<>();
        boolean hasActive = false;

        for (EffectInstance effectInstance : mc.player.getActivePotionEffects()) {
            Effect effect = effectInstance.getPotion();
            if (!badeffects.get() && effect.getEffectType() == EffectType.HARMFUL) continue;

            AnimationUtil anim = ordinaryEffectAnimations.computeIfAbsent(effect, k -> new AnimationUtil(0.0f, 10));
            anim.update(1.0f);

            activeEffects.add(effectInstance);
            hasActive = true;
        }

        for (var entry : ordinaryEffectAnimations.entrySet()) {
            boolean hasEffect = mc.player.getActivePotionEffects().stream()
                    .anyMatch(e -> e.getPotion() == entry.getKey() && (badeffects.get() || e.getPotion().getEffectType() != EffectType.HARMFUL));
            if (!hasEffect) {
                entry.getValue().update(0.0f);
            }
        }

        boolean isChatOpen = mc.currentScreen instanceof ChatScreen;
        boolean show = isChatOpen || hasActive;

        ordinaryAlphaAnimation.update(show ? 1.0f : 0.0f);
        float globalAlpha = ordinaryAlphaAnimation.getValue();
        if (globalAlpha <= 0.01f) {
            dragging.setWidth(51.5f);
            dragging.setHeight(11f);
            return;
        }

        List<String> currentIds = new ArrayList<>();
        for (EffectInstance effectInstance : activeEffects) {
            Effect effect = effectInstance.getPotion();
            currentIds.add(effect.getName());
        }
        ordinaryEffectAnimations.entrySet().removeIf(e -> !currentIds.contains(e.getKey()) && e.getValue().isDone() && e.getValue().getValue() <= 0.01f);

        if (isChatOpen && activeEffects.isEmpty()) {
            renderPreviewMode(event, activeEffects, globalAlpha);
            return;
        }

        float baseCardWidth = 51.5f;
        float cardHeight = 22f;
        float cardRadius = 5f;
        float cardPaddingX = 6f;
        float cardPaddingYTop = 3.5f;
        float cardPaddingYBottom = 4f;
        float cardGap = 5f;
        int mainColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int cardBgColor = ColorUtil.darken(mainColor, 0.1f);
        float iconSize = 13f;
        float iconTextGap = 3f;

        float currentY = posY;
        float maxCardWidth = baseCardWidth;
        for (EffectInstance effectInstance : activeEffects) {
            Effect effect = effectInstance.getPotion();
            AnimationUtil itemAnim = ordinaryEffectAnimations.get(effect);
            if (itemAnim == null) continue;
            float animVal = itemAnim.getValue();
            if (animVal <= 0.01f) continue;

            float itemAlpha = animVal * globalAlpha;
            int nameColor = ColorUtil.rgba(255, 255, 255, (int)(255 * itemAlpha));
            int timeColor = ColorUtil.rgba(255, 255, 255, (int)(64 * itemAlpha));

            String effectName = getEffectNameTranslated(effect, effectInstance.getAmplifier());
            String durationText = EffectUtils.getPotionDurationString(effectInstance, 1f);

            float nameWidth = Fonts.sfregular[12].getWidth(effectName);
            float timeWidth = Fonts.sfregular[12].getWidth(durationText);
            float textWidth = Math.max(nameWidth, timeWidth);
            float cardWidth = Math.max(baseCardWidth, cardPaddingX + iconSize + iconTextGap + textWidth + cardPaddingX);
            maxCardWidth = Math.max(maxCardWidth, cardWidth);

            int bgWithAlpha = ColorUtil.applyOpacity(cardBgColor, itemAlpha);
            RenderUtil.drawRoundedRectangle(posX, currentY, cardWidth, cardHeight, cardRadius, bgWithAlpha);

            PotionSpriteUploader potionSpriteUploader = mc.getPotionSpriteUploader();
            TextureAtlasSprite sprite = potionSpriteUploader.getSprite(effect);
            float iconX = posX + cardPaddingX;
            float iconY = currentY + cardPaddingYTop + (cardHeight - cardPaddingYTop - cardPaddingYBottom - iconSize) / 2f;
            mc.getTextureManager().bindTexture(sprite.getAtlasTexture().getTextureLocation());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1f, 1f, 1f, itemAlpha);
            AbstractGui.blit(ms, (int) iconX, (int) iconY, 0, (int) iconSize, (int) iconSize, sprite);
            RenderSystem.color4f(1f, 1f, 1f, 1f);

            float textX = iconX + iconSize + iconTextGap;
            float textY = currentY + cardPaddingYTop + 2.5f;
            float lineHeight = Fonts.sf_medium[12].getFontHeight();

            Fonts.sfregular[12].drawString(ms, effectName, textX, textY, nameColor);
            Fonts.sfregular[12].drawString(ms, durationText, textX, textY + lineHeight + 0.5f, timeColor);

            currentY += (cardHeight + cardGap) * animVal;
        }

        float totalHeight = activeEffects.size() * (cardHeight + cardGap) - cardGap;
        if (totalHeight < 0) totalHeight = cardHeight;
        dragging.setWidth(maxCardWidth);
        dragging.setHeight(totalHeight);
    }

    private void renderPreviewMode(EventRender2D.Post event, List<EffectInstance> activeEffects, float globalAlpha) {
        MatrixStack ms = event.getStack();
        float posX = dragging.getX(), posY = dragging.getY();

        List<String> effectKeys = new ArrayList<>(EFFECT_TRANSLATIONS.keySet());
        if (effectKeys.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEffectSwitchTime >= EFFECT_SWITCH_INTERVAL) {
            lastEffectSwitchTime = currentTime;
            currentEffectIndex = (currentEffectIndex + 1) % effectKeys.size();
        }

        String currentEffectKey = effectKeys.get(currentEffectIndex % effectKeys.size());
        String effectName = EFFECT_TRANSLATIONS.getOrDefault(currentEffectKey, "Эффект");

        effectName += " 2";

        float baseCardWidth = 51.5f;
        float cardHeight = 22f;
        float cardRadius = 5f;
        float cardPaddingX = 6f;
        float cardPaddingYTop = 3.5f;
        float cardPaddingYBottom = 4f;
        int mainColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int cardBgColor = ColorUtil.darken(mainColor, 0.1f);
        float iconSize = 13f;
        float iconTextGap = 3f;

        String durationText = "1:23";

        float nameWidth = Fonts.sfregular[12].getWidth(effectName);
        float timeWidth = Fonts.sfregular[12].getWidth(durationText);
        float textWidth = Math.max(nameWidth, timeWidth);
        float cardWidth = Math.max(baseCardWidth, cardPaddingX + iconSize + iconTextGap + textWidth + cardPaddingX);

        int nameColor = ColorUtil.rgba(255, 255, 255, (int)(255 * globalAlpha));
        int timeColor = ColorUtil.rgba(255, 255, 255, (int)(64 * globalAlpha));
        int bgColor = ColorUtil.darken(mainColor, 0.1f);
        int bgWithAlpha = ColorUtil.applyOpacity(bgColor, globalAlpha);

        RenderUtil.drawRoundedRectangle(posX, posY, cardWidth, cardHeight, cardRadius, bgWithAlpha);

        float iconX = posX + cardPaddingX;
        float iconY = posY + cardPaddingYTop + (cardHeight - cardPaddingYTop - cardPaddingYBottom - iconSize) / 2f;

        String effectId = currentEffectKey.replace("effect.minecraft.", "minecraft:");
        Effect previewEffect = net.minecraft.util.registry.Registry.EFFECTS.getOrDefault(new net.minecraft.util.ResourceLocation(effectId));

        if (previewEffect != null) {
            PotionSpriteUploader potionSpriteUploader = mc.getPotionSpriteUploader();
            TextureAtlasSprite sprite = potionSpriteUploader.getSprite(previewEffect);
            mc.getTextureManager().bindTexture(sprite.getAtlasTexture().getTextureLocation());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1f, 1f, 1f, globalAlpha);
            AbstractGui.blit(ms, (int) iconX, (int) iconY, 0, (int) iconSize, (int) iconSize, sprite);
            RenderSystem.color4f(1f, 1f, 1f, 1f);
        } else {
            int iconColor = EFFECT_ICON_COLORS.getOrDefault(currentEffectKey, 0x8A8A8A);
            int iconWithAlpha = ColorUtil.applyOpacity(iconColor, globalAlpha);
            RenderUtil.drawRoundedRectangle(iconX, iconY, iconSize, iconSize, 2f, iconWithAlpha);
        }

        float textX = iconX + iconSize + iconTextGap;
        float textY = posY + cardPaddingYTop + 2.5f;
        float lineHeight = Fonts.sf_medium[12].getFontHeight();

        Fonts.sfregular[12].drawString(ms, effectName, textX, textY, nameColor);
        Fonts.sfregular[12].drawString(ms, durationText, textX, textY + lineHeight + 0.5f, timeColor);

        dragging.setWidth(cardWidth);
        dragging.setHeight(cardHeight);
    }

    private void renderNursultan(EventRender2D.Post event) {
        MatrixStack ms = event.getStack();
        float posX = dragging.getX();
        float posY = dragging.getY();

        List<EffectInstance> activeEffects = new ArrayList<>();
        boolean hasActive = false;

        for (EffectInstance effectInstance : mc.player.getActivePotionEffects()) {
            Effect effect = effectInstance.getPotion();
            if (!badeffects.get() && effect.getEffectType() == EffectType.HARMFUL) continue;

            AnimationUtil anim = ordinaryEffectAnimations.computeIfAbsent(effect, k -> new AnimationUtil(0.0f, 10));
            anim.update(1.0f);

            activeEffects.add(effectInstance);
            hasActive = true;
        }

        for (var entry : ordinaryEffectAnimations.entrySet()) {
            boolean hasEffect = mc.player.getActivePotionEffects().stream()
                    .anyMatch(e -> e.getPotion() == entry.getKey() && (badeffects.get() || e.getPotion().getEffectType() != EffectType.HARMFUL));
            if (!hasEffect) {
                entry.getValue().update(0.0f);
            }
        }

        boolean isChatOpen = mc.currentScreen instanceof ChatScreen;
        boolean show = isChatOpen || hasActive;
        ordinaryAlphaAnimation.update(show ? 1.0f : 0.0f);
        float globalAlpha = ordinaryAlphaAnimation.getValue();

        if (globalAlpha <= 0.01f) {
            dragging.setWidth(51.5f);
            dragging.setHeight(11f);
            return;
        }

        List<String> currentIds = new ArrayList<>();
        for (EffectInstance effectInstance : activeEffects) {
            Effect effect = effectInstance.getPotion();
            currentIds.add(effect.getName());
        }
        ordinaryEffectAnimations.entrySet().removeIf(e ->
                !currentIds.contains(e.getKey().getName()) &&
                        e.getValue().isDone() &&
                        e.getValue().getValue() <= 0.01f);

        float headerHeight = 12.3f;
        float itemHeight = 9.6f;
        float topOffset = 3.8f;

        float maxCombinedWidthCalc = 0.0f;
        for (EffectInstance effectInstance : activeEffects) {
            Effect effect = effectInstance.getPotion();
            AnimationUtil anim = ordinaryEffectAnimations.get(effect);
            if (anim != null && (anim.getValue() > 0.01f || activeEffects.contains(effectInstance))) {
                String effectName = I18n.format(effectInstance.getEffectName());
                String amplifierText = I18n.format("enchantment.level." + (effectInstance.getAmplifier() + 1));
                String durationText = EffectUtils.getPotionDurationString(effectInstance, 1.0f);
                float combined = Fonts.sf_medium[14].getWidth(effectName) +
                        Fonts.sf_medium[14].getWidth(amplifierText) +
                        Fonts.sf_medium[14].getWidth(durationText) + 22.0f;
                maxCombinedWidthCalc = Math.max(maxCombinedWidthCalc, combined);
            }
        }

        float width = Math.max(65.0f, maxCombinedWidthCalc);
        float contentHeight = 0.0f;
        for (EffectInstance effectInstance : activeEffects) {
            Effect effect = effectInstance.getPotion();
            AnimationUtil anim = ordinaryEffectAnimations.get(effect);
            if (anim != null) {
                contentHeight += itemHeight * anim.getValue();
            }
        }

        float windowHeight = headerHeight + (contentHeight <= 0.01f ? 6.0f : contentHeight + 6.7f);

        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int iconColor = ColorUtil.applyOpacity(logoColor, ThemeEditor.getAlpha(ThemeSettings.LOGO) / 255f * globalAlpha);
        int textColor = ColorUtil.applyOpacity(0xFFFFFFFF, globalAlpha);

        RenderUtil.drawBlurredRoundedRectangle(posX, posY + topOffset, width, windowHeight - topOffset, 4.9f,
                alphabg.get() ? ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.WINDOW_BG), 0) : ThemeEditor.getColor(ThemeSettings.WINDOW_BG),
                globalAlpha);

        if (width > 35f) {
            float iconX = posX + 54.0f + (width - 65.0f);
            Fonts.skycore[13].drawString(ms, "e", iconX + 0.5f, posY + 10.0f, iconColor);
            Fonts.sf_medium[15].drawString(ms, "Potions", posX + 4.0f, posY + 9.0f, textColor);
        }

        float renderY = posY + headerHeight + 3;
        for (EffectInstance effectInstance : activeEffects) {
            Effect effect = effectInstance.getPotion();
            AnimationUtil itemAnim = ordinaryEffectAnimations.get(effect);
            if (itemAnim == null) continue;

            float animVal = itemAnim.getValue();
            if (animVal <= 0.01f) continue;

            float slideOffset = (1.0f - animVal) * 10.0f;
            float itemAlpha = animVal * globalAlpha;

            int itemTextColor = ColorUtil.applyOpacity(0xFFFFFFFF, itemAlpha);
            int itemLevelColor = itemTextColor;
            int itemDurationColor = itemTextColor;

            String effectName = I18n.format(effectInstance.getEffectName());
            String amplifierText = I18n.format("enchantment.level." + (effectInstance.getAmplifier() + 1));
            String durationText = EffectUtils.getPotionDurationString(effectInstance, 1.0f);

            PotionSpriteUploader potionSpriteUploader = mc.getPotionSpriteUploader();
            TextureAtlasSprite sprite = potionSpriteUploader.getSprite(effect);

            float iconSize = 7.0f;
            float iconX = posX + 3.0f - slideOffset;
            float iconY = renderY + (itemHeight - iconSize) / 2.0f + 1.9f;

            mc.getTextureManager().bindTexture(sprite.getAtlasTexture().getTextureLocation());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1f, 1f, 1f, itemAlpha);
            AbstractGui.blit(ms, (int) iconX, (int) iconY, 0, (int) iconSize, (int) iconSize, sprite);
            RenderSystem.color4f(1f, 1f, 1f, 1f);

            float textY = renderY + 4.05f;
            float nameX = iconX + iconSize + 4.0f;

            Fonts.sf_medium[14].drawString(ms, effectName, nameX - 1.0f, textY, itemTextColor);
            float nameWidth = Fonts.sf_medium[14].getWidth(effectName);
            Fonts.sf_medium[14].drawString(ms, amplifierText, nameX + nameWidth + 2.0f, textY, itemLevelColor);

            float durationWidth = Fonts.sf_medium[14].getWidth(durationText);
            float dx = posX + width - durationWidth - 3.0f + slideOffset;
            if (width > durationWidth + 12.0f) {
                Fonts.sf_medium[14].drawString(ms, durationText, dx, textY, itemDurationColor);
            }

            renderY += itemHeight * animVal;
        }

        dragging.setHeight(windowHeight);
        dragging.setWidth(width);
    }

    private List<PotionItem> getActiveItems() {
        List<PotionItem> activeItems = new ArrayList<>();
        for (EffectInstance instance : mc.player.getActivePotionEffects()) {
            Effect effect = instance.getPotion();
            if (badeffects.get() || effect.getEffectType() != EffectType.HARMFUL) {
                activeItems.add(new PotionItem(effect, instance.getAmplifier()));
            }
        }
        return activeItems;
    }

    private float[] calculateDimensions(List<PotionItem> dimsItems, Map<PotionItem, EffectInstance> effectInstances) {
        float maxNameWidth = 0, maxDurationWidth = 0;
        for (PotionItem item : dimsItems) {
            maxNameWidth = Math.max(maxNameWidth, Fonts.sf_medium[12].getWidth(item.getEffectName()));
            String duration;
            if (effectInstances.containsKey(item)) duration = effectInstances.get(item).getDuration() > DURATION_HIDE_TICKS ? "**:**" : EffectUtils.getPotionDurationString(effectInstances.get(item), 1);
            else duration = lastDurationTicks.getOrDefault(item, 0) > DURATION_HIDE_TICKS ? "**:**" : lastDurationTexts.getOrDefault(item, "");
            maxDurationWidth = Math.max(maxDurationWidth, Fonts.sf_medium[12].getWidth(duration));
        }
        float width = Math.max(90, 3.5f + maxNameWidth + 3.5f + maxDurationWidth + 3.5f + 0.5f + 2.5f + 7f + 3.5f);
        return new float[]{width};
    }

    @Getter
    private static class PotionItem {
        private final Effect effect;
        private final int amplifier;

        public PotionItem(Effect effect, int amplifier) {
            this.effect = effect;
            this.amplifier = amplifier;
        }

        public String getEffectName() {
            return I18n.format(effect.getName()) + (amplifier > 0 ? " " + (amplifier + 1) : "");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PotionItem that = (PotionItem) o;
            return amplifier == that.amplifier && effect.equals(that.effect);
        }

        @Override
        public int hashCode() {
            return 31 * effect.hashCode() + amplifier;
        }
    }
}