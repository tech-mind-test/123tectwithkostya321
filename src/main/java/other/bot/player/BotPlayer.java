/* Decompiler 1021ms, total 1494ms, lines 1089 */
package other.bot.player;

import com.darkmagician6.eventapi.EventManager;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;


import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mods.baritone.api.api.java.baritone.api.BaritoneAPI;
import mods.baritone.api.api.java.baritone.api.IBaritone;
import mods.baritone.api.api.java.baritone.api.event.events.ChatEvent;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.BiomeSoundHandler;
import net.minecraft.client.audio.BubbleColumnAmbientSoundHandler;
import net.minecraft.client.audio.ElytraSound;
import net.minecraft.client.audio.IAmbientSoundHandler;
import net.minecraft.client.audio.RidingMinecartTickableSound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.audio.UnderwaterAmbientSoundHandler;
import net.minecraft.client.audio.UnderwaterAmbientSounds.UnderWaterSound;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.CommandBlockScreen;
import net.minecraft.client.gui.screen.EditBookScreen;
import net.minecraft.client.gui.screen.EditMinecartCommandBlockScreen;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.gui.screen.EditStructureScreen;
import net.minecraft.client.gui.screen.JigsawScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.util.ClientRecipeBook;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IJumpingMount;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.network.play.client.CClientStatusPacket;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CInputPacket;
import net.minecraft.network.play.client.CMarkRecipeSeenPacket;
import net.minecraft.network.play.client.CMoveVehiclePacket;
import net.minecraft.network.play.client.CPlayerAbilitiesPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CClientStatusPacket.State;
import net.minecraft.network.play.client.CEntityActionPacket.Action;
import net.minecraft.network.play.client.CPlayerPacket.PositionPacket;
import net.minecraft.network.play.client.CPlayerPacket.PositionRotationPacket;
import net.minecraft.network.play.client.CPlayerPacket.RotationPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.CommandBlockLogic;
import net.minecraft.tileentity.CommandBlockTileEntity;
import net.minecraft.tileentity.JigsawTileEntity;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.StructureBlockTileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.MovementInput;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import other.bot.connection.BotClientPlayNetHandler;
import other.bot.world.BotWorld;
import sky.core.SkyCore;
import sky.core.events.EventNoPush;
import sky.core.events.EventNoSlow;
import sky.core.modules.impl.movement.Sprint;
import sky.core.utils.math.TimeUtil;

public class BotPlayer extends AbstractClientPlayerEntity {
    public final BotClientPlayNetHandler connection;
    public final StatisticsManager stats;
    public final ClientRecipeBook recipeBook;
    public final List<IAmbientSoundHandler> ambientSoundHandlers = Lists.newArrayList();
    public int permissionLevel = 0;
    public List<BotPlayer.WindowClickMemory> windowClickMemory = new ArrayList();
    public RayTraceResult objectMouseOver;
    public double lastReportedPosX;
    public double lastReportedPosY;
    public double lastReportedPosZ;
    public float lastReportedYaw;
    public float lastReportedPitch;
    public boolean prevOnGround;
    public boolean isCrouching;
    public boolean clientSneakState;
    public boolean serverSprintState;
    public int positionUpdateTicks;
    public boolean hasValidHealth;
    public String serverBrand;
    public MovementInput movementInput;
    protected final Minecraft mc;
    protected int sprintToggleTimer;
    public int sprintingTicksLeft;
    public float renderArmYaw;
    public float renderArmPitch;
    public float prevRenderArmYaw;
    public float prevRenderArmPitch;
    public int horseJumpPowerCounter;
    public float horseJumpPower;
    public float timeInPortal;
    public float prevTimeInPortal;
    public boolean handActive;
    public Hand activeHand;
    public boolean rowingBoat;
    public boolean autoJumpEnabled = true;
    public int autoJumpTime;
    public boolean wasFallFlying;
    public int counterInWater;
    public boolean showDeathScreen = true;

    public BotPlayer(Minecraft mc, BotWorld world, BotClientPlayNetHandler connection, StatisticsManager stats, ClientRecipeBook recipeBook, boolean clientSneakState, boolean clientSprintState) {
        super(world, connection.getGameProfile());
        this.mc = mc;
        this.connection = connection;
        this.stats = stats;
        this.recipeBook = recipeBook;
        this.clientSneakState = clientSneakState;
        this.serverSprintState = clientSprintState;
        this.ambientSoundHandlers.add(new UnderwaterAmbientSoundHandler(this, mc.getSoundHandler()));
        this.ambientSoundHandlers.add(new BubbleColumnAmbientSoundHandler(this));
        this.ambientSoundHandlers.add(new BiomeSoundHandler(this, mc.getSoundHandler(), world.getBiomeManager()));
    }

    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    public void heal(float healAmount) {
    }

    public boolean startRiding(Entity entityIn, boolean force) {
        if (!super.startRiding(entityIn, force)) {
            return false;
        } else {
            if (entityIn instanceof AbstractMinecartEntity) {
                this.mc.getSoundHandler().play(new RidingMinecartTickableSound(this, (AbstractMinecartEntity) entityIn));
            }

            if (entityIn instanceof BoatEntity) {
                this.prevRotationYaw = entityIn.rotationYaw;
                this.rotationYaw = entityIn.rotationYaw;
                this.setRotationYawHead(entityIn.rotationYaw);
            }

            return true;
        }
    }

