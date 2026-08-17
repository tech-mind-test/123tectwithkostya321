package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventAspectRatio;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;

public class AspectRatio extends Module {
    public final ModeSetting mode = new ModeSetting("Соотношение", "Кастомное", "4:3", "16:9", "1:1", "16:10", "Кастомное");
    public final SliderSetting aspectRatio = new SliderSetting("Значение", 1.0f, 0.1f, 5, 0.1f, () -> mode.is("Кастомное"));

    public AspectRatio() {
        super("AspectRatio", "Изменяет соотношение сторон экрана", Category.Visuals);
        addSettings(mode, aspectRatio);
    }

    @EventTarget
    public void onEvent(EventAspectRatio event) {
        float value = switch (mode.get()) {
            case "4:3" -> 4.0f / 3.0f;
            case "16:9" -> 16.0f / 9.0f;
            case "1:1" -> 1.0f;
            case "16:10" -> 16.0f / 10.0f;
            default -> aspectRatio.get();
        };

        event.setAspectRatio(value);
    }
}