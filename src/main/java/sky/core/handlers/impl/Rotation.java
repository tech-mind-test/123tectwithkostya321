package sky.core.handlers.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import org.joml.Vector2f;
import sky.core.utils.Wrapper;

import static sky.core.utils.player.RayTraceUtil.getVectorForRotation;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rotation implements Wrapper {
    private float yaw, pitch;

    public Rotation(Entity entity) {
        yaw = entity.rotationYaw;
        pitch = entity.rotationPitch;
    }

    public float getDelta(Rotation target) {
        float yawDelta = MathHelper.wrapDegrees(target.getYaw() - this.yaw);
        float pitchDelta = target.getPitch() - this.pitch;
        return (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
    }

    public static Vector2f camera() {
        return new Vector2f(cameraYaw(), cameraPitch());
    }

    public static float cameraYaw() {
        return MathHelper.wrapDegrees(mc.gameRenderer.getActiveRenderInfo().getYaw() + (mc.gameRenderer.getActiveRenderInfo().isThirdPersonReverse() ? 180 : 0));
    }
    public static RayTraceResult rayTrace(double rayTraceDistance, float yaw, float pitch, Entity entity, RayTraceContext.BlockMode mode) {
        Vector3d startVec = mc.player.getEyePosition(1.0F);
        Vector3d directionVec = getVectorForRotation(pitch, yaw);
        Vector3d endVec = startVec.add(directionVec.x * rayTraceDistance, directionVec.y * rayTraceDistance, directionVec.z * rayTraceDistance);

        return mc.world.rayTraceBlocks(new RayTraceContext(startVec, endVec, mode, RayTraceContext.FluidMode.NONE, entity));
    }
    public static Rotation getReported() {
        return new Rotation(mc.player.getLastReportedYaw(), mc.player.getLastReportedPitch());
    }
    public static float cameraPitch() {
        return (mc.gameRenderer.getActiveRenderInfo().isThirdPersonReverse() ? -1 : 1) * mc.gameRenderer.getActiveRenderInfo().getPitch();
    }
}
