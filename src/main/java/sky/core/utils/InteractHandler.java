package sky.core.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TieredItem;
import net.minecraft.item.ItemTier;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

import static sky.core.utils.Wrapper.mc;

public class InteractHandler {



    public void attack() {
        RayTraceResult target = mc.objectMouseOver;

        if (target instanceof EntityRayTraceResult) {
            EntityRayTraceResult entityHit = (EntityRayTraceResult) target;
            Entity entity = entityHit.getEntity();

            if (entity instanceof EnderCrystalEntity) {
                EnderCrystalEntity crystal = (EnderCrystalEntity) entity;
                if (this.canDestroyEndCrystal()) {
                    this.destroyEndCrystal(crystal);
                }
            }
        }
    }


    private boolean canDestroyEndCrystal() {
        PlayerEntity player = mc.player;
        if (player == null) return false;

        EffectInstance weakness = player.getActivePotionEffect(Effects.WEAKNESS);
        EffectInstance strength = player.getActivePotionEffect(Effects.STRENGTH);

        return weakness == null ||
                (strength != null && strength.getAmplifier() > weakness.getAmplifier()) ||
                this.isStrongTool(player.getHeldItemMainhand());
    }


    private boolean isStrongTool(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof TieredItem) {
            TieredItem tool = (TieredItem) stack.getItem();
            return tool.getTier() == ItemTier.DIAMOND || tool.getTier() == ItemTier.NETHERITE;
        }
        return false;
    }

    private void destroyEndCrystal(EnderCrystalEntity crystal) {
        crystal.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
        crystal.remove();
    }
}