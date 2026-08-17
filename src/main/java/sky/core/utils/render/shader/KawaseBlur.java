package sky.core.utils.render.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

public class KawaseBlur {

    public static KawaseBlur blur = new KawaseBlur();

    public final CustomFramebuffer BLURRED;
    public final CustomFramebuffer ADDITIONAL;

    public KawaseBlur() {
        BLURRED = new CustomFramebuffer(false).setLinear();
        ADDITIONAL = new CustomFramebuffer(false).setLinear();
    }

    public void updateBlur(float offset, int steps) {

        Minecraft mc = Minecraft.getInstance();
        Framebuffer mcFramebuffer = mc.getFramebuffer();
        ADDITIONAL.setup();
        mcFramebuffer.bindFramebufferTexture();
        ShaderUtil.light_kawase_down.attach();
        ShaderUtil.light_kawase_down.setUniform("offset", offset);
        ShaderUtil.light_kawase_down.setUniformf("resolution", 1f / mc.getMainWindow().getWidth(),
                1f / mc.getMainWindow().getHeight());
        CustomFramebuffer.drawQuads();
        CustomFramebuffer[] buffers = {this.ADDITIONAL, this.BLURRED };
        for (int i = 1; i < steps; ++i) {
            int step = i % 2;
            buffers[step].setup();
            buffers[(step + 1) % 2].draw();
        }
        ShaderUtil.light_kawase_down.detach();
        ShaderUtil.light_kawase_up.attach();
        ShaderUtil.light_kawase_up.setUniform("offset", offset);
        ShaderUtil.light_kawase_up.setUniformf("resolution", 1f / mc.getMainWindow().getWidth(),
                1f / mc.getMainWindow().getHeight());
        for (int i = 0; i < steps; ++i) {
            int step = i % 2;
            buffers[(step + 1) % 2].setup();
            buffers[step].draw();
        }
        ShaderUtil.light_kawase_up.detach();
        mcFramebuffer.bindFramebuffer(false);
    }
}