    public void dismount() {
        super.dismount();
        this.rowingBoat = false;
    }

    public float getPitch(float partialTicks) {
        return this.rotationPitch;
    }

    public float getYaw(float partialTicks) {
        return this.isPassenger() ? super.getYaw(partialTicks) : this.rotationYaw;
    }

    public void tick() {
        if (this.world.isBlockLoaded(new BlockPos(this.getPosX(), 0.0D, this.getPosZ()))) {
            super.tick();
            if (this.isPassenger()) {
                this.connection.sendPacket(new RotationPacket(this.rotationYaw, this.rotationPitch, this.onGround));
                this.connection.sendPacket(new CInputPacket(this.moveStrafing, this.moveForward, this.movementInput.jump, this.movementInput.sneaking));
                Entity entity = this.getLowestRidingEntity();
                if (entity != this && entity.canPassengerSteer()) {
                    this.connection.sendPacket(new CMoveVehiclePacket(entity));
                }
            } else {
                this.onUpdateWalkingPlayer();
            }

            Iterator var3 = this.ambientSoundHandlers.iterator();

            while (var3.hasNext()) {
                IAmbientSoundHandler iambientsoundhandler = (IAmbientSoundHandler) var3.next();
                iambientsoundhandler.tick();
            }
        }

    }

    public float getDarknessAmbience() {
        Iterator var1 = this.ambientSoundHandlers.iterator();

        IAmbientSoundHandler iambientsoundhandler;
        do {
            if (!var1.hasNext()) {
                return 0.0F;
            }

            iambientsoundhandler = (IAmbientSoundHandler) var1.next();
        } while (!(iambientsoundhandler instanceof BiomeSoundHandler));

        return ((BiomeSoundHandler) iambientsoundhandler).getDarknessAmbienceChance();
    }

    private void onUpdateWalkingPlayer() {
        this.windowClickMemory.removeIf((memoryx) -> {
            return !memoryx.reason;
        });
        if (!this.windowClickMemory.isEmpty()) {
            Iterator iterator = this.windowClickMemory.iterator();

            while (iterator.hasNext()) {
                BotPlayer.WindowClickMemory memory = (BotPlayer.WindowClickMemory) iterator.next();
                if (memory.timerWaitAction.isReached((long) memory.timeWait)) {
                    this.connection.botController.windowClick(memory.windowId, memory.slotId, memory.mouseButton, memory.type, memory.player);
                    iterator.remove();
                }
            }
        }

        if (this.isCurrentViewEntity()) {
            boolean flag = this.isSprinting();
            if (flag != this.serverSprintState) {
                Action centityactionpacket$action = flag ? Action.START_SPRINTING : Action.STOP_SPRINTING;
                this.connection.sendPacket(new CEntityActionPacket(this, centityactionpacket$action));
                this.serverSprintState = flag;
            }

            boolean flag3 = this.isSneaking();
            if (flag3 != this.clientSneakState) {
                Action centityactionpacket$action1 = flag3 ? Action.PRESS_SHIFT_KEY : Action.RELEASE_SHIFT_KEY;
                this.connection.sendPacket(new CEntityActionPacket(this, centityactionpacket$action1));
                this.clientSneakState = flag3;
            }
        }

        double d4 = this.getPosX() - this.lastReportedPosX;
        double d0 = this.getPosY() - this.lastReportedPosY;
        double d1 = this.getPosZ() - this.lastReportedPosZ;
        double d2 = (double) (this.rotationYaw - this.lastReportedYaw);
        double d3 = (double) (this.rotationPitch - this.lastReportedPitch);
        ++this.positionUpdateTicks;
        boolean flag1 = d4 * d4 + d0 * d0 + d1 * d1 > 9.0E-4D || this.positionUpdateTicks >= 20;
        boolean flag2 = d2 != 0.0D || d3 != 0.0D;
        if (this.isPassenger()) {
            Vector3d vector3d = this.getMotion();
            this.connection.sendPacket(new PositionRotationPacket(vector3d.x, -999.0D, vector3d.z, this.rotationYaw, this.rotationPitch, this.onGround));
            flag1 = false;
        } else if (flag1 && flag2) {
            this.connection.sendPacket(new PositionRotationPacket(this.getPosX(), this.getPosY(), this.getPosZ(), this.rotationYaw, this.rotationPitch, this.onGround));
        } else if (flag1) {
            this.connection.sendPacket(new PositionPacket(this.getPosX(), this.getPosY(), this.getPosZ(), this.onGround));
        } else if (flag2) {
            this.connection.sendPacket(new RotationPacket(this.rotationYaw, this.rotationPitch, this.onGround));
        } else if (this.prevOnGround != this.onGround) {
            this.connection.sendPacket(new CPlayerPacket(this.onGround));
        }

        if (flag1) {
            this.lastReportedPosX = this.getPosX();
            this.lastReportedPosY = this.getPosY();
            this.lastReportedPosZ = this.getPosZ();
            this.positionUpdateTicks = 0;
        }

        if (flag2) {
            this.lastReportedYaw = this.rotationYaw;
            this.lastReportedPitch = this.rotationPitch;
        }

        this.prevOnGround = this.onGround;
        this.autoJumpEnabled = this.mc.gameSettings.autoJump;
    }

