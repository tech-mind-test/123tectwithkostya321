package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventSwingAnimation;
import sky.core.events.EventSwingSpeed;
import sky.core.events.EventTransformSideFirstPerson;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;

public class HoldMyItems extends Module {
    public final SliderSetting swingSpeed = new SliderSetting("Скорость", 12, 1, 20, 1);
    public final SliderSetting power = new SliderSetting("Сила", 1.0F, 0.0F, 2.0F, 0.05F);
    public final SliderSetting xOffset = new SliderSetting("X", 0.0F, -1.5F, 1.5F, 0.01F);
    public final SliderSetting yOffset = new SliderSetting("Y", 0.0F, -1.5F, 1.5F, 0.01F);
    public final SliderSetting zOffset = new SliderSetting("Z", 0.0F, -1.5F, 1.5F, 0.01F);
    public final SliderSetting rotX = new SliderSetting("Поворот X", 0.0F, -180.0F, 180.0F, 1.0F);
    public final SliderSetting rotY = new SliderSetting("Поворот Y", 0.0F, -180.0F, 180.0F, 1.0F);
    public final SliderSetting rotZ = new SliderSetting("Поворот Z", 0.0F, -180.0F, 180.0F, 1.0F);
    public final SliderSetting scale = new SliderSetting("Размер", 1.0F, 0.2F, 2.5F, 0.01F);
    public final SliderSetting swingX = new SliderSetting("Анимация X", -80.0F, -180.0F, 180.0F, 1.0F);
    public final SliderSetting swingY = new SliderSetting("Анимация Y", -20.0F, -180.0F, 180.0F, 1.0F);
    public final SliderSetting swingZ = new SliderSetting("Анимация Z", -20.0F, -180.0F, 180.0F, 1.0F);

    public HoldMyItems() {
        super("HoldMyItems", "First-person animations", Category.Visuals);
        addSettings(
                swingSpeed, power,
                xOffset, yOffset, zOffset,
                rotX, rotY, rotZ,
                scale,
                swingX, swingY, swingZ
        );
    }

    @java.lang.Override
    public void onEnable() {
        super.onEnable();
        syncToMod();
        mods.holdmyitems.HoldMyItems.enabled = true;
    }

    @java.lang.Override
    public void onDisable() {
        mods.holdmyitems.HoldMyItems.enabled = false;
        super.onDisable();
    }

    private void syncToMod() {
        mods.holdmyitems.HoldMyItems.swingSpeed = swingSpeed.get().intValue();
        mods.holdmyitems.HoldMyItems.power = power.get();
        mods.holdmyitems.HoldMyItems.xOffset = xOffset.get();
        mods.holdmyitems.HoldMyItems.yOffset = yOffset.get();
        mods.holdmyitems.HoldMyItems.zOffset = zOffset.get();
        mods.holdmyitems.HoldMyItems.rotX = rotX.get();
        mods.holdmyitems.HoldMyItems.rotY = rotY.get();
        mods.holdmyitems.HoldMyItems.rotZ = rotZ.get();
        mods.holdmyitems.HoldMyItems.itemScale = scale.get();
        mods.holdmyitems.HoldMyItems.swingX = swingX.get();
        mods.holdmyitems.HoldMyItems.swingY = swingY.get();
        mods.holdmyitems.HoldMyItems.swingZ = swingZ.get();
    }

    @EventTarget
    public void onEvent(EventSwingSpeed event) {
        syncToMod();
        mods.holdmyitems.HoldMyItems.onSwingSpeed(event);
    }

    @EventTarget
    public void onEvent(EventTransformSideFirstPerson event) {
        syncToMod();
        mods.holdmyitems.HoldMyItems.onTransform(event);
    }

    @EventTarget
    public void onEvent(EventSwingAnimation event) {
        syncToMod();
        mods.holdmyitems.HoldMyItems.onSwingAnimation(event);
    }
}

