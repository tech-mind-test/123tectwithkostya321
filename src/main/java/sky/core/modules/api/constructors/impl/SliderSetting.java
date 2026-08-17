package sky.core.modules.api.constructors.impl;

import sky.core.modules.api.constructors.Setting;
import sky.core.utils.math.MathUtil;
import net.minecraft.util.math.MathHelper;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class SliderSetting extends Setting<Float> {
    public float min;
    public float max;
    public float increment;
    public float defaultVal;
    private final Supplier<Float> maxSupplier;
    private Supplier<Float> minSupplier;

    private SliderSetting(String name, float defaultVal, float min, float max, float increment, BooleanSupplier visible, Supplier<Float> minSupplier, Supplier<Float> maxSupplier) {
        super(name, defaultVal);
        this.min = min;
        this.max = max;
        this.defaultVal = defaultVal;
        this.increment = increment;
        if (visible != null) visible(visible);
        this.minSupplier = minSupplier;
        this.maxSupplier = maxSupplier;
    }

    public SliderSetting(String name, float defaultVal, float min, float max, float increment) {
        this(name, defaultVal, min, max, increment, null, null, null);
    }

    public SliderSetting(String name, float defaultVal, float min, float max, float increment, BooleanSupplier visible) {
        this(name, defaultVal, min, max, increment, visible, null, null);
    }

    public SliderSetting(String name, float defaultVal, float min, float max, float increment, Supplier<Float> maxSupplier) {
        this(name, defaultVal, min, max, increment, null, null, maxSupplier);
    }

    public SliderSetting(String name, float defaultVal, float min, float max, float increment, BooleanSupplier visible, Supplier<Float> maxSupplier) {
        this(name, defaultVal, min, max, increment, visible, null, maxSupplier);
    }

    public SliderSetting(String name, float defaultVal, float min, float max, float increment, SliderSetting maxValue) {
        this(name, defaultVal, min, max, increment, null, null, maxValue::get);
        maxValue.min(this);
    }

    public SliderSetting(String name, float defaultVal, float min, float max, float increment, BooleanSupplier visible, SliderSetting maxValue) {
        this(name, defaultVal, min, max, increment, visible, null, maxValue::get);
        maxValue.min(this);
    }

    @Override
    public void set(Float value) {
        float effMin = (minSupplier != null) ? minSupplier.get() : this.min;
        float effMax = (maxSupplier != null) ? maxSupplier.get() : this.max;
        float rounded = MathUtil.round(value, this.increment);
        float clamped = MathHelper.clamp(rounded, effMin, effMax);
        if (Math.abs(this.get() - clamped) < 1e-6f) return;

        super.set(clamped);
    }

    public SliderSetting min(SliderSetting minValue) {
        this.minSupplier = minValue::get;
        return this;
    }
}