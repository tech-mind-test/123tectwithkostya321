package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;

public class Wings extends Module {

    private static final float DEFAULT_SPREAD = 8.0f;
    private static final int   DEFAULT_ALPHA  = 220;

    private static final float[][] VECTOR_SHAPE = {
            {0.08f,  0.10f,  0.88f},
            {0.28f,  0.34f,  0.78f},
            {0.56f,  0.82f,  0.62f},
            {0.86f,  0.30f,  0.52f},
            {1.14f,  0.46f,  0.40f},
            {1.24f,  0.04f,  0.30f},
            {1.02f, -0.18f,  0.28f},
            {1.18f, -0.64f,  0.22f},
            {0.86f, -0.46f,  0.20f},
            {0.80f, -0.98f,  0.14f},
            {0.54f, -0.74f,  0.16f},
            {0.30f, -1.16f,  0.12f},
            {0.10f, -0.54f,  0.18f}
    };

    private final ModeSetting   mode    = new ModeSetting("Вид", "Обычные", "Обычные");
    private final BooleanSetting self    = new BooleanSetting("На себя",    true);
    private final BooleanSetting players = new BooleanSetting("На игроков", false);
    private final BooleanSetting fill    = new BooleanSetting("Заливка",    true);
    private final SliderSetting  opacity = new SliderSetting("Прозрачность", 220f, 10f, 255f, 5f);
    private final SliderSetting  size    = new SliderSetting("Размер",       1.0f, 0.5f, 2.0f, 0.05f);

    private float   selfBodyYaw;
    private boolean selfBodyYawInitialized;

    public Wings() {
        super("Wings", "Полупрозрачные крылья за спиной", Category.Visuals);
        addSettings(mode, self, players, fill, opacity, size);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;

        float tickDelta = event.getPartialTicks();
        Vector3d camera = mc.getRenderManager().info.getProjectedView();

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        if (self.get()
                && mc.gameSettings.getPointOfView() != net.minecraft.client.settings.PointOfView.FIRST_PERSON
                && mc.player.isAlive()
                && !hasElytra(mc.player)) {
            try { renderVectorWings(mc.player, tickDelta, camera); } catch (Exception ignored) {}
        }

        if (players.get()) {
            for (Entity entity : mc.world.getAllEntities()) {
                if (!(entity instanceof PlayerEntity player) || player == mc.player) continue;
                if (!player.isAlive() || hasElytra(player)) continue;
                try { renderVectorWings(player, tickDelta, camera); } catch (Exception ignored) {}
            }
        }

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();
    }

    private boolean hasElytra(PlayerEntity player) {
        return player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA;
    }

