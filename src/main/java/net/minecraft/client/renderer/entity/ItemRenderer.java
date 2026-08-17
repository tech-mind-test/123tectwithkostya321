package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.Random;

import sky.core.SkyCore;
import sky.core.modules.impl.visuals.ItemPhysics;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
// тут
public class ItemRenderer extends EntityRenderer<ItemEntity> {
    private final net.minecraft.client.renderer.ItemRenderer itemRenderer;
    private final Random random = new Random();

    public ItemRenderer(EntityRendererManager renderManagerIn,
                        net.minecraft.client.renderer.ItemRenderer itemRendererIn) {
        super(renderManagerIn);
        this.itemRenderer = itemRendererIn;
        this.shadowSize = 0.15F;
        this.shadowOpaque = 0.75F;
    }

    private static int getModelCount(ItemStack stack) {
        int count = stack.getCount();
        if (count > 48) return 5;
        if (count > 32) return 4;
        if (count > 16) return 3;
        if (count > 1)  return 2;
        return 1;
    }

    @Override
    public void render(ItemEntity entity, float entityYaw, float partialTicks,
                       MatrixStack matrixStack, IRenderTypeBuffer buffer, int light) {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) return;

        double distSq = renderManager.squareDistanceTo(entity);
        if (distSq > 4096.0D) {
            renderSingle(entity, entityYaw, partialTicks, matrixStack, buffer, light);
            return;
        }

        matrixStack.push();

        random.setSeed(Item.getIdFromItem(stack.getItem()) + stack.getDamage());
        IBakedModel model = itemRenderer.getItemModelWithOverrides(stack, entity.world, (LivingEntity) null);
        boolean is3d = model.isGui3d();
        int count = getModelCount(stack);

        ItemCameraTransforms transforms = model.getItemCameraTransforms();
        Vector3f scale = transforms.getTransform(TransformType.GROUND).scale;

        boolean physics = SkyCore.getInstance()
                .getModuleManager()
                .getModule(ItemPhysics.class)
                .isEnabled();

        float bob = shouldBob()
                ? MathHelper.sin((entity.getAge() + partialTicks) / 10.0F + entity.hoverStart) * 0.1F + 0.1F
                : 0.0F;

        if (!physics) {
            matrixStack.translate(0.0D, bob + 0.25F * scale.getY(), 0.0D);
            matrixStack.rotate(Vector3f.YP.rotation(entity.getItemHover(partialTicks)));
        } else {
            float angle = entity.isOnGround() ? 90.0F : entity.getItemHover(partialTicks) * 300.0F;
            matrixStack.rotate(Vector3f.XP.rotationDegrees(angle));
        }

        if (!is3d) {
            matrixStack.translate(0.0D, 0.0D, -0.09375F * (count - 1) * 0.5F * scale.getZ());
        }

        boolean spread3d = is3d && shouldSpreadItems();
        for (int i = 0; i < count; i++) {
            matrixStack.push();
            if (i > 0) {
                float dx = (random.nextFloat() * 2.0F - 1.0F) * (spread3d ? 0.15F : 0.075F);
                float dy = (random.nextFloat() * 2.0F - 1.0F) * (spread3d ? 0.15F : 0.075F);
                float dz = spread3d ? (random.nextFloat() * 2.0F - 1.0F) * 0.15F : 0.0F;
                matrixStack.translate(dx, dy, dz);
            }

            itemRenderer.renderItem(stack, TransformType.GROUND, false,
                    matrixStack, buffer, light, OverlayTexture.NO_OVERLAY, model);

            matrixStack.pop();
            if (!is3d) {
                matrixStack.translate(0.0D, 0.0D, 0.09375F * scale.getZ());
            }
        }

        matrixStack.pop();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, light);
    }

    private void renderSingle(ItemEntity entity, float entityYaw, float partialTicks,
                              MatrixStack matrixStack, IRenderTypeBuffer buffer, int light) {
        matrixStack.push();
        ItemStack stack = entity.getItem();
        random.setSeed(Item.getIdFromItem(stack.getItem()) + stack.getDamage());
        IBakedModel model = itemRenderer.getItemModelWithOverrides(stack, entity.world, (LivingEntity) null);
        itemRenderer.renderItem(stack, TransformType.GROUND, false,
                matrixStack, buffer, light, OverlayTexture.NO_OVERLAY, model);
        matrixStack.pop();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, light);
    }

    @Override
    public ResourceLocation getEntityTexture(ItemEntity entity) {
        return AtlasTexture.LOCATION_BLOCKS_TEXTURE;
    }

    public boolean shouldSpreadItems() {
        return true;
    }

    public boolean shouldBob() {
        return true;
    }
}