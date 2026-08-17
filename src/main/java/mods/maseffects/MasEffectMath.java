package mods.maseffects;

import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;

public final class MasEffectMath {
    private MasEffectMath() {
    }

    public static Quaternion euler(float x, float y, float z) {
        Quaternion qx = new Quaternion(new Vector3f(1f, 0f, 0f), x, true);
        Quaternion qy = new Quaternion(new Vector3f(0f, 1f, 0f), y, true);
        Quaternion qz = new Quaternion(new Vector3f(0f, 0f, 1f), z, true);
        Quaternion result = new Quaternion(qx);
        result.multiply(qy);
        result.multiply(qz);
        return result;
    }
}
