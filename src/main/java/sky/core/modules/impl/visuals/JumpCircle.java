package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventJump;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

import java.util.concurrent.CopyOnWriteArrayList;

public class JumpCircle extends Module {
    private final SliderSetting radius = new SliderSetting("Радиус", 1.5f, 1, 2, 0.1f);
    private final CopyOnWriteArrayList<Circle> circles = new CopyOnWriteArrayList<>();

    public JumpCircle() {
        super("Jump Circle", "Добавляет эффект при прыжке", Category.Visuals);
        addSettings(radius);
    }

    @EventTarget
    private void onJump(EventJump event) {
        Vector3d pos = new Vector3d(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
        circles.add(new Circle(pos));
        circles.add(new Circle(pos));
    }

    @EventTarget
    private void onRender3D(EventRender3D event) {
        if (circles.isEmpty()) return;

        float spin = (mc.world.getGameTime() + event.getPartialTicks()) * 20f;
        float angleStep = 360f / 120;
        int themeColor = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
        int darkerColor = ColorUtil.darken(themeColor, .6f);
        int boost = 45;

        for (Circle circle : circles) {
            if (!circle.hasExpanded) {
                circle.scaleAnim.update(1.1f);
                if (circle.alphaAnim.getTarget() != 1.0f || !circle.alphaAnim.isDone()) circle.alphaAnim.update(1.0f);
                if (circle.scaleAnim.isDone()) {
                    circle.hasExpanded = true;
                    circle.scaleAnim.setValue(1.1f);
                    circle.scaleAnim.update(1.0f);
                }
            } else {
                circle.scaleAnim.update(1.0f);
                if (!circle.fading) {
                    if (circle.alphaAnim.getTarget() != 1.0f || !circle.alphaAnim.isDone())
                        circle.alphaAnim.update(1.0f);
                    if (circle.alphaAnim.isDone()) {
                        circle.fading = true;
                        circle.alphaAnim.setSpeed(0.25f);
                        circle.alphaAnim.update(0f);
                    }
                } else {
                    if (circle.alphaAnim.getTarget() != 0f || !circle.alphaAnim.isDone()) circle.alphaAnim.update(0f);
                    if (circle.scaleAnim.isDone() && !circle.speedSwitched) {
                        circle.speedSwitched = true;
                        circle.alphaAnim.setSpeed(3f);
                        circle.alphaAnim.setValue(circle.alphaAnim.getValue());
                        circle.alphaAnim.update(0f);
                    }
                    if (circle.alphaAnim.isDone()) {
                        circles.remove(circle);
                        continue;
                    }
                }
            }

            float worldX = (float) (circle.vector3d.x - mc.getRenderManager().info.getProjectedView().x);
            float worldY = (float) (circle.vector3d.y - mc.getRenderManager().info.getProjectedView().y) + 0.05f;
            float worldZ = (float) (circle.vector3d.z - mc.getRenderManager().info.getProjectedView().z);
            float circleRadius = (radius.get() + 1) * circle.scaleAnim.getValue() * 0.5f;
            float innerRadius = Math.max(0.001f, circleRadius * 0.02f);
            float alpha = circle.alphaAnim.getValue();
            int colorInner = ((int) (alpha * 25.5f) << 24) | (Math.min(255, ColorUtil.red(themeColor) + boost) << 16) | (Math.min(255, ColorUtil.green(themeColor) + boost) << 8) | Math.min(255, ColorUtil.blue(themeColor) + boost);

            for (int i = 0; i < 120; i++) {
                float rad1 = (float) Math.toRadians(spin + i * angleStep);
                float rad2 = (float) Math.toRadians(spin + (i + 1) * angleStep);
                float cos1 = (float) Math.cos(rad1), sin1 = (float) Math.sin(rad1);
                float cos2 = (float) Math.cos(rad2), sin2 = (float) Math.sin(rad2);

                float p1 = (i * angleStep) % 360f;
                p1 = p1 >= 180f ? 360f - p1 : p1;
                float p2 = ((i + 1) * angleStep) % 360f;
                p2 = p2 >= 180f ? 360f - p2 : p2;

                float easedP1 = (float) Easings.SINE_IN_OUT.ease(p1 / 180f);
                float easedP2 = (float) Easings.SINE_IN_OUT.ease(p2 / 180f);

                int col1 = ColorUtil.overCol(ColorUtil.applyOpacity(themeColor, alpha), ColorUtil.applyOpacity(darkerColor, alpha), easedP1);
                int col2 = ColorUtil.overCol(ColorUtil.applyOpacity(themeColor, alpha), ColorUtil.applyOpacity(darkerColor, alpha), easedP2);
                int c0 = (ColorUtil.alpha(col1) << 24) | (Math.min(255, ColorUtil.red(col1) + boost) << 16) | (Math.min(255, ColorUtil.green(col1) + boost) << 8) | Math.min(255, ColorUtil.blue(col1) + boost);
                int c1 = (ColorUtil.alpha(col2) << 24) | (Math.min(255, ColorUtil.red(col2) + boost) << 16) | (Math.min(255, ColorUtil.green(col2) + boost) << 8) | Math.min(255, ColorUtil.blue(col2) + boost);

                RenderUtil.drawImage3DQuadInternal(new ResourceLocation("SkyCore/icons/world_render/jump.png"), worldX + cos1 * circleRadius, worldY, worldZ + sin1 * circleRadius, worldX + cos2 * circleRadius, worldY, worldZ + sin2 * circleRadius, worldX + cos2 * innerRadius, worldY, worldZ + sin2 * innerRadius, worldX + cos1 * innerRadius, worldY, worldZ + sin1 * innerRadius, c0, c1, colorInner, colorInner, cos1 * 0.5f + 0.5f, sin1 * 0.5f + 0.5f, cos2 * 0.5f + 0.5f, sin2 * 0.5f + 0.5f, 0.5f, 0.5f, 0.5f, 0.5f);
            }
        }
        RenderUtil.flushImage3DBatch();
    }

    private static class Circle {
        private final Vector3d vector3d;
        private boolean hasExpanded = false, fading = false, speedSwitched = false;
        private final AnimationUtil scaleAnim = new AnimationUtil(0.1f, 3f, Easings.SINE_OUT);
        private final AnimationUtil alphaAnim = new AnimationUtil(0, 3f, Easings.SINE_OUT);

        public Circle(Vector3d vector3d) {
            this.vector3d = vector3d;
            this.scaleAnim.update(1.1f);
            this.alphaAnim.update(1.0f);
        }
    }
}