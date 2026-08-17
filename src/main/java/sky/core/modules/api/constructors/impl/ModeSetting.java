package sky.core.modules.api.constructors.impl;

import sky.core.modules.api.constructors.Setting;

import java.util.function.Supplier;

public class ModeSetting extends Setting<String> {

    public String[] strings;
    public String defaultVal;

    public ModeSetting(String name, String defaultVal, String... strings) {
        super(name, defaultVal);
        this.defaultVal = defaultVal;
        this.strings = strings;
    }

    public ModeSetting(String name, String defaultVal, Supplier<Boolean> visible, String... strings) {
        super(name, defaultVal);
        this.defaultVal = defaultVal;
        this.strings = strings;
        visible(visible);
    }

    public int getIndex() {
        int index = 0;
        for (String val : strings) {
            if (val.equalsIgnoreCase(get())) {
                return index;
            }
            index++;
        }
        return 0;
    }

    public boolean is(String s) {
        return get().equalsIgnoreCase(s);
    }
}