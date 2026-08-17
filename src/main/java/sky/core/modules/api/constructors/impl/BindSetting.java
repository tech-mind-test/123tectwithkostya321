package sky.core.modules.api.constructors.impl;

import sky.core.modules.api.constructors.Setting;

import java.util.function.Supplier;

public class BindSetting extends Setting<Integer> {
    public BindSetting(String name) {
        super(name, -1);
    }

    public BindSetting(String name, Supplier<Boolean> visible) {
        super(name, -1);
        visible(visible);
    }
}