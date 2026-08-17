package sky.core.utils.managers.impl.notificationmanager;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.notificationmanager.impl.FontNotification;
import sky.core.utils.managers.impl.notificationmanager.impl.ImageNotification;
import sky.core.utils.managers.impl.notificationmanager.impl.StackNotification;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.Interface.elements.impl.NotificationRender;
import sky.core.utils.Wrapper;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.LinkedList;
import java.util.List;

public class NotificationManager implements Wrapper {
    private static final List<AbstractNotification> notifications = new LinkedList<>();

    private static void addNotification(AbstractNotification notification) {
        notification.getYAnimation().setValue(mw.getScaledHeight() / 2f + 21);
        notification.getYAnimation().update(13.5f);

        notifications.add(0, notification);

        if (notifications.size() > 10) {
            for (int i = 10; i < notifications.size(); i++) {
                notifications.get(i).expireNow();
            }
        }
    }

    public static boolean hasActiveNotifications() {
        return !notifications.isEmpty();
    }

    public static void addNotification(ResourceLocation image, ITextComponent message) {
        addNotification(new ImageNotification(image, message));
    }

    public static void addNotification(ResourceLocation image, ITextComponent message, int color) {
        addNotification(new ImageNotification(image, message, color));
    }

    public static void addNotification(ItemStack stack, ITextComponent message) {
        addNotification(new StackNotification(stack, message));
    }

    public static void addNotification(ItemStack stack, ITextComponent message, boolean autoSwapStyle) {
        addNotification(new StackNotification(stack, message, autoSwapStyle));
    }

    public static void addNotification(String categoryIcon, String message, int color) {
        addNotification(new FontNotification(categoryIcon, message, color));
    }

    @EventTarget
    public void render(EventRender2D.Pre event) {
        boolean ordinaryMode = Interface.isNewHud();
        float startY = mw.getScaledHeight() / 2f + (ordinaryMode ? 15f : 21f);

        notifications.removeIf(n -> n.isExpired() && n.getAlphaAnimation().isDone());

        for (AbstractNotification notification : notifications) {
            float width;
            if (ordinaryMode) {
                if (notification instanceof StackNotification || notification instanceof ImageNotification) {
                    float textWidth = Fonts.sfregular[12].getWidth(notification.getMessage().getString());
                    if (notification instanceof StackNotification stackNotification && stackNotification.isAutoSwapStyle()) {
                        width = 6f + 10f + 4f + textWidth + 6f;
                    } else {
                        width = 8f + 10f + 4f + textWidth + 8f;
                    }
                } else if (notification instanceof FontNotification fontNotification) {
                    String displayMessage = fontNotification.getMessage().getString();
                    String prefix = "";
                    String moduleName = displayMessage;
                    String status = "";

                    if (displayMessage.startsWith("Module ")) {
                        prefix = "Module ";
                        moduleName = displayMessage.substring(7);
                    }

                    int enabledIdx = moduleName.indexOf(" enabled");
                    int disabledIdx = moduleName.indexOf(" disabled");
                    if (enabledIdx != -1) {
                        status = moduleName.substring(enabledIdx);
                        moduleName = moduleName.substring(0, enabledIdx);
                    } else if (disabledIdx != -1) {
                        status = moduleName.substring(disabledIdx);
                        moduleName = moduleName.substring(0, disabledIdx);
                    }

                    float textWidth = Fonts.sfregular[12].getWidth(prefix)
                            + Fonts.sfregular[12].getWidth(moduleName)
                            + Fonts.sfregular[12].getWidth(status);
                    width = 8f + 7f + 4f + textWidth + 8f;
                } else {
                    width = Fonts.sfregular[12].getWidth(notification.getMessage().getString()) + 24f;
                }
            } else if (notification instanceof ImageNotification || notification instanceof StackNotification) {
                width = Fonts.sf_medium[12].getWidth(notification.getMessage().getString()) + 24;
            } else {
                FontNotification fontNotification = (FontNotification) notification;
                String categoryIcon = fontNotification.getCategoryIcon();
                float iconWidth = (categoryIcon != null && !categoryIcon.isEmpty()) ? Fonts.skycore[12].getWidth(categoryIcon) : 7f;
                width = iconWidth + 4f + Fonts.sfregular[12].getWidth(notification.getMessage().getString()) + 16f;
            }

            float x = (mw.getScaledWidth() - width) / 2f;
            if (SkyCore.getInstance().getModuleManager().getModule(Interface.class).isEnabled()
                    && Interface.elements.is("Уведомления")) {
                RenderUtil.beginInterfaceRounding();
                RenderUtil.beginHudLiquidGlass();
                try {
                    notification.render(x, startY, event.getStack());
                } finally {
                    RenderUtil.endHudLiquidGlass();
                    RenderUtil.endInterfaceRounding();
                }
            }
            startY += ordinaryMode ? 21f : 13.5f;
        }
    }
}
