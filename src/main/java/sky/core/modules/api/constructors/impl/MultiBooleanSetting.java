package sky.core.modules.api.constructors.impl;

import sky.core.modules.api.constructors.Setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class MultiBooleanSetting extends Setting<List<BooleanSetting>> {

    public MultiBooleanSetting(String name, BooleanSetting... settings) {
        super(name, Arrays.asList(settings));
        for (BooleanSetting setting : get()) {
            setting.defaultVal = setting.get();
        }
    }

    public MultiBooleanSetting(String name, Supplier<Boolean> visible, BooleanSetting... settings) {
        super(name, Arrays.asList(settings));
        for (BooleanSetting setting : get()) {
            setting.defaultVal = setting.get();
        }
        visible(visible);
    }

    public Boolean is(String settingName) {
        return get().stream().filter(s -> s.getName().equalsIgnoreCase(settingName)).findFirst().map(Setting::get).orElse(null);
    }

    public BooleanSetting getIndex(int index) {
        List<BooleanSetting> settings = get();
        if (index >= 0 && index < settings.size()) {
            return settings.get(index);
        }
        throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for size " + settings.size());
    }
}