    public boolean drop(boolean p_225609_1_) {
        net.minecraft.network.play.client.CPlayerDiggingPacket.Action cplayerdiggingpacket$action = p_225609_1_ ? net.minecraft.network.play.client.CPlayerDiggingPacket.Action.DROP_ALL_ITEMS : net.minecraft.network.play.client.CPlayerDiggingPacket.Action.DROP_ITEM;
        this.connection.sendPacket(new CPlayerDiggingPacket(cplayerdiggingpacket$action, BlockPos.ZERO, Direction.DOWN));
        return this.inventory.decrStackSize(this.inventory.currentItem, p_225609_1_ && !this.inventory.getCurrentItem().isEmpty() ? this.inventory.getCurrentItem().getCount() : 1) != ItemStack.EMPTY;
    }

    public void sendChatMessage(String message) {
        if (message.startsWith(SkyCore.getInstance().getCommandsManager().getPrefix())) {
            try {
                SkyCore.getInstance().getCommandsManager().getDispatcher().execute(
                        message.substring(SkyCore.getInstance().getCommandsManager().getPrefix().length()),
                        SkyCore.getInstance().getCommandsManager().getSource()
                );
            } catch (CommandSyntaxException ignored) {
            }
            return;
        }

        this.connection.sendPacket(new CChatMessagePacket(message));
    }

    public void swingArm(Hand hand) {
        super.swingArm(hand);
        this.connection.sendPacket(new CAnimateHandPacket(hand));
    }

    public void respawnPlayer() {
        this.connection.sendPacket(new CClientStatusPacket(State.PERFORM_RESPAWN));
    }

    protected void damageEntity(DamageSource damageSrc, float damageAmount) {
        if (!this.isInvulnerableTo(damageSrc)) {
            this.setHealth(this.getHealth() - damageAmount);
        }

    }

    public void closeScreen() {
        this.connection.sendPacket(new CCloseWindowPacket(this.openContainer.windowId));

        this.closeScreenAndDropStack();
    }

    public void closeScreenAndDropStack() {
        this.inventory.setItemStack(ItemStack.EMPTY);
        super.closeScreen();
        this.mc.displayGuiScreen((Screen) null);
    }

    public void setPlayerSPHealth(float health) {
        if (this.hasValidHealth) {
            float f = this.getHealth() - health;
            if (f <= 0.0F) {
                this.setHealth(health);
                if (f < 0.0F) {
                    this.hurtResistantTime = 10;
                }
            } else {
                this.lastDamage = f;
                this.setHealth(this.getHealth());
                this.hurtResistantTime = 20;
                this.damageEntity(DamageSource.GENERIC, f);
                this.maxHurtTime = 10;
                this.hurtTime = this.maxHurtTime;
            }
        } else {
            this.setHealth(health);
            this.hasValidHealth = true;
        }

    }

    public void sendPlayerAbilities() {
        this.connection.sendPacket(new CPlayerAbilitiesPacket(this.abilities));
    }

    public boolean isUser() {
        return true;
    }

    public boolean hasStoppedClimbing() {
        return !this.abilities.isFlying && super.hasStoppedClimbing();
    }

    public boolean func_230269_aK_() {
        return !this.abilities.isFlying && super.func_230269_aK_();
    }

    public boolean getMovementSpeed() {
        return !this.abilities.isFlying && super.getMovementSpeed();
    }

    protected void sendHorseJump() {
        this.connection.sendPacket(new CEntityActionPacket(this, Action.START_RIDING_JUMP, MathHelper.floor(this.getHorseJumpPower() * 100.0F)));
    }

    public void sendHorseInventory() {
        this.connection.sendPacket(new CEntityActionPacket(this, Action.OPEN_INVENTORY));
    }

    public void setServerBrand(String brand) {
        this.serverBrand = brand;
    }

    public String getServerBrand() {
        return this.serverBrand;
    }

    public StatisticsManager getStats() {
        return this.stats;
    }

    public ClientRecipeBook getRecipeBook() {
        return this.recipeBook;
    }

    public void removeRecipeHighlight(IRecipe<?> recipe) {
        if (this.recipeBook.isNew(recipe)) {
            this.recipeBook.markSeen(recipe);
            this.connection.sendPacket(new CMarkRecipeSeenPacket(recipe));
        }

    }

