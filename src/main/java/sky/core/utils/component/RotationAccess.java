package sky.core.utils.component;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.utils.Wrapper;

public class RotationAccess implements Wrapper {

    private final String name;
    public static LivingEntity target;

    public RotationAccess(String name) {
        this.name = name;
    }

    public static Vector2f calculateDelta(Vector2f current, Vector2f target) {
        float yawDelta = MathHelper.wrapDegrees(target.x - current.x);
        float pitchDelta = MathHelper.wrapDegrees(target.y - current.y);
        return new Vector2f(yawDelta, pitchDelta);
    }

    public static boolean canSeen(Vector3d pos) {
        return pos.y < 400 && pos.y > -64;
    }
    public static void updateTarget(LivingEntity entity) {
        target = entity;
    }
}