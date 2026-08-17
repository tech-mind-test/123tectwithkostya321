package sky.core.modules.api.constructors.impl;

import sky.core.modules.api.constructors.Setting;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class ButtonSetting extends Setting<Boolean> {

    public boolean defaultVal;
    public final String textOn;
    public final String textOff;

    public ButtonSetting(String name, Boolean defaultVal, String textOn, String textOff) {
        super(name, defaultVal);
        this.defaultVal = defaultVal;
        this.textOff = textOff;
        this.textOn = textOn;
    }

    public ButtonSetting(String name, Boolean defaultVal, String textOn, String textOff, Supplier<Boolean> visible) {
        super(name, defaultVal);
        this.defaultVal = defaultVal;
        this.textOff = textOff;
        this.textOn = textOn;
        visible(visible);
    }
}