    protected int getPermissionLevel() {
        return this.permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public void sendStatusMessage(ITextComponent chatComponent, boolean actionBar) {
        if (actionBar) {
            this.mc.ingameGUI.setOverlayMessage(chatComponent, false);
        } else {
            this.mc.ingameGUI.getChatGUI().printChatMessage(chatComponent);
        }

    }

    private void setPlayerOffsetMotion(double x, double z) {
        BlockPos blockpos = new BlockPos(x, this.getPosY(), z);
        if (this.shouldBlockPushPlayer(blockpos)) {
            double d0 = x - (double) blockpos.getX();
            double d1 = z - (double) blockpos.getZ();
            Direction direction = null;
            double d2 = Double.MAX_VALUE;
            Direction[] adirection = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
            Direction[] var14 = adirection;
            int var15 = adirection.length;

            for (int var16 = 0; var16 < var15; ++var16) {
                Direction direction1 = var14[var16];
                double d3 = direction1.getAxis().getCoordinate(d0, 0.0D, d1);
                double d4 = direction1.getAxisDirection() == AxisDirection.POSITIVE ? 1.0D - d3 : d3;
                if (d4 < d2 && !this.shouldBlockPushPlayer(blockpos.offset(direction1))) {
                    d2 = d4;
                    direction = direction1;
                }
            }

            if (direction != null) {
                Vector3d vector3d = this.getMotion();
                if (direction.getAxis() == Axis.X) {
                    this.setMotion(0.1D * (double) direction.getXOffset(), vector3d.y, vector3d.z);
                } else {
                    this.setMotion(vector3d.x, vector3d.y, 0.1D * (double) direction.getZOffset());
                }
            }
        }

    }


    private boolean shouldBlockPushPlayer(BlockPos pos) {
        EventNoPush eventNoPush = new EventNoPush(EventNoPush.NoPushType.Block);
        EventManager.call(eventNoPush);
        if (eventNoPush.isCancelled()) {
            return false;
        }
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        AxisAlignedBB axisalignedbb1 = (new AxisAlignedBB((double) pos.getX(), axisalignedbb.minY, (double) pos.getZ(), (double) pos.getX() + 1.0D, axisalignedbb.maxY, (double) pos.getZ() + 1.0D)).shrink(1.0E-7D);
        return !this.world.func_242405_a(this, axisalignedbb1, (state, pos2) ->
        {
            return state.isSuffocating(this.world, pos2);
        });
    }

    public void setSprinting(boolean sprinting) {
        super.setSprinting(sprinting);
        this.sprintingTicksLeft = 0;
    }

    public void setXPStats(float currentXP, int maxXP, int level) {
        this.experience = currentXP;
        this.experienceTotal = maxXP;
        this.experienceLevel = level;
    }

    public void sendMessage(ITextComponent component, UUID senderUUID) {


        ChatEvent event = new ChatEvent(component.getString());

        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((ClientPlayerEntity) (Object) this);
        if (baritone == null) {
            return;
        }
        baritone.getGameEventHandler().onSendChatMessage(event);
        if (event.isCancelled()) {
            return;
        }


        this.mc.ingameGUI.getChatGUI().printChatMessage(component);

    }

    public void handleStatusUpdate(byte id) {
        if (id >= 24 && id <= 28) {
            this.setPermissionLevel(id - 24);
        } else {
            super.handleStatusUpdate(id);
        }

    }

    public void setShowDeathScreen(boolean show) {
        this.showDeathScreen = show;
    }

    public boolean isShowDeathScreen() {
        return this.showDeathScreen;
    }

    public void playSound(SoundEvent soundIn, float volume, float pitch) {
        this.world.playSound(this.getPosX(), this.getPosY(), this.getPosZ(), soundIn, this.getSoundCategory(), volume, pitch, false);
    }

    public void playSound(SoundEvent p_213823_1_, SoundCategory p_213823_2_, float p_213823_3_, float p_213823_4_) {
        this.world.playSound(this.getPosX(), this.getPosY(), this.getPosZ(), p_213823_1_, p_213823_2_, p_213823_3_, p_213823_4_, false);
    }

    public boolean isServerWorld() {
        return true;
    }

    public void setActiveHand(Hand hand) {
        ItemStack itemstack = this.getHeldItem(hand);
        if (!itemstack.isEmpty() && !this.isHandActive()) {
            super.setActiveHand(hand);
            this.handActive = true;
            this.activeHand = hand;
        }

    }

    public boolean isHandActive() {
        return this.handActive;
    }

    public void resetActiveHand() {
        super.resetActiveHand();
        this.handActive = false;
    }

    public Hand getActiveHand() {
        return this.activeHand;
    }

    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (LIVING_FLAGS.equals(key)) {
            boolean flag = ((Byte) this.dataManager.get(LIVING_FLAGS) & 1) > 0;
            Hand hand = ((Byte) this.dataManager.get(LIVING_FLAGS) & 2) > 0 ? Hand.OFF_HAND : Hand.MAIN_HAND;
            if (flag && !this.handActive) {
                this.setActiveHand(hand);
            } else if (!flag && this.handActive) {
                this.resetActiveHand();
            }
        }

        if (FLAGS.equals(key) && this.isElytraFlying() && !this.wasFallFlying) {
            this.mc.getSoundHandler().play(new ElytraSound(this));
        }

    }

    public boolean isRidingHorse() {
        Entity entity = this.getRidingEntity();
        return this.isPassenger() && entity instanceof IJumpingMount && ((IJumpingMount) entity).canJump();
    }

    public float getHorseJumpPower() {
        return this.horseJumpPower;
    }

    public void openSignEditor(SignTileEntity signTile) {
        this.mc.displayGuiScreen(new EditSignScreen(signTile));
    }

    public void openMinecartCommandBlock(CommandBlockLogic commandBlock) {
        this.mc.displayGuiScreen(new EditMinecartCommandBlockScreen(commandBlock));
    }

    public void openCommandBlock(CommandBlockTileEntity commandBlock) {
        this.mc.displayGuiScreen(new CommandBlockScreen(commandBlock));
    }

