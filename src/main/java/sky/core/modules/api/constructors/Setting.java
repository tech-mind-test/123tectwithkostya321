package sky.core.modules.api.constructors;

import lombok.Getter;
import lombok.Setter;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Setter
@Getter
public class Setting<Value> {
    Value defaultVal;
    String settingName;
    private Runnable onChange;
    public boolean visible = true;
    private Supplier<Boolean> visibleSupplier = null;

    public Setting(String name, Value defaultVal) {
        this.settingName = name;
        this.defaultVal = defaultVal;
    }

    public String getName() {
        return settingName;
    }

    public void set(Value value) {
        this.defaultVal = value;
        if (onChange != null) {
            onChange.run();
        }
    }

    public Value get() {
        return defaultVal;
    }

    public Setting<?> visible(Supplier<Boolean> bool) {
        this.visibleSupplier = bool;
        return this;
    }

    public Setting<?> visible(BooleanSupplier bool) {
        this.visibleSupplier = bool::getAsBoolean;
        return this;
    }

    public boolean isVisible() {
        return visibleSupplier != null ? Boolean.TRUE.equals(visibleSupplier.get()) : visible;
    }
}