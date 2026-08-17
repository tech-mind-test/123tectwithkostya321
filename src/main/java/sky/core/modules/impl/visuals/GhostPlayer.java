package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import sky.core.events.EventPopTotem;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GhostPlayer extends Module {

    private static final float GHOST_RISE_SPEED = 1.0f;

    private final List<GhostModel> ghostModels = new ArrayList<>();

    public GhostPlayer() {
        super("TotemAngel", "Модель-призрак при тотеме", Category.Visuals);
    }

    @java.lang.Override
    public void onDisable() {
        super.onDisable();
        ghostModels.clear();
    }

    @EventTarget
    public void onEvent(EventPopTotem eventPopTotem) {
        if (mc.player == null || mc.world == null) return;
        if (eventPopTotem.getEntity() instanceof AbstractClientPlayerEntity player) {
            if (player != mc.player) {
                addGhostModel(player);
            }
        }
    }

    @EventTarget
    public void onEvent(EventRender3D eventRender3D) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrixStack = eventRender3D.getMatrixStack();
        float partialTicks = eventRender3D.getPartialTicks();

        Iterator<GhostModel> iterator = ghostModels.iterator();
        while (iterator.hasNext()) {
            GhostModel ghost = iterator.next();
            if (ghost.isExpired()) {
                iterator.remove();
                continue;
            }
            renderGhostModel(ghost, matrixStack, partialTicks);
        }
    }

    private void addGhostModel(AbstractClientPlayerEntity player) {
        try {
            EntityRendererManager renderManager = mc.getRenderManager();
            PlayerRenderer playerRenderer = (PlayerRenderer) renderManager.getRenderer(player);
            if (playerRenderer == null) return;

            PlayerModel<AbstractClientPlayerEntity> sourceModel = playerRenderer.getEntityModel();

            float partialTicks = mc.getRenderPartialTicks();
            float limbSwing = player.limbSwing - player.limbSwingAmount * (1.0F - partialTicks);
            float limbSwingAmount = MathHelper.lerp(partialTicks, player.prevLimbSwingAmount, player.limbSwingAmount);
            float ageInTicks = player.ticksExisted + partialTicks;

            float bodyYaw = MathHelper.interpolateAngle(partialTicks, player.prevRenderYawOffset, player.renderYawOffset);
            float headYaw = MathHelper.interpolateAngle(partialTicks, player.prevRotationYawHead, player.rotationYawHead);
            float headYawRelative = headYaw - bodyYaw;

            float headPitch = MathHelper.lerp(partialTicks, player.prevRotationPitch, player.rotationPitch);

            if (limbSwingAmount > 1.0F) limbSwingAmount = 1.0F;

            sourceModel.setRotationAngles(player, limbSwing, limbSwingAmount, ageInTicks, headYawRelative, headPitch);
            sourceModel.setLivingAnimations(player, limbSwing, limbSwingAmount, partialTicks);

            double posX = MathHelper.lerp(partialTicks, player.lastTickPosX, player.getPosX());
            double posY = MathHelper.lerp(partialTicks, player.lastTickPosY, player.getPosY());
            double posZ = MathHelper.lerp(partialTicks, player.lastTickPosZ, player.getPosZ());

            CopiedModel copiedModel = new CopiedModel(sourceModel);

            boolean sneaking = player.isCrouching();

            GhostModel ghost = new GhostModel(
                    new Vector3d(posX, posY, posZ),
                    bodyYaw,
                    copiedModel,
                    2000,
                    GHOST_RISE_SPEED,
                    sneaking
            );
            ghostModels.add(ghost);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderGhostModel(GhostModel ghost, MatrixStack matrixStack, float partialTicks) {
        if (mc.player == null) return;

        EntityRendererManager renderManager = mc.getRenderManager();
        Vector3d renderPos = renderManager.info.getProjectedView();
        Vector3d ghostPos = ghost.getCurrentPosition();
        double x = ghostPos.x - renderPos.x;
        double y = ghostPos.y - renderPos.y;
        double z = ghostPos.z - renderPos.z;
        float progress = ghost.getProgress();
        float currentAlpha = 0.5f * (1.0f - progress);

        float red = 1.0F;
        float green = 1.0F;
        float blue = 1.0F;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        MatrixStack ws = new MatrixStack();
        ws.translate(x, y, z);
        ws.translate(0, 1.501, 0);
        ws.scale(1.0f, -1.0f, -1.0f);
        ws.rotate(Vector3f.YP.rotationDegrees(180.0F - ghost.yaw));
        if (ghost.sneaking) {
            ws.translate(0, 0.125, 0);
        }

        renderModelPartWorld(ws, bb, ghost.model.head,
                0, progress * 0.5f, progress * 0.2f, red, green, blue, currentAlpha);
        renderModelPartWorld(ws, bb, ghost.model.body,
                0, progress * 0.3f, 0, red, green, blue, currentAlpha);
        renderModelPartWorld(ws, bb, ghost.model.rightArm,
                -progress * 0.4f, progress * 0.35f, 0, red, green, blue, currentAlpha);
        renderModelPartWorld(ws, bb, ghost.model.leftArm,
                progress * 0.4f, progress * 0.35f, 0, red, green, blue, currentAlpha);
        renderModelPartWorld(ws, bb, ghost.model.rightLeg,
                -progress * 0.3f, progress * 0.2f, 0, red, green, blue, currentAlpha);
        renderModelPartWorld(ws, bb, ghost.model.leftLeg,
                progress * 0.3f, progress * 0.2f, 0, red, green, blue, currentAlpha);

        Tessellator.getInstance().draw();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    private void renderModelPartWorld(MatrixStack stack, BufferBuilder bb,
                                      ModelPart part, float offsetX, float offsetY, float offsetZ,
                                      float red, float green, float blue, float alpha) {
        stack.push();
        stack.translate(offsetX, offsetY, offsetZ);
        stack.translate(part.x / 16.0f, part.y / 16.0f, part.z / 16.0f);

        if (part.zRot != 0.0F) stack.rotate(Vector3f.ZP.rotation(part.zRot));
        if (part.yRot != 0.0F) stack.rotate(Vector3f.YP.rotation(part.yRot));
        if (part.xRot != 0.0F) stack.rotate(Vector3f.XP.rotation(part.xRot));

        renderCubeWorld(bb, stack.getLast().getMatrix(), part, red, green, blue, alpha);
        stack.pop();
    }

    private void renderCubeWorld(BufferBuilder bb, Matrix4f mat, ModelPart part,
                                 float red, float green, float blue, float alpha) {
        float minX = part.minX / 16.0f;
        float minY = part.minY / 16.0f;
        float minZ = part.minZ / 16.0f;
        float maxX = part.maxX / 16.0f;
        float maxY = part.maxY / 16.0f;
        float maxZ = part.maxZ / 16.0f;

        quadWorld(bb, mat, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        quadWorld(bb, mat, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        quadWorld(bb, mat, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);
        quadWorld(bb, mat, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        quadWorld(bb, mat, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        quadWorld(bb, mat, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
    }

    private void quadWorld(BufferBuilder bb, Matrix4f mat,
                           float x0, float y0, float z0,
                           float x1, float y1, float z1,
                           float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float r, float g, float b, float a) {
        posWorld(bb, mat, x0, y0, z0, r, g, b, a);
        posWorld(bb, mat, x1, y1, z1, r, g, b, a);
        posWorld(bb, mat, x2, y2, z2, r, g, b, a);
        posWorld(bb, mat, x3, y3, z3, r, g, b, a);
    }

    private void posWorld(BufferBuilder bb, Matrix4f mat, float x, float y, float z,
                          float r, float g, float b, float a) {
        net.minecraft.util.math.vector.Vector4f p = new net.minecraft.util.math.vector.Vector4f(x, y, z, 1.0f);
        p.transform(mat);
        bb.pos(p.getX(), p.getY(), p.getZ()).color(r, g, b, a).endVertex();
    }

    private static class ModelPart {
        float x, y, z;
        float xRot, yRot, zRot;
        float minX, minY, minZ;
        float maxX, maxY, maxZ;

        public ModelPart(ModelRenderer renderer) {
            this.x = renderer.rotationPointX;
            this.y = renderer.rotationPointY;
            this.z = renderer.rotationPointZ;
            this.xRot = renderer.rotateAngleX;
            this.yRot = renderer.rotateAngleY;
            this.zRot = renderer.rotateAngleZ;

            if (renderer.cubeList != null && !renderer.cubeList.isEmpty()) {
                ModelRenderer.ModelBox box = renderer.cubeList.get(0);
                this.minX = box.posX1;
                this.minY = box.posY1;
                this.minZ = box.posZ1;
                this.maxX = box.posX2;
                this.maxY = box.posY2;
                this.maxZ = box.posZ2;
            } else {
                this.minX = -4;
                this.minY = 0;
                this.minZ = -2;
                this.maxX = 4;
                this.maxY = 8;
                this.maxZ = 2;
            }
        }
    }

    private static class CopiedModel {
        ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

        public CopiedModel(PlayerModel<AbstractClientPlayerEntity> source) {
            this.head = new ModelPart(source.bipedHead);
            this.body = new ModelPart(source.bipedBody);
            this.rightArm = new ModelPart(source.bipedRightArm);
            this.leftArm = new ModelPart(source.bipedLeftArm);
            this.rightLeg = new ModelPart(source.bipedRightLeg);
            this.leftLeg = new ModelPart(source.bipedLeftLeg);
        }
    }

    private static class GhostModel {
        private final Vector3d startPos;
        private final float yaw;
        private final CopiedModel model;
        private final long duration;
        private final float speed;
        private final long startTime;
        private final boolean sneaking;

        public GhostModel(Vector3d startPos, float yaw, CopiedModel model,
                          long duration, float speed, boolean sneaking) {
            this.startPos = startPos;
            this.yaw = yaw;
            this.model = model;
            this.duration = duration;
            this.speed = speed;
            this.startTime = System.currentTimeMillis();
            this.sneaking = sneaking;
        }

        public Vector3d getCurrentPosition() {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1.0f, elapsed / (float) duration);

            double yOffset = progress * speed * 2.0;
            return startPos.add(0, yOffset, 0);
        }

        public float getProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.min(1.0f, elapsed / (float) duration);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime >= duration;
        }
    }
}