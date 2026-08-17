package sky.core.modules.api.constructors.impl;

import sky.core.modules.api.constructors.Setting;

public class StringSetting extends Setting<String> {

    public StringSetting(String name) {
        super(name, "");
    }

    public void setValue(String value) {
        set(value);
    }

    public String getValue() {
        return get();
    }
}
