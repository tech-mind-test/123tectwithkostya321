package net.minecraft.client.renderer;

import com.darkmagician6.eventapi.EventManager;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.World;
import other.bot.Bot;
import other.bot.BotManager;
import other.bot.player.BotPlayer;
import sky.core.SkyCore;
import sky.core.events.EventSwingAnimation;
import sky.core.events.EventTransformSideFirstPerson;
import sky.core.events.EventViewModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.storage.MapData;
import net.optifine.Config;
import net.optifine.CustomItems;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import sky.core.modules.impl.visuals.SwordAnimations;

public class FirstPersonRenderer {
    private static final RenderType MAP_BACKGROUND = RenderType.getText(new ResourceLocation("textures/map/map_background.png"));
    private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderType.getText(new ResourceLocation("textures/map/map_background_checkerboard.png"));
    private final Minecraft mc;
    private ItemStack itemStackMainHand = ItemStack.EMPTY;
    private ItemStack itemStackOffHand = ItemStack.EMPTY;
    private float equippedProgressMainHand;
    private float prevEquippedProgressMainHand;
    private float equippedProgressOffHand;
    private float prevEquippedProgressOffHand;
    private final EntityRendererManager renderManager;
    private final ItemRenderer itemRenderer;

    public FirstPersonRenderer(Minecraft mcIn) {
        this.mc = mcIn;
        this.renderManager = mcIn.getRenderManager();
        this.itemRenderer = mcIn.getItemRenderer();
    }

    private SwordAnimations getSwordAnimationsNewMode() {
        if (SkyCore.getInstance() == null || SkyCore.getInstance().getModuleManager() == null) {
            return null;
        }

        SwordAnimations swordAnimations = SkyCore.getInstance().getModuleManager().getSwordAnimations();
        if (swordAnimations == null || !swordAnimations.isEnabled() || !swordAnimations.swordAnim.is("3D")) {
            return null;
        }

        return swordAnimations;
    }

    private float easeInOutBack(float value) {
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        if (value < 0.5F) {
            return (float) (Math.pow(2.0F * value, 2.0D) * ((c2 + 1.0F) * 2.0F * value - c2) / 2.0D);
        }

        return (float) ((Math.pow(2.0F * value - 2.0F, 2.0D) * ((c2 + 1.0F) * (value * 2.0F - 2.0F) + c2) + 2.0D) / 2.0D);
    }

    private float getNewModeSwingRot(float swingProgress) {
        return swingProgress < 0.6F
                ? MathHelper.sin(MathHelper.clamp(swingProgress, 0.0F, 0.12506F) * 12.56F)
                : MathHelper.sin(MathHelper.clamp(swingProgress, 0.62532F, 0.75038F) * 12.56F);
    }

    private float getNewModeSwing(float swingProgress) {
        return this.easeInOutBack(MathHelper.sin(swingProgress * (float) Math.PI));
    }