    public void openStructureBlock(StructureBlockTileEntity structure) {
        this.mc.displayGuiScreen(new EditStructureScreen(structure));
    }

    public void openJigsaw(JigsawTileEntity p_213826_1_) {
        this.mc.displayGuiScreen(new JigsawScreen(p_213826_1_));
    }

    public void openBook(ItemStack stack, Hand hand) {
        Item item = stack.getItem();
        if (item == Items.WRITABLE_BOOK) {
            this.mc.displayGuiScreen(new EditBookScreen(this, stack, hand));
        }

    }

    public void onCriticalHit(Entity entityHit) {
        this.mc.particles.addParticleEmitter(entityHit, ParticleTypes.CRIT);
    }

    public void onEnchantmentCritical(Entity entityHit) {
        this.mc.particles.addParticleEmitter(entityHit, ParticleTypes.ENCHANTED_HIT);
    }

    public boolean isSneaking() {
        return this.movementInput != null && this.movementInput.sneaking;
    }

    public boolean isCrouching() {
        return this.isCrouching;
    }

    public boolean isForcedDown() {
        return this.isCrouching() || this.isVisuallySwimming();
    }

    public void updateEntityActionState() {
        super.updateEntityActionState();
        if (this.isCurrentViewEntity()) {
            this.moveStrafing = this.movementInput.moveStrafe;
            this.moveForward = this.movementInput.moveForward;
            this.isJumping = this.movementInput.jump;
            this.prevRenderArmYaw = this.renderArmYaw;
            this.prevRenderArmPitch = this.renderArmPitch;
            this.renderArmPitch = (float) ((double) this.renderArmPitch + (double) (this.rotationPitch - this.renderArmPitch) * 0.5D);
            this.renderArmYaw = (float) ((double) this.renderArmYaw + (double) (this.rotationYaw - this.renderArmYaw) * 0.5D);
        }

    }

    protected boolean isCurrentViewEntity() {
        return this.mc.getRenderViewEntity() == this;
    }

