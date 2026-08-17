package net.minecraft.client.gui.screen.inventory;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import other.bot.Bot;
import other.bot.BotManager;
import sky.core.SkyCore;
import sky.core.modules.impl.visuals.Animation;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DisplayEffectsScreen;
import net.minecraft.client.gui.recipebook.IRecipeShownListener;
import net.minecraft.client.gui.recipebook.RecipeBookGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class InventoryScreen extends DisplayEffectsScreen<PlayerContainer> implements IRecipeShownListener {
    private static final ResourceLocation RECIPE_BUTTON_TEXTURE = new ResourceLocation("textures/gui/recipe_button.png");

    /**
     * The old x position of the mouse pointer
     */
    private float oldMouseX;

    /**
     * The old y position of the mouse pointer
     */
    private float oldMouseY;
    private final RecipeBookGui recipeBookGui = new RecipeBookGui();
    private boolean removeRecipeBookGui;
    private boolean widthTooNarrow;
    private boolean buttonClicked;
    private Button dropAllButton;
    private boolean shouldDropItems;
    private TimeUtil timerDelayDrop = new TimeUtil();

    private final AnimationUtil openScaleAnim = new AnimationUtil(0, 12.0f, Easings.CUBIC_OUT);

    private boolean isInventoryAnimationEnabled() {
        Animation animationModule = (Animation) SkyCore.getInstance().getModuleManager().getModule(Animation.class);
        if (animationModule == null || !animationModule.isEnabled()) return false;
        Boolean enabled = animationModule.mode.is("Инвентарь");
        return Boolean.TRUE.equals(enabled);
    }

    public InventoryScreen(PlayerEntity player) {
        super(player.container, player.inventory, new TranslationTextComponent("container.crafting"));
        this.passEvents = true;
        this.titleX = 97;
    }

    public void tick() {
        if (this.minecraft == null) return;

        Bot bot1 = null;
        boolean botsImprovements = false;

        for (Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
                botsImprovements = true;
                break;
            }
        }

        boolean isInCreative = (botsImprovements && bot1 != null)
                ? bot1.botController.isInCreativeMode()
                : this.minecraft.playerController.isInCreativeMode();

        if (isInCreative) {
            if (botsImprovements) {
                this.minecraft.displayGuiScreen(new CreativeScreen(bot1.connection.bot));
            } else {
                this.minecraft.displayGuiScreen(new CreativeScreen(this.minecraft.player));
            }
            return;
        }

        this.recipeBookGui.tick();

        if (this.dropAllButton != null) {
            this.dropAllButton.active = !this.minecraft.player.inventory.isEmpty();
        }

        if (shouldDropItems && container != null) {
            if (timerDelayDrop.hasTimeElapsed(50)) {
                int maxDrops = 5;
                boolean anyItemToDrop = false;

                for (int iaa = 0; iaa < maxDrops; iaa++) {
                    for (Slot slot : container.inventorySlots) {
                        if (slot.getHasStack()) {
                            // Определяем цель для клика (бот или игрок)
                            var entityPlayer = botsImprovements ? bot1.connection.bot : this.minecraft.player;

                            this.minecraft.playerController.windowClick(
                                    this.container.windowId,
                                    slot.slotNumber,
                                    slot.getStack().getCount() > 1 ? 1 : 0,
                                    ClickType.THROW,
                                    entityPlayer
                            );
                            anyItemToDrop = true;
                            break;
                        }
                    }
                }

                if (!anyItemToDrop) {
                    shouldDropItems = false;
                    this.closeScreen();
                }
                timerDelayDrop.reset();
            }
        }
    }

    protected void init() {
        Bot bot1 = null;
        boolean botsImprovements = false;

        // Поиск активного бота
        if (this.minecraft != null) {
            for (Bot bot : BotManager.allBots) {
                if (this.minecraft.renderViewEntity == bot.connection.bot) {
                    bot1 = bot;
                    botsImprovements = true;
                    break;
                }
            }
        }

        // Проверка креативного режима (игрок или бот)
        boolean playerInCreative = this.minecraft.playerController.isInCreativeMode();
        boolean botInCreative = (bot1 != null && bot1.botController.isInCreativeMode());

        if (playerInCreative || botInCreative) {
            if (botsImprovements) {
                this.minecraft.displayGuiScreen(new CreativeScreen(bot1.connection.bot));
            } else {
                this.minecraft.displayGuiScreen(new CreativeScreen(this.minecraft.player));
            }
        } else {
            // Режим выживания
            super.init();

            // Кнопка "Выбросить все" с учетом ваших координат и проверки UnHook
            this.dropAllButton = this.addButton(new Button(width / 2 - 50, height / 2 - 105, 100, 20, new StringTextComponent("Выбросить все"), (button) -> {
                shouldDropItems = true;
                timerDelayDrop.reset();
            }));

            // Состояние активности кнопки зависит от инвентаря игрока или бота
            if (botsImprovements) {
                this.dropAllButton.active = !bot1.connection.bot.inventory.isEmpty();
            } else {
                this.dropAllButton.active = !this.minecraft.player.inventory.isEmpty();
            }


            // Логика книги рецептов
            this.widthTooNarrow = this.width < 379;
            this.recipeBookGui.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.container);
            this.removeRecipeBookGui = true;
            this.guiLeft = this.recipeBookGui.updateScreenPosition(this.widthTooNarrow, this.width, this.xSize);
            this.children.add(this.recipeBookGui);
            this.setFocusedDefault(this.recipeBookGui);

            // Кнопка открытия книги рецептов
            this.addButton(new ImageButton(this.guiLeft + 104, this.height / 2 - 22, 20, 18, 0, 0, 19, RECIPE_BUTTON_TEXTURE, (button) -> {
                this.recipeBookGui.initSearchBar(this.widthTooNarrow);
                this.recipeBookGui.toggleVisibility();
                this.guiLeft = this.recipeBookGui.updateScreenPosition(this.widthTooNarrow, this.width, this.xSize);
                ((ImageButton) button).setPosition(this.guiLeft + 104, this.height / 2 - 22);
                this.buttonClicked = true;
            }));

            // Анимация открытия
            if (isInventoryAnimationEnabled()) {
                openScaleAnim.setValue(0.5f);
            }
        }
    }

    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {
        this.font.func_243248_b(matrixStack, this.title, (float) this.titleX, (float) this.titleY, 4210752);
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        boolean doAnim = isInventoryAnimationEnabled();
        if (doAnim) {
            openScaleAnim.update(1.0f);
            RenderUtil.scaleStart(this.width / 2.0f, this.height / 2.0f, openScaleAnim.getValue());
        }
        this.hasActivePotionEffects = !this.recipeBookGui.isVisible();

        if (this.recipeBookGui.isVisible() && this.widthTooNarrow) {
            this.drawGuiContainerBackgroundLayer(matrixStack, partialTicks, mouseX, mouseY);
            this.recipeBookGui.render(matrixStack, mouseX, mouseY, partialTicks);
        } else {
            this.recipeBookGui.render(matrixStack, mouseX, mouseY, partialTicks);
            super.render(matrixStack, mouseX, mouseY, partialTicks);
            this.recipeBookGui.func_230477_a_(matrixStack, this.guiLeft, this.guiTop, false, partialTicks);
        }

        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
        this.recipeBookGui.func_238924_c_(matrixStack, this.guiLeft, this.guiTop, mouseX, mouseY);
        this.oldMouseX = (float) mouseX;
        this.oldMouseY = (float) mouseY;
        if (doAnim) {
            RenderUtil.scaleEnd();
        }
    }

    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
        int i = this.guiLeft;
        int j = this.guiTop;
        this.blit(matrixStack, i, j, 0, 0, this.xSize, this.ySize);
        ClientPlayerEntity player = this.minecraft.player;

        Bot bot1 = null;

        for (Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
            }
        }
        drawEntityOnScreen(i + 51, j + 75, 30, (float) (i + 51) - (float) x, (float) (j + 75 - 50) - (float) y, (LivingEntity) (bot1 != null ? this.minecraft.getBotPlayer() : player));
    }

    public static void drawEntityOnScreen(int posX, int posY, int scale, float mouseX, float mouseY, LivingEntity livingEntity) {
        float f = (float) Math.atan((double) (mouseX / 40.0F));
        float f1 = (float) Math.atan((double) (mouseY / 40.0F));
        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) posX, (float) posY, 1050.0F);
        RenderSystem.scalef(1.0F, 1.0F, -1.0F);
        MatrixStack matrixstack = new MatrixStack();
        matrixstack.translate(0.0D, 0.0D, 1000.0D);
        matrixstack.scale((float) scale, (float) scale, (float) scale);
        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion1 = Vector3f.XP.rotationDegrees(f1 * 20.0F);
        quaternion.multiply(quaternion1);
        matrixstack.rotate(quaternion);
        float f2 = livingEntity.renderYawOffset;
        float f3 = livingEntity.rotationYaw;
        float f4 = livingEntity.rotationPitch;
        float f4Prev = livingEntity.prevRotationPitch;
        float f3Prev = livingEntity.prevRotationYaw;
        float f2Prev = livingEntity.prevRenderYawOffset;
        float f5 = livingEntity.prevRotationYawHead;
        float f6 = livingEntity.rotationYawHead;
        float f7 = livingEntity.prevRotationPitchHead;
        float f8 = livingEntity.rotationPitchHead;
        float visualYawBody = 180.0F + f * 20.0F;
        float visualYawHead = 180.0F + f * 40.0F;
        float visualPitch = -f1 * 20.0F;
        livingEntity.renderYawOffset = visualYawBody;
        livingEntity.prevRenderYawOffset = visualYawBody;
        livingEntity.rotationYaw = visualYawHead;
        livingEntity.prevRotationYaw = visualYawHead;
        livingEntity.rotationPitch = visualPitch;
        livingEntity.prevRotationPitch = visualPitch;
        livingEntity.rotationYawHead = livingEntity.rotationYaw;
        livingEntity.prevRotationYawHead = livingEntity.rotationYaw;
        livingEntity.rotationPitchHead = visualPitch;
        livingEntity.prevRotationPitchHead = visualPitch;
        EntityRendererManager entityrenderermanager = Minecraft.getInstance().getRenderManager();
        quaternion1.conjugate();
        entityrenderermanager.setCameraOrientation(quaternion1);
        entityrenderermanager.setRenderShadow(false);
        IRenderTypeBuffer.Impl irendertypebuffer$impl = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        RenderSystem.runAsFancy(() -> {
            entityrenderermanager.renderEntityStatic(livingEntity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, matrixstack, irendertypebuffer$impl, 15728880);
        });
        irendertypebuffer$impl.finish();
        entityrenderermanager.setRenderShadow(true);
        livingEntity.renderYawOffset = f2;
        livingEntity.rotationYaw = f3;
        livingEntity.rotationPitch = f4;
        livingEntity.prevRotationPitch = f4Prev;
        livingEntity.prevRotationYaw = f3Prev;
        livingEntity.prevRenderYawOffset = f2Prev;
        livingEntity.prevRotationYawHead = f5;
        livingEntity.rotationYawHead = f6;
        livingEntity.prevRotationPitchHead = f7;
        livingEntity.rotationPitchHead = f8;
        RenderSystem.popMatrix();
    }

    protected boolean isPointInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {
        return (!this.widthTooNarrow || !this.recipeBookGui.isVisible()) && super.isPointInRegion(x, y, width, height, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.recipeBookGui.mouseClicked(mouseX, mouseY, button)) {
            this.setListener(this.recipeBookGui);
            return true;
        } else {
            return this.widthTooNarrow && this.recipeBookGui.isVisible() ? false : super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.buttonClicked) {
            this.buttonClicked = false;
            return true;
        } else {
            return super.mouseReleased(mouseX, mouseY, button);
        }
    }

    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
        boolean flag = mouseX < (double) guiLeftIn || mouseY < (double) guiTopIn || mouseX >= (double) (guiLeftIn + this.xSize) || mouseY >= (double) (guiTopIn + this.ySize);
        return this.recipeBookGui.func_195604_a(mouseX, mouseY, this.guiLeft, this.guiTop, this.xSize, this.ySize, mouseButton) && flag;
    }

    /**
     * Called when the mouse is clicked over a slot or outside the gui.
     */
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        super.handleMouseClick(slotIn, slotId, mouseButton, type);
        this.recipeBookGui.slotClicked(slotIn);
    }

    public void recipesUpdated() {
        this.recipeBookGui.recipesUpdated();
    }

    public void onClose() {
        if (this.removeRecipeBookGui) {
            this.recipeBookGui.removed();
        }

        super.onClose();
    }

    public RecipeBookGui getRecipeGui() {
        return this.recipeBookGui;
    }
}
