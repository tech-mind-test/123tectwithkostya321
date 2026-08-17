package sky.core.modules.api.constructors.impl;

import com.adl.nativeprotect.Native;
import sky.core.modules.api.constructors.Setting;

import java.util.function.Supplier;

public class ClickSetting extends Setting<Void> {
    private final Runnable action;

    public ClickSetting(String name, Runnable action) {
        super(name, null);
        this.action = action;
    }

    public ClickSetting(String name, Runnable action, Supplier<Boolean> visible) {
        super(name, null);
        this.action = action;
        visible(visible);
    }

    @Native
    public void performAction() {
        if (action != null) {
            action.run();
        }
    }
}