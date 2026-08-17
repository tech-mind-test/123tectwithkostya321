package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import sky.core.events.EventRender2D;
import sky.core.handlers.impl.StaffHandler;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.utils.managers.impl.staffmanager.Staff;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import sky.core.utils.render.shader.ShaderUtil;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class StaffListRender implements ElementRender {
    public static BooleanSetting skins = new BooleanSetting("Отображать скин", true);
    public static BooleanSetting alphabg = new BooleanSetting("Прозрачный фон", false);

    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");
    private static final ResourceLocation ALEX_SKIN = new ResourceLocation("textures/entity/alex.png");

    private final Dragging dragging;
    private final AnimationUtil alphaAnimation = new AnimationUtil(0.0f, 10);
    private final Map<String, AnimationUtil[]> itemAnimations = new HashMap<>();
    private final Map<String, Staff> lastSeenStaff = new HashMap<>();
    private final Map<String, AnimationUtil> ordinaryItemAnimations = new HashMap<>();
    private float ordinaryAnimatedWidth = 40f;

    private static final float PADDING_LEFT = 4f;
    private static final float PADDING_RIGHT = 4f;
    private static final float MIN_GAP_BEFORE_STATUS = 6f;
    private static final float PREFIX_NAME_GAP = 1.5f;

    @Override
    public void render(EventRender2D.Post event) {
        if (Interface.isOldHud()) {
            renderOrdinary(event);
        } else {
            renderNursultan(event);
        }
    }

    private void renderOrdinary(EventRender2D.Post event) {
        MatrixStack ms = event.getStack();
        float posX = dragging.getX(), posY = dragging.getY();
        List<Staff> activeItems = StaffHandler.getInstance() != null ? StaffHandler.getInstance().getStaff() : new ArrayList<>();
        boolean hasActive = false;

        for (Staff staff : activeItems) {
            ordinaryItemAnimations.computeIfAbsent(staff.getName(), k -> new AnimationUtil(0.0f, 10));
            hasActive = true;
        }

        for (var entry : ordinaryItemAnimations.entrySet()) {
            boolean exists = activeItems.stream().anyMatch(s -> s.getName().equals(entry.getKey()));
            entry.getValue().update(exists ? 1.0f : 0.0f);
        }

        boolean isChatOpen = mc.currentScreen instanceof ChatScreen;
        boolean show = isChatOpen || hasActive;

        alphaAnimation.update(show ? 1.0f : 0.0f);
        float globalAlpha = alphaAnimation.getValue();
        if (globalAlpha <= 0.01f) {
            ordinaryAnimatedWidth = 65f;
            dragging.setWidth(ordinaryAnimatedWidth);
            dragging.setHeight(18.5f);
            return;
        }

        float headerHeight = 12.3f;
        float itemHeight = 9.3f;
        float maxLeftPartWidth = 0.0f;
        float maxStatusWidth = 0.0f;
        List<String> currentIds = new ArrayList<>();
        for (Staff staff : activeItems) {
            currentIds.add(staff.getName());
            AnimationUtil anim = ordinaryItemAnimations.get(staff.getName());
            if (anim != null && (anim.getValue() > 0.01f || activeItems.contains(staff))) {
                String prefixPlain = TextFormatting.getTextWithoutFormattingCodes(staff.getPrefix().getString());
                if (prefixPlain == null) prefixPlain = "";
                String statusText = staff.isVanished() ? "[V]" : "[Online]";
                float prefixWidth = Fonts.sf_medium[14].getWidth(prefixPlain);
                float nameWidth = Fonts.sf_medium[14].getWidth(" " + staff.getName());
                float statusWidth = Fonts.sf_medium[14].getWidth(statusText);
                maxLeftPartWidth = Math.max(maxLeftPartWidth, prefixWidth + nameWidth);
                maxStatusWidth = Math.max(maxStatusWidth, statusWidth);
            }
        }
        ordinaryItemAnimations.entrySet().removeIf(e -> !currentIds.contains(e.getKey()) && e.getValue().isDone() && e.getValue().getValue() <= 0.01f);

        float contentWidth = PADDING_LEFT + maxLeftPartWidth + MIN_GAP_BEFORE_STATUS + maxStatusWidth + PADDING_RIGHT;
        float minWidth = 65f;
        float targetWidth = show ? Math.max(minWidth, contentWidth) : minWidth;
        ordinaryAnimatedWidth = targetWidth;

        float contentHeight = 0.0f;
        for (Staff staff : activeItems) {
            AnimationUtil anim = ordinaryItemAnimations.get(staff.getName());
            if (anim != null) {
                contentHeight += itemHeight * anim.getValue();
            }
        }
        float windowHeight = headerHeight + (contentHeight <= 0.01f ? 6.0f : contentHeight + 6.7f);

        int headerColor = ColorUtil.applyOpacity(0xFFFFFFFF, globalAlpha);
        int iconColor = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.LOGO), ThemeEditor.getAlpha(ThemeSettings.LOGO) / 255f * globalAlpha);
        int windowBgColor = ThemeEditor.getColor(ThemeSettings.WINDOW_BG);

        float topOffset = 3.8f;
        RenderUtil.drawBlurredRoundedRectangle(posX, posY + topOffset, ordinaryAnimatedWidth, windowHeight - topOffset, 4.9f,
                alphabg.get() ? ColorUtil.applyOpacity(windowBgColor, 0) : windowBgColor, globalAlpha);

        if (ordinaryAnimatedWidth > 35f) {
            float iconWidth = Fonts.lox[12].getWidth("f");
            float iconX = posX + ordinaryAnimatedWidth - PADDING_RIGHT - iconWidth;
            Fonts.lox[12].drawString(ms, "f", iconX - 0.5f, posY + 9.74f, iconColor);
            Fonts.sf_medium[15].drawString(ms, "Staffs", posX + PADDING_LEFT, posY + 8.55f, headerColor);
        }

        float renderY = posY + headerHeight + 3;
        for (Staff staff : activeItems) {
            AnimationUtil itemAnim = ordinaryItemAnimations.get(staff.getName());
            if (itemAnim == null) continue;
            float animVal = itemAnim.getValue();
            if (animVal <= 0.01f) continue;

            float slideOffset = (1.0f - animVal) * 10.0f;
            int itemTextColor = ColorUtil.applyOpacity(0xFFFFFFFF, animVal * globalAlpha);

            String prefixPlain = TextFormatting.getTextWithoutFormattingCodes(staff.getPrefix().getString());
            if (prefixPlain == null) prefixPlain = "";

            float currentX = posX + PADDING_LEFT - slideOffset;
            Fonts.sf_medium[14].drawText(ms, staff.getPrefix(), currentX, renderY + 4.20f, ColorUtil.applyOpacity(-1, globalAlpha * animVal));
            currentX += Fonts.sf_medium[14].getWidth(prefixPlain);

            Fonts.sf_medium[14].drawString(ms, " " + staff.getName(), currentX, renderY + 4.20f, itemTextColor);

            String statusDisplay = staff.isVanished() ? "[V]" : "[Online]";
            int statusColor = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.LOGO), animVal * globalAlpha);
            float statusWidth = Fonts.sf_medium[14].getWidth(statusDisplay);
            float statusX = posX + ordinaryAnimatedWidth - PADDING_RIGHT - statusWidth + slideOffset;
            Fonts.sf_medium[14].drawString(ms, statusDisplay, statusX, renderY + 4.20f, statusColor);

            renderY += itemHeight * animVal;
        }

        dragging.setHeight(windowHeight);
        dragging.setWidth(ordinaryAnimatedWidth);
    }

    private void renderNursultan(EventRender2D.Post event) {
        MatrixStack ms = event.getStack();
        float posX = dragging.getX(), posY = dragging.getY();
        List<Staff> entries = StaffHandler.getInstance() != null ? StaffHandler.getInstance().getStaff() : new ArrayList<>();
        boolean hasActive = !entries.isEmpty();
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
        float headSize = 6f;
        float headGap = 4f;

        float maxItemWidth = 0;
        List<String> currentIds = new ArrayList<>();
        float headerHeight = 12 + gap;
        float contentHeight = 0;

        for (Staff entry : entries) {
            currentIds.add(entry.getName());
            AnimationUtil[] anim = itemAnimations.computeIfAbsent(
                    entry.getName(),
                    key -> new AnimationUtil[]{new AnimationUtil(0.0f, 10), new AnimationUtil(-5, 10), new AnimationUtil(0, 10)}
            );
            anim[0].update(1.0f);

            float animVal = anim[0].getValue();
            if (animVal > 0.01f) {
                String prefixPlain = TextFormatting.getTextWithoutFormattingCodes(entry.getPrefix().getString());
                if (prefixPlain == null) prefixPlain = "";
                float prefixWidth = Fonts.sfregular[13].getWidth(prefixPlain);
                float nameWidth = Fonts.sfregular[13].getWidth(entry.getName());
                String statusText = entry.isVanished() ? "SPEC" : "ACTIVE";
                float statusWidth = Fonts.sfregular[13].getWidth(statusText);
                float itemWidth = headSize + headGap + prefixWidth + PREFIX_NAME_GAP + nameWidth + itemGap + statusWidth;
                maxItemWidth = Math.max(maxItemWidth, itemWidth);
                contentHeight += (itemHeight + itemGap) * animVal;
            }
        }

        itemAnimations.entrySet().removeIf(e ->
                !currentIds.contains(e.getKey()) &&
                        e.getValue()[0].isDone() &&
                        e.getValue()[0].getValue() <= 0.01f
        );

        float targetWidth = Math.max(totalWidth, maxItemWidth + paddingX * 2);
        float animatedHeight = paddingY + headerHeight + contentHeight + paddingY - 10;

        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(logoColor, 0.1f);
        int iconColor = logoColor;
        int textColor = 0xFFFFFFFF;
        int statusColor = logoColor;

        RenderUtil.drawRoundedRectangle(posX, posY, targetWidth, animatedHeight, borderRadius, ColorUtil.applyOpacity(bgColor, (int) (255 * globalAlpha)));

        float currentY = posY + paddingY;
        float currentX = posX + paddingX;

        Fonts.skycore[14].drawString(ms, "d", currentX, currentY + 2.9f, ColorUtil.applyOpacity(iconColor, (int) (255 * globalAlpha)));
        currentX += iconSize + headerGap;
        Fonts.sfregular[13].drawString(ms, "Personal", currentX + 1, currentY + 2.5f, ColorUtil.applyOpacity(textColor, (int) (255 * globalAlpha)));

        currentY += headerHeight;

        for (Staff entry : entries) {
            AnimationUtil[] itemAnims = itemAnimations.get(entry.getName());
            if (itemAnims == null) continue;
            float animVal = itemAnims[0].getValue();
            if (animVal <= 0.01f) continue;

            currentX = posX + paddingX;
            float slideOffset = (1.0f - animVal) * 10.0f;
            float headY = currentY - 3.5f - slideOffset;

            if (skins.get()) {
                ResourceLocation skinLocation = getStaffSkin(entry.getName());

                mc.getTextureManager().bindTexture(skinLocation);
                ShaderUtil.rounded_head_texture.attach();
                ShaderUtil.rounded_head_texture.setUniformf("size", headSize, headSize);
                ShaderUtil.rounded_head_texture.setUniformf("radius", headSize / 2f);
                ShaderUtil.rounded_head_texture.setUniformf("roundType", 0.0f);
                ShaderUtil.rounded_head_texture.setUniformf("hurt_time", 0f);
                ShaderUtil.rounded_head_texture.setUniformf("alpha", animVal * globalAlpha);
                ShaderUtil.rounded_head_texture.setUniformf("texXSize", 64);
                ShaderUtil.rounded_head_texture.setUniformf("texYSize", 64);

                RenderUtil.beginRectBatch(false, true);
                RenderUtil.drawQuads(currentX, headY, headSize, headSize);
                RenderUtil.endRectBatch();

                ShaderUtil.rounded_head_texture.detach();
                currentX += headSize + headGap;
            }

            String prefixPlain = TextFormatting.getTextWithoutFormattingCodes(entry.getPrefix().getString());
            if (prefixPlain == null) prefixPlain = "";

            Fonts.sfregular[13].drawText(ms, entry.getPrefix(), currentX, currentY - 2 - slideOffset,
                    ColorUtil.applyOpacity(-1, (int) (255 * animVal * globalAlpha)));
            currentX += Fonts.sfregular[13].getWidth(prefixPlain) + PREFIX_NAME_GAP;

            Fonts.sfregular[13].drawString(ms, entry.getName(), currentX, currentY - 2 - slideOffset,
                    ColorUtil.applyOpacity(textColor, (int) (255 * animVal * globalAlpha)));

            String statusText = entry.isVanished() ? "SPEC" : "ACTIVE";
            float statusWidth = Fonts.sfregular[13].getWidth(statusText);
            float statusX = posX + targetWidth - paddingX - statusWidth;
            Fonts.sfregular[13].drawString(ms, statusText, statusX + slideOffset, currentY - 2 - slideOffset, ColorUtil.applyOpacity(statusColor, (int) (255 * animVal * globalAlpha)));

            currentY += (itemHeight + itemGap) * animVal;
        }

        dragging.setWidth(targetWidth);
        dragging.setHeight(animatedHeight);
    }

    private ResourceLocation getStaffSkin(String name) {
        if (mc.getConnection() != null) {
            for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
                if (info != null && info.getGameProfile() != null
                        && name.equalsIgnoreCase(info.getGameProfile().getName())) {
                    try {
                        ResourceLocation skin = info.getLocationSkin();
                        if (skin != null
                                && !skin.equals(STEVE_SKIN)
                                && !skin.equals(ALEX_SKIN)
                                && !skin.getPath().contains("steve")
                                && !skin.getPath().contains("alex")) {
                            return skin;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if (mc.world != null) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (name.equalsIgnoreCase(player.getGameProfile().getName())) {
                    ResourceLocation skin = mc.getRenderManager().getRenderer(player).getEntityTexture(player);
                    if (skin != null) {
                        return skin;
                    }
                }
            }
        }

        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return (offlineUuid.hashCode() & 1) == 0 ? STEVE_SKIN : ALEX_SKIN;
    }

    private float[] calculateDimensions(List<Staff> dimsItems) {
        float maxTextWidth = 0;

        for (Staff item : dimsItems) {
            String plainName = TextFormatting.getTextWithoutFormattingCodes(item.getPrefix().getString());
            float prefixWidth = Fonts.sf_medium[12].getWidth(plainName);
            float nameWidth = Fonts.sf_medium[12].getWidth(item.getName());
            maxTextWidth = Math.max(maxTextWidth, prefixWidth + nameWidth);
        }
        float avatarBlock = skins.get() ? 9.5f : 0.0f;
        float width = Math.max(68, avatarBlock + maxTextWidth + 21);
        return new float[]{width};
    }
}
