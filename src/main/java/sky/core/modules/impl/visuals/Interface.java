package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import lombok.Getter;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ButtonSetting;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.ui.Interface.elements.impl.*;
import net.minecraft.client.gui.screen.ChatScreen;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;

public class Interface extends Module {

    public static ButtonSetting themeeditor = new ButtonSetting("Темы", false, "Открыть темы", "Закрыть темы");
    public static ModeSetting hudStyle = new ModeSetting("Свап HUD", "Old", "Old", "New");
    public static ModeSetting newHudMode = new ModeSetting("Режим худа", "Обычный", Interface::isNewHud, "Обычный", "Блюр", "LiquidGlass");
    public static ModeSetting roundingMode = new ModeSetting("Скругление", "Обычное", "Обычное", "iOS");

    public static boolean isNewHud() {
        return hudStyle.is("New");
    }

    public static boolean isOldHud() {
        return hudStyle.is("Old");
    }

    public static boolean isIosRounding() {
        return roundingMode.is("iOS");
    }

    public static boolean isNewHudBlur() {
        return isNewHud() && newHudMode.is("Блюр");
    }
    public static MultiBooleanSetting elements = new MultiBooleanSetting("Выбор",
            new BooleanSetting("Броня", true),
            new BooleanSetting("Счетчик Тотемов", true),
            new BooleanSetting("Информация о цели", true),
            new BooleanSetting("Логотип", true),
            new BooleanSetting("Информация", true),
            //new BooleanSetting("Музыка", true),
            new BooleanSetting("Администрация онлайн", true),
            new BooleanSetting("Уведомления", true),
            new BooleanSetting("Зелья", true),
            new BooleanSetting("Горячие клавиши", true),
            new BooleanSetting("Хотбар", true),
            new BooleanSetting("Arraylist", true)
    );
    @Getter
    public static ColorSetting colorSetting = new ColorSetting("Цвет", true, ColorUtil.hex("#8A98FFFF"));

    TargetHudRender targetHudRender;
    KeybindsRender keybindsRender;
    WatermarkRender watermarkRender;
    ArmorRender armorRender;
    TotemCounterRender totemCounterRender;
    NotificationRender notificationRender;
    PotionsRender potionsRender;
    StaffListRender staffListRender;
    InformationRender informationRender;
    //MusicRender musicRender;
    ArrayListRender arrayListRender;

    private Dragging targetHudDrag;
    private Dragging keybindsDrag;
    private Dragging potionsDrag;
    private Dragging staffDrag;
    private Dragging watermarkDrag;
    private Dragging informationDrag;
    private Dragging musicDrag;
    private Dragging notificationDrag;
    private Dragging arrayListDrag;

    private static boolean isElementEnabled(String name) {
        Boolean value = elements.is(name);
        return value == null || value;
    }

    @EventTarget
    private void onRender(EventRender2D.Post e) {
        if (!isEnabled() || mc.gameSettings.showDebugInfo) {
            return;
        }

        RenderUtil.beginInterfaceRounding();
        try {
        if (isEnabled() && mc.currentScreen instanceof ChatScreen && !mc.gameSettings.showDebugInfo) {
            double windowW = mc.getMainWindow().getWidth();
            double windowH = mc.getMainWindow().getHeight();
            float mouseX = (float) (mc.mouseHelper.getMouseX() * mc.getMainWindow().getScaledWidth() / windowW);
            float mouseY = (float) (mc.mouseHelper.getMouseY() * mc.getMainWindow().getScaledHeight() / windowH);
            Dragging.prepareHudDragFrame(mouseX, mouseY);
        } else if (isEnabled()) {
            Dragging.tickAllSmoothing();
        }

        for (Dragging draggable : SkyCore.getInstance().getDraggingManager().getRenderOrder()) {
            String name = draggable.getName();
            if (isElementEnabled(name)) {
                draggable.pushRenderTransform();
                try {
                    renderHudElement(() -> {
                switch (name) {
                    case "Информация о цели":
                        targetHudRender.render(e);
                        break;
                    case "Горячие клавиши":
                        keybindsRender.render(e);
                        break;
                    case "Зелья":
                        potionsRender.render(e);
                        break;
                    case "Администрация онлайн":
                        staffListRender.render(e);
                        break;
                    case "Музыка":
                        //musicRender.render(e);
                        break;
                }
                    });
                } finally {
                    draggable.popRenderTransform();
                }
            }
        }

        if (isElementEnabled("Arraylist")) {
            renderWithDragTransform(arrayListDrag, () -> arrayListRender.render(e));
        }

        if (isElementEnabled("Счетчик Тотемов")) {
            renderHudElement(() -> totemCounterRender.render(e));
        }

        if (isElementEnabled("Броня")) {
            renderHudElement(() -> armorRender.render(e));
        }
        if (isElementEnabled("Логотип")) {
            renderWithDragTransform(watermarkDrag, () -> watermarkRender.render(e));
        }
        if (isElementEnabled("Информация")) {
            renderWithDragTransform(informationDrag, () -> informationRender.render(e));
        }
        if (isElementEnabled("Уведомления")) {
            renderWithDragTransform(notificationDrag, () -> notificationRender.render(e));
        }
        } finally {
            RenderUtil.endInterfaceRounding();
        }
    }

