package sky.core.utils.component.impl;

import lombok.experimental.UtilityClass;
import sky.core.SkyCore;
import sky.core.utils.Wrapper;

@UtilityClass
public class SensUtil implements Wrapper {

    public float getSens(float rotation) {
        return getDeltaMouse(rotation) * getGCDValue();
    }


    public float getGCDValue() {
        boolean testas =
                SkyCore.getInstance().getModuleManager().getAttackAura().componentMode.is("Funtime");

        double multiplier = testas ? 0.015D : 0.15D;

        return (float) (getGCD() * multiplier);
    }
    public static float getGCD() {
        double mouseSensitivity = mc.gameSettings.mouseSensitivity;
        return (float) (Math.pow(mouseSensitivity * 0.6F + 0.2F, 3.0D) * 8F);
    }

    public float getDeltaMouse(float delta) {
        return Math.round(delta / getGCDValue());
    }

}