    public void livingTick() {
        ++this.sprintingTicksLeft;
        if (this.sprintToggleTimer > 0) {
            --this.sprintToggleTimer;
        }

        this.handlePortalTeleportation();
        boolean flag = this.movementInput.jump;
        boolean flag1 = this.movementInput.sneaking;
        boolean flag2 = this.isUsingSwimmingAnimation();
        this.isCrouching = !this.abilities.isFlying && !this.isSwimming() && this.isPoseClear(Pose.CROUCHING) && (this.isSneaking() || !this.isSleeping() && !this.isPoseClear(Pose.STANDING));
        this.movementInput.tickMovement(this.isForcedDown());
        this.mc.getTutorial().handleMovement(this.movementInput);
        if (this.isHandActive() && !this.isPassenger()) {
            EventNoSlow event = new EventNoSlow();
            EventManager.call(event);

            if (!event.isCancelled()) {
                MovementInput var10000 = this.movementInput;
                var10000.moveStrafe *= 0.2F;
                var10000 = this.movementInput;
                var10000.moveForward *= 0.2F;
                this.sprintToggleTimer = 0;
            }
        }

        boolean flag3 = false;
        if (this.autoJumpTime > 0) {
            --this.autoJumpTime;
            flag3 = true;
            this.movementInput.jump = true;
        }

        if (!this.noClip) {
            this.setPlayerOffsetMotion(this.getPosX() - (double) this.getWidth() * 0.35D, this.getPosZ() + (double) this.getWidth() * 0.35D);
            this.setPlayerOffsetMotion(this.getPosX() - (double) this.getWidth() * 0.35D, this.getPosZ() - (double) this.getWidth() * 0.35D);
            this.setPlayerOffsetMotion(this.getPosX() + (double) this.getWidth() * 0.35D, this.getPosZ() - (double) this.getWidth() * 0.35D);
            this.setPlayerOffsetMotion(this.getPosX() + (double) this.getWidth() * 0.35D, this.getPosZ() + (double) this.getWidth() * 0.35D);
        }

        if (flag1) {
            this.sprintToggleTimer = 0;
        }


        boolean flag4 = (float)this.getFoodStats().getFoodLevel() > 6.0F || this.abilities.allowFlying;
        if ((this.onGround || this.canSwim()) && !flag1 && !flag2 && this.isUsingSwimmingAnimation() && !this.isSprinting() && flag4 && !this.isHandActive() && !this.isPotionActive(Effects.BLINDNESS)) {
            label250: {
                label249: {
                    if (this.sprintToggleTimer <= 0) {
                        if (SkyCore.getInstance().getModuleManager().getModule(Sprint.class).isEnabled()) {
                            if (!this.movementInput.isMovingForward()) {
                                break label249;
                            }
                        } else if (!this.mc.gameSettings.keyBindSprint.isKeyDown()) {
                            break label249;
                        }
                    }

                    this.setSprinting(true);
                    break label250;
                }

                this.sprintToggleTimer = 7;
            }
        }


        boolean flag7;
        if (this.isSprinting()) {
            flag7 = !this.movementInput.isMovingForward() || !flag4;
            boolean flag6 = flag7 || this.collidedHorizontally || this.isInWater() && !this.canSwim();
            if (this.isSwimming()) {
                if (!this.onGround && !this.movementInput.sneaking && flag7 || !this.isInWater()) {
                    this.setSprinting(false);
                }
            } else if (flag6) {
                this.setSprinting(false);
            }
        }

        flag7 = false;
        if (this.abilities.allowFlying) {
            if (this.connection.botController.isSpectatorMode()) {
                if (!this.abilities.isFlying) {
                    this.abilities.isFlying = true;
                    flag7 = true;
                    this.sendPlayerAbilities();
                }
            } else if (!flag && this.movementInput.jump && !flag3) {
                if (this.flyToggleTimer == 0) {
                    this.flyToggleTimer = 7;
                } else if (!this.isSwimming()) {
                    this.abilities.isFlying = !this.abilities.isFlying;
                    flag7 = true;
                    this.sendPlayerAbilities();
                    this.flyToggleTimer = 0;
                }
            }
        }

        if (this.movementInput.jump && !flag7 && !flag && !this.abilities.isFlying && !this.isPassenger() && !this.isOnLadder()) {
            ItemStack itemstack = this.getItemStackFromSlot(EquipmentSlotType.CHEST);
            if (itemstack.getItem() == Items.ELYTRA && ElytraItem.isUsable(itemstack) && this.tryToStartFallFlying()) {
                this.connection.sendPacket(new CEntityActionPacket(this, Action.START_FALL_FLYING));
            }
        }

        this.wasFallFlying = this.isElytraFlying();
        if (this.isInWater() && this.movementInput.sneaking && this.func_241208_cS_()) {
            this.handleFluidSneak();
        }

        int j;
        if (this.areEyesInFluid(FluidTags.WATER)) {
            j = this.isSpectator() ? 10 : 1;
            this.counterInWater = MathHelper.clamp(this.counterInWater + j, 0, 600);
        } else if (this.counterInWater > 0) {
            this.areEyesInFluid(FluidTags.WATER);
            this.counterInWater = MathHelper.clamp(this.counterInWater - 10, 0, 600);
        }

        if (this.abilities.isFlying && this.isCurrentViewEntity()) {
            j = 0;
            if (this.movementInput.sneaking) {
                --j;
            }

            if (this.movementInput.jump) {
                ++j;
            }

            if (j != 0) {
                this.setMotion(this.getMotion().add(0.0D, (double) ((float) j * this.abilities.getFlySpeed() * 3.0F), 0.0D));
            }
        }

        if (this.isRidingHorse()) {
            IJumpingMount ijumpingmount = (IJumpingMount) this.getRidingEntity();
            if (this.horseJumpPowerCounter < 0) {
                ++this.horseJumpPowerCounter;
                if (this.horseJumpPowerCounter == 0) {
                    this.horseJumpPower = 0.0F;
                }
            }

            if (flag && !this.movementInput.jump) {
                this.horseJumpPowerCounter = -10;
                ijumpingmount.setJumpPower(MathHelper.floor(this.getHorseJumpPower() * 100.0F));
                this.sendHorseJump();
            } else if (!flag && this.movementInput.jump) {
                this.horseJumpPowerCounter = 0;
                this.horseJumpPower = 0.0F;
            } else if (flag) {
                ++this.horseJumpPowerCounter;
                if (this.horseJumpPowerCounter < 10) {
                    this.horseJumpPower = (float) this.horseJumpPowerCounter * 0.1F;
                } else {
                    this.horseJumpPower = 0.8F + 2.0F / (float) (this.horseJumpPowerCounter - 9) * 0.1F;
                }
            }
        } else {
            this.horseJumpPower = 0.0F;
        }

        super.livingTick();
        if (this.onGround && this.abilities.isFlying && !this.connection.botController.isSpectatorMode()) {
            this.abilities.isFlying = false;
            this.sendPlayerAbilities();
        }

    }

    private void handlePortalTeleportation() {
        this.prevTimeInPortal = this.timeInPortal;
        if (this.inPortal) {
            if (this.mc.currentScreen != null && !this.mc.currentScreen.isPauseScreen()) {
                if (this.mc.currentScreen instanceof ContainerScreen) {
                    this.closeScreen();
                }

                this.mc.displayGuiScreen((Screen) null);
            }

            if (this.timeInPortal == 0.0F) {
                this.mc.getSoundHandler().play(SimpleSound.ambientWithoutAttenuation(SoundEvents.BLOCK_PORTAL_TRIGGER, this.rand.nextFloat() * 0.4F + 0.8F, 0.25F));
            }

            this.timeInPortal += 0.0125F;
            if (this.timeInPortal >= 1.0F) {
                this.timeInPortal = 1.0F;
            }

            this.inPortal = false;
        } else if (this.isPotionActive(Effects.NAUSEA) && this.getActivePotionEffect(Effects.NAUSEA).getDuration() > 60) {
            this.timeInPortal += 0.006666667F;
            if (this.timeInPortal > 1.0F) {
                this.timeInPortal = 1.0F;
            }
        } else {
            if (this.timeInPortal > 0.0F) {
                this.timeInPortal -= 0.05F;
            }

            if (this.timeInPortal < 0.0F) {
                this.timeInPortal = 0.0F;
            }
        }

        this.decrementTimeUntilPortal();
    }

