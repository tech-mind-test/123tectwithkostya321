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

import mods.baritone.api.api.java.baritone.api.utils.IPlayerContext;
import com.darkmagician6.eventapi.EventManager;
import sky.core.events.EventBlockDamage;
import mods.viaversion.viamcp.fixes.AttackOrder;
import net.minecraft.util.Hand;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

/**
 * @author Brady
 * @since 8/25/2018
 */
public final class BlockBreakHelper {

    private final IPlayerContext ctx;
    private boolean didBreakLastTick;

    BlockBreakHelper(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public void stopBreakingBlock() {
        // The player controller will never be null, but the player can be
        if (ctx.player() != null && didBreakLastTick) {
            if (!ctx.playerController().hasBrokenBlock()) {
                // insane bypass to check breaking succeeded
                ctx.playerController().setHittingBlock(true);
            }
            ctx.playerController().resetBlockRemoving();
            didBreakLastTick = false;
        }
    }

    public void tick(boolean isLeftClick) {
        RayTraceResult trace = ctx.objectMouseOver();

        if (!isLeftClick) {
            ctx.minecraft().leftClickCounter = 0;
        }

        if (ctx.minecraft().leftClickCounter <= 0 && ctx.player() != null && !ctx.player().isHandActive()) {
            if (isLeftClick && trace != null && trace.getType() == RayTraceResult.Type.BLOCK) {
                BlockRayTraceResult blockTrace = (BlockRayTraceResult) trace;
                BlockPos blockPos = blockTrace.getPos();

                if (!ctx.world().getBlockState(blockPos).isAir()) {
                    Direction direction = blockTrace.getFace();

                    EventManager.call(new EventBlockDamage(ctx.world().getBlockState(blockPos), blockPos, EventBlockDamage.State.START,direction));
                    if (ctx.playerController().onPlayerDamageBlock(blockPos, direction)) {
                        ctx.minecraft().particles.addBlockHitEffects(blockPos, direction);
                        AttackOrder.sendConditionalSwing(trace, Hand.MAIN_HAND);
                    }
                    EventManager.call(new EventBlockDamage(ctx.world().getBlockState(blockPos), blockPos, EventBlockDamage.State.STOP,direction));
                }
            } else {
                ctx.playerController().resetBlockRemoving();
            }
        }
    }
}
