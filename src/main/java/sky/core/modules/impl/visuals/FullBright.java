package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;

public class FullBright extends Module {
    private static FullBright instance;

    public ModeSetting mode = new ModeSetting("Режим", "Гамма", "Гамма", "Зелье");
    public BooleanSetting adaptiveBrightness = new BooleanSetting("Адаптивная яркость", false, () -> mode.is("Гамма"));
    public SliderSetting gammaValue = new SliderSetting("Яркость", 8.0f, 0.5f, 20.0f, 0.1f, () -> !adaptiveBrightness.get() && mode.is("Гамма"));

    private double savedGamma;
    private float renderGamma;

    public FullBright() {
        super("Full Bright", "Освещает темные места в мире", Category.Visuals);
        addSettings(mode, adaptiveBrightness, gammaValue);
        instance = this;
    }

    public static float getGammaForRendering(float vanillaGamma) {
        if (instance == null || !instance.isEnabled() || !instance.mode.is("Гамма")) {
            return vanillaGamma;
        }
        return instance.renderGamma;
    }

    @Override
    public void onEnable() {
        if (mc.gameSettings != null) {
            savedGamma = mc.gameSettings.gamma;
            renderGamma = (float) savedGamma;
        }
        super.onEnable();
    }

    @EventTarget
    public void onEvent(EventUpdate event) {
        if (mode.is("Гамма")) {
            if (adaptiveBrightness.get()) {
                BlockPos p = mc.player.getPosition();
                int lvl = Math.max(mc.world.getLightFor(LightType.BLOCK, p), mc.world.getLightFor(LightType.SKY, p));
                double t = 1.2 + Math.pow(1.0 - (lvl / 15.0), 2.0) * (12.0 - 1.2);
                float target = MathHelper.clamp((float) t, 1.2f, 12.0f);
                renderGamma += (target - renderGamma) * 0.3f;
            } else {
                renderGamma = gammaValue.get() / 5.0f;
            }
        }

        if (this.mode.is("Зелье")) {
            mc.player.addPotionEffect(new EffectInstance(Effects.NIGHT_VISION, 1337, 1));
        } else {
            mc.player.removePotionEffect(Effects.NIGHT_VISION);
        }
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.gamma = savedGamma;
        }
        renderGamma = 0.0f;
        mc.player.removeActivePotionEffect((new EffectInstance(Effects.NIGHT_VISION)).getPotion());
        super.onDisable();
    }
}
