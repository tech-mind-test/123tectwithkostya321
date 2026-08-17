package sky.core.utils.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class GhostRenderer3D {
    private Vector3d position;
    private Vector3d motion;
    private float alpha = 1.0f;
    private int color;
    private final float size;
    private final List<Vector4f> tail = new ArrayList<>();

    public GhostRenderer3D(Vector3d position, Vector3d motion, float size, int color) {
        this.position = position;
        this.motion = motion;
        this.size = size;
        this.color = color;
    }

    public void render(MatrixStack ms, ResourceLocation texture) {
        this.position = this.position.add(motion);

        final float tailLifetime = 55.0f;
        this.tail.add(new Vector4f((float) position.x, (float) position.y + 0.4f, (float) position.z, tailLifetime));

        Vector3d camera = Minecraft.getInstance().getRenderManager().info.getProjectedView();
        List<Vector4f> expired = new ArrayList<>();

        for (Vector4f point : tail) {
            if (point.getW() <= 0.0f) {
                expired.add(point);
                continue;
            }

            float miniSize = size * point.getW() / tailLifetime;
            float alphaMul = (point.getW() / tailLifetime) * alpha;
            int pointAlpha = Math.max(0, Math.min(255, (int) (alphaMul * 255.0f)));
            int tinted = ColorUtil.getColor(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), pointAlpha);

            ms.push();
            ms.translate(point.getX() - camera.x, point.getY() - camera.y, point.getZ() - camera.z);
            ms.rotate(Minecraft.getInstance().getRenderManager().info.getRotation());
            RenderUtil.drawTexture(ms, texture, -miniSize / 2.0f, -miniSize / 2.0f, miniSize, miniSize, tinted);
            ms.pop();

            point.set(point.getX(), point.getY() + (0.010f / Math.max((float) Minecraft.getDebugFPS(), 5.0f) * 300.0f), point.getZ(), point.getW() - (0.3f / Math.max((float) Minecraft.getDebugFPS(), 10.0f) * 450.0f));
            if (point.getW() <= 0.0f) {
                expired.add(point);
            }
        }

        tail.removeAll(expired);
    }

    public void tick() {
        position = position.add(motion);
    }

    public Vector3d getPosition() {
        return position;
    }

    public void setPosition(Vector3d position) {
        this.position = position;
    }

    public Vector3d getMotion() {
        return motion;
    }

    public void setMotion(Vector3d motion) {
        this.motion = motion;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
