package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.optifine.util.TextureUtils;
import org.lwjgl.opengl.GL11;
import sky.core.events.EventRender2D;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.math.AnimationTest;
import sky.core.utils.math.Easing;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.shader.CustomFramebuffer;
import sky.core.utils.render.shader.ShaderUtil;

public class BlockOutline extends Module {

    private static final float WAVE_SPEED = 1.2f;
    private static final float WAVE_FREQUENCY = 1.0f;
    private static final float OUTLINE_WIDTH = 0.1f;
    private static final float GLOW_STRENGTH = 10.0f;
    private static final float FILL_STRENGTH = 0.6f;
    private static final float ALPHA_VALUE = 1.0f;

    private final AnimationTest stretchAnim = new AnimationTest(Easing.EASE_OUT_CUBIC, 170);
    private BlockPos lastpensibBlock;
    private AxisAlignedBB mirHitb;
    private AxisAlignedBB toWorldBB;

    public BlockOutline() {
        super("Block Overlay", "Шейдерная обводка блока.",Category.Visuals);
    }

    private final Minecraft mc = Minecraft.getInstance();
    private CustomFramebuffer maskFramebuffer = new CustomFramebuffer(true);

    @EventTarget
    private void renderMask(EventRender3D event) {
        if (mc.objectMouseOver == null || mc.objectMouseOver.getType() != RayTraceResult.Type.BLOCK) {
            this.lastpensibBlock = null;
            this.mirHitb = null;
            this.toWorldBB = null;
            return;
        }

        if (mc.objectMouseOver.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) mc.objectMouseOver;
            BlockPos blockPos = blockRayTraceResult.getPos();

            if (blockPos != null && !mc.world.isAirBlock(blockPos)) {
                BlockState blockState = mc.world.getBlockState(blockPos);
                VoxelShape voxelShape = blockState.getShape(mc.world, blockPos);

                AxisAlignedBB currentWorldBB;
                if (voxelShape.isEmpty()) {
                    currentWorldBB = new AxisAlignedBB(blockPos);
                } else {
                    currentWorldBB = voxelShape.getBoundingBox().offset(blockPos);
                }

                if (this.lastpensibBlock == null || this.mirHitb == null || this.toWorldBB == null) {
                    this.lastpensibBlock = blockPos;
                    this.mirHitb = currentWorldBB;
                    this.toWorldBB = currentWorldBB;
                    this.stretchAnim.setValue(1.0);
                    this.stretchAnim.setDestinationValue(1.0);
                } else if (!blockPos.equals(this.lastpensibBlock)) {
                    float t = (float) this.stretchAnim.getValue();
                    AxisAlignedBB currentInterp = lerpAABB(this.mirHitb, this.toWorldBB, t);

                    this.lastpensibBlock = blockPos;
                    this.mirHitb = currentInterp;
                    this.toWorldBB = currentWorldBB;

                    this.stretchAnim.setValue(0.0);
                    this.stretchAnim.setDestinationValue(1.0);
                    this.stretchAnim.reset();
                } else {
                    this.toWorldBB = currentWorldBB;
                }

                this.stretchAnim.run(1.0);
                float animT = (float) this.stretchAnim.getValue();
                AxisAlignedBB animatedWorldBB = lerpAABB(this.mirHitb, this.toWorldBB, animT);

                Vector3d viewPos = mc.getRenderManager().info.getProjectedView();
                AxisAlignedBB bb = animatedWorldBB.offset(-viewPos.x, -viewPos.y, -viewPos.z);

                this.maskFramebuffer = CustomFramebuffer.createFramebuffer(this.maskFramebuffer);
                this.maskFramebuffer.setup(true);
                drawMaskBox(bb);
                this.maskFramebuffer.unbindFramebuffer();
                mc.getFramebuffer().bindFramebuffer(true);
            } else {
                this.lastpensibBlock = null;
                this.mirHitb = null;
                this.toWorldBB = null;
            }
        }
    }

    @EventTarget
    private void renderShader(EventRender2D event) {
        if (mc.world == null) return;
        if (this.lastpensibBlock == null || this.mirHitb == null || this.toWorldBB == null) return;
        if (this.maskFramebuffer == null) return;

        int theme = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
        float[] rgb1 = ColorUtil.getColor(theme);
        float[] rgb2 = ColorUtil.getColor(ColorUtil.darken(theme, 0.65f));

        RenderSystem.pushMatrix();
        GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(516, 0.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 1, 1, 0);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.shadeModel(7425);

        ShaderUtil.blockout.useProgram();
        ShaderUtil.blockout.setUniform("texture", new int[]{0});
        ShaderUtil.blockout.setUniform("texelSize", new float[]{
                1.0F / (float) mc.getMainWindow().getFramebufferWidth(),
                1.0F / (float) mc.getMainWindow().getFramebufferHeight()
        });
        ShaderUtil.blockout.setUniform("color", new float[]{rgb1[0], rgb1[1], rgb1[2]});
        ShaderUtil.blockout.setUniform("color2", new float[]{rgb2[0], rgb2[1], rgb2[2]});
        ShaderUtil.blockout.setUniform("time", new float[]{(System.currentTimeMillis() % 100000L) / 1000.0F});

        ShaderUtil.blockout.setUniform("speed", new float[]{WAVE_SPEED});
        ShaderUtil.blockout.setUniform("scale", new float[]{WAVE_FREQUENCY});

        ShaderUtil.blockout.setUniform("outline", new float[]{OUTLINE_WIDTH});
        ShaderUtil.blockout.setUniform("glow", new float[]{GLOW_STRENGTH});
        ShaderUtil.blockout.setUniform("fill", new float[]{FILL_STRENGTH});
        ShaderUtil.blockout.setUniform("alpha", new float[]{ALPHA_VALUE + 1});

        TextureUtils.bindTexture(this.maskFramebuffer.framebufferTexture);
        CustomFramebuffer.drawQuads();
        ShaderUtil.blockout.unloadProgram();

        RenderSystem.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.disableAlphaTest();
        RenderSystem.popMatrix();
    }

    private static AxisAlignedBB lerpAABB(AxisAlignedBB a, AxisAlignedBB b, float t) {
        return new AxisAlignedBB(
                a.minX + (b.minX - a.minX) * t,
                a.minY + (b.minY - a.minY) * t,
                a.minZ + (b.minZ - a.minZ) * t,
                a.maxX + (b.maxX - a.maxX) * t,
                a.maxY + (b.maxY - a.maxY) * t,
                a.maxZ + (b.maxZ - a.maxZ) * t
        );
    }

    private void drawMaskBox(AxisAlignedBB bb) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(bb.minX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();

        buffer.pos(bb.minX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();

        buffer.pos(bb.maxX, bb.minY, bb.minZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(255, 255, 255, 255).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(255, 255, 255, 255).endVertex();

        tessellator.draw();

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}