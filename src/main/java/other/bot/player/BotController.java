/* Decompiler 386ms, total 777ms, lines 464 */
package other.bot.player;


import com.darkmagician6.eventapi.EventManager;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CommandBlockBlock;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.StructureBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.util.ClientRecipeBook;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.play.client.CClickWindowPacket;
import net.minecraft.network.play.client.CCreativeInventoryActionPacket;
import net.minecraft.network.play.client.CEnchantItemPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPickItemPacket;
import net.minecraft.network.play.client.CPlaceRecipePacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket.Action;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import other.bot.connection.BotClientPlayNetHandler;
import other.bot.world.BotWorld;
import sky.core.events.EventAttack;

public class BotController {
    public static final Logger LOGGER = LogManager.getLogger();
    public final Minecraft mc;
    public final BotClientPlayNetHandler connection;
    public BlockPos currentBlock = new BlockPos(-1, -1, -1);
    public ItemStack currentItemHittingBlock;
    public float curBlockDamageMP;
    public float stepSoundTickCounter;
    public int blockHitDelay;
    public boolean isHittingBlock;
    public GameType currentGameType;
    public GameType field_239166_k_;
    public final Object2ObjectLinkedOpenHashMap<Pair<BlockPos, Action>, Vector3d> unacknowledgedDiggingPackets;
    public int currentPlayerItem;
    public BotPlayer bot;

    public BotController(Minecraft mcIn, BotClientPlayNetHandler netHandler) {
        this.currentItemHittingBlock = ItemStack.EMPTY;
        this.currentGameType = GameType.SURVIVAL;
        this.field_239166_k_ = GameType.NOT_SET;
        this.unacknowledgedDiggingPackets = new Object2ObjectLinkedOpenHashMap();
        this.mc = mcIn;
        this.connection = netHandler;
    }

    public void setPlayerCapabilities(PlayerEntity player) {
        this.currentGameType.configurePlayerCapabilities(player.abilities);
    }

    public void func_241675_a_(GameType p_241675_1_) {
        this.field_239166_k_ = p_241675_1_;
    }

    public void setGameType(GameType type) {
        if (type != this.currentGameType) {
            this.field_239166_k_ = this.currentGameType;
        }

        this.currentGameType = type;
        this.currentGameType.configurePlayerCapabilities(this.bot.abilities);
    }

    public boolean shouldDrawHUD() {
        return this.currentGameType.isSurvivalOrAdventure();
    }

    public boolean onPlayerDestroyBlock(BlockPos pos) {
        if (this.bot.blockActionRestricted(this.connection.getWorld(), pos, this.currentGameType)) {
            return false;
        } else {
            World world = this.connection.getWorld();
            BlockState blockstate = world.getBlockState(pos);
            if (!this.bot.getHeldItemMainhand().getItem().canPlayerBreakBlockWhileHolding(blockstate, world, pos, this.bot)) {
                return false;
            } else {
                Block block = blockstate.getBlock();
                if ((block instanceof CommandBlockBlock || block instanceof StructureBlock || block instanceof JigsawBlock) && !this.bot.canUseCommandBlock()) {
                    return false;
                } else if (blockstate.isAir()) {
                    return false;
                } else {
                    block.onBlockHarvested(world, pos, blockstate, this.bot);
                    FluidState fluidstate = world.getFluidState(pos);
                    boolean flag = world.setBlockState(pos, fluidstate.getBlockState(), 11);
                    if (flag) {
                        block.onPlayerDestroy(world, pos, blockstate);
                    }

                    return flag;
                }
            }
        }
    }

    public boolean clickBlock(BlockPos loc, Direction face) {
        if (this.bot.blockActionRestricted(this.connection.getWorld(), loc, this.currentGameType)) {
            return false;
        } else if (!this.connection.getWorld().getWorldBorder().contains(loc)) {
            return false;
        } else {
            BlockState blockstate1;
            if (this.currentGameType.isCreative()) {
                blockstate1 = this.connection.getWorld().getBlockState(loc);
                this.sendDiggingPacket(Action.START_DESTROY_BLOCK, loc, face);
                this.onPlayerDestroyBlock(loc);
                this.blockHitDelay = 5;
            } else if (!this.isHittingBlock || !this.isHittingPosition(loc)) {
                if (this.isHittingBlock) {
                    this.sendDiggingPacket(Action.ABORT_DESTROY_BLOCK, this.currentBlock, face);
                }

                blockstate1 = this.connection.getWorld().getBlockState(loc);
                this.sendDiggingPacket(Action.START_DESTROY_BLOCK, loc, face);
                boolean flag = !blockstate1.isAir();
                if (flag && this.curBlockDamageMP == 0.0F) {
                    blockstate1.onBlockClicked(this.connection.getWorld(), loc, this.bot);
                }

                if (flag && blockstate1.getPlayerRelativeBlockHardness(this.bot, this.bot.world, loc) >= 1.0F) {
                    this.onPlayerDestroyBlock(loc);
                } else {
                    this.isHittingBlock = true;
                    this.currentBlock = loc;
                    this.currentItemHittingBlock = this.bot.getHeldItemMainhand();
                    this.curBlockDamageMP = 0.0F;
                    this.stepSoundTickCounter = 0.0F;
                    this.connection.getWorld().sendBlockBreakProgress(this.bot.getEntityId(), this.currentBlock, (int)(this.curBlockDamageMP * 10.0F) - 1);
                }
            }

            return true;
        }
    }

