/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package mods.baritone.utils;

import mods.baritone.api.api.java.baritone.api.utils.input.Input;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;

public class PlayerMovementInput extends MovementInput {

    private final InputOverrideHandler handler;

    PlayerMovementInput(InputOverrideHandler handler) {
        this.handler = handler;
    }

    @Override
    public void tickMovement(boolean p_225607_1_) {
        MovementInputFromOptions base = new MovementInputFromOptions(handler.ctx.minecraft().gameSettings);
        base.tickMovement(p_225607_1_);

        this.moveStrafe = base.moveStrafe;
        this.moveForward = base.moveForward;
        this.jump = base.jump;
        this.sneaking = base.sneaking;
        this.forwardKeyDown = base.forwardKeyDown;
        this.backKeyDown = base.backKeyDown;
        this.leftKeyDown = base.leftKeyDown;
        this.rightKeyDown = base.rightKeyDown;

        if (handler.isInputForcedDown(Input.MOVE_FORWARD)) {
            this.moveForward = Math.max(this.moveForward, 1.0F);
            this.forwardKeyDown = true;
        }
        if (handler.isInputForcedDown(Input.MOVE_BACK)) {
            this.moveForward = Math.min(this.moveForward, -1.0F);
            this.backKeyDown = true;
        }
        if (handler.isInputForcedDown(Input.MOVE_LEFT)) {
            this.moveStrafe = Math.max(this.moveStrafe, 1.0F);
            this.leftKeyDown = true;
        }
        if (handler.isInputForcedDown(Input.MOVE_RIGHT)) {
            this.moveStrafe = Math.min(this.moveStrafe, -1.0F);
            this.rightKeyDown = true;
        }

        if (handler.isInputForcedDown(Input.JUMP)) {
            this.jump = true;
        }
        if (handler.isInputForcedDown(Input.SNEAK)) {
            this.sneaking = true;
        }

        if (this.moveForward > 1.0F) this.moveForward = 1.0F;
        if (this.moveForward < -1.0F) this.moveForward = -1.0F;
        if (this.moveStrafe > 1.0F) this.moveStrafe = 1.0F;
        if (this.moveStrafe < -1.0F) this.moveStrafe = -1.0F;

        if (this.sneaking) {
            this.moveStrafe *= 0.3D;
            this.moveForward *= 0.3D;
        }
    }
}
