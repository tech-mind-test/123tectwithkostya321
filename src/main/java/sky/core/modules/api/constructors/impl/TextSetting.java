package sky.core.modules.api.constructors.impl;

import sky.core.modules.api.constructors.Setting;

import java.util.function.Supplier;

public class TextSetting extends Setting<String> {
    public TextSetting(String name) {
        super(name, "");
    }

    public TextSetting(String name, Supplier<Boolean> visible) {
        super(name, "");
        visible(visible);
    }
}