    public void resetBlockRemoving() {
        if (this.isHittingBlock) {
            BlockState blockstate = this.connection.getWorld().getBlockState(this.currentBlock);
            this.sendDiggingPacket(Action.ABORT_DESTROY_BLOCK, this.currentBlock, Direction.DOWN);
            this.isHittingBlock = false;
            this.curBlockDamageMP = 0.0F;
            this.connection.getWorld().sendBlockBreakProgress(this.bot.getEntityId(), this.currentBlock, -1);
            this.bot.resetCooldown();
        }

    }

    public boolean onPlayerDamageBlock(BlockPos posBlock, Direction directionFacing) {
        this.syncCurrentPlayItem();
        if (this.blockHitDelay > 0) {
            --this.blockHitDelay;
            return true;
        } else {
            BlockState blockstate;
            if (this.currentGameType.isCreative() && this.connection.getWorld().getWorldBorder().contains(posBlock)) {
                this.blockHitDelay = 5;
                blockstate = this.connection.getWorld().getBlockState(posBlock);
                this.sendDiggingPacket(Action.START_DESTROY_BLOCK, posBlock, directionFacing);
                this.onPlayerDestroyBlock(posBlock);
                return true;
            } else if (this.isHittingPosition(posBlock)) {
                blockstate = this.connection.getWorld().getBlockState(posBlock);
                if (blockstate.isAir()) {
                    this.isHittingBlock = false;
                    return false;
                } else {
                    this.curBlockDamageMP += blockstate.getPlayerRelativeBlockHardness(this.bot, this.bot.world, posBlock);
                    if (this.stepSoundTickCounter % 4.0F == 0.0F) {
                        SoundType soundtype = blockstate.getSoundType();
                        this.mc.getSoundHandler().play(new SimpleSound(soundtype.getHitSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 8.0F, soundtype.getPitch() * 0.5F, posBlock));
                    }

                    ++this.stepSoundTickCounter;
                    if (this.curBlockDamageMP >= 1.0F) {
                        this.isHittingBlock = false;
                        this.sendDiggingPacket(Action.STOP_DESTROY_BLOCK, posBlock, directionFacing);
                        this.onPlayerDestroyBlock(posBlock);
                        this.curBlockDamageMP = 0.0F;
                        this.stepSoundTickCounter = 0.0F;
                        this.blockHitDelay = 5;
                    }

                    this.connection.getWorld().sendBlockBreakProgress(this.bot.getEntityId(), this.currentBlock, (int)(this.curBlockDamageMP * 10.0F) - 1);
                    return true;
                }
            } else {
                return this.clickBlock(posBlock, directionFacing);
            }
        }
    }

    public float getBlockReachDistance() {
        return this.currentGameType.isCreative() ? 5.0F : 4.5F;
    }

    public void tick() {
        this.syncCurrentPlayItem();
        if (this.connection.getBotNetwork().isChannelOpen()) {
            this.connection.getBotNetwork().tick();
        } else {
            this.connection.getBotNetwork().handleDisconnection();
        }

    }

    private boolean isHittingPosition(BlockPos pos) {
        ItemStack itemstack = this.bot.getHeldItemMainhand();
        boolean flag = this.currentItemHittingBlock.isEmpty() && itemstack.isEmpty();
        if (!this.currentItemHittingBlock.isEmpty() && !itemstack.isEmpty()) {
            flag = itemstack.getItem() == this.currentItemHittingBlock.getItem() && ItemStack.areItemStackTagsEqual(itemstack, this.currentItemHittingBlock) && (itemstack.isDamageable() || itemstack.getDamage() == this.currentItemHittingBlock.getDamage());
        }

        return pos.equals(this.currentBlock) && flag;
    }

    public void syncCurrentPlayItem() {
        int i = this.bot.inventory.currentItem;
        if (i != this.currentPlayerItem) {
            this.currentPlayerItem = i;
            this.connection.sendPacket(new CHeldItemChangePacket(this.currentPlayerItem));
        }

    }