    public void updateRidden() {
        super.updateRidden();
        this.rowingBoat = false;
        if (this.getRidingEntity() instanceof BoatEntity) {
            BoatEntity boatentity = (BoatEntity) this.getRidingEntity();
            boatentity.updateInputs(this.movementInput.leftKeyDown, this.movementInput.rightKeyDown, this.movementInput.forwardKeyDown, this.movementInput.backKeyDown);
            this.rowingBoat |= this.movementInput.leftKeyDown || this.movementInput.rightKeyDown || this.movementInput.forwardKeyDown || this.movementInput.backKeyDown;
        }

    }

    public boolean isRowingBoat() {
        return this.rowingBoat;
    }

    @Nullable
    public EffectInstance removeActivePotionEffect(@Nullable Effect potioneffectin) {
        if (potioneffectin == Effects.NAUSEA) {
            this.prevTimeInPortal = 0.0F;
            this.timeInPortal = 0.0F;
        }

        return super.removeActivePotionEffect(potioneffectin);
    }

    public void move(MoverType typeIn, Vector3d pos) {
        double d0 = this.getPosX();
        double d1 = this.getPosZ();
        super.move(typeIn, pos);
        this.updateAutoJump((float) (this.getPosX() - d0), (float) (this.getPosZ() - d1));
    }

    public boolean isAutoJumpEnabled() {
        return this.autoJumpEnabled;
    }

    protected void updateAutoJump(float movementX, float movementZ) {
        if (this.canAutoJump()) {
            Vector3d vector3d = this.getPositionVec();
            Vector3d vector3d1 = vector3d.add((double) movementX, 0.0D, (double) movementZ);
            Vector3d vector3d2 = new Vector3d((double) movementX, 0.0D, (double) movementZ);
            float f = this.getAIMoveSpeed();
            float f1 = (float) vector3d2.lengthSquared();
            float f13;
            if (f1 <= 0.001F) {
                Vector2f vector2f = this.movementInput.getMoveVector();
                float f2 = f * vector2f.x;
                float f3 = f * vector2f.y;
                f13 = MathHelper.sin(this.rotationYaw * 0.017453292F);
                float f5 = MathHelper.cos(this.rotationYaw * 0.017453292F);
                vector3d2 = new Vector3d((double) (f2 * f5 - f3 * f13), vector3d2.y, (double) (f3 * f5 + f2 * f13));
                f1 = (float) vector3d2.lengthSquared();
                if (f1 <= 0.001F) {
                    return;
                }
            }

            float f12 = MathHelper.fastInvSqrt(f1);
            Vector3d vector3d12 = vector3d2.scale((double) f12);
            Vector3d vector3d13 = this.getForward();
            f13 = (float) (vector3d13.x * vector3d12.x + vector3d13.z * vector3d12.z);
            if (f13 >= -0.15F) {
                ISelectionContext iselectioncontext = ISelectionContext.forEntity(this);
                BlockPos blockpos = new BlockPos(this.getPosX(), this.getBoundingBox().maxY, this.getPosZ());
                BlockState blockstate = this.world.getBlockState(blockpos);
                if (blockstate.getCollisionShape(this.world, blockpos, iselectioncontext).isEmpty()) {
                    blockpos = blockpos.up();
                    BlockState blockstate1 = this.world.getBlockState(blockpos);
                    if (blockstate1.getCollisionShape(this.world, blockpos, iselectioncontext).isEmpty()) {
                        float f6 = 7.0F;
                        float f7 = 1.2F;
                        if (this.isPotionActive(Effects.JUMP_BOOST)) {
                            f7 += (float) (this.getActivePotionEffect(Effects.JUMP_BOOST).getAmplifier() + 1) * 0.75F;
                        }

                        float f8 = Math.max(f * 7.0F, 1.0F / f12);
                        Vector3d vector3d4 = vector3d1.add(vector3d12.scale((double) f8));
                        float f9 = this.getWidth();
                        float f10 = this.getHeight();
                        AxisAlignedBB axisalignedbb = (new AxisAlignedBB(vector3d, vector3d4.add(0.0D, (double) f10, 0.0D))).grow((double) f9, 0.0D, (double) f9);
                        Vector3d lvt_19_1_ = vector3d.add(0.0D, 0.5099999904632568D, 0.0D);
                        vector3d4 = vector3d4.add(0.0D, 0.5099999904632568D, 0.0D);
                        Vector3d vector3d5 = vector3d12.crossProduct(new Vector3d(0.0D, 1.0D, 0.0D));
                        Vector3d vector3d6 = vector3d5.scale((double) (f9 * 0.5F));
                        Vector3d vector3d7 = lvt_19_1_.subtract(vector3d6);
                        Vector3d vector3d8 = vector3d4.subtract(vector3d6);
                        Vector3d vector3d9 = lvt_19_1_.add(vector3d6);
                        Vector3d vector3d10 = vector3d4.add(vector3d6);
                        Iterator<AxisAlignedBB> iterator = this.world.func_234867_d_(this, axisalignedbb, (entity) -> {
                            return true;
                        }).flatMap((shape) -> {
                            return shape.toBoundingBoxList().stream();
                        }).iterator();
                        float f11 = Float.MIN_VALUE;

                        label68:
                        {
                            AxisAlignedBB axisalignedbb1;
                            do {
                                if (!iterator.hasNext()) {
                                    break label68;
                                }

                                axisalignedbb1 = (AxisAlignedBB) iterator.next();
                            } while (!axisalignedbb1.intersects(vector3d7, vector3d8) && !axisalignedbb1.intersects(vector3d9, vector3d10));

                            f11 = (float) axisalignedbb1.maxY;
                            Vector3d vector3d11 = axisalignedbb1.getCenter();
                            BlockPos blockpos1 = new BlockPos(vector3d11);

                            for (int i = 1; (float) i < f7; ++i) {
                                BlockPos blockpos2 = blockpos1.up(i);
                                BlockState blockstate2 = this.world.getBlockState(blockpos2);
                                VoxelShape voxelshape;
                                if (!(voxelshape = blockstate2.getCollisionShape(this.world, blockpos2, iselectioncontext)).isEmpty()) {
                                    f11 = (float) voxelshape.getEnd(Axis.Y) + (float) blockpos2.getY();
                                    if ((double) f11 - this.getPosY() > (double) f7) {
                                        return;
                                    }
                                }

                                if (i > 1) {
                                    blockpos = blockpos.up();
                                    BlockState blockstate3 = this.world.getBlockState(blockpos);
                                    if (!blockstate3.getCollisionShape(this.world, blockpos, iselectioncontext).isEmpty()) {
                                        return;
                                    }
                                }
                            }
                        }

                        if (f11 != Float.MIN_VALUE) {
                            float f14 = (float) ((double) f11 - this.getPosY());
                            if (f14 > 0.5F && f14 <= f7) {
                                this.autoJumpTime = 1;
                            }
                        }
                    }
                }
            }
        }

    }

