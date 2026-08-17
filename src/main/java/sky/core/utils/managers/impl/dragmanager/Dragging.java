package sky.core.utils.managers.impl.dragmanager;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.eventapinew.EventTarget;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import sky.core.SkyCore;
import sky.core.events.EventDragging;
import sky.core.events.EventMouseClicked;
import sky.core.events.EventMouseReleased;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.dragmanager.elements.impl.DraggingBooleanElement;
import sky.core.utils.managers.impl.dragmanager.elements.impl.DraggingModeElement;
import sky.core.modules.api.constructors.Setting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.elements.Element;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.Wrapper;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.misc.OtherUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class Dragging implements Wrapper {
    private String name;
    @Getter(AccessLevel.NONE)
    private float x, y;
    private float width, height;
    private final float initialX, initialY;
    private boolean isDragging, isHovered, settingsVisible;
    private float offsetX, offsetY, settingsX, settingsY, settingsOffsetX, settingsOffsetY;

    private final MultiBooleanSetting elements;
    private final AnimationUtil textAlphaAnimation = new AnimationUtil(0f, 15f, Easings.LINEAR);
    private final AnimationUtil scaleAnimation = new AnimationUtil(0f, 10f, Easings.LINEAR);
    private final AnimationUtil dragScaleAnimation = new AnimationUtil(1f, 12f, Easings.QUAD_OUT);

    private static final float DRAG_SCALE = 1.045f;
    private static final float MAX_TILT_DEGREES = 16f;
    private static final float TILT_VELOCITY_FACTOR = 4.4f;
    private static final float POS_LERP_DRAG = 0.1f;
    private static final float POS_LERP_IDLE = 0.18f;
    private static final float TILT_LERP_DRAG = 0.12f;
    private static final float TILT_LERP_IDLE = 0.08f;
    private static final float VELOCITY_SMOOTH = 0.1f;

    private float renderX;
    private float renderY;
    private float renderTilt;
    private float prevTargetX;
    private float prevTargetY;
    private float smoothedVelocityX;
    private boolean renderTransformPushed;
    private boolean renderScalePushed;

    private static Dragging currentlyDragging = null;

    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Element> settingElements = new ArrayList<>();
    private final DraggingManager draggingManager;
    private final boolean draggable;

    public Dragging(String name, float x, float y, MultiBooleanSetting elements) {
        this(name, x, y, elements, true);
    }

    public Dragging(String name, float x, float y, MultiBooleanSetting elements, boolean draggable) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.initialX = x;
        this.initialY = y;
        this.elements = elements;
        this.draggable = draggable;
        this.renderX = x;
        this.renderY = y;
        this.prevTargetX = x;
        this.prevTargetY = y;

        this.draggingManager = SkyCore.getInstance().getDraggingManager();
        draggingManager.addDraggable(this);
        EventManager.register(this);
    }

    public void addSettings(Setting<?>... settings) {
        for (Setting<?> s : settings) {
            this.settings.add(s);
            if (s instanceof BooleanSetting bs) {
                settingElements.add(new DraggingBooleanElement(bs));
            }
            if (s instanceof ModeSetting bs) {
                settingElements.add(new DraggingModeElement(bs));
            }
        }

        draggingManager.loadSettingsFor(this);
    }

    @EventTarget
    public void onMouseClicked(EventMouseClicked event) {
        if (!(mc.currentScreen instanceof ChatScreen)) return;
        if (mc.gameSettings.showDebugInfo) return;
        // ChatScreen already provides scaled gui coordinates.
        float mx = event.getMouseX();
        float my = event.getMouseY();

        if (settingsVisible && event.getKey() == 0) {
            float elemW = 75f;
            float offset = 24f;
            for (Element element : settingElements) {
                if (element instanceof DraggingBooleanElement dbe) {
                    float textWidth = Fonts.sf_medium[12].getWidth(dbe.getSetting().getName());
                    elemW = Math.max(elemW, textWidth + offset);
                } else if (element instanceof DraggingModeElement dme) {
                    float textWidth = Fonts.sf_medium[12].getWidth(dme.getSetting().getName());
                    elemW = Math.max(elemW, textWidth + offset);
                }
            }
            float totalHeight = 2f;
            for (Element element : settingElements) {
                totalHeight += element.getHeight();
            }

            if (!MathUtil.isHovered(mx, my, settingsX, settingsY, elemW, totalHeight)) {
                settingsVisible = false;
                event.setCancelled(true);
                return;
            }

            for (Element element : settingElements) {
                if (element.isHovered(mx, my)) {
                    element.mouseClicked(mx, my, event.getKey());
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (isAnySettingsOpen() && !settingsVisible) {
            if (event.getKey() != 1) return;
        }

        if (event.getKey() == 1 && isInside(mx, my)) {
            if (!isTopmostUnderMouse(mx, my)) {
                return;
            }
            for (Dragging d : draggingManager.getRenderOrder()) {
                if (d != this && d.settingsVisible) {
                    d.settingsVisible = false;
                }
            }
            if (!settingsVisible) {
                settingsOffsetX = mx - x;
                settingsOffsetY = my - y;
                settingsX = x + settingsOffsetX;
                settingsY = y + settingsOffsetY;
                scaleAnimation.setValue(0f);
            }
            settingsVisible = !settingsVisible;
            event.setCancelled(true);
            return;
        }

        if (draggable && event.getKey() == 0 && currentlyDragging == null) {
            for (Dragging draggable : draggingManager.getRenderOrder()) {
                if (!draggable.isVisible() || !draggable.isInside(mx, my)) continue;
                if (draggable == this) {
                    settingsVisible = false;
                    isDragging = true;
                    currentlyDragging = this;
                    offsetX = mx - x;
                    offsetY = my - y;
                    draggingManager.bringToFront(this);
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (draggable && event.getKey() == 2 && isInside(mx, my)) {
            x = initialX;
            y = initialY;
            syncSmoothPosition();
            renderTilt = 0f;
            smoothedVelocityX = 0f;
            draggingManager.save();
            event.setCancelled(true);
        }
    }


    @EventTarget
    public void onMouseReleased(EventMouseReleased event) {
        if (draggable && mc.currentScreen instanceof ChatScreen && !mc.gameSettings.showDebugInfo && event.getKey() == 0 && isDragging) {
            isDragging = false;
            currentlyDragging = null;
            draggingManager.save();
            event.setCancelled(true);
        }
    }

    private boolean isInside(float mouseX, float mouseY) {
        return MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
    }

    public void syncSmoothPosition() {
        renderX = x;
        renderY = y;
        dragScaleAnimation.setValue(1f);
    }

    public static void prepareHudDragFrame(float mouseX, float mouseY) {
        if (currentlyDragging != null) {
            currentlyDragging.updatePosition(mouseX, mouseY);
        }
        for (Dragging drag : SkyCore.getInstance().getDraggingManager().getRenderOrder()) {
            if (drag != null) {
                drag.tickSmoothing();
            }
        }
    }

    public static void tickAllSmoothing() {
        for (Dragging drag : SkyCore.getInstance().getDraggingManager().getRenderOrder()) {
            if (drag != null) {
                drag.tickSmoothing();
            }
        }
    }

    private boolean isVisible() {
        if (elements == null) {
            return true;
        }
        Boolean value = elements.is(name);
        return value == null || value;
    }

    public float getTargetX() {
        return x;
    }

    public float getTargetY() {
        return y;
    }

    public float getX() {
        return renderX;
    }

    public float getY() {
        return renderY;
    }

    public void tickSmoothing() {
        float dx = x - prevTargetX;
        prevTargetX = x;
        prevTargetY = y;
        dragScaleAnimation.update(draggable && isDragging ? DRAG_SCALE : 1f);

        if (draggable && isDragging) {
            renderX = MathHelper.lerp(POS_LERP_DRAG, renderX, x);
            renderY = MathHelper.lerp(POS_LERP_DRAG, renderY, y);

            smoothedVelocityX = MathHelper.lerp(VELOCITY_SMOOTH, smoothedVelocityX, dx);
            float targetTilt = MathHelper.clamp(smoothedVelocityX * TILT_VELOCITY_FACTOR, -MAX_TILT_DEGREES, MAX_TILT_DEGREES);
            renderTilt = MathHelper.lerp(TILT_LERP_DRAG, renderTilt, targetTilt);
        } else {
            smoothedVelocityX = MathHelper.lerp(0.32f, smoothedVelocityX, 0f);
            renderX = MathHelper.lerp(POS_LERP_IDLE, renderX, x);
            renderY = MathHelper.lerp(POS_LERP_IDLE, renderY, y);
            renderTilt = MathHelper.lerp(TILT_LERP_IDLE, renderTilt, 0f);
        }
    }

    public void pushRenderTransform() {
        float centerX = getX() + width / 2f;
        float centerY = getY() + height / 2f;
        float tilt = renderTilt;
        float scale = dragScaleAnimation.getValue();
        if (Math.abs(tilt) < 0.01f && Math.abs(scale - 1f) < 0.001f) {
            return;
        }
        if (Math.abs(scale - 1f) >= 0.001f) {
            RenderUtil.scaleStart(centerX, centerY, scale);
            renderScalePushed = true;
        }
        if (Math.abs(tilt) >= 0.01f) {
            RenderUtil.rotateStart(centerX, centerY, tilt);
            renderTransformPushed = true;
        }
    }

    public void popRenderTransform() {
        if (renderTransformPushed) {
            RenderUtil.rotateEnd();
            renderTransformPushed = false;
        }
        if (renderScalePushed) {
            RenderUtil.scaleEnd();
            renderScalePushed = false;
        }
    }

    public void updatePosition(float mouseX, float mouseY) {
        if (!draggable) return;
        if (!isDragging) return;

        int screenWidth = mc.getMainWindow().getScaledWidth();
        int screenHeight = mc.getMainWindow().getScaledHeight();

        float maxX = Math.max(0f, screenWidth - width);
        float maxY = Math.max(0f, screenHeight - height);

        x = MathHelper.clamp(mouseX - offsetX, 0f, maxX);
        y = MathHelper.clamp(mouseY - offsetY, 0f, maxY);

        if (scaleAnimation.getValue() > 0f) {
            settingsX = x + settingsOffsetX;
            settingsY = y + settingsOffsetY;
        }
    }


    @EventTarget
    public void onEvent(EventDragging e) {
        if (currentlyDragging == this) {
            updatePosition((float) e.getMouseX(), (float) e.getMouseY());
        }
    }

    @EventTarget
    public void onEvent(EventRender2D.Send e) {
        Vector2f mouse = OtherUtil.getMouse((int) mc.mouseHelper.getMouseX(), (int) mc.mouseHelper.getMouseY());
        float mx = mouse.x / 2, my = mouse.y / 2;
        if (!draggable && name.equals("Уведомления")) {
            float previewWidth = 8f + 7f + 4f + Fonts.sfregular[12].getWidth("Это пример уведомления") + 8f;
            x = (mc.getMainWindow().getScaledWidth() - previewWidth) / 2f;
            y = mc.getMainWindow().getScaledHeight() / 2f + 13f;
            width = previewWidth;
            height = 19f;
            renderX = x;
            renderY = y;
        }

        boolean chatOpen = mc.currentScreen instanceof ChatScreen && SkyCore.getInstance().getModuleManager().getModule(Interface.class).isEnabled() && !mc.gameSettings.showDebugInfo;
        if (!chatOpen) {
            settingsVisible = false;
            if (isDragging) {
                isDragging = false;
                currentlyDragging = null;
                draggingManager.save();
            }
        }

        Dragging topmostHovered = null;
        if (chatOpen && !isDragging) {
            for (Dragging d : draggingManager.getRenderOrder()) {
                if (d == null) continue;
                boolean dVisible = d.isVisible();
                if (!dVisible) continue;
                if (MathUtil.isHovered(mx, my, d.x, d.y, d.width, d.height)) {
                    topmostHovered = d;
                }
            }
        }

        isHovered = chatOpen && !isDragging && topmostHovered == this && !isAnySettingsOpen() && !settingsVisible;
        textAlphaAnimation.update(isHovered ? 1.0f : 0.0f);
        if (textAlphaAnimation.getValue() > 0f && isVisible()) {
            renderTooltip(e);
        }
        scaleAnimation.update(settingsVisible ? 1f : 0f);
        if (scaleAnimation.getValue() > 0f && !settingElements.isEmpty()) {
            renderSettingsElements(e, mx, my);
        }
    }

    private void renderTooltip(EventRender2D.Send e) {
        float alpha = (ThemeEditor.getAlpha(ThemeSettings.TOOLTIP) / 255f) * textAlphaAnimation.getValue();
        if (alpha <= 0.01f) {
            return;
        }

        int color = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TOOLTIP), alpha);
        int outlineColor = ColorUtil.applyOpacity(ColorUtil.getColor(0, 0, 0, 255), alpha * 0.75f);
        float screenWidth = mc.getMainWindow().getScaledWidth();
        float screenHeight = mc.getMainWindow().getScaledHeight();
        float elementX = getX();
        float elementY = getY();
        float elementCenterX = elementX + width / 2.0f;
        float textHeight = Fonts.sf_medium[11].getHeight();
        float lineGap = 2.5f;

        if (!draggable) {
            String settingsText = "ПКМ - Дополнительные настройки";
            float textY = getTooltipY(elementY, height, textHeight, screenHeight);
            drawCenteredTooltipLine(e, settingsText, elementCenterX, textY, screenWidth, color, outlineColor);
        } else {
            String resetText = "СКМ - Сбросить расположение";
            String settingsText = "ПКМ - Дополнительные настройки";
            float blockHeight = textHeight * 2.0f + lineGap;
            float textY = getTooltipY(elementY, height, blockHeight, screenHeight);
            boolean below = textY > elementY;
            float resetY = below ? textY : textY + textHeight + lineGap;
            float settingsY = below ? textY + textHeight + lineGap : textY;
            float slide = (1.0f - textAlphaAnimation.getValue()) * (below ? -textHeight - lineGap : textHeight + lineGap);

            drawCenteredTooltipLine(e, resetText, elementCenterX, resetY + slide, screenWidth, color, outlineColor);
            drawCenteredTooltipLine(e, settingsText, elementCenterX, settingsY - slide, screenWidth, color, outlineColor);
        }
    }

    private float getTooltipY(float elementY, float elementHeight, float tooltipHeight, float screenHeight) {
        float belowY = elementY + elementHeight + 3.0f;
        float aboveY = elementY - tooltipHeight - 2.5f;
        float preferredY = elementY + elementHeight < screenHeight / 2.0f ? belowY : aboveY;
        return MathHelper.clamp(preferredY, 1.0f, screenHeight - tooltipHeight - 1.0f);
    }

    private void drawCenteredTooltipLine(EventRender2D.Send e, String text, float centerX, float y,
                                         float screenWidth, int color, int outlineColor) {
        float textWidth = Fonts.sf_medium[11].getWidth(text);
        float textX = MathHelper.clamp(centerX - textWidth / 2.0f, 1.0f, screenWidth - textWidth - 1.0f);
        Fonts.sfregular[11].drawOutlineString(e.getStack(), text, textX, y, color, true, true, true, true, outlineColor);
    }

    private void renderSettingsElements(EventRender2D.Send e, float mouseX, float mouseY) {
        float elemW = 75f;
        float offset = 24f;
        for (Element element : settingElements) {
            if (element instanceof DraggingBooleanElement dbe) {
                float textWidth = Fonts.sf_medium[12].getWidth(dbe.getSetting().getName());
                elemW = Math.max(elemW, textWidth + offset);
            } else if (element instanceof DraggingModeElement dme) {
                float textWidth = Fonts.sf_medium[12].getWidth(dme.getSetting().getName());
                elemW = Math.max(elemW, textWidth + offset);
            }
        }
        float totalHeight = 4f;
        for (Element element : settingElements) {
            totalHeight += element.getHeight();
        }
        RenderUtil.scaleStart(settingsX + elemW / 2.0f, settingsY + totalHeight / 2.0f, scaleAnimation.getValue());
        RenderUtil.beginNoLiquidGlass();
        try {
            int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
            int bgColor = ColorUtil.darken(logoColor, 0.1f);
            int bgWithAlpha = ColorUtil.applyOpacity(bgColor, ThemeEditor.getAlpha(ThemeSettings.LOGO) / 255f * scaleAnimation.getValue());
            RenderUtil.drawRoundedRectangle(settingsX - 3, settingsY - 1, elemW + 6, totalHeight + 2, 5, bgWithAlpha);

            float currentY = settingsY + 1;
            for (Element element : settingElements) {
                element.setX(settingsX);
                element.setY(currentY);
                element.setWidth(elemW);
                element.render(e.getStack(), mouseX, mouseY, 1f);
                currentY += element.getHeight();
            }
        } finally {
            RenderUtil.endNoLiquidGlass();
            RenderUtil.scaleEnd();
        }
    }

    private boolean isAnySettingsOpen() {
        for (Dragging d : draggingManager.getRenderOrder()) {
            if (d == null) continue;
            if (d.settingsVisible) return true;
        }
        return false;
    }


    private boolean isTopmostUnderMouse(float mouseX, float mouseY) {
        Dragging candidate = null;
        for (Dragging d : draggingManager.getRenderOrder()) {
            if (d == null) continue;
            boolean dVisible = d.isVisible();
            if (!dVisible) continue;
            if (MathUtil.isHovered(mouseX, mouseY, d.x, d.y, d.width, d.height)) {
                candidate = d;
            }
        }
        return candidate == this;
    }
}
