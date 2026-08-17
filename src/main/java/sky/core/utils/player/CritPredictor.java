package sky.core.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
public class CritPredictor {

    private final Minecraft mc = Minecraft.getInstance();
    private final PlayerEntity player;

    private double simY;
    private double simMotionY;
    private boolean simOnGround;
    private float simFallDistance;

    public CritPredictor(PlayerEntity p) {
        this.player = p;

        // копируем состояние
        this.simY = p.getPosY();
        this.simMotionY = p.getMotion().y;
        this.simOnGround = p.isOnGround();
        this.simFallDistance = p.fallDistance;
    }

    /**
     * Симулируем N тиков вперёд только по вертикали
     */
    public void simulateTicks(int ticks) {
        for (int i = 0; i < ticks; i++) {
//            ChatUtil.addText("1");
            tickVerticalPhysics();
        }
    }

    private void tickVerticalPhysics() {
        double gravity = -0.08;
        double drag = 0.98;

        // применяем гравитацию
        if (!player.abilities.isFlying) {
            simMotionY += gravity;
        }

        // перемещаем
        simY += simMotionY;

        // проверяем землю (очень упрощённо)
        BlockPos pos = new BlockPos(player.getPosX(), simY - 0.1, player.getPosZ());
        boolean ground = !player.world.isAirBlock(pos);

        if (ground) {
            simOnGround = true;
            simMotionY = 0;
            simFallDistance = 0;
        } else {
            simOnGround = false;

            if (simMotionY < 0) simFallDistance += -simMotionY;
        }

        // сопротивление воздуха
        simMotionY *= drag;
    }

    // ----------------------------------------------------------
    //                  CRIT CHECK LOGIC
    // ----------------------------------------------------------

    /**
     * Логика shouldCritical() под MCP 1.16.5
     */
    public boolean shouldCritical() {
        boolean debuffed = player.isPotionActive(Effects.LEVITATION)
                || player.isPotionActive(Effects.BLINDNESS)
                || player.isPotionActive(Effects.SLOW_FALLING);

        boolean inLiquid =
                player.areEyesInFluid(FluidTags.WATER)
                        || player.areEyesInFluid(FluidTags.LAVA);

        boolean flying = player.abilities.isFlying || player.isElytraFlying();
        boolean climbing = player.isOnLadder();
        boolean cantJump = player.isPassenger();
        boolean inWeb = mc.player.isInWeb;

        return !(debuffed || inLiquid || flying || climbing || cantJump || inWeb);
    }

    /**
     * Основная проверка типа твоего canDefaultCrit
     */
    public boolean willBeAbleToCrit(boolean onlyCrits, boolean critModuleEnabled, boolean canCritSetting) {
        boolean canDefaultCrit =
                ((simFallDistance > 0 && canCritSetting) || (critModuleEnabled && !simOnGround));

        if (onlyCrits) {
            return shouldCritical() && canDefaultCrit;
        }

        return true;
    }

    /**
     * Возвращает – будет ли игрок падать через N тиков
     */
    public boolean willBeFalling() {
        return !simOnGround && simMotionY < 0;
    }

    /**
     * Возвращает предсказанную fallDistance
     */
    public float getPredictedFallDistance() {
        return simFallDistance;
    }
}