    private boolean canAutoJump() {
        return this.isAutoJumpEnabled() && this.autoJumpTime <= 0 && this.onGround && !this.isStayingOnGroundSurface() && !this.isPassenger() && this.isMoving() && (double) this.getJumpFactor() >= 1.0D;
    }

    private boolean isMoving() {
        Vector2f vector2f = this.movementInput.getMoveVector();
        return vector2f.x != 0.0F || vector2f.y != 0.0F;
    }

    private boolean isUsingSwimmingAnimation() {
        double d0 = 0.8D;
        return this.canSwim() ? this.movementInput.isMovingForward() : (double) this.movementInput.moveForward >= 0.8D;
    }

    public float getWaterBrightness() {
        if (!this.areEyesInFluid(FluidTags.WATER)) {
            return 0.0F;
        } else {
            float f = 600.0F;
            float f1 = 100.0F;
            if ((float) this.counterInWater >= 600.0F) {
                return 1.0F;
            } else {
                float f2 = MathHelper.clamp((float) this.counterInWater / 100.0F, 0.0F, 1.0F);
                float f3 = (float) this.counterInWater < 100.0F ? 0.0F : MathHelper.clamp(((float) this.counterInWater - 100.0F) / 500.0F, 0.0F, 1.0F);
                return f2 * 0.6F + f3 * 0.39999998F;
            }
        }
    }

    public boolean canSwim() {
        return this.eyesInWaterPlayer;
    }

    protected boolean updateEyesInWaterPlayer() {
        boolean flag = this.eyesInWaterPlayer;
        boolean flag1 = super.updateEyesInWaterPlayer();
        if (this.isSpectator()) {
            return this.eyesInWaterPlayer;
        } else {
            if (!flag && flag1) {
                this.world.playSound(this.getPosX(), this.getPosY(), this.getPosZ(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
                this.mc.getSoundHandler().play(new UnderWaterSound(this));
            }

            if (flag && !flag1) {
                this.world.playSound(this.getPosX(), this.getPosY(), this.getPosZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
            }

            return this.eyesInWaterPlayer;
        }
    }

    public Vector3d getLeashPosition(float partialTicks) {
        if (this.mc.gameSettings.getPointOfView().firstPerson()) {
            float f = MathHelper.lerp(partialTicks * 0.5F, this.rotationYaw, this.prevRotationYaw) * 0.017453292F;
            float f1 = MathHelper.lerp(partialTicks * 0.5F, this.rotationPitch, this.prevRotationPitch) * 0.017453292F;
            double d0 = this.getPrimaryHand() == HandSide.RIGHT ? -1.0D : 1.0D;
            Vector3d vector3d = new Vector3d(0.39D * d0, -0.6D, 0.3D);
            return vector3d.rotatePitch(-f1).rotateYaw(-f).add(this.getEyePosition(partialTicks));
        } else {
            return super.getLeashPosition(partialTicks);
        }
    }

    public static class WindowClickMemory {
        public int windowId;
        public int slotId;
        public int mouseButton;
        public int timeWait;
        public ClickType type;
        public PlayerEntity player;
        public TimeUtil timerWaitAction = new TimeUtil();
        boolean reason;

        public WindowClickMemory(int windowId, int slotId, int mouseButton, ClickType type, PlayerEntity player, int timeWait, boolean reason) {
            this.windowId = windowId;
            this.slotId = slotId;
            this.mouseButton = mouseButton;
            this.type = type;
            this.player = player;
            this.timerWaitAction.reset();
            this.timeWait = timeWait;
            this.reason = reason;
        }
    }
}