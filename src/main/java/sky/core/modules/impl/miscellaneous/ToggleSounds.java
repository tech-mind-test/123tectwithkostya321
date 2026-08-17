package sky.core.modules.impl.miscellaneous;

import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;

public class ToggleSounds extends Module {
    public static ModeSetting type = new ModeSetting("Тип звука", "Тип 1", "1", "2","3","4");

    public static SliderSetting volume = new SliderSetting("Громкость", 75, 0, 100, 1);

    public ToggleSounds() {
        super("ClientSounds", "ЗВУКИ!!!!!!!!!!!!!!!!!!!", Category.Miscellaneous);
        addSettings(type, volume);
    }

    public static String getSoundFile(boolean isEnable) {
        String mode = type.get();
        return switch (mode) {
            case "1" -> isEnable ? "enabled0" : "disabled0";
            case "2" -> isEnable ? "enabled3" : "disabled3";
            case "3" -> isEnable ? "MODULE_ON3" : "MODULE_OFF3";
            case "4" -> isEnable ? "enabled2" : "disabled2";
            default -> null;
        };
    }
}