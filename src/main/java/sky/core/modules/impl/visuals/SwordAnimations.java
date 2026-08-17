package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.*;
import sky.core.SkyCore;
import sky.core.events.EventSwingAnimation;
import sky.core.events.EventSwingSpeed;
import sky.core.events.EventTransformSideFirstPerson;
import sky.core.modules.Category;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.impl.combat.AttackAura;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import sky.core.modules.Module;

public class SwordAnimations extends Module {
    public final ModeSetting swordAnim = new ModeSetting("Мод", "Мод 1", "Мод 1", "Мод 2", "Мод 3", "Мод 4", "Мод 5", "Мод 6", "Мод 7", "360", "Slant", "3D", "Fade");
    public final SliderSetting angle = new SliderSetting("Угол", 100, 0, 360, 1, () -> swordAnim.is("Мод 2") || swordAnim.is("Мод 4"));
    public final SliderSetting swipePower = new SliderSetting("Сила взмаха", 8, 1, 10, 1, () -> swordAnim.is("Мод 1") || swordAnim.is("Мод 2") || swordAnim.is("Мод 3") || swordAnim.is("Мод 4") || swordAnim.is("Мод 6"));
    public final SliderSetting transform = new SliderSetting("Сила взмаха вниз", 0, 0, 1, 0.1F);
    public final SliderSetting swipeSpeed = new SliderSetting("Плавность взмаха", 11, 1, 20, 1);
    public final BooleanSetting onlyaura = new BooleanSetting("Только с активной AttackAura", false);
    public final BooleanSetting newShowLeftArm = new BooleanSetting("Левая рука", false, () -> swordAnim.is("3D"));
    private boolean newSlashReversed;
    private boolean newSwingInProgress;

    public SwordAnimations() {
        super("Sword Animations", "Позволяет изменить анимацию удара", Category.Visuals);
        addSettings(swordAnim, angle, swipePower, swipeSpeed, transform, newShowLeftArm, onlyaura);
    }

    private float easeInOutBack(float x) {
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        if (x < 0.5F) {
            return (float) (Math.pow(2.0F * x, 2.0D) * ((c2 + 1.0F) * 2.0F * x - c2) / 2.0D);
        }
        return (float) ((Math.pow(2.0F * x - 2.0F, 2.0D) * ((c2 + 1.0F) * (x * 2.0F - 2.0F) + c2) + 2.0D) / 2.0D);
    }

    private void updateNewSwingState(float swingProgress) {
        boolean swinging = swingProgress > 0.01F;
        if (swinging && !newSwingInProgress) {
            newSlashReversed = !newSlashReversed;
        }
        newSwingInProgress = swinging;
    }