    public ActionResultType func_217292_a(BotPlayer p_217292_1_, BotWorld p_217292_2_, Hand p_217292_3_, BlockRayTraceResult p_217292_4_) {
        this.syncCurrentPlayItem();
        BlockPos blockpos = p_217292_4_.getPos();
        if (!this.connection.getWorld().getWorldBorder().contains(blockpos)) {
            return ActionResultType.FAIL;
        } else {
            ItemStack itemstack = p_217292_1_.getHeldItem(p_217292_3_);
            if (this.currentGameType == GameType.SPECTATOR) {
                this.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(p_217292_3_, p_217292_4_));
                return ActionResultType.SUCCESS;
            } else {
                boolean flag = !p_217292_1_.getHeldItemMainhand().isEmpty() || !p_217292_1_.getHeldItemOffhand().isEmpty();
                boolean flag1 = p_217292_1_.isSecondaryUseActive() && flag;
                if (!flag1) {
                    ActionResultType actionresulttype = p_217292_2_.getBlockState(blockpos).onBlockActivated(p_217292_2_, p_217292_1_, p_217292_3_, p_217292_4_);
                    if (actionresulttype.isSuccessOrConsume()) {
                        this.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(p_217292_3_, p_217292_4_));
                        return actionresulttype;
                    }
                }

                this.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(p_217292_3_, p_217292_4_));
                if (!itemstack.isEmpty() && !p_217292_1_.getCooldownTracker().hasCooldown(itemstack.getItem())) {
                    ItemUseContext itemusecontext = new ItemUseContext(p_217292_1_, p_217292_3_, p_217292_4_);
                    ActionResultType actionresulttype1;
                    if (this.currentGameType.isCreative()) {
                        int i = itemstack.getCount();
                        actionresulttype1 = itemstack.onItemUse(itemusecontext);
                        itemstack.setCount(i);
                    } else {
                        actionresulttype1 = itemstack.onItemUse(itemusecontext);
                    }

                    return actionresulttype1;
                } else {
                    return ActionResultType.PASS;
                }
            }
        }
    }

    public ActionResultType processRightClick(PlayerEntity player, World worldIn, Hand hand) {
        if (this.currentGameType == GameType.SPECTATOR) {
            return ActionResultType.PASS;
        } else {
            this.syncCurrentPlayItem();
            this.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
            ItemStack itemstack = player.getHeldItem(hand);
            if (player.getCooldownTracker().hasCooldown(itemstack.getItem())) {
                return ActionResultType.PASS;
            } else {
                int i = itemstack.getCount();
                ActionResult<ItemStack> actionresult = itemstack.useItemRightClick(worldIn, player, hand);
                ItemStack itemstack1 = (ItemStack)actionresult.getResult();
                if (itemstack1 != itemstack) {
                    player.setHeldItem(hand, itemstack1);
                }

                return actionresult.getType();
            }
        }
    }

    public BotPlayer createPlayer(BotWorld worldIn, StatisticsManager statsManager, ClientRecipeBook recipes) {
        return this.func_239167_a_(worldIn, statsManager, recipes, false, false);
    }

    public BotPlayer func_239167_a_(BotWorld p_239167_1_, StatisticsManager p_239167_2_, ClientRecipeBook p_239167_3_, boolean p_239167_4_, boolean p_239167_5_) {
        return new BotPlayer(this.mc, p_239167_1_, this.connection, p_239167_2_, p_239167_3_, p_239167_4_, p_239167_5_);
    }

    public void attackEntity(PlayerEntity playerIn, Entity targetEntity) {
        EventAttack event = new EventAttack(targetEntity);
        EventManager.call(event);
        if (!event.isCancelled()) {
            this.syncCurrentPlayItem();
            this.connection.sendPacket(new CUseEntityPacket(targetEntity, playerIn.isSneaking()));
            if (this.currentGameType != GameType.SPECTATOR) {
                playerIn.attackTargetEntityWithCurrentItem(targetEntity);
                playerIn.resetCooldown();
            }

        }
    }

    public ActionResultType interactWithEntity(PlayerEntity player, Entity target, Hand hand) {
        this.syncCurrentPlayItem();
        this.connection.sendPacket(new CUseEntityPacket(target, hand, player.isSneaking()));
        return this.currentGameType == GameType.SPECTATOR ? ActionResultType.PASS : player.interactOn(target, hand);
    }

    public ActionResultType interactWithEntity(PlayerEntity player, Entity target, EntityRayTraceResult ray, Hand hand) {
        this.syncCurrentPlayItem();
        Vector3d vector3d = ray.getHitVec().subtract(target.getPosX(), target.getPosY(), target.getPosZ());
        this.connection.sendPacket(new CUseEntityPacket(target, hand, vector3d, player.isSneaking()));
        return this.currentGameType == GameType.SPECTATOR ? ActionResultType.PASS : target.applyPlayerInteraction(player, vector3d, hand);
    }

    public ItemStack windowClick(int windowId, int slotId, int mouseButton, ClickType type, PlayerEntity player) {
        short short1 = player.openContainer.getNextTransactionID(player.inventory);
        ItemStack itemstack = player.openContainer.slotClick(slotId, mouseButton, type, player);
        this.connection.sendPacket(new CClickWindowPacket(windowId, slotId, mouseButton, type, itemstack, short1));
        return itemstack;
    }

    public ItemStack windowClick(int windowId, int slotId, int mouseButton, ClickType type, ItemStack itemStack, PlayerEntity player) {
        short short1 = player.openContainer.getNextTransactionID(player.inventory);
        ItemStack itemstack = player.openContainer.slotClick(slotId, mouseButton, type, player);
        this.connection.sendPacket(new CClickWindowPacket(windowId, slotId, mouseButton, type, itemStack, short1));
        return itemstack;
    }

    public void windowClickFixed(int windowId, int slotId, int mouseButton, ClickType type, PlayerEntity player, int timeWait, boolean reason) {
        this.bot.windowClickMemory.add(new BotPlayer.WindowClickMemory(windowId, slotId, mouseButton, type, player, timeWait, reason));
    }

    public void sendPlaceRecipePacket(int p_203413_1_, IRecipe<?> p_203413_2_, boolean p_203413_3_) {
        this.connection.sendPacket(new CPlaceRecipePacket(p_203413_1_, p_203413_2_, p_203413_3_));
    }

    public void sendEnchantPacket(int windowID, int button) {
        this.connection.sendPacket(new CEnchantItemPacket(windowID, button));
    }

    public void sendSlotPacket(ItemStack itemStackIn, int slotId) {
        if (this.currentGameType.isCreative()) {
            this.connection.sendPacket(new CCreativeInventoryActionPacket(slotId, itemStackIn));
        }

    }

    public void sendPacketDropItem(ItemStack itemStackIn) {
        if (this.currentGameType.isCreative() && !itemStackIn.isEmpty()) {
            this.connection.sendPacket(new CCreativeInventoryActionPacket(-1, itemStackIn));
        }

    }

    public void onStoppedUsingItem(PlayerEntity playerIn) {
        this.syncCurrentPlayItem();
        this.connection.sendPacket(new CPlayerDiggingPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
        playerIn.stopActiveHand();
    }

    public boolean gameIsSurvivalOrAdventure() {
        return this.currentGameType.isSurvivalOrAdventure();
    }

    public boolean isNotCreative() {
        return !this.currentGameType.isCreative();
    }

    public boolean isInCreativeMode() {
        return this.currentGameType.isCreative();
    }

    public boolean extendedReach() {
        return this.currentGameType.isCreative();
    }

    public boolean isRidingHorse() {
        return this.bot.isPassenger() && this.bot.getRidingEntity() instanceof AbstractHorseEntity;
    }

    public boolean isSpectatorMode() {
        return this.currentGameType == GameType.SPECTATOR;
    }

    public GameType func_241822_k() {
        return this.field_239166_k_;
    }

    public GameType getCurrentGameType() {
        return this.currentGameType;
    }

    public boolean getIsHittingBlock() {
        return this.isHittingBlock;
    }

    public void pickItem(int index) {
        this.connection.sendPacket(new CPickItemPacket(index));
    }

    private void sendDiggingPacket(Action action, BlockPos pos, Direction dir) {
        BotPlayer clientplayerentity = this.bot;
        this.unacknowledgedDiggingPackets.put(Pair.of(pos, action), clientplayerentity.getPositionVec());
        this.connection.sendPacket(new CPlayerDiggingPacket(action, pos, dir));
    }

    public void acknowledgePlayerDiggingReceived(BotWorld worldIn, BlockPos pos, BlockState blockIn, Action action, boolean successful) {
        Vector3d vector3d = (Vector3d)this.unacknowledgedDiggingPackets.remove(Pair.of(pos, action));
        BlockState blockstate = worldIn.getBlockState(pos);
        if ((vector3d == null || !successful || action != Action.START_DESTROY_BLOCK && blockstate != blockIn) && blockstate != blockIn) {
            worldIn.invalidateRegionAndSetBlock(pos, blockIn);
            PlayerEntity playerentity = this.bot;
            if (vector3d != null && worldIn == playerentity.world && playerentity.func_242278_a(pos, blockIn)) {
                playerentity.func_242281_f(vector3d.x, vector3d.y, vector3d.z);
            }
        }

        while(this.unacknowledgedDiggingPackets.size() >= 50) {
            Pair<BlockPos, Action> pair = (Pair)this.unacknowledgedDiggingPackets.firstKey();
            this.unacknowledgedDiggingPackets.removeFirst();
            LOGGER.error("Too many unacked block actions, dropping " + String.valueOf(pair));
        }

    }
}