    private static void renderWithDragTransform(Dragging drag, Runnable render) {
        if (drag == null) {
            renderHudElement(render);
            return;
        }
        drag.pushRenderTransform();
        try {
            renderHudElement(render);
        } finally {
            drag.popRenderTransform();
        }
    }

    private static void renderHudElement(Runnable render) {
        RenderUtil.beginHudLiquidGlass();
        try {
            render.run();
        } finally {
            RenderUtil.endHudLiquidGlass();
        }
    }

    public Interface() {
        super("Interface", "Визуальный интерфейс клиента", Category.Visuals);

        targetHudDrag = new Dragging("Информация о цели", 4, 100, elements);
        keybindsDrag = new Dragging("Горячие клавиши", 297, 100, elements);
        potionsDrag = new Dragging("Зелья", 357, 100, elements);
        staffDrag = new Dragging("Администрация онлайн", 447, 100, elements);
        watermarkDrag = new Dragging("Логотип", 4, 4, elements, true);
        informationDrag = new Dragging("Информация", 4, 26, elements, true);
        musicDrag = new Dragging("Музыка", 4, 50, elements, true);
        notificationDrag = new Dragging("Уведомления", mc.getMainWindow().getScaledWidth() / 2f, mc.getMainWindow().getScaledHeight() / 2f + 21f, elements, false);
        arrayListDrag = new Dragging("Arraylist", 4f, 8f, elements, true);

        targetHudRender = new TargetHudRender(targetHudDrag);
        keybindsRender = new KeybindsRender(keybindsDrag);
        armorRender = new ArmorRender();
        totemCounterRender = new TotemCounterRender();
        watermarkRender = new WatermarkRender(watermarkDrag);
        informationRender = new InformationRender(informationDrag);
       //musicRender = new MusicRender(musicDrag);
        notificationRender = new NotificationRender(notificationDrag);
        potionsRender = new PotionsRender(potionsDrag);
        staffListRender = new StaffListRender(staffDrag);
        arrayListRender = new ArrayListRender(arrayListDrag);

        keybindsDrag.addSettings(KeybindsRender.alphabg);
        staffDrag.addSettings(StaffListRender.skins, StaffListRender.alphabg);
        potionsDrag.addSettings(PotionsRender.badeffects, PotionsRender.alphabg);
        notificationDrag.addSettings(NotificationRender.alphabg, NotificationRender.shield, NotificationRender.spec, NotificationRender.warps, NotificationRender.module, NotificationRender.lowstrength, NotificationRender.effects);
        targetHudDrag.addSettings(TargetHudRender.hpbar, TargetHudRender.goldhealth, TargetHudRender.alphabg, TargetHudRender.particles2, TargetHudRender.ontarget, TargetHudRender.armor);
        watermarkDrag.addSettings(WatermarkRender.alphabg);
        informationDrag.addSettings(InformationRender.fps, InformationRender.bps, InformationRender.coords);
        arrayListDrag.addSettings(
                ArrayListRender.showCombat,
                ArrayListRender.showMovement,
                ArrayListRender.showVisuals,
                ArrayListRender.showPlayer,
                ArrayListRender.showMisc
        );

        addSettings(elements, themeeditor, hudStyle, newHudMode, roundingMode);
    }
}
