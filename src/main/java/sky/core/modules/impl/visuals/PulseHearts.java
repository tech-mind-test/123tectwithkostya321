package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import sky.core.SkyCore;
import sky.core.events.EventNoRender;
import sky.core.events.EventRender2D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.Wrapper;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;

public class PulseHearts extends Module implements Wrapper {

    //chlen 12313221
    private final SliderSetting hpThreshold = new SliderSetting("Порог ХП", 20.0f, 1.0f, 40.0f, 1.0f);

    private final AnimationUtil heartAnimation = new AnimationUtil(1.0f, 8.0f, Easings.SINE_IN_OUT);
    private long lastAnimationTime = 0;
    private boolean fadingIn = true;
    private float currentAlpha = 1.0f;

    public PulseHearts() {
        super("PulseHearts", "Делает сердца в хотбаре моргающими при низком ХП", Category.Visuals);
        addSettings(hpThreshold);
    }

    @EventTarget
    public void onRender2D(EventRender2D.Post e) {
        if (mc.player == null || mc.gameSettings.showDebugInfo) {
            return;
        }
        PlayerEntity player = mc.player;
        float currentHealth = player.getHealth();
        float threshold = hpThreshold.get();

        if (currentHealth <= threshold) {
            float healthRatio = currentHealth / threshold;
            float blinkSpeed = 200.0f + (1000.0f - 200.0f) * healthRatio;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAnimationTime >= blinkSpeed) {
                fadingIn = !fadingIn;
                lastAnimationTime = currentTime;
            }

            float targetAlpha = fadingIn ? 1.0f : 0.3f;
            heartAnimation.update(targetAlpha);
            currentAlpha = heartAnimation.getValue();
        } else {
            currentAlpha = 1.0f;
        }
    }

    public static float getHeartAlpha() {
        try {
            Module module = SkyCore.getInstance().getModuleManager().getModule(PulseHearts.class);
            if (module instanceof PulseHearts && module.isEnabled()) {
                return ((PulseHearts) module).currentAlpha;
            }
        } catch (Exception ignored) {
        }
        return 1.0f;
    }
}
