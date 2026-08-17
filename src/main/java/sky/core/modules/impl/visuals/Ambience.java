package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import net.minecraft.network.play.server.SUpdateTimePacket;

import java.time.LocalTime;

public class Ambience extends Module {
    public static ModeSetting skyMode = new ModeSetting("Небо", "Обычная", "Обычная", "Плазма", "Лето");
    public static ModeSetting summerMode = new ModeSetting("Режим лета", "Дневной", () -> skyMode.is("Лето"), "Дневной", "Ночной");
    public static ModeSetting timeMode = new ModeSetting("Время", "Не менять", () -> !skyMode.is("Лето"), "Рассвет", "Утро", "День", "Вечер", "Заход солнца", "Ночь", "Время из реальной жизни", "Не менять");
    public static ModeSetting fogMode = new ModeSetting("Туман", "Ничего не делать", "Ничего не делать", "Очистить", "Переопределить");
    public static ColorSetting fogColor = new ColorSetting("Цвет тумана", false, -1, () -> fogMode.is("Переопределить"));
    public static SliderSetting fogEnd = new SliderSetting("Конец тумана", 1.0f, 0.1f, 1.5f, 0.1f, () -> fogMode.is("Переопределить"));
    public static SliderSetting fogStart = new SliderSetting("Начало тумана", 0.5f, 0.1f, 1.5f, 0.1f, () -> fogMode.is("Переопределить"), fogEnd);
    public static SliderSetting skySpeed = new SliderSetting("Скорость шейдера", 1.2f, 0.1f, 5.0f, 0.1f, () -> skyMode.is("Плазма"));
    public static SliderSetting skyScale = new SliderSetting("Масштаб шейдера", 1.0f, 0.2f, 4.0f, 0.1f, () -> skyMode.is("Плазма") || skyMode.is("Лето"));
    public static SliderSetting skyOutline = new SliderSetting("Контур шейдера", 0.1f, 0.01f, 1.0f, 0.01f, () -> skyMode.is("Плазма"));
    public static SliderSetting skyGlow = new SliderSetting("Глоу шейдера", 10.0f, 0.0f, 30.0f, 0.5f, () -> skyMode.is("Плазма"));
    public static SliderSetting skyFill = new SliderSetting("Заливка шейдера", 0.6f, 0.0f, 2.0f, 0.05f, () -> skyMode.is("Плазма"));
    public static SliderSetting skyAlpha = new SliderSetting("Альфа шейдера", 1.0f, 0.0f, 2.0f, 0.05f, () -> skyMode.is("Плазма"));

    public Ambience() {
        super("Ambience", "Позволяет изменить атмосферу игры", Category.Visuals);
        addSettings(timeMode, fogMode, fogColor, fogStart, fogEnd, skyMode, summerMode, skySpeed, skyScale, skyOutline, skyGlow, skyFill, skyAlpha);
    }

    @EventTarget
    public void onUpdate(EventPacket event) {
        if (event.getPacket() instanceof SUpdateTimePacket && (!timeMode.is("Не менять") || skyMode.is("Лето"))) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (skyMode.is("Лето")) {
            mc.world.setDayTime(18000L);
            return;
        }
        if (!timeMode.get().equals("Не менять")) {
            long time;
            if (timeMode.get().equals("Время из реальной жизни")) {
                time = getRealWorldTime();
            } else {
                time = switch (timeMode.get()) {
                    case "Рассвет" -> 23000L;
                    case "Утро" -> 1000L;
                    case "День" -> 6000L;
                    case "Вечер" -> 12000L;
                    case "Заход солнца" -> 13000L;
                    case "Ночь" -> 18000L;
                    default -> mc.world.getDayTime();
                };
            }
            mc.world.setDayTime(time);
        }
    }

    private long getRealWorldTime() {
        LocalTime now = LocalTime.now();
        int hours = now.getHour();
        int minutes = now.getMinute();
        int seconds = now.getSecond();

        int totalSeconds = hours * 3600 + minutes * 60 + seconds;
        int offsetSeconds = (totalSeconds - 6 * 3600) % (24 * 3600);
        if (offsetSeconds < 0) offsetSeconds += 24 * 3600;

        return (long) ((offsetSeconds / 86400.0) * 24000);
    }
}