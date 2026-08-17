package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import sky.core.events.EventRender2D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.render.shader.CustomFramebuffer;
import sky.core.utils.render.shader.ShaderUtil;

public class Saturation extends Module {
    private final SliderSetting saturation = new SliderSetting("Насыщенность", 1.0f, 0.0f, 2.0f, 0.05f);

    private CustomFramebuffer cacheFramebuffer;

    public Saturation() {
        super("Saturation", "Изменяет насыщенность цветов на экране", Category.Visuals);
        addSettings(this.saturation);
    }

    @EventTarget
    public void onRender(EventRender2D.Send event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        RenderSystem.bindRenderThread();
        if (!RenderSystem.isOnRenderThread()) {
            return;
        }

        float saturationValue = this.saturation.get();
        if (Math.abs(saturationValue - 1.0f) < 0.001f) {
            return;
        }

        Framebuffer mainFramebuffer = mc.getFramebuffer();
        cacheFramebuffer = CustomFramebuffer.createFramebuffer(cacheFramebuffer);

        RenderSystem.pushMatrix();
        GlStateManager.disableDepthTest();
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        cacheFramebuffer.setup(true);
        mainFramebuffer.bindFramebufferTexture();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainFramebuffer.framebufferTexture);
        CustomFramebuffer.drawQuads();

        mainFramebuffer.bindFramebuffer(true);
        ShaderUtil.saturation.useProgram();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, cacheFramebuffer.framebufferTexture);
        ShaderUtil.saturation.setUniform("texture", 0);
        ShaderUtil.saturation.setUniformf("saturation", saturationValue);
        CustomFramebuffer.drawQuads();
        ShaderUtil.saturation.unloadProgram();

        GlStateManager.enableBlend();
        GlStateManager.enableDepthTest();
        RenderSystem.popMatrix();
    }

    @Override
    public void onDisable() {
        if (cacheFramebuffer != null) {
            cacheFramebuffer.deleteFramebuffer();
            cacheFramebuffer = null;
        }
        super.onDisable();
    }
}
