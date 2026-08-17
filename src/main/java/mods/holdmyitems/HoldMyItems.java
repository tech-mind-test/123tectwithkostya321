package mods.holdmyitems;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import sky.core.events.EventSwingAnimation;
import sky.core.events.EventSwingSpeed;
import sky.core.events.EventTransformSideFirstPerson;

/**
 * Native 1.16.5 implementation inspired by Hold My Items.
 * Lives in mods/* package as requested.
 */
public final class HoldMyItems {
    private HoldMyItems() {}

    public static boolean enabled = false;
    public static float power = 1.0F;
    public static float xOffset = 0.0F;
    public static float yOffset = 0.0F;
    public static float zOffset = 0.0F;
    public static float rotX = 0.0F;
    public static float rotY = 0.0F;
    public static float rotZ = 0.0F;
    public static float itemScale = 1.0F;
    public static float swingX = -80.0F;
    public static float swingY = -20.0F;
    public static float swingZ = -20.0F;
    public static int swingSpeed = 12;

    public static void onSwingSpeed(EventSwingSpeed event) {
        if (!enabled) return;
        event.setSwipeSpeed(swingSpeed);
        event.setCancelled(true);
    }

    public static void onTransform(EventTransformSideFirstPerson event) {
        if (!enabled) return;
        event.setEquippedProg(MathHelper.clamp(event.getEquippedProg() * 0.75F, 0.0F, 1.0F));
    }

    public static void onSwingAnimation(EventSwingAnimation event) {
        if (!enabled) return;
        if (Minecraft.getInstance().player == null) return;

        MatrixStack ms = event.getMatrixStack();
        boolean isLeft = Minecraft.getInstance().player.getPrimaryHand() == HandSide.LEFT;
        float dir = isLeft ? -1.0F : 1.0F;

        float sp = event.getSwingProgress();
        float anim = MathHelper.sin(sp * sp * (float) Math.PI);
        float anim2 = MathHelper.sin(MathHelper.sqrt(sp) * (float) Math.PI);

        // Keep vanilla base pose (do NOT cancel), then apply user offsets and animation.
        ms.translate((event.getHand() == Hand.OFF_HAND ? -xOffset : xOffset), yOffset, zOffset);
        ms.rotate(Vector3f.XP.rotationDegrees(rotX));
        ms.rotate(Vector3f.YP.rotationDegrees(event.getHand() == Hand.OFF_HAND ? -rotY : rotY));
        ms.rotate(Vector3f.ZP.rotationDegrees(event.getHand() == Hand.OFF_HAND ? -rotZ : rotZ));
        ms.scale(itemScale, itemScale, itemScale);

        ms.rotate(Vector3f.ZP.rotationDegrees(dir * anim * swingZ * power * 0.25F));
        ms.rotate(Vector3f.YP.rotationDegrees(dir * anim * swingY * power * 0.25F));
        ms.rotate(Vector3f.XP.rotationDegrees(anim2 * swingX * power));
    }
}

