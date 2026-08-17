package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.EventTarget;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;

public class TapeMouse extends Module {

    public static ModeSetting modeClick = new ModeSetting("Клавиша", "Левая", "Левая", "Правая");
    public static SliderSetting delayForClick = new SliderSetting("Задержка", 1, 1, 30, 1);

    private int tickCounter = 0;

    public TapeMouse() {
        super("TapeMouse", "Автоматически воспроизводит клик мыши", Category.Miscellaneous);
        addSettings(modeClick, delayForClick);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        tickCounter++;

        if ((float) tickCounter >= delayForClick.get()) {
            if (modeClick.is("Левая")) {
                mc.clickMouse();
            } else if (modeClick.is("Правая")) {
                mc.rightClickMouse();
            }
            tickCounter = 0;
        }
    }
}