    private void renderVectorWings(PlayerEntity player, float tickDelta, Vector3d camera) {
        double x = MathHelper.lerp(tickDelta, player.lastTickPosX, player.getPosX()) - camera.x;
        double y = MathHelper.lerp(tickDelta, player.lastTickPosY, player.getPosY()) - camera.y;
        double z = MathHelper.lerp(tickDelta, player.lastTickPosZ, player.getPosZ()) - camera.z;

        float bodyYaw  = resolveBodyYaw(player, tickDelta);
        float move     = MathHelper.clamp(player.limbSwingAmount, 0f, 1f);

        WingPose pose = resolveVectorPose(player);
        if (pose == null) return;

        float flap      = (float) Math.sin((player.ticksExisted + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
        float open      = (DEFAULT_SPREAD + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
        float wingScale = size.get() * pose.scaleMultiplier;

        int baseColor = resolveBaseColor();
        int glowColor = resolveGlowColor(baseColor);
        int coreColor = resolveCoreColor(baseColor);

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(180f - bodyYaw, 0f, 1f, 0f);
        if (pose.preTranslateY != 0f || pose.preTranslateZ != 0f)
            GL11.glTranslatef(0f, pose.preTranslateY, pose.preTranslateZ);
        if (pose.pitchRotation != 0f)
            GL11.glRotatef(pose.pitchRotation, 1f, 0f, 0f);
        if (pose.rollRotation != 0f)
            GL11.glRotatef(pose.rollRotation, 0f, 0f, 1f);
        GL11.glTranslatef(0f, pose.anchorY, pose.anchorZ);
        GL11.glScalef(wingScale, wingScale, wingScale);

        renderVectorWingSide(-1f, open, baseColor, glowColor, coreColor, pose);
        renderVectorWingSide( 1f, open, baseColor, glowColor, coreColor, pose);
        GL11.glPopMatrix();
    }

    private void renderVectorWingSide(float side, float open, int baseColor, int glowColor, int coreColor, WingPose pose) {
        GL11.glPushMatrix();
        GL11.glTranslatef(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        GL11.glRotatef(side * open, 0f, 1f, 0f);
        GL11.glRotatef(side * pose.sideRoll, 0f, 0f, 1f);
        GL11.glRotatef(pose.sidePitch, 1f, 0f, 0f);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (fill.get()) drawWingLayer(side, 1.0f, setAlpha(baseColor, opacity.get().intValue()), setAlpha(baseColor, Math.max(20, opacity.get().intValue() / 4)));

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        drawWingLayer(side, 1.22f, setAlpha(glowColor, (int)(DEFAULT_ALPHA * 0.22f)), setAlpha(glowColor, 0));
        drawWingLayer(side, 0.84f, setAlpha(coreColor, (int)(DEFAULT_ALPHA * 0.26f)), setAlpha(coreColor, 0));
        drawWingOutline(side, 1.0f, setAlpha(baseColor, (int)(DEFAULT_ALPHA * 0.62f)));
        drawWingRibs(side, 0.96f, setAlpha(glowColor, (int)(DEFAULT_ALPHA * 0.20f)));

        GL11.glPopMatrix();
    }

    private void drawWingLayer(float side, float scale, int rootColor, int edgeColor) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (int i = 0; i < VECTOR_SHAPE.length; i++) {
            float[] cur  = VECTOR_SHAPE[i];
            float[] next = VECTOR_SHAPE[(i + 1) % VECTOR_SHAPE.length];
            glColor(rootColor);
            GL11.glVertex3f(0f, 0f, 0f);
            glColor(edgeColor);
            GL11.glVertex3f(side * cur[0] * scale, cur[1] * scale, 0f);
            glColor(edgeColor);
            GL11.glVertex3f(side * next[0] * scale, next[1] * scale, 0f);
        }
        GL11.glEnd();
    }

    private void drawWingOutline(float side, float scale, int color) {
        GL11.glLineWidth(1.35f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (float[] point : VECTOR_SHAPE) {
            glColor(color);
            GL11.glVertex3f(side * point[0] * scale, point[1] * scale, 0f);
        }
        glColor(color);
        GL11.glVertex3f(side * VECTOR_SHAPE[0][0] * scale, VECTOR_SHAPE[0][1] * scale, 0f);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private void drawWingRibs(float side, float scale, int color) {
        int[] ribIndices = {2, 4, 7, 9, 11};
        GL11.glLineWidth(0.9f);
        GL11.glBegin(GL11.GL_LINES);
        for (int idx : ribIndices) {
            if (idx >= VECTOR_SHAPE.length) continue;
            float[] point = VECTOR_SHAPE[idx];
            glColor(setAlpha(color, Math.max(8, (int)(alpha(color) * 0.75f))));
            GL11.glVertex3f(0f, 0f, 0f);
            glColor(applyPointAlpha(color, point[2]));
            GL11.glVertex3f(side * point[0] * scale, point[1] * scale, 0f);
        }
        GL11.glEnd();
    }

    private void glColor(int color) {
        GL11.glColor4f(red(color) / 255f, green(color) / 255f, blue(color) / 255f, alpha(color) / 255f);
    }

    private int applyPointAlpha(int color, float multiplier) {
        return setAlpha(color, Math.max(20, Math.min(255, (int)(alpha(color) * multiplier))));
    }

    private static int setAlpha(int color, int a) {
        return (MathHelper.clamp(a, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int alpha(int color) { return (color >> 24) & 0xFF; }
    private static int red(int color)   { return (color >> 16) & 0xFF; }
    private static int green(int color) { return (color >>  8) & 0xFF; }
    private static int blue(int color)  { return  color        & 0xFF; }

    private int resolveBaseColor() {
        int color = ThemeEditor.getColor(ThemeSettings.LOGO);
        return (color & 0x00FFFFFF) | 0xFF000000;
    }

    private int resolveGlowColor(int base) {
        int r = Math.min(255, red(base)   + (int)((255 - red(base))   * 0.28f));
        int g = Math.min(255, green(base) + (int)((255 - green(base)) * 0.28f));
        int b = Math.min(255, blue(base)  + (int)((255 - blue(base))  * 0.28f));
        return (alpha(base) << 24) | (r << 16) | (g << 8) | b;
    }

    private int resolveCoreColor(int base) {
        int r = Math.min(255, red(base)   + (int)((255 - red(base))   * 0.55f));
        int g = Math.min(255, green(base) + (int)((255 - green(base)) * 0.55f));
        int b = Math.min(255, blue(base)  + (int)((255 - blue(base))  * 0.55f));
        return (alpha(base) << 24) | (r << 16) | (g << 8) | b;
    }

    private float resolveBodyYaw(PlayerEntity player, float tickDelta) {
        float target = MathHelper.lerp(tickDelta, player.prevRenderYawOffset, player.renderYawOffset);
        if (player != mc.player) return target;
        if (!selfBodyYawInitialized || player.ticksExisted < 2) {
            selfBodyYaw = target;
            selfBodyYawInitialized = true;
            return selfBodyYaw;
        }
        selfBodyYaw = approachDegrees(selfBodyYaw, target, 14f);
        return selfBodyYaw;
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = MathHelper.wrapDegrees(target - current);
        delta = MathHelper.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }

    private WingPose resolveVectorPose(PlayerEntity player) {
        if (player.isElytraFlying()) return null;
        if (player.isSwimming()) return null;
        if (player.isSneaking()) {
            return new WingPose(0f, 0f, 0.96f, 0.10f, 18f, 0f,
                    1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, -4f, 0.12f);
        }
        return new WingPose(0f, 0f, 1.38f, 0.10f, 0f, 0f,
                1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, -4f, 0.12f);
    }

    @Override
    public void onDisable() {
        selfBodyYawInitialized = false;
        super.onDisable();
    }

    private static final class WingPose {
        final float preTranslateY, preTranslateZ, anchorY, anchorZ;
        final float pitchRotation, rollRotation, openMultiplier, scaleMultiplier;
        final float motionSpreadBoost, flapAmplitude, sideOffset, sideYOffset;
        final float sideZOffset, sideRoll, sidePitch, flapSpeed;

        WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ,
                 float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier,
                 float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideZOffset,
                 float sideRoll, float sidePitch, float flapSpeed) {
            this.preTranslateY = preTranslateY; this.preTranslateZ = preTranslateZ;
            this.anchorY = anchorY; this.anchorZ = anchorZ;
            this.pitchRotation = pitchRotation; this.rollRotation = rollRotation;
            this.openMultiplier = openMultiplier; this.scaleMultiplier = scaleMultiplier;
            this.motionSpreadBoost = motionSpreadBoost; this.flapAmplitude = flapAmplitude;
            this.sideOffset = sideOffset; this.sideYOffset = 0f;
            this.sideZOffset = sideZOffset; this.sideRoll = sideRoll;
            this.sidePitch = sidePitch; this.flapSpeed = flapSpeed;
        }
    }

}
