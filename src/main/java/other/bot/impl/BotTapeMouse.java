/* Decompiler 105ms, total 491ms, lines 114 */
package other.bot.impl;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import other.bot.Bot;
import other.bot.player.BotPlayer;

public class BotTapeMouse {
    Bot bot;

    public void clickBotMouse() {
        Bot bot1 = this.bot;
        BotPlayer player = this.bot.connection.bot;
        if (!player.isRowingBoat()) {
            switch(bot1.connection.bot.objectMouseOver.getType()) {
                case ENTITY:
                    bot1.connection.botController.attackEntity(player, ((EntityRayTraceResult)bot1.connection.bot.objectMouseOver).getEntity());
                    break;
                case BLOCK:
                    BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult)bot1.connection.bot.objectMouseOver;
                    BlockPos blockpos = blockraytraceresult.getPos();
                    if (!bot1.connection.getWorld().getBlockState(blockpos).isAir()) {
                        bot1.connection.botController.clickBlock(blockpos, blockraytraceresult.getFace());
                        break;
                    }
                case MISS:
                    player.resetCooldown();
            }

            player.swingArm(Hand.MAIN_HAND);
        }

    }

    public void rightClickBotMouse(GameRenderer gameRenderer) {
        Bot bot1 = this.bot;
        BotPlayer player = this.bot.connection.bot;
        if (!bot1.connection.botController.getIsHittingBlock() && !player.isRowingBoat()) {
            Hand[] var4 = Hand.values();
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                Hand hand = var4[var6];
                ItemStack itemstack = player.getHeldItem(hand);
                if (bot1.connection.bot.objectMouseOver != null) {
                    switch(bot1.connection.bot.objectMouseOver.getType()) {
                        case ENTITY:
                            EntityRayTraceResult entityraytraceresult = (EntityRayTraceResult)bot1.connection.bot.objectMouseOver;
                            Entity entity = entityraytraceresult.getEntity();
                            ActionResultType actionresulttype = bot1.connection.botController.interactWithEntity(player, entity, entityraytraceresult, hand);
                            if (!actionresulttype.isSuccessOrConsume()) {
                                actionresulttype = bot1.connection.botController.interactWithEntity(player, entity, hand);
                            }

                            if (actionresulttype.isSuccessOrConsume()) {
                                if (actionresulttype.isSuccess()) {
                                    player.swingArm(hand);
                                }

                                return;
                            }
                            break;
                        case BLOCK:
                            BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult)bot1.connection.bot.objectMouseOver;
                            int i = itemstack.getCount();
                            ActionResultType actionresulttype1 = bot1.connection.botController.func_217292_a(bot1.connection.bot, bot1.connection.getWorld(), hand, blockraytraceresult);
                            if (actionresulttype1.isSuccessOrConsume()) {
                                if (actionresulttype1.isSuccess()) {
                                    player.swingArm(hand);
                                    if (!itemstack.isEmpty() && (itemstack.getCount() != i || bot1.connection.botController.isInCreativeMode())) {
                                        gameRenderer.itemRenderer.resetEquippedProgress(hand);
                                    }
                                }

                                return;
                            }

                            if (actionresulttype1 == ActionResultType.FAIL) {
                                return;
                            }
                    }
                }

                if (!itemstack.isEmpty()) {
                    ActionResultType actionresulttype2 = bot1.connection.botController.processRightClick(player, bot1.connection.getWorld(), hand);
                    if (actionresulttype2.isSuccessOrConsume()) {
                        if (actionresulttype2.isSuccess()) {
                            player.swingArm(hand);
                        }

                        gameRenderer.itemRenderer.resetEquippedProgress(hand);
                        return;
                    }
                }
            }
        }

    }

    public BotTapeMouse(Bot bot) {
        this.bot = bot;
    }

    public Bot getBot() {
        return this.bot;
    }
}