    private void applyNewMode(EventSwingAnimation event, boolean isLeft) {
        ItemStack stack = event.getPlayer().getHeldItem(event.getHand());
        if (stack.isEmpty()) {
            return;
        }

        updateNewSwingState(event.getSwingProgress());

        MatrixStack matrixStack = event.getMatrixStack();
        float direction = isLeft ? -1.0F : 1.0F;
        float swingProgress = event.getSwingProgress();
        float swingRot = swingProgress < 0.6F ? MathHelper.sin(MathHelper.clamp(swingProgress, 0.0F, 0.12506F) * 12.56F) : MathHelper.sin(MathHelper.clamp(swingProgress, 0.62532F, 0.75038F) * 12.56F);
        float swing = easeInOutBack(MathHelper.sin(swingProgress * (float) Math.PI));

        boolean sword = stack.getItem() instanceof SwordItem;
        boolean axe = stack.getItem() instanceof AxeItem;
        boolean shovel = stack.getItem() instanceof ShovelItem;
        boolean tool = stack.getItem() instanceof ToolItem;
        boolean spear = stack.getUseAction() == UseAction.SPEAR;
        boolean block = stack.getUseAction() == UseAction.BLOCK;
        boolean primarySlash = newSlashReversed || axe || spear || block;
        if (primarySlash && !shovel) {
            if (sword || axe) {
                matrixStack.translate(0.8F * direction * swingRot, 0.3F * swingRot, -0.5F * swing);
                matrixStack.rotate(Vector3f.YP.rotationDegrees(15.0F * swingRot * direction));
                matrixStack.rotate(Vector3f.XN.rotationDegrees(-20.0F * swingRot));
                matrixStack.rotate(Vector3f.ZP.rotationDegrees(-70.0F * swingRot * direction));
                matrixStack.rotate(Vector3f.XN.rotationDegrees((sword ? 40.0F : 30.0F) * swing));
                return;
            }
            if (spear) {
                matrixStack.translate(0.0D, 0.0D, 0.45D * swingRot);
                matrixStack.translate(-0.25D * direction * swing, -0.35D * swingRot, -0.6D * swing);
                matrixStack.translate(0.0D, 0.1D * swing, 0.0D);
                matrixStack.rotate(Vector3f.YP.rotationDegrees(15.0F * swingRot * direction));
                matrixStack.rotate(Vector3f.ZP.rotationDegrees(30.0F * swingRot * direction));
                return;
            }
            if (tool && !block) {
                matrixStack.translate(0.1F * direction * swingRot, 0.1F * swingRot, -0.5F * swing);
                matrixStack.rotate(Vector3f.XN.rotationDegrees(-30.0F * swingRot));
                matrixStack.rotate(Vector3f.ZP.rotationDegrees(-20.0F * swingRot * direction));
                matrixStack.rotate(Vector3f.XN.rotationDegrees(40.0F * swing));
                return;
            }
            if (!block) {
                matrixStack.translate(0.1F * direction * swingRot, 0.1F * swingRot, -0.1F * swing);
                matrixStack.rotate(Vector3f.XN.rotationDegrees(-30.0F * swingRot));
                matrixStack.rotate(Vector3f.ZP.rotationDegrees(-10.0F * swingRot * direction));
                matrixStack.rotate(Vector3f.XN.rotationDegrees(40.0F * swing));
                matrixStack.rotate(Vector3f.YP.rotationDegrees(10.0F * swing * direction));
                return;
            }
            matrixStack.translate(0.1F * direction * swingRot, 0.1F * swingRot, -0.2F * swing);
            matrixStack.rotate(Vector3f.XN.rotationDegrees(-10.0F * swingRot));
            matrixStack.rotate(Vector3f.ZP.rotationDegrees(-10.0F * swingRot * direction));
            matrixStack.rotate(Vector3f.XN.rotationDegrees(20.0F * swing));
            return;
        }
        if (shovel) {
            matrixStack.translate(0.0D, 0.15D * swingRot, -0.25D * swingRot);
            matrixStack.translate(0.0D, 0.0D, -0.2D * swing);
            matrixStack.rotate(Vector3f.YP.rotationDegrees(15.0F * swingRot));
            matrixStack.rotate(Vector3f.XP.rotationDegrees(-35.0F * swingRot));
            matrixStack.rotate(Vector3f.XP.rotationDegrees(30.0F * swing));
            return;
        }
        if (sword) {
            matrixStack.translate(-0.55F * direction * swingRot, -0.8F * swingRot, -0.77F * swing);
            matrixStack.rotate(Vector3f.YP.rotationDegrees(5.0F * swingRot * direction));
            matrixStack.rotate(Vector3f.XN.rotationDegrees(-30.0F * swingRot));
            matrixStack.rotate(Vector3f.ZP.rotationDegrees(70.0F * swingRot * direction));
            matrixStack.rotate(Vector3f.XN.rotationDegrees(50.0F * swing));
            return;
        }
        if (tool) {
            matrixStack.translate(0.1F * direction * swingRot, 0.1F * swingRot, -0.5F * swing);
            matrixStack.rotate(Vector3f.XN.rotationDegrees(-30.0F * swingRot));
            matrixStack.rotate(Vector3f.ZP.rotationDegrees(-20.0F * swingRot * direction));
            matrixStack.rotate(Vector3f.XN.rotationDegrees(40.0F * swing));
            return;
        }
        matrixStack.translate(0.1F * direction * swingRot, 0.1F * swingRot, -0.1F * swing);
        matrixStack.rotate(Vector3f.XN.rotationDegrees(-30.0F * swingRot));
        matrixStack.rotate(Vector3f.ZP.rotationDegrees(-10.0F * swingRot * direction));
        matrixStack.rotate(Vector3f.XN.rotationDegrees(40.0F * swing));
        matrixStack.rotate(Vector3f.YP.rotationDegrees(10.0F * swing * direction));
    }

