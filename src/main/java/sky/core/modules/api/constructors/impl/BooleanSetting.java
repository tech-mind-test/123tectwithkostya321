package sky.core.modules.api.constructors.impl;


import sky.core.modules.Module;
import sky.core.modules.api.constructors.Setting;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

@Setter
@Getter
public class BooleanSetting extends Setting<Boolean> {
    public boolean defaultVal;

    private int bind = -100;

    private Module.ToggleMode toggleMode = Module.ToggleMode.TOGGLE;

    private boolean Keybindvisible = true;

    public BooleanSetting(String name, Boolean defaultVal) {
        super(name, defaultVal);
        this.defaultVal = defaultVal;
    }

    public BooleanSetting(String name, Boolean defaultVal, Supplier<Boolean> visible) {
        super(name, defaultVal);
        this.defaultVal = defaultVal;
        visible(visible);
    }
}