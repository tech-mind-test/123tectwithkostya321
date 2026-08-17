package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import sky.core.events.EventAttack;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.RenderUtil;

import java.util.ArrayList;
import java.util.Random;

public class HitEffect extends Module {
    private final SliderSetting size = new SliderSetting("Размер", 2f, 0.5f, 5f, 0.1f);
    private final SliderSetting lifeTime = new SliderSetting("Время жизни", 1000f, 200f, 3000f, 100f);
    private final SliderSetting speed = new SliderSetting("Скорость", 0.05f, 0.01f, 0.2f, 0.01f);
    private final ArrayList<float[]> effects = new ArrayList<>();
    private final Random rnd = new Random();
    private static final ResourceLocation TEX = new ResourceLocation("SkyCore/icons/world_render/cross.png");

    public HitEffect() { super("HitEffect", "Снежинки при ударе", Category.Visuals); addSettings(size, lifeTime, speed); }

    @java.lang.Override
    public void onDisable() { effects.clear(); super.onDisable(); }

    @EventTarget
    public void onEvent(EventAttack e) {
        Entity t = e.getTarget();
        if (t == null) return;
        Vector3d tp = t.getPositionVec().add(0, t.getHeight() / 2, 0);
        Vector3d pp = mc.player.getPositionVec().add(0, mc.player.getEyeHeight(), 0);
        Vector3d dir = pp.subtract(tp).normalize().add((rnd.nextDouble() - 0.5) * 1.5, (rnd.nextDouble() - 0.5) * 1.5, (rnd.nextDouble() - 0.5) * 1.5).normalize();
        float s = speed.get();
        effects.add(new float[]{(float)tp.x, (float)tp.y, (float)tp.z, (float)(dir.x * s), (float)(dir.y * s), (float)(dir.z * s), 0, lifeTime.get()});
    }

    @EventTarget
    public void onEvent(EventRender3D e) {
        effects.removeIf(p -> p[6] >= p[7]);
        if (effects.isEmpty()) return;
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
        Quaternion rot = new Quaternion(mc.getRenderManager().info.getRotation());
        rot.multiply(Vector3f.ZP.rotationDegrees(180f));
        Vector3f r = new Vector3f(1, 0, 0), u = new Vector3f(0, 1, 0);
        r.transform(rot); u.transform(rot);
        Vector3d cam = mc.getRenderManager().info.getProjectedView();

        for (float[] p : effects) {
            float prog = p[6] / p[7];
            float factor = 1f - prog;
            p[0] += p[3] * factor;
            p[1] += p[4] * factor;
            p[2] += p[5] * factor;
            p[3] *= 0.95f; p[4] *= 0.95f; p[5] *= 0.95f;
            p[6] += 16f;
            float sz = (float)Math.sqrt(1 - Math.pow(prog - 1, 2)) * size.get() * 0.5f;
            float alpha = prog > 0.6f ? 1f - (prog - 0.6f) / 0.4f : 1f;
            Vector3d c = new Vector3d(p[0] - cam.x, p[1] - cam.y, p[2] - cam.z);
            Vector3d hr = new Vector3d(r.getX(), r.getY(), r.getZ()).scale(sz);
            Vector3d hu = new Vector3d(u.getX(), u.getY(), u.getZ()).scale(sz);
            int col = (ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL) & 0xFFFFFF) | ((int)(alpha * 255) << 24);
            RenderUtil.drawImage3DQuad(TEX, true, (float)(c.x - hr.x - hu.x), (float)(c.y - hr.y - hu.y), (float)(c.z - hr.z - hu.z), (float)(c.x + hr.x - hu.x), (float)(c.y + hr.y - hu.y), (float)(c.z + hr.z - hu.z), (float)(c.x + hr.x + hu.x), (float)(c.y + hr.y + hu.y), (float)(c.z + hr.z + hu.z), (float)(c.x - hr.x + hu.x), (float)(c.y - hr.y + hu.y), (float)(c.z - hr.z + hu.z), col);
            RenderUtil.drawImage3DQuad(TEX, true, (float)(c.x - hr.x + hu.x), (float)(c.y - hr.y + hu.y), (float)(c.z - hr.z + hu.z), (float)(c.x + hr.x + hu.x), (float)(c.y + hr.y + hu.y), (float)(c.z + hr.z + hu.z), (float)(c.x + hr.x - hu.x), (float)(c.y + hr.y - hu.y), (float)(c.z + hr.z - hu.z), (float)(c.x - hr.x - hu.x), (float)(c.y - hr.y - hu.y), (float)(c.z - hr.z - hu.z), col);
        }
        RenderUtil.flushImage3DBatch();
        RenderSystem.depthMask(true); RenderSystem.enableDepthTest();
    }
}