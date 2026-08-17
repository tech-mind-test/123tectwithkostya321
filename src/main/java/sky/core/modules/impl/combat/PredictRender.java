package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;

public class PredictRender extends Module {

    public PredictRender() {
        super("Predict Render", Category.Visuals);
    }

    @EventTarget
    public void onEvent(EventRender3D event) {
//        ChatUtil.addText("3D");
        renderTarget();
    }

    @EventTarget
    public void onEvent(EventRender2D event) {
//        ChatUtil.addText("2D");
//        renderTarget();
    }

    private void renderTarget() {
        AttackAura killAura = SkyCore.getInstance().getModuleManager().getAttackAura();
        if (mc.world == null || mc.player == null) return;

        if (killAura.getTarget() == null) return;

//        if (!(AttackAura.getTarget() instanceof AbstractClientPlayerEntity)) return;


        AbstractClientPlayerEntity target =
                (AbstractClientPlayerEntity) killAura.getTarget();

        float pt = mc.getRenderPartialTicks();


        double sx = MathUtil.lerp(target.serverX, target.prevServerX, mc.getRenderPartialTicks());
        double sy = MathUtil.lerp(target.serverY, target.prevServerY, mc.getRenderPartialTicks());
        double sz = MathUtil.lerp(target.serverZ, target.prevServerZ, mc.getRenderPartialTicks());

        Vector3d serverPos = new Vector3d(sx, sy, sz);


        Vector3d cam = mc.getRenderManager().info.getProjectedView();


        boolean slim = "slim".equalsIgnoreCase(target.getSkinType());

        PlayerModel<AbstractClientPlayerEntity> model =
                new PlayerModel<>(0F, slim);

        applyModelVisibilities(target, model);

        model.isChild = target.isChild();
        model.isSneak = target.isCrouching();
        model.isSitting = target.isPassenger();

        model.swingProgress = target.getSwingProgress(pt);


        float bodyYaw = MathHelper.interpolateAngle(
                pt,
                target.prevRenderYawOffset,
                target.renderYawOffset
        );

        float headYaw = MathHelper.interpolateAngle(
                pt,
                target.prevRotationYawHead,
                target.rotationYawHead
        );

        float headPitch = MathHelper.lerp(
                pt,
                target.prevRotationPitch,
                target.rotationPitch
        );

        float netHeadYaw = headYaw - bodyYaw;


        float limbSwingAmount =
                MathHelper.lerp(pt,
                        target.prevLimbSwingAmount,
                        target.limbSwingAmount);

        float limbSwing =
                target.limbSwing -
                        target.limbSwingAmount * (1F - pt);

        float age = target.ticksExisted + pt;

        model.setLivingAnimations(
                target,
                limbSwing,
                limbSwingAmount,
                pt
        );

        model.setRotationAngles(
                target,
                limbSwing,
                limbSwingAmount,
                age,
                netHeadYaw,
                headPitch
        );


        int color = resolveColor();

        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;


        MatrixStack stack = new MatrixStack();

        stack.push();

        double rx = serverPos.x - cam.x;
        double ry = serverPos.y - cam.y;
        double rz = serverPos.z - cam.z;

        stack.translate(rx, ry, rz);

        stack.rotate(Vector3f.YP.rotationDegrees(180F - bodyYaw));

        stack.scale(-1F, -1F, 1F);

        stack.translate(0, -1.501, 0);

        float alpha = MathHelper.clamp(100F / 255F, 0F, 0.75F);

        renderFlatModel(
                stack,
                r, g, b,
                alpha,
                model,
                true
        );

        stack.pop();
    }



    private void renderFlatModel(MatrixStack stack, float r, float g, float b,
                                 float a, PlayerModel<AbstractClientPlayerEntity> model,
                                 boolean chams) {

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        if (chams) {
            RenderSystem.disableDepthTest();
        }

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        renderFlatPart(stack, buf, model.bipedHead, -5, -6, -5, 8, 8, 8, r, g, b, a);
        renderFlatPart(stack, buf, model.bipedBody, -4, 0, -2, 8, 12, 4, r, g, b, a);
        renderFlatPart(stack, buf, model.bipedRightArm, -3, -2, -2, 4, 12, 4, r, g, b, a);
        renderFlatPart(stack, buf, model.bipedLeftArm, -3, -2, -2, 4, 12, 4, r, g, b, a);
        renderFlatPart(stack, buf, model.bipedRightLeg, -2, 0, -2, 4, 12, 4, r, g, b, a);
        renderFlatPart(stack, buf, model.bipedLeftLeg, -2, 0, -2, 4, 12, 4, r, g, b, a);

        tess.draw();

        RenderSystem.enableCull();

        if (chams) {
            RenderSystem.enableDepthTest();
        }

        RenderSystem.enableTexture();
    }


