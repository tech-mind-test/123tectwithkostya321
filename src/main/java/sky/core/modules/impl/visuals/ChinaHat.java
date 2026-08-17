package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import sky.core.SkyCore;
import sky.core.events.EventLayerHead;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import net.minecraft.client.renderer.entity.model.IHasHead;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.optifine.render.RenderStateManager;
import org.lwjgl.opengl.GL11;


public class ChinaHat extends Module {
    private final BooleanSetting viewinfriend = new BooleanSetting("Отображать на друзьях", true);
    private static final float PI = MathHelper.PI2 / 90;

    public ChinaHat() {
        super("China Hat", "Добавляет китайскую шапку", Category.Visuals);
        addSettings(viewinfriend);
    }

    @EventTarget
    public void onEvent(EventLayerHead event) {
        if (event.getModel() instanceof IHasHead head && event.getEntity() instanceof PlayerEntity player) {
            boolean isFriend = SkyCore.getInstance().getFriendManager().isFriend(player.getGameProfile().getName());
            boolean isLocalPlayer = player == mc.player;

            if (isLocalPlayer || (viewinfriend.get() && isFriend)) {
                MatrixStack matrixStack = event.getMatrix();
                AxisAlignedBB entityBoundingBox = event.getEntity().getBoundingBox();
                double radius = entityBoundingBox.maxX - entityBoundingBox.minX;
                float offset = player.inventory.armorInventory.get(3).isEmpty() ? 0.42F : 0.5F;

                matrixStack.push();
                head.getModelHead().translateRotate(matrixStack);
                RenderStateManager.disableCache();
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.disableCull();
                RenderSystem.disableTexture();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableAlphaTest();
                RenderSystem.shadeModel(7425);
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.shadeModel(GL11.GL_SMOOTH);

                RenderSystem.lineWidth(2F);
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
                matrixStack.translate(0, -offset, 0);
                matrixStack.rotate(Vector3f.ZN.rotationDegrees(180));
                matrixStack.rotate(Vector3f.YP.rotationDegrees(90));
                Matrix4f matrix4f = matrixStack.getLast().getMatrix();
                BUFFER.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);

                float iPi;
                int angle;
                float x, y = 0, z;
                int coneColor, outlineColor;

                for (int i = 0; i <= 180; i++) {
                    iPi = i * PI;
                    angle = i * 8;
                    int clientColor = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);

                    x = (float) (MathHelper.sin(iPi) * radius);
                    z = (float) (MathHelper.cos(iPi) * radius);

                    outlineColor = ColorUtil.fade(3, angle, ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), ColorUtil.darken(ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), 0.5f));
                    coneColor = ColorUtil.multAlpha(outlineColor, .5F);

                    BUFFER.pos(matrix4f, x, y, z).color(coneColor).endVertex();
                    BUFFER.pos(matrix4f, 0, 0.3F, 0).color(clientColor).endVertex();
                }

                TESSELLATOR.draw();
                RenderSystem.depthMask(false);
                BUFFER.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);

                for (int i = 0; i <= 180; i++) {
                    float iPi2 = i * PI;
                    int angle2 = i * 8;
                    float x2 = (float) (MathHelper.sin(iPi2) * radius);
                    float z2 = (float) (MathHelper.cos(iPi2) * radius);
                    int outlineColor2 = ColorUtil.fade(3, angle2, ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), ColorUtil.darken(ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), 0.5f));
                    BUFFER.pos(matrix4f, x2, y, z2).color(outlineColor2).endVertex();
                }

                TESSELLATOR.draw();
                RenderSystem.depthMask(true);

                RenderSystem.enableCull();
                RenderSystem.enableTexture();
                RenderSystem.shadeModel(GL11.GL_FLAT);
                RenderSystem.enableAlphaTest();
                RenderSystem.shadeModel(7424);
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_LINE_SMOOTH);

                matrixStack.pop();
            }
        }
    }
}