    private void applyNewModeHandBase(MatrixStack matrixStackIn, float equippedProgress, HandSide side) {
        int direction = side == HandSide.RIGHT ? 1 : -1;
        matrixStackIn.translate((double) direction, -equippedProgress * 0.3D, 0.3D);
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(45.0F * direction));
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(-40.0F * direction));
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(30.0F));
        matrixStackIn.scale(0.9F, 0.9F, 0.9F);
    }

    private void renderNewModeArm(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, HandSide side, float equippedProgress, float swingProgress) {
        this.renderArmFirstPerson(matrixStackIn, bufferIn, combinedLightIn, equippedProgress, swingProgress, side);
    }

    private boolean shouldRenderNewModeLeftArm(AbstractClientPlayerEntity player) {
        SwordAnimations swordAnimations = this.getSwordAnimationsNewMode();
        return swordAnimations != null
                && swordAnimations.newShowLeftArm.get()
                && !player.isInvisible();
    }

    private boolean renderNewModeEmptyHand(AbstractClientPlayerEntity player, HandSide handside, float swingProgress, float equippedProgress, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn) {
        if (player.isInvisible()) {
            return false;
        }

        int direction = handside == HandSide.RIGHT ? 1 : -1;
        boolean movementPose = player.isSwimming() || player.isVisuallySwimming() || !player.isOnGround() && player.isOnLadder();

        if (handside == HandSide.LEFT) {
            float swingRot = this.getNewModeSwingRot(swingProgress);
            float swing = this.getNewModeSwing(swingProgress);

            if (movementPose) {
                this.applyNewModeHandBase(matrixStackIn, equippedProgress, handside);
                matrixStackIn.translate(0.05D, -0.08D, -0.1D);
                matrixStackIn.rotate(Vector3f.YP.rotationDegrees(12.0F));
            } else {
                matrixStackIn.translate(-0.1D, -0.08D - equippedProgress * 0.05D, -0.12D);
                matrixStackIn.translate(-0.03D * swing, 0.04D * swingRot, -0.05D * swing);
                matrixStackIn.rotate(Vector3f.YP.rotationDegrees(18.0F));
                matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(-10.0F));
                matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-12.0F));
                matrixStackIn.rotate(Vector3f.YP.rotationDegrees(8.0F * swing));
                matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-6.0F * swingRot));
            }

            this.renderNewModeArm(matrixStackIn, bufferIn, combinedLightIn, handside, equippedProgress * 0.35F, 0.0F);
            return true;
        }

        if (movementPose) {
            this.applyNewModeHandBase(matrixStackIn, equippedProgress, handside);
        } else {
            float swingRot = this.getNewModeSwingRot(swingProgress);
            float swing = this.getNewModeSwing(swingProgress);
            matrixStackIn.translate(0.0D, 0.2D * swingRot, 0.15D * swingRot);
            matrixStackIn.translate(0.1D * direction * swing, 0.15D * swing, -0.45D * swing);
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(35.0F * swing * direction));
            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-30.0F * swing));
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(-10.0F * swingRot * direction));
            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(10.0F * swingRot));
        }

        this.renderNewModeArm(matrixStackIn, bufferIn, combinedLightIn, handside, 0.0F, 0.0F);
        return true;
    }

    private boolean renderNewModeOffhand(AbstractClientPlayerEntity player, float partialTicks, float swingProgress, ItemStack stack, float equippedProgress, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn) {
        if (!this.shouldRenderNewModeLeftArm(player)) {
            return false;
        }

        HandSide handside = player.getPrimaryHand().opposite();
        boolean usingThisHand = player.isHandActive() && player.getItemInUseCount() > 0 && player.getActiveHand() == Hand.OFF_HAND;

        if (stack.isEmpty()) {
            return this.renderNewModeEmptyHand(player, handside, swingProgress, equippedProgress, matrixStackIn, bufferIn, combinedLightIn);
        }

        if (stack.getItem() instanceof FilledMapItem || stack.getItem() instanceof CrossbowItem) {
            return false;
        }

        if (usingThisHand) {
            float useTicks = (float) stack.getUseDuration() - ((float) player.getItemInUseCount() - partialTicks + 1.0F);

            switch (stack.getUseAction()) {
                case EAT:
                case DRINK:
                    this.applyNewModeEatDrinkHandPose(matrixStackIn, handside, useTicks);
                    this.renderNewModeArm(matrixStackIn, bufferIn, combinedLightIn, handside, 0.0F, swingProgress);
                    break;

                case BLOCK:
                    this.applyNewModeBlockHandPose(matrixStackIn, handside, useTicks);
                    this.renderNewModeArm(matrixStackIn, bufferIn, combinedLightIn, handside, 0.0F, swingProgress);
                    break;

                default:
                    this.applyNewModeHandBase(matrixStackIn, equippedProgress, handside);
                    this.renderNewModeArm(matrixStackIn, bufferIn, combinedLightIn, handside, 0.0F, 0.0F);
                    break;
            }
        } else {
            this.applyNewModeHandBase(matrixStackIn, equippedProgress, handside);
            this.renderNewModeArm(matrixStackIn, bufferIn, combinedLightIn, handside, 0.0F, 0.0F);
        }

        this.applyNewModeCommonItemAnchor(matrixStackIn, handside);
        this.applyNewModeItemPose(matrixStackIn, handside, stack);
        this.renderItemSide(player, stack, handside == HandSide.RIGHT ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, handside == HandSide.LEFT, matrixStackIn, bufferIn, combinedLightIn);
        return true;
    }

    private boolean isNewModeWeaponLike(ItemStack stack) {
        Item item = stack.getItem();
        UseAction useAction = stack.getUseAction();
        return item instanceof SwordItem
                || item instanceof AxeItem
                || item instanceof ToolItem
                || item instanceof ShieldItem
                || item instanceof TridentItem
                || item instanceof FishingRodItem
                || useAction == UseAction.BOW
                || useAction == UseAction.BLOCK
                || useAction == UseAction.SPEAR
                || item == Items.SHEARS
                || item == Items.CARROT_ON_A_STICK
                || item == Items.WARPED_FUNGUS_ON_A_STICK;
    }

    private boolean isNewModeTorch(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.TORCH || item == Items.SOUL_TORCH || item == Items.REDSTONE_TORCH;
    }

    private boolean isNewModeThinItem(ItemStack stack) {
        Item item = stack.getItem();
        Block block = Block.getBlockFromItem(item);
        return item == Items.STRING
                || item == Items.REDSTONE
                || item == Items.LEVER
                || item == Items.TRIPWIRE_HOOK
                || block.isIn(BlockTags.RAILS)
                || block.isIn(BlockTags.CLIMBABLE)
                || block.isIn(BlockTags.DOORS)
                || block.isIn(BlockTags.BUTTONS)
                || block.isIn(BlockTags.PRESSURE_PLATES);
    }

    private boolean isNewModeSmallItem(ItemStack stack) {
        Item item = stack.getItem();
        return item.isFood() || item instanceof BucketItem || (!isNewModeWeaponLike(stack) && !(item instanceof BlockItem));
    }

    private void applyNewModeCommonItemAnchor(MatrixStack matrixStackIn, HandSide side) {
        int direction = side == HandSide.RIGHT ? 1 : -1;
        matrixStackIn.translate(-0.3D * direction, 0.65D, -0.1D);
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(-65.0F * direction));
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(10.0F));
    }

    private void applyNewModeEatDrinkHandPose(MatrixStack matrixStackIn, HandSide side, float useTicks) {
        int direction = side == HandSide.RIGHT ? 1 : -1;
        float progress = MathHelper.clamp(useTicks / 5.0F, 0.0F, 1.0F);
        float wobble = MathHelper.sin(useTicks / 2.0F * (float) Math.PI) / 10.0F;

        matrixStackIn.translate((double) direction, 0.1D, 0.3D);
        matrixStackIn.translate(0.2D * direction * progress, -0.7D * progress, -0.2D * progress);
        matrixStackIn.translate(0.0D, -0.2D * wobble, -0.2D * wobble);
        matrixStackIn.translate(0.0D, 0.1D * easeInOutBack(MathHelper.sin(progress * (float) Math.PI)), 0.0D);
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(45.0F * direction));
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(-40.0F * direction));
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(30.0F));
        matrixStackIn.scale(0.9F, 0.9F, 0.9F);
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(45.0F * progress * direction));
    }

    private void applyNewModeBlockHandPose(MatrixStack matrixStackIn, HandSide side, float useTicks) {
        int direction = side == HandSide.RIGHT ? 1 : -1;
        float blockProgress = MathHelper.clamp(useTicks / 4.0F, 0.0F, 1.0F);
        float settleProgress = MathHelper.clamp(useTicks / 6.0F, 0.0F, 1.0F);

        matrixStackIn.translate(0.0D, -0.2D, 0.0D);
        matrixStackIn.translate((double) direction, 0.0D, 0.3D);
        matrixStackIn.translate(0.7D * blockProgress * direction, 0.0D, -1.3D * blockProgress);
        matrixStackIn.translate(-0.2D * direction * settleProgress, 0.0D, 0.0D);
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(10.0F * MathHelper.sin(settleProgress * (float) Math.PI)));
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(70.0F * blockProgress * direction));
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(45.0F * direction));
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(-40.0F * direction));
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(30.0F));
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(5.0F * direction * blockProgress));
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-10.0F * blockProgress));
        matrixStackIn.translate(0.0D, 0.0D, -0.2D * blockProgress);
        matrixStackIn.scale(0.9F, 0.9F, 0.9F);
    }

    private void applyNewModeItemPose(MatrixStack matrixStackIn, HandSide side, ItemStack stack) {
        int direction = side == HandSide.RIGHT ? 1 : -1;

        if (isNewModeTorch(stack)) {
            matrixStackIn.scale(1.5F, 1.5F, 1.5F);
            matrixStackIn.rotate(Vector3f.YN.rotationDegrees(25.0F * direction));
            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(5.0F));
            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(75.0F * direction));
            matrixStackIn.translate(0.2D * direction, 0.2D, 0.05D);
            return;
        }

        if (isNewModeSmallItem(stack)) {
            matrixStackIn.rotate(Vector3f.YN.rotationDegrees(5.0F * direction));
            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(15.0F));
            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(75.0F * direction));
            matrixStackIn.translate(0.0D, -0.05D, -0.1D);
            matrixStackIn.scale(0.7F, 0.7F, 0.7F);
            return;
        }

        if (isNewModeThinItem(stack)) {
            matrixStackIn.translate(0.0D, 0.0D, -0.1D);
            matrixStackIn.rotate(Vector3f.YN.rotationDegrees(5.0F * direction));
            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(15.0F));
            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(75.0F * direction));
            return;
        }

        if (stack.getItem() instanceof BlockItem) {
            matrixStackIn.rotate(Vector3f.YN.rotationDegrees(25.0F * direction));
            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(5.0F));
            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(75.0F * direction));
            matrixStackIn.translate(0.2D * direction, 0.2D, 0.05D);
            return;
        }

        if (stack.getUseAction() == UseAction.BLOCK) {
            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(160.0F * direction));
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(-60.0F * direction));
            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-70.0F));
            matrixStackIn.scale(0.75F, 0.75F, 0.75F);
            matrixStackIn.translate(0.15D * direction, side == HandSide.RIGHT ? 0.35D : 0.45D, side == HandSide.RIGHT ? -0.15D : -0.1D);
            matrixStackIn.translate(0.17D * direction, 0.0D, 0.3D);
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(-90.0F * direction));
            return;
        }

        if (stack.getUseAction() == UseAction.SPEAR) {
            matrixStackIn.rotate(Vector3f.YN.rotationDegrees(75.0F * direction));
            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(90.0F));
            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(45.0F * direction));
            matrixStackIn.translate(-0.3D * direction, 0.0D, 0.0D);
        } else {
            matrixStackIn.rotate(Vector3f.YN.rotationDegrees(75.0F * direction));
            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(70.0F));
            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(45.0F * direction));
        }

        matrixStackIn.scale(1.2F, 1.2F, 1.2F);

        if (stack.getUseAction() == UseAction.BOW) {
            matrixStackIn.translate(-0.1D * direction, -0.2D, 0.0D);
        }
    }

    private boolean renderSwordAnimationsNew(AbstractClientPlayerEntity player, float partialTicks, Hand handIn, float swingProgress, ItemStack stack, float equippedProgress, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn) {
        SwordAnimations swordAnimations = this.getSwordAnimationsNewMode();
        if (swordAnimations == null || player.isSpinAttacking()) {
            return false;
        }

        if (handIn == Hand.OFF_HAND) {
            return this.renderNewModeOffhand(player, partialTicks, swingProgress, stack, equippedProgress, matrixStackIn, bufferIn, combinedLightIn);
        }

        if (stack.isEmpty()) {
            return this.renderNewModeEmptyHand(player, player.getPrimaryHand(), swingProgress, equippedProgress, matrixStackIn, bufferIn, combinedLightIn);
        }

        if (stack.getItem() instanceof FilledMapItem || stack.getItem() instanceof CrossbowItem) {
            return false;
        }

        HandSide handside = player.getPrimaryHand();
        boolean rightHand = handside == HandSide.RIGHT;
        boolean usingThisHand = player.isHandActive() && player.getItemInUseCount() > 0 && player.getActiveHand() == handIn;

        if (usingThisHand) {
            float useTicks = (float) stack.getUseDuration() - ((float) player.getItemInUseCount() - partialTicks + 1.0F);

            switch (stack.getUseAction()) {
                case EAT:
                case DRINK:
                    this.applyNewModeEatDrinkHandPose(matrixStackIn, handside, useTicks);
                    this.renderNewModeArm(matrixStackIn, bufferIn, combinedLightIn, handside, 0.0F, swingProgress);
                    break;

                case BLOCK:
                    this.applyNewModeBlockHandPose(matrixStackIn, handside, useTicks);
                    this.renderNewModeArm(matrixStackIn, bufferIn, combinedLightIn, handside, 0.0F, swingProgress);
                    break;

                default:
                    return false;
            }
        } else {
            EventSwingAnimation eventSwingAnimation = new EventSwingAnimation(player, swingProgress, handIn, matrixStackIn);
            EventManager.call(eventSwingAnimation);
            this.applyNewModeHandBase(matrixStackIn, equippedProgress, handside);
            this.renderNewModeArm(matrixStackIn, bufferIn, combinedLightIn, handside, 0.0F, 0.0F);
        }

        this.applyNewModeCommonItemAnchor(matrixStackIn, handside);
        this.applyNewModeItemPose(matrixStackIn, handside, stack);
        this.renderItemSide(player, stack, rightHand ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !rightHand, matrixStackIn, bufferIn, combinedLightIn);
        return true;
    }

    public void renderItemSide(LivingEntity livingEntityIn, ItemStack itemStackIn, ItemCameraTransforms.TransformType transformTypeIn, boolean leftHand, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn) {
        CustomItems.setRenderOffHand(leftHand);

        if (!itemStackIn.isEmpty()) {
            this.itemRenderer.renderItem(livingEntityIn, itemStackIn, transformTypeIn, leftHand, matrixStackIn, bufferIn, livingEntityIn.world, combinedLightIn, OverlayTexture.NO_OVERLAY);
        }

        CustomItems.setRenderOffHand(false);
    }

    /**
     * Return the angle to render the Map
     */
    private float getMapAngleFromPitch(float pitch) {
        float f = 1.0F - pitch / 45.0F + 0.1F;
        f = MathHelper.clamp(f, 0.0F, 1.0F);
        return -MathHelper.cos(f * (float) Math.PI) * 0.5F + 0.5F;
    }

    private void renderArm(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, HandSide side) {
        Bot bot1 = null;

        for (Bot bot : BotManager.allBots) {
            if (this.mc.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
            }
        }
        this.mc.getTextureManager().bindTexture(bot1 != null ? bot1.connection.bot.getLocationSkin() : this.mc.player.getLocationSkin());
        PlayerRenderer playerrenderer = (PlayerRenderer) this.renderManager.getRenderer((bot1 != null ? bot1.connection.bot : this.mc.player));
        matrixStackIn.push();
        float f = side == HandSide.RIGHT ? 1.0F : -1.0F;
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(92.0F));
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(45.0F));
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(f * -41.0F));
        matrixStackIn.translate((double) (f * 0.3F), (double) -1.1F, (double) 0.45F);

        if (side == HandSide.RIGHT) {
            playerrenderer.renderRightArm(matrixStackIn, bufferIn, combinedLightIn, (AbstractClientPlayerEntity) (bot1 != null ? bot1.connection.bot : this.mc.player));
        } else {
            playerrenderer.renderLeftArm(matrixStackIn, bufferIn, combinedLightIn, (AbstractClientPlayerEntity) (bot1 != null ? bot1.connection.bot : this.mc.player));
        }

        matrixStackIn.pop();
    }

    private void renderMapFirstPersonSide(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, float equippedProgress, HandSide handIn, float swingProgress, ItemStack stack) {
        float f = handIn == HandSide.RIGHT ? 1.0F : -1.0F;
        matrixStackIn.translate((double) (f * 0.125F), -0.125D, 0.0D);
        Bot bot1 = null;

        for (Bot bot : BotManager.allBots) {
            if (this.mc.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
            }
        }
        if (!this.mc.player.isInvisible() || (bot1 != null && !bot1.connection.bot.isInvisible())) {
            matrixStackIn.push();
            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(f * 10.0F));
            this.renderArmFirstPerson(matrixStackIn, bufferIn, combinedLightIn, equippedProgress, swingProgress, handIn);
            matrixStackIn.pop();
        }

        matrixStackIn.push();
        matrixStackIn.translate((double) (f * 0.51F), (double) (-0.08F + equippedProgress * -1.2F), -0.75D);
        float f1 = MathHelper.sqrt(swingProgress);
        float f2 = MathHelper.sin(f1 * (float) Math.PI);
        float f3 = -0.5F * f2;
        float f4 = 0.4F * MathHelper.sin(f1 * ((float) Math.PI * 2F));
        float f5 = -0.3F * MathHelper.sin(swingProgress * (float) Math.PI);
        matrixStackIn.translate((double) (f * f3), (double) (f4 - 0.3F * f2), (double) f5);
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f2 * -45.0F));
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(f * f2 * -30.0F));
        this.renderMapFirstPerson(matrixStackIn, bufferIn, combinedLightIn, stack);
        matrixStackIn.pop();
    }

    private void renderMapFirstPerson(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, float pitch, float equippedProgress, float swingProgress) {
        float f = MathHelper.sqrt(swingProgress);
        float f1 = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
        float f2 = -0.4F * MathHelper.sin(f * (float) Math.PI);
        matrixStackIn.translate(0.0D, (double) (-f1 / 2.0F), (double) f2);
        float f3 = this.getMapAngleFromPitch(pitch);
        matrixStackIn.translate(0.0D, (double) (0.04F + equippedProgress * -1.2F + f3 * -0.5F), (double) -0.72F);
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f3 * -85.0F));
        Bot bot1 = null;

        for (Bot bot : BotManager.allBots) {
            if (this.mc.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
            }
        }
        if (!this.mc.player.isInvisible() || (bot1 != null && !bot1.connection.bot.isInvisible())) {
            matrixStackIn.push();
            matrixStackIn.rotate(Vector3f.YP.rotationDegrees(90.0F));
            this.renderArm(matrixStackIn, bufferIn, combinedLightIn, HandSide.RIGHT);
            this.renderArm(matrixStackIn, bufferIn, combinedLightIn, HandSide.LEFT);
            matrixStackIn.pop();
        }

        float f4 = MathHelper.sin(f * (float) Math.PI);
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f4 * 20.0F));
        matrixStackIn.scale(2.0F, 2.0F, 2.0F);
        this.renderMapFirstPerson(matrixStackIn, bufferIn, combinedLightIn, this.itemStackMainHand);
    }

    private void renderMapFirstPerson(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, ItemStack stack) {
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(180.0F));
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(180.0F));
        matrixStackIn.scale(0.38F, 0.38F, 0.38F);
        matrixStackIn.translate(-0.5D, -0.5D, 0.0D);
        matrixStackIn.scale(0.0078125F, 0.0078125F, 0.0078125F);
        World world = this.mc.world;

        for (Bot bot : BotManager.allBots) {
            if (this.mc.renderViewEntity == bot.connection.bot) {
                world = bot.connection.getWorld();
            }
        }

        MapData mapdata = FilledMapItem.getMapData(stack, world);
        IVertexBuilder ivertexbuilder = bufferIn.getBuffer(mapdata == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD);
        Matrix4f matrix4f = matrixStackIn.getLast().getMatrix();
        ivertexbuilder.pos(matrix4f, -7.0F, 135.0F, 0.0F).color(255, 255, 255, 255).tex(0.0F, 1.0F).lightmap(combinedLightIn).endVertex();
        ivertexbuilder.pos(matrix4f, 135.0F, 135.0F, 0.0F).color(255, 255, 255, 255).tex(1.0F, 1.0F).lightmap(combinedLightIn).endVertex();
        ivertexbuilder.pos(matrix4f, 135.0F, -7.0F, 0.0F).color(255, 255, 255, 255).tex(1.0F, 0.0F).lightmap(combinedLightIn).endVertex();
        ivertexbuilder.pos(matrix4f, -7.0F, -7.0F, 0.0F).color(255, 255, 255, 255).tex(0.0F, 0.0F).lightmap(combinedLightIn).endVertex();

        if (mapdata != null) {
            this.mc.gameRenderer.getMapItemRenderer().renderMap(matrixStackIn, bufferIn, mapdata, false, combinedLightIn);
        }
    }

    private void renderArmFirstPerson(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, float equippedProgress, float swingProgress, HandSide side) {
        boolean flag = side != HandSide.LEFT;
        float f = flag ? 1.0F : -1.0F;
        float f1 = MathHelper.sqrt(swingProgress);
        float f2 = -0.3F * MathHelper.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * MathHelper.sin(f1 * ((float) Math.PI * 2F));
        float f4 = -0.4F * MathHelper.sin(swingProgress * (float) Math.PI);
        matrixStackIn.translate((double) (f * (f2 + 0.64000005F)), (double) (f3 + -0.6F + equippedProgress * -0.6F), (double) (f4 + -0.71999997F));
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(f * 45.0F));
        float f5 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f6 = MathHelper.sin(f1 * (float) Math.PI);
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(f * f6 * 70.0F));
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(f * f5 * -20.0F));
        Bot bot1 = null;

        for (Bot bot : BotManager.allBots) {
            if (this.mc.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
            }
        }
        AbstractClientPlayerEntity abstractclientplayerentity = (AbstractClientPlayerEntity) (bot1 != null ? bot1.connection.bot : this.mc.player);
        this.mc.getTextureManager().bindTexture(abstractclientplayerentity.getLocationSkin());
        matrixStackIn.translate((double) (f * -1.0F), (double) 3.6F, 3.5D);
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(f * 120.0F));
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(200.0F));

        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(f * -135.0F));
        matrixStackIn.translate((double) (f * 5.6F), 0.0D, 0.0D);
        PlayerRenderer playerrenderer = (PlayerRenderer) this.renderManager.<AbstractClientPlayerEntity>getRenderer(abstractclientplayerentity);

        if (flag) {
            playerrenderer.renderRightArm(matrixStackIn, bufferIn, combinedLightIn, abstractclientplayerentity);
        } else {
            playerrenderer.renderLeftArm(matrixStackIn, bufferIn, combinedLightIn, abstractclientplayerentity);
        }
    }

    private void transformEatFirstPerson(MatrixStack matrixStackIn, float partialTicks, HandSide handIn, ItemStack stack) {
        Bot bot1 = null;

        for (Bot bot : BotManager.allBots) {
            if (this.mc.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
            }
        }
        float f = bot1 != null ? (float) bot1.connection.bot.getItemInUseCount() - partialTicks + 1.0F : (float) this.mc.player.getItemInUseCount() - partialTicks + 1.0F;
        float f1 = f / (float) stack.getUseDuration();

        if (f1 < 0.8F) {
            float f2 = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            matrixStackIn.translate(0.0D, (double) f2, 0.0D);
        }

        float f3 = 1.0F - (float) Math.pow((double) f1, 27.0D);
        int i = handIn == HandSide.RIGHT ? 1 : -1;
        matrixStackIn.translate((double) (f3 * 0.6F * (float) i), (double) (f3 * -0.5F), (double) (f3 * 0.0F));
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees((float) i * f3 * 90.0F));
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f3 * 10.0F));
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees((float) i * f3 * 30.0F));
    }

    private void transformFirstPerson(MatrixStack matrixStackIn, HandSide handIn, float swingProgress) {
        int i = handIn == HandSide.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        float f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees((float) i * f1 * -20.0F));
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(f1 * -80.0F));
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees((float) i * -45.0F));
    }

    private void transformSideFirstPerson(MatrixStack matrixStackIn, HandSide handIn, float equippedProg) {
        int i = handIn == HandSide.RIGHT ? 1 : -1;
        EventTransformSideFirstPerson event = new EventTransformSideFirstPerson(handIn, equippedProg);
        EventManager.call(event);
        matrixStackIn.translate((double) ((float) i * 0.56F), (double) (-0.52F + equippedProg * -event.getEquippedProg()), (double) -0.72F);
    }

    public void renderItemInFirstPerson(float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer.Impl bufferIn, ClientPlayerEntity playerEntityIn, int combinedLightIn)
    {        float f = playerEntityIn.getSwingProgress(partialTicks);
        Hand hand = MoreObjects.firstNonNull(playerEntityIn.swingingHand, Hand.MAIN_HAND);
        float f1 = MathHelper.lerp(partialTicks, playerEntityIn.prevRotationPitch, playerEntityIn.rotationPitch);
        boolean flag = true;
        boolean flag1 = true;

        if (playerEntityIn.isHandActive()) {
            ItemStack itemstack = playerEntityIn.getActiveItemStack();

            if (itemstack.getItem() instanceof ShootableItem) {
                flag = playerEntityIn.getActiveHand() == Hand.MAIN_HAND;
                flag1 = !flag;
            }

            Hand hand1 = playerEntityIn.getActiveHand();

            if (hand1 == Hand.MAIN_HAND) {
                ItemStack itemstack1 = playerEntityIn.getHeldItemOffhand();

                if (itemstack1.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack1)) {
                    flag1 = false;
                }
            }
        } else {
            ItemStack itemstack2 = playerEntityIn.getHeldItemMainhand();
            ItemStack itemstack3 = playerEntityIn.getHeldItemOffhand();

            if (itemstack2.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack2)) {
                flag1 = !flag;
            }

            if (itemstack3.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack3)) {
                flag = !itemstack2.isEmpty();
                flag1 = !flag;
            }
        }

        float f3 = MathHelper.lerp(partialTicks, playerEntityIn.prevRenderArmPitch, playerEntityIn.renderArmPitch);
        float f4 = MathHelper.lerp(partialTicks, playerEntityIn.prevRenderArmYaw, playerEntityIn.renderArmYaw);

//        matrixStackIn.rotate(Vector3f.XP.rotationDegrees((LookHandler.getFreePitch() - f3) * 0.1F));
//        matrixStackIn.rotate(Vector3f.YP.rotationDegrees((LookHandler.getFreeYaw() - f4) * 0.1F));

        if (flag) {
            float f5 = hand == Hand.MAIN_HAND ? f : 0.0F;
            float f2 = 1.0F - MathHelper.lerp(partialTicks, this.prevEquippedProgressMainHand, this.equippedProgressMainHand);

            if (!Reflector.ForgeHooksClient_renderSpecificFirstPersonHand.exists() || !Reflector.callBoolean(Reflector.ForgeHooksClient_renderSpecificFirstPersonHand, Hand.MAIN_HAND, matrixStackIn, bufferIn, combinedLightIn, partialTicks, f1, f5, f2, this.itemStackMainHand)) {
                this.renderItemInFirstPerson(playerEntityIn, partialTicks, f1, Hand.MAIN_HAND, f5, this.itemStackMainHand, f2, matrixStackIn, bufferIn, combinedLightIn);
            }
        }

        if (flag1) {
            float f6 = hand == Hand.OFF_HAND ? f : 0.0F;
            float f7 = 1.0F - MathHelper.lerp(partialTicks, this.prevEquippedProgressOffHand, this.equippedProgressOffHand);

            if (!Reflector.ForgeHooksClient_renderSpecificFirstPersonHand.exists() || !Reflector.callBoolean(Reflector.ForgeHooksClient_renderSpecificFirstPersonHand, Hand.OFF_HAND, matrixStackIn, bufferIn, combinedLightIn, partialTicks, f1, f6, f7, this.itemStackOffHand)) {
                this.renderItemInFirstPerson(playerEntityIn, partialTicks, f1, Hand.OFF_HAND, f6, this.itemStackOffHand, f7, matrixStackIn, bufferIn, combinedLightIn);
            }
        }

        bufferIn.finish();
    }
    public void renderItemInFirstPerson(float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer.Impl bufferIn, BotPlayer playerEntityIn, int combinedLightIn) {
        float f = playerEntityIn.getSwingProgress(partialTicks);
        Hand hand = (Hand)MoreObjects.firstNonNull(playerEntityIn.swingingHand, Hand.MAIN_HAND);
        float f1 = MathHelper.lerp(partialTicks, playerEntityIn.prevRotationPitch, playerEntityIn.rotationPitch);
        boolean flag = true;
        boolean flag1 = true;
        if (playerEntityIn.isHandActive()) {
            ItemStack itemstack = playerEntityIn.getActiveItemStack();
            if (itemstack.getItem() instanceof ShootableItem) {
                flag = playerEntityIn.getActiveHand() == Hand.MAIN_HAND;
                flag1 = !flag;
            }

            Hand hand1 = playerEntityIn.getActiveHand();
            if (hand1 == Hand.MAIN_HAND) {
                ItemStack itemstack1 = playerEntityIn.getHeldItemOffhand();
                if (itemstack1.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack1)) {
                    flag1 = false;
                }
            }
        } else {
            ItemStack itemstack2 = playerEntityIn.getHeldItemMainhand();
            ItemStack itemstack3 = playerEntityIn.getHeldItemOffhand();
            if (itemstack2.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack2)) {
                flag1 = !flag;
            }

            if (itemstack3.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack3)) {
                flag = !itemstack2.isEmpty();
                flag1 = !flag;
            }
        }

        float f3 = MathHelper.lerp(partialTicks, playerEntityIn.prevRenderArmPitch, playerEntityIn.renderArmPitch);
        float f4 = MathHelper.lerp(partialTicks, playerEntityIn.prevRenderArmYaw, playerEntityIn.renderArmYaw);
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees((playerEntityIn.getPitch(partialTicks) - f3) * 0.1F));
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees((playerEntityIn.getYaw(partialTicks) - f4) * 0.1F));
        if (flag) {
            float f5 = hand == Hand.MAIN_HAND ? f : 0.0F;
            float f2 = 1.0F - MathHelper.lerp(partialTicks, this.prevEquippedProgressMainHand, this.equippedProgressMainHand);
            if (!Reflector.ForgeHooksClient_renderSpecificFirstPersonHand.exists() || !Reflector.callBoolean(Reflector.ForgeHooksClient_renderSpecificFirstPersonHand, new Object[]{Hand.MAIN_HAND, matrixStackIn, bufferIn, combinedLightIn, partialTicks, f1, f5, f2, this.itemStackMainHand})) {
                this.renderItemInFirstPerson(playerEntityIn, partialTicks, f1, Hand.MAIN_HAND, f5, this.itemStackMainHand, f2, matrixStackIn, bufferIn, combinedLightIn);
            }
        }

        if (flag1) {
            float f6 = hand == Hand.OFF_HAND ? f : 0.0F;
            float f7 = 1.0F - MathHelper.lerp(partialTicks, this.prevEquippedProgressOffHand, this.equippedProgressOffHand);
            if (!Reflector.ForgeHooksClient_renderSpecificFirstPersonHand.exists() || !Reflector.callBoolean(Reflector.ForgeHooksClient_renderSpecificFirstPersonHand, new Object[]{Hand.OFF_HAND, matrixStackIn, bufferIn, combinedLightIn, partialTicks, f1, f6, f7, this.itemStackOffHand})) {
                this.renderItemInFirstPerson(playerEntityIn, partialTicks, f1, Hand.OFF_HAND, f6, this.itemStackOffHand, f7, matrixStackIn, bufferIn, combinedLightIn);
            }
        }

        bufferIn.finish();
    }
    private void renderItemInFirstPerson(AbstractClientPlayerEntity player, float partialTicks, float pitch, Hand handIn, float swingProgress, ItemStack stack, float equippedProgress, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn) {
        Bot bot1 = null;

        for (Bot bot : BotManager.allBots) {
            if (this.mc.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
            }
        }
        if (!Config.isShaders() || !Shaders.isSkipRenderHand(handIn)) {
            boolean flag = handIn == Hand.MAIN_HAND;
            HandSide handside = flag ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
            matrixStackIn.push();
            EventManager.call(new EventViewModel(matrixStackIn, handside));
            if (this.renderSwordAnimationsNew(player, partialTicks, handIn, swingProgress, stack, equippedProgress, matrixStackIn, bufferIn, combinedLightIn)) {
                matrixStackIn.pop();
                return;
            }
            if (stack.isEmpty()) {
                if (flag && !player.isInvisible()) {
                    this.renderArmFirstPerson(matrixStackIn, bufferIn, combinedLightIn, equippedProgress, swingProgress, handside);
                }
            } else if (stack.getItem() instanceof FilledMapItem) {
                if (flag && this.itemStackOffHand.isEmpty()) {
                    this.renderMapFirstPerson(matrixStackIn, bufferIn, combinedLightIn, pitch, equippedProgress, swingProgress);
                } else {
                    this.renderMapFirstPersonSide(matrixStackIn, bufferIn, combinedLightIn, equippedProgress, handside, swingProgress, stack);
                }
            } else if (stack.getItem() instanceof CrossbowItem) {
                boolean flag1 = CrossbowItem.isCharged(stack);
                boolean flag2 = handside == HandSide.RIGHT;
                int i = flag2 ? 1 : -1;

                if (player.isHandActive() && player.getItemInUseCount() > 0 && player.getActiveHand() == handIn) {
                    this.transformSideFirstPerson(matrixStackIn, handside, equippedProgress);
                    matrixStackIn.translate((double) ((float) i * -0.4785682F), (double) -0.094387F, (double) 0.05731531F);
                    matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-11.935F));
                    matrixStackIn.rotate(Vector3f.YP.rotationDegrees((float) i * 65.3F));
                    matrixStackIn.rotate(Vector3f.ZP.rotationDegrees((float) i * -9.785F));
                    float f9 = (float) stack.getUseDuration() - (bot1 != null ? (float) bot1.connection.bot.getItemInUseCount() - partialTicks + 1.0F : (float) this.mc.player.getItemInUseCount() - partialTicks + 1.0F);
                    float f12 = f9 / (float) CrossbowItem.getChargeTime(stack);

                    if (f12 > 1.0F) {
                        f12 = 1.0F;
                    }

                    if (f12 > 0.1F) {
                        float f15 = MathHelper.sin((f9 - 0.1F) * 1.3F);
                        float f3 = f12 - 0.1F;
                        float f4 = f15 * f3;
                        matrixStackIn.translate((double) (f4 * 0.0F), (double) (f4 * 0.004F), (double) (f4 * 0.0F));
                    }

                    matrixStackIn.translate((double) (f12 * 0.0F), (double) (f12 * 0.0F), (double) (f12 * 0.04F));
                    matrixStackIn.scale(1.0F, 1.0F, 1.0F + f12 * 0.2F);
                    matrixStackIn.rotate(Vector3f.YN.rotationDegrees((float) i * 45.0F));
                } else {
                    float f = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float) Math.PI * 2F));
                    float f2 = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                    matrixStackIn.translate((double) ((float) i * f), (double) f1, (double) f2);
                    this.transformSideFirstPerson(matrixStackIn, handside, equippedProgress);
                    this.transformFirstPerson(matrixStackIn, handside, swingProgress);

                    if (flag1 && swingProgress < 0.001F) {
                        matrixStackIn.translate((double) ((float) i * -0.641864F), 0.0D, 0.0D);
                        matrixStackIn.rotate(Vector3f.YP.rotationDegrees((float) i * 10.0F));
                    }
                }

                this.renderItemSide(player, stack, flag2 ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !flag2, matrixStackIn, bufferIn, combinedLightIn);
            } else {
                boolean flag3 = handside == HandSide.RIGHT;

                if (player.isHandActive() && player.getItemInUseCount() > 0 && player.getActiveHand() == handIn) {
                    int k = flag3 ? 1 : -1;

                    switch (stack.getUseAction()) {
                        case NONE:
                            this.transformSideFirstPerson(matrixStackIn, handside, equippedProgress);
                            break;

                        case EAT:
                        case DRINK:
                            this.transformEatFirstPerson(matrixStackIn, partialTicks, handside, stack);
                            this.transformSideFirstPerson(matrixStackIn, handside, equippedProgress);
                            break;

                        case BLOCK:
                            this.transformSideFirstPerson(matrixStackIn, handside, equippedProgress);
                            break;

                        case BOW:
                            this.transformSideFirstPerson(matrixStackIn, handside, equippedProgress);
                            matrixStackIn.translate((double) ((float) k * -0.2785682F), (double) 0.18344387F, (double) 0.15731531F);
                            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-13.935F));
                            matrixStackIn.rotate(Vector3f.YP.rotationDegrees((float) k * 35.3F));
                            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees((float) k * -9.785F));
                            float f8 = (float) stack.getUseDuration() - (bot1 != null ? (float) bot1.connection.bot.getItemInUseCount() - partialTicks + 1.0F : (float) this.mc.player.getItemInUseCount() - partialTicks + 1.0F);
                            float f11 = f8 / 20.0F;
                            f11 = (f11 * f11 + f11 * 2.0F) / 3.0F;

                            if (f11 > 1.0F) {
                                f11 = 1.0F;
                            }

                            if (f11 > 0.1F) {
                                float f14 = MathHelper.sin((f8 - 0.1F) * 1.3F);
                                float f17 = f11 - 0.1F;
                                float f19 = f14 * f17;
                                matrixStackIn.translate((double) (f19 * 0.0F), (double) (f19 * 0.004F), (double) (f19 * 0.0F));
                            }

                            matrixStackIn.translate((double) (f11 * 0.0F), (double) (f11 * 0.0F), (double) (f11 * 0.04F));
                            matrixStackIn.scale(1.0F, 1.0F, 1.0F + f11 * 0.2F);
                            matrixStackIn.rotate(Vector3f.YN.rotationDegrees((float) k * 45.0F));
                            break;

                        case SPEAR:
                            this.transformSideFirstPerson(matrixStackIn, handside, equippedProgress);
                            matrixStackIn.translate((double) ((float) k * -0.5F), (double) 0.7F, (double) 0.1F);
                            matrixStackIn.rotate(Vector3f.XP.rotationDegrees(-55.0F));
                            matrixStackIn.rotate(Vector3f.YP.rotationDegrees((float) k * 35.3F));
                            matrixStackIn.rotate(Vector3f.ZP.rotationDegrees((float) k * -9.785F));
                            float f13 = (float) stack.getUseDuration() - (bot1 != null ? (float) bot1.connection.bot.getItemInUseCount() - partialTicks + 1.0F : (float) this.mc.player.getItemInUseCount() - partialTicks + 1.0F);
                            float f16 = f13 / 10.0F;

                            if (f16 > 1.0F) {
                                f16 = 1.0F;
                            }

                            if (f16 > 0.1F) {
                                float f18 = MathHelper.sin((f13 - 0.1F) * 1.3F);
                                float f20 = f16 - 0.1F;
                                float f5 = f18 * f20;
                                matrixStackIn.translate((double) (f5 * 0.0F), (double) (f5 * 0.004F), (double) (f5 * 0.0F));
                            }

                            matrixStackIn.translate(0.0D, 0.0D, (double) (f16 * 0.2F));
                            matrixStackIn.scale(1.0F, 1.0F, 1.0F + f16 * 0.2F);
                            matrixStackIn.rotate(Vector3f.YN.rotationDegrees((float) k * 45.0F));
                    }
                } else if (player.isSpinAttacking()) {
                    this.transformSideFirstPerson(matrixStackIn, handside, equippedProgress);
                    int j = flag3 ? 1 : -1;
                    matrixStackIn.translate((double) ((float) j * -0.4F), (double) 0.8F, (double) 0.3F);
                    matrixStackIn.rotate(Vector3f.YP.rotationDegrees((float) j * 65.0F));
                    matrixStackIn.rotate(Vector3f.ZP.rotationDegrees((float) j * -85.0F));
                } else {
                    this.transformSideFirstPerson(matrixStackIn, handside, equippedProgress);
                    EventSwingAnimation eventSwingAnimation = new EventSwingAnimation(player, swingProgress, handIn, matrixStackIn);
                    EventManager.call(eventSwingAnimation);
                    float f6 = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float f7 = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float) Math.PI * 2F));
                    float f10 = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                    int l = flag3 ? 1 : -1;
                    if (!eventSwingAnimation.isCancelled()) {
                        matrixStackIn.translate((double) ((float) l * f6), (double) f7, (double) f10);
                        this.transformFirstPerson(matrixStackIn, handside, swingProgress);
                    } else if (handside != eventSwingAnimation.getPlayer().getPrimaryHand()) {
                        matrixStackIn.translate((double) ((float) l * f6), (double) f7, (double) f10);
                        this.transformFirstPerson(matrixStackIn, handside, swingProgress);
                    }
                }

                this.renderItemSide(player, stack, flag3 ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !flag3, matrixStackIn, bufferIn, combinedLightIn);
            }

            matrixStackIn.pop();
        }
    }

    public void tick() {
        this.prevEquippedProgressMainHand = this.equippedProgressMainHand;
        this.prevEquippedProgressOffHand = this.equippedProgressOffHand;
        Bot bot1 = null;

        for (Bot bot : BotManager.allBots) {
            if (this.mc.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
            }
        }
        PlayerEntity clientplayerentity = (PlayerEntity) (bot1 != null ? bot1.connection.bot : this.mc.player);
        ItemStack itemstack = clientplayerentity.getHeldItemMainhand();
        ItemStack itemstack1 = clientplayerentity.getHeldItemOffhand();

        if (ItemStack.areItemStacksEqual(this.itemStackMainHand, itemstack)) {
            this.itemStackMainHand = itemstack;
        }

        if (ItemStack.areItemStacksEqual(this.itemStackOffHand, itemstack1)) {
            this.itemStackOffHand = itemstack1;
        }

        if (mc.player.isRowingBoat() || (bot1 != null && bot1.connection.bot.isRowingBoat())) {
            this.equippedProgressMainHand = MathHelper.clamp(this.equippedProgressMainHand - 0.4F, 0.0F, 1.0F);
            this.equippedProgressOffHand = MathHelper.clamp(this.equippedProgressOffHand - 0.4F, 0.0F, 1.0F);
        } else {
            float f = clientplayerentity.getCooledAttackStrength(1.0F);

            if (Reflector.ForgeHooksClient_shouldCauseReequipAnimation.exists()) {
                boolean flag = Reflector.callBoolean(Reflector.ForgeHooksClient_shouldCauseReequipAnimation, this.itemStackMainHand, itemstack, clientplayerentity.inventory.currentItem);
                boolean flag1 = Reflector.callBoolean(Reflector.ForgeHooksClient_shouldCauseReequipAnimation, this.itemStackOffHand, itemstack1, -1);

                if (!flag && !Objects.equals(this.itemStackMainHand, itemstack)) {
                    this.itemStackMainHand = itemstack;
                }

                if (!flag1 && !Objects.equals(this.itemStackOffHand, itemstack1)) {
                    this.itemStackOffHand = itemstack1;
                }
            }

            this.equippedProgressMainHand += MathHelper.clamp((this.itemStackMainHand == itemstack ? f * f * f : 0.0F) - this.equippedProgressMainHand, -0.4F, 0.4F);
            this.equippedProgressOffHand += MathHelper.clamp((float) (this.itemStackOffHand == itemstack1 ? 1 : 0) - this.equippedProgressOffHand, -0.4F, 0.4F);
        }

        if (this.equippedProgressMainHand < 0.1F) {
            this.itemStackMainHand = itemstack;

            if (Config.isShaders()) {
                Shaders.setItemToRenderMain(this.itemStackMainHand);
            }
        }

        if (this.equippedProgressOffHand < 0.1F) {
            this.itemStackOffHand = itemstack1;

            if (Config.isShaders()) {
                Shaders.setItemToRenderOff(this.itemStackOffHand);
            }
        }
    }

    public void resetEquippedProgress(Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            this.equippedProgressMainHand = 0.0F;
        } else {
            this.equippedProgressOffHand = 0.0F;
        }
    }
}