    private void renderFlatPart(MatrixStack parent, BufferBuilder buf,
                                ModelRenderer part,
                                float minX, float minY, float minZ,
                                float sizeX, float sizeY, float sizeZ,
                                float r, float g, float b, float a) {

        parent.push();

        float unit = 0.0625F;

        parent.translate(
                part.rotationPointX * unit,
                part.rotationPointY * unit,
                part.rotationPointZ * unit
        );

        if (part.rotateAngleZ != 0)
            parent.rotate(Vector3f.ZP.rotation(part.rotateAngleZ));

        if (part.rotateAngleY != 0)
            parent.rotate(Vector3f.YP.rotation(part.rotateAngleY));

        if (part.rotateAngleX != 0)
            parent.rotate(Vector3f.XP.rotation(part.rotateAngleX));

        float x1 = minX * unit;
        float y1 = minY * unit;
        float z1 = minZ * unit;

        float x2 = (minX + sizeX) * unit;
        float y2 = (minY + sizeY) * unit;
        float z2 = (minZ + sizeZ) * unit;

        Matrix4f matrix = parent.getLast().getMatrix();

        // Front
        buf.pos(matrix, x1, y1, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y1, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y2, z2).color(r, g, b, a).endVertex();

        // Back
        buf.pos(matrix, x2, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y2, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z1).color(r, g, b, a).endVertex();

        // Left
        buf.pos(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y1, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y2, z1).color(r, g, b, a).endVertex();

        // Right
        buf.pos(matrix, x2, y1, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z2).color(r, g, b, a).endVertex();

        // Top
        buf.pos(matrix, x1, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y2, z1).color(r, g, b, a).endVertex();

        // Bottom
        buf.pos(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y1, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y1, z2).color(r, g, b, a).endVertex();

        parent.pop();
    }


    private void quad(BufferBuilder buf, Matrix4f m,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float r, float g, float b, float a) {

        buf.pos(m, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(m, x2, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(m, x2, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(m, x1, y2, z2).color(r, g, b, a).endVertex();
    }


    private int resolveColor() {
        return ColorUtil.getColor(255, 255, 255, 100);
    }


    /* ================= ARM POSE ================= */

    private void applyModelVisibilities(AbstractClientPlayerEntity player,
                                        PlayerModel<AbstractClientPlayerEntity> model) {

        model.setVisible(true);

        model.bipedHeadwear.showModel = player.isWearing(PlayerModelPart.HAT);
        model.bipedBodyWear.showModel = player.isWearing(PlayerModelPart.JACKET);
        model.bipedLeftLegwear.showModel = player.isWearing(PlayerModelPart.LEFT_PANTS_LEG);
        model.bipedRightLegwear.showModel = player.isWearing(PlayerModelPart.RIGHT_PANTS_LEG);
        model.bipedLeftArmwear.showModel = player.isWearing(PlayerModelPart.LEFT_SLEEVE);
        model.bipedRightArmwear.showModel = player.isWearing(PlayerModelPart.RIGHT_SLEEVE);

        BipedModel.ArmPose main = getArmPose(player, Hand.MAIN_HAND);
        BipedModel.ArmPose off = getArmPose(player, Hand.OFF_HAND);

        if (main.func_241657_a_()) {
            off = player.getHeldItemOffhand().isEmpty()
                    ? BipedModel.ArmPose.EMPTY
                    : BipedModel.ArmPose.ITEM;
        }

        if (player.getPrimaryHand() == HandSide.RIGHT) {
            model.rightArmPose = main;
            model.leftArmPose = off;
        } else {
            model.rightArmPose = off;
            model.leftArmPose = main;
        }
    }


    private BipedModel.ArmPose getArmPose(AbstractClientPlayerEntity player,
                                          Hand hand) {

        ItemStack stack = player.getHeldItem(hand);

        if (stack.isEmpty())
            return BipedModel.ArmPose.EMPTY;

        if (player.getActiveHand() == hand && player.getItemInUseCount() > 0) {

            UseAction action = stack.getUseAction();

            if (action == UseAction.BLOCK) return BipedModel.ArmPose.BLOCK;
            if (action == UseAction.BOW) return BipedModel.ArmPose.BOW_AND_ARROW;
            if (action == UseAction.SPEAR) return BipedModel.ArmPose.THROW_SPEAR;

            if (action == UseAction.CROSSBOW && hand == player.getActiveHand())
                return BipedModel.ArmPose.CROSSBOW_CHARGE;

        } else if (stack.getItem() == Items.CROSSBOW
                && CrossbowItem.isCharged(stack)) {

            return BipedModel.ArmPose.CROSSBOW_HOLD;
        }

        return BipedModel.ArmPose.ITEM;
    }
}
