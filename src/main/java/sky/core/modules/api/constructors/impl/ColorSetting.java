package sky.core.modules.api.constructors.impl;

import sky.core.modules.api.constructors.Setting;

import java.util.function.Supplier;

public class ColorSetting extends Setting<Integer> {
    public int defaultVal;
    public boolean alphaBar;

    public ColorSetting(String name, boolean alphaBar, Integer defaultVal) {
        super(name, defaultVal);
        this.defaultVal = defaultVal;
        this.alphaBar = alphaBar;
    }

    public ColorSetting(String name, boolean alphaBar, Integer defaultVal, Supplier<Boolean> visible) {
        super(name, defaultVal);
        this.defaultVal = defaultVal;
        this.alphaBar = alphaBar;
        visible(visible);
    }

    public float getAlpha() {
        return (get() >> 24) & 0xFF;
    }
}