    @EventTarget
    public void onEvent(EventSwingAnimation event) {
        AttackAura aura = SkyCore.getInstance().getModuleManager().getAttackAura();
        if (onlyaura.get() && aura.target == null) return;
        MatrixStack matrixStack = event.getMatrixStack();
        float anim = MathHelper.sin(MathHelper.sqrt(event.getSwingProgress()) * (float) Math.PI);
        String mode = swordAnim.get();
        if (event.getHand() == Hand.MAIN_HAND) {
            boolean isLeft = event.getPlayer().getPrimaryHand() == HandSide.LEFT;
            switch (mode) {
                case "Мод 1": {
                    float swingSqrt = MathHelper.sqrt(event.getSwingProgress());
                    float direction = isLeft ? -1 : 1;

                    matrixStack.rotate(Vector3f.YP.rotationDegrees(direction * (20 + MathHelper.sin(event.getSwingProgress() * event.getSwingProgress() * (float) Math.PI) / 4 * -10)));
                    matrixStack.rotate(Vector3f.ZP.rotationDegrees(direction * MathHelper.sin(swingSqrt * (float) Math.PI) * -20));
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(MathHelper.sin(swingSqrt * (float) Math.PI) * -swipePower.get() * 10));
                    matrixStack.rotate(Vector3f.YP.rotationDegrees(-direction * 45));
                    break;
                }
                case "Мод 2": {
                    matrixStack.translate(0, 0.15, -0.3F);
                    matrixStack.rotate(Vector3f.YP.rotationDegrees(isLeft ? -90 : 90));
                    matrixStack.rotate(Vector3f.ZP.rotationDegrees(isLeft ? 60 : -60));
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(-angle.get() - swipePower.get() * 10 * anim));
                    break;
                }
                case "Мод 3": {
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(event.getSwingProgress() * (float) Math.PI - swipePower.get() * 10 * anim));
                    break;
                }
                case "Мод 4": {
                    matrixStack.translate(0, 0.15, -0.3F);
                    matrixStack.rotate(Vector3f.YP.rotationDegrees(isLeft ? -70 : 70));
                    matrixStack.rotate(Vector3f.ZP.rotationDegrees(isLeft ? 30 : -30));
                    matrixStack.scale(0.9f, 0.9f, 0.9f);
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(-angle.get() - swipePower.get() * 10 * anim));
                    break;
                }
                case "Мод 5": {
                    float direction = isLeft ? -1 : 1;
                    matrixStack.rotate(Vector3f.YP.rotationDegrees(direction * 45.0f));
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(anim * -20.0f));
                    matrixStack.rotate(Vector3f.ZP.rotationDegrees(direction * anim * -20.0f));
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(anim * -80.0f));
                    matrixStack.translate(direction * 0.4f, 0.2f, 0.2f);
                    matrixStack.translate(direction * -0.5f, 0.08f, 0.0f);
                    matrixStack.rotate(Vector3f.YP.rotationDegrees(direction * 20.0f));
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(-80.0f));
                    matrixStack.rotate(Vector3f.YP.rotationDegrees(direction * 20.0f));
                    break;
                }
                case "Мод 7": {
                    float direction = isLeft ? -1.0F : 1.0F;
                    matrixStack.scale(1, 1, 1);
                    matrixStack.translate(direction * (0.4F - anim * 0.3F), 0.0D, (-0.f - anim * 0.2F));
                    matrixStack.rotate(Vector3f.YP.rotationDegrees(direction * 90.0F));
                    matrixStack.rotate(Vector3f.ZP.rotationDegrees(direction * -30.0F));
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(-70.0F - 30.0F * anim));
                    break;
                }
                case "360": {
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(-360 * event.getSwingProgress()));
                    break;
                }
                case "Fade": {
                    float swingProgress = event.getSwingProgress();
                    float direction = isLeft ? -1 : 1;
                    float f2 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                    matrixStack.rotate(Vector3f.YP.rotationDegrees(direction * (45.0F + f2 * -5.0F)));
                    float f13 = MathHelper.sin(MathHelper.sqrt(swingProgress * swingProgress) * (float) Math.PI);
                    matrixStack.rotate(Vector3f.ZP.rotationDegrees(direction * f13 * -20.0F));
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(f13 * -(swipePower.get() * 8.0F)));
                    matrixStack.rotate(Vector3f.YP.rotationDegrees(direction * -45.0F));
                    break;
                }
                case "Мод 6": {
                    float i = isLeft ? -1 : 1;
                    float f = MathHelper.sin(event.getSwingProgress() * event.getSwingProgress() * (float) Math.PI);
                    matrixStack.rotate(Vector3f.YP.rotationDegrees((float) i * (45.0F + f * -20.0F)));
                    float f1 = MathHelper.sin(MathHelper.sqrt(event.getSwingProgress()) * (float) Math.PI);
                    matrixStack.rotate(Vector3f.ZP.rotationDegrees((float) i * f1 * -20.0F));
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(f1 * -swipePower.get() * 10));
                    matrixStack.rotate(Vector3f.YP.rotationDegrees((float) i * -45.0F));
                    break;
                }
                case "Slant": {
                    float rotate = 35;
                    matrixStack.translate(0, 0, -0.3 * anim);
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(anim * -rotate));
                    matrixStack.rotate(Vector3f.ZP.rotationDegrees(anim * rotate));
                    break;
                }
                case "3D": {
                    applyNewMode(event, isLeft);
                    break;
                }
            }
        }
        event.setCancelled(true);
    }

    @EventTarget
    public void onEvent(EventSwingSpeed event) {
        if (event.getHand() != Hand.MAIN_HAND) return;
        event.setSwipeSpeed(swipeSpeed.get().intValue());
        event.setCancelled(true);
    }

    @EventTarget
    public void onEvent(EventTransformSideFirstPerson event) {
        if (event.getHandSide() != mc.player.getPrimaryHand()) return;
        event.setEquippedProg(transform.get());
    }
}
