package net.minecraft.util;

import com.darkmagician6.eventapi.EventManager;
import sky.core.events.EventInput;
import net.minecraft.client.GameSettings;

public class MovementInputFromOptions extends MovementInput {
    private final GameSettings gameSettings;

    public MovementInputFromOptions(GameSettings gameSettingsIn) {
        this.gameSettings = gameSettingsIn;
    }

    @Override
    public void tickMovement(boolean p_225607_1_) {
        this.forwardKeyDown = this.gameSettings.keyBindForward.isKeyDown();
        this.backKeyDown = this.gameSettings.keyBindBack.isKeyDown();
        this.leftKeyDown = this.gameSettings.keyBindLeft.isKeyDown();
        this.rightKeyDown = this.gameSettings.keyBindRight.isKeyDown();
        this.moveForward = this.forwardKeyDown == this.backKeyDown ? 0.0F : (this.forwardKeyDown ? 1.0F : -1.0F);
        this.moveStrafe = this.leftKeyDown == this.rightKeyDown ? 0.0F : (this.leftKeyDown ? 1.0F : -1.0F);
        this.jump = this.gameSettings.keyBindJump.isKeyDown();
        this.sneaking = this.gameSettings.keyBindSneak.isKeyDown();
        EventInput event = new EventInput(
                this.moveForward,
                this.moveStrafe,
                this.forwardKeyDown,
                this.backKeyDown,
                this.leftKeyDown,
                this.rightKeyDown,
                this.jump,
                this.sneaking,
                0.3D
        );

        EventManager.call(event);

        final double sneakMultiplier = event.getSneakSlowDownMultiplier();
        this.moveForward = event.getForward();
        this.moveStrafe = event.getStrafe();
        this.forwardKeyDown = event.isForwardKeyDown();
        this.backKeyDown = event.isBackKeyDown();
        this.leftKeyDown = event.isLeftKeyDown();
        this.rightKeyDown = event.isRightKeyDown();
        this.jump = event.isJump();
        this.sneaking = event.isSneak();

        if (p_225607_1_) {
            this.moveStrafe = (float) ((double) this.moveStrafe * sneakMultiplier);
            this.moveForward = (float) ((double) this.moveForward * sneakMultiplier);
        }
    }
}