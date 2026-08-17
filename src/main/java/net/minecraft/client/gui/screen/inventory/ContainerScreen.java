package net.minecraft.client.gui.screen.inventory;

import com.darkmagician6.eventapi.EventManager;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.player.PlayerEntity;
import other.bot.Bot;
import other.bot.BotManager;
import sky.core.SkyCore;
import sky.core.events.EventContainerRender;
import sky.core.events.EventHandleMouseClick;
import sky.core.modules.impl.player.AutoBuy;
import sky.core.modules.impl.player.ItemScroller;
import sky.core.ui.gui.autobuy.PurchaseHistory;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ShulkerBoxContainer;
import net.minecraft.inventory.container.HopperContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public abstract class ContainerScreen<T extends Container> extends Screen implements IHasContainer<T> {
    public static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("textures/gui/container/inventory.png");
    protected int xSize = 176;
    protected int ySize = 166;
    protected int titleX;
    protected int titleY;
    protected int playerInventoryTitleX;
    protected int playerInventoryTitleY;
    protected final T container;
    protected final PlayerInventory playerInventory;
    @Nullable
    protected Slot hoveredSlot;
    @Nullable
    private Slot clickedSlot;
    @Nullable
    private Slot returningStackDestSlot;
    @Nullable
    private Slot currentDragTargetSlot;
    @Nullable
    private Slot lastClickSlot;
    protected int guiLeft;
    protected int guiTop;
    private boolean isRightMouseClick;
    private ItemStack draggedStack = ItemStack.EMPTY;
    private int touchUpX;
    private int touchUpY;
    private long returningStackTime;
    private ItemStack returningStack = ItemStack.EMPTY;
    private long dragItemDropDelay;
    public static boolean autoBuyPaused = false;
    private Button takeAllButton;
    private Button putAllButton;
    private Button throwAllButton;
    private Button autoBuyButton;

    protected final Set<Slot> dragSplittingSlots = Sets.newHashSet();
    protected boolean dragSplitting;
    private int dragSplittingLimit;
    private int dragSplittingButton;
    private boolean ignoreMouseUp;
    private int dragSplittingRemnant;
    private long lastClickTime;
    private int lastClickButton;
    private boolean doubleClick;
    private ItemStack shiftClickedSlot = ItemStack.EMPTY;

    private float historyScroll = 0;
    private float targetHistoryScroll = 0;
    private float smoothHistoryScroll = 0;
    public ContainerScreen(T screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(titleIn);
        this.container = screenContainer;
        this.playerInventory = inv;
        this.ignoreMouseUp = true;
        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = this.ySize - 94;
    }

    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        addContainerButtons();
    }

    private TimeUtil timeUtil = new TimeUtil();

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int i = this.guiLeft;
        int j = this.guiTop;
        this.drawGuiContainerBackgroundLayer(matrixStack, partialTicks, mouseX, mouseY);
        RenderSystem.disableRescaleNormal();
        RenderSystem.disableDepthTest();

        drawHistoryPanel(matrixStack, mouseX, mouseY);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        updateButtonStates();

        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) i, (float) j, 0.0F);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableRescaleNormal();
        this.hoveredSlot = null;
        RenderSystem.glMultiTexCoord2f(33986, 240.0F, 240.0F);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        for (int i1 = 0; i1 < this.container.inventorySlots.size(); ++i1) {
            Slot slot = this.container.inventorySlots.get(i1);
            if (slot.isEnabled()) {
                this.moveItems(matrixStack, slot);
            }
            if (this.isSlotSelected(slot, (double) mouseX, (double) mouseY) && slot.isEnabled()) {
                ItemScroller itemScroller = (ItemScroller) SkyCore.getInstance().getModuleManager().getModule(ItemScroller.class);
                if (itemScroller != null && itemScroller.isEnabled()) {
                    if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS &&
                            GLFW.glfwGetKey(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
                        if (timeUtil.hasTimeElapsed(itemScroller.delay.get().longValue() * 100) && slot.getHasStack()) {
                            handleMouseClick(slot, slot.slotNumber, 1, ClickType.QUICK_MOVE);
                            timeUtil.reset();
                        }
                    }
                }
                this.hoveredSlot = slot;
                RenderSystem.disableDepthTest();
                this.fillGradient(matrixStack, slot.xPos, slot.yPos, slot.xPos + 16, slot.yPos + 16, -2130706433, -2130706433);
                RenderSystem.enableDepthTest();
            }
        }

        this.drawGuiContainerForegroundLayer(matrixStack, mouseX, mouseY);

        PlayerInventory playerinventory = this.minecraft.player.inventory;
        ItemStack itemstack = this.draggedStack.isEmpty() ? playerinventory.getItemStack() : this.draggedStack;

        if (!itemstack.isEmpty()) {
            this.drawItemStack(itemstack, mouseX - i - 8, mouseY - j - (this.draggedStack.isEmpty() ? 8 : 16), null);
        }

        if (!this.returningStack.isEmpty()) {
            float f = (float) (Util.milliTime() - this.returningStackTime) / 100.0F;
            if (f >= 1.0F) { f = 1.0F; this.returningStack = ItemStack.EMPTY; }
            int l1 = this.touchUpX + (int) ((float) (this.returningStackDestSlot.xPos - this.touchUpX) * f);
            int i2 = this.touchUpY + (int) ((float) (this.returningStackDestSlot.yPos - this.touchUpY) * f);
            this.drawItemStack(this.returningStack, l1, i2, null);
        }

        RenderSystem.popMatrix();

        RenderSystem.enableDepthTest();
        drawHistoryTooltips(matrixStack, mouseX, mouseY);

        EventManager.call(new EventContainerRender.Post(matrixStack, this.guiLeft, this.guiTop, this.container));
    }

    private void drawHistoryPanel(MatrixStack stack, int mouseX, int mouseY) {
        AutoBuy autoBuy = (AutoBuy) SkyCore.getInstance().getModuleManager().getModule(AutoBuy.class);
        if (autoBuy == null || !autoBuy.isEnabled() || !this.title.getString().toLowerCase().contains("аукцион")) return;

        List<PurchaseHistory.Log> logs = PurchaseHistory.getHistory();
        int panelW = 176, panelH = 146, pX = this.guiLeft - panelW - 10, pY = this.guiTop;

        RenderUtil.drawBlurredRoundedRectangle(pX, pY, panelW, panelH, 10, ThemeEditor.getColor(ThemeSettings.MAIN), 1);
        RenderUtil.drawOutlineRectangle(pX, pY, panelW, panelH, 10, ThemeEditor.getColor(ThemeSettings.OUTLINE), 1f);
        Fonts.sf_bold[16].drawString(stack, "История Покупок", pX + 8, pY + 8, -1);

        for (int i = 0; i < 54; i++) {
            int slotX = pX + 7 + (i % 9) * 18;
            int slotY = pY + 22 + (i / 9) * 18;

            RenderUtil.drawRoundedRectangle(slotX, slotY, 16, 16, 3, 0x30000000);
            RenderUtil.drawOutlineRectangle(slotX, slotY, 16, 16, 3, 0x10FFFFFF, 0.5f);

            if (i < logs.size()) {
                ItemStack is = logs.get(i).stack;
                if (mouseX >= slotX && mouseX <= slotX + 16 && mouseY >= slotY && mouseY <= slotY + 16) {
                    RenderUtil.drawRoundedRectangle(slotX, slotY, 16, 16, 3, 0x25FFFFFF);
                }

                RenderSystem.pushMatrix();
                RenderSystem.translatef(0, 0, 32.0F);
                this.itemRenderer.renderItemAndEffectIntoGUI(is, slotX, slotY);
                this.itemRenderer.renderItemOverlayIntoGUI(this.font, is, slotX, slotY, null);
                RenderSystem.popMatrix();
            }
        }
        Fonts.sf_medium[9].drawString(stack, "PRESS DELETE TO CLEAR", pX + 60, pY + panelH - 10, 0x60FFFFFF);
    }

    private void drawHistoryTooltips(MatrixStack stack, int mouseX, int mouseY) {
        AutoBuy autoBuy = (AutoBuy) SkyCore.getInstance().getModuleManager().getModule(AutoBuy.class);
        if (autoBuy == null || !autoBuy.isEnabled() || !this.title.getString().toLowerCase().contains("аукцион")) return;

        List<PurchaseHistory.Log> logs = PurchaseHistory.getHistory();
        int pX = this.guiLeft - 186, pY = this.guiTop;

        for (int i = 0; i < Math.min(logs.size(), 54); i++) {
            int slotX = pX + 7 + (i % 9) * 18;
            int slotY = pY + 22 + (i / 9) * 18;

            if (mouseX >= slotX && mouseX <= slotX + 16 && mouseY >= slotY && mouseY <= slotY + 16) {
                RenderSystem.pushMatrix();
                RenderSystem.translatef(0, 0, 500.0F);
                this.renderTooltip(stack, logs.get(i).stack, mouseX, mouseY);
                RenderSystem.popMatrix();
            }
        }
    }

    private String formatTimeDiff(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "с";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "м";
        long hours = minutes / 60;
        if (hours < 24) return hours + "ч";
        long days = hours / 24;
        return days + "д";
    }



    protected void renderHoveredTooltip(MatrixStack matrixStack, int x, int y)
    {
        PlayerEntity player = this.minecraft.player;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                player = bot.connection.bot;
            }
        }

        if (player.inventory.getItemStack().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.getHasStack()) {
            this.renderTooltip(matrixStack, this.hoveredSlot.getStack(), x, y);
        }
    }

    private void drawItemStack(ItemStack stack, int x, int y, String altText) {
        RenderSystem.translatef(0.0F, 0.0F, 32.0F);
        this.setBlitOffset(200);
        this.itemRenderer.zLevel = 200.0F;
        this.itemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
        this.itemRenderer.renderItemOverlayIntoGUI(this.font, stack, x, y - (this.draggedStack.isEmpty() ? 0 : 8), altText);
        this.setBlitOffset(0);
        this.itemRenderer.zLevel = 0.0F;
    }

    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {
        this.font.func_243248_b(matrixStack, this.title, (float) this.titleX, (float) this.titleY, 4210752);
        this.font.func_243248_b(matrixStack, this.playerInventory.getDisplayName(), (float) this.playerInventoryTitleX, (float) this.playerInventoryTitleY, 4210752);
    }

    protected abstract void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y);

    private void moveItems(MatrixStack matrixStack, Slot slot) {
        PlayerEntity player = this.minecraft.player;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                player = bot.connection.bot;
            }
        }
        int i = slot.xPos;
        int j = slot.yPos;
        ItemStack itemstack = slot.getStack();
        boolean flag = false;
        boolean flag1 = slot == this.clickedSlot && !this.draggedStack.isEmpty() && !this.isRightMouseClick;
        ItemStack itemstack1 = player.inventory.getItemStack();
        String s = null;

        if (slot == this.clickedSlot && !this.draggedStack.isEmpty() && this.isRightMouseClick && !itemstack.isEmpty()) {
            itemstack = itemstack.copy();
            itemstack.setCount(itemstack.getCount() / 2);
        } else if (this.dragSplitting && this.dragSplittingSlots.contains(slot) && !itemstack1.isEmpty()) {
            if (this.dragSplittingSlots.size() == 1) {
                return;
            }

            if (Container.canAddItemToSlot(slot, itemstack1, true) && this.container.canDragIntoSlot(slot)) {
                itemstack = itemstack1.copy();
                flag = true;
                Container.computeStackSize(this.dragSplittingSlots, this.dragSplittingLimit, itemstack, slot.getStack().isEmpty() ? 0 : slot.getStack().getCount());
                int k = Math.min(itemstack.getMaxStackSize(), slot.getItemStackLimit(itemstack));

                if (itemstack.getCount() > k) {
                    s = TextFormatting.YELLOW.toString() + k;
                    itemstack.setCount(k);
                }
            } else {
                this.dragSplittingSlots.remove(slot);
                this.updateDragSplitting();
            }
        }

        this.setBlitOffset(100);
        this.itemRenderer.zLevel = 100.0F;

        if (itemstack.isEmpty() && slot.isEnabled()) {
            Pair<ResourceLocation, ResourceLocation> pair = slot.getBackground();

            if (pair != null) {
                TextureAtlasSprite textureatlassprite = this.minecraft.getAtlasSpriteGetter(pair.getFirst()).apply(pair.getSecond());
                this.minecraft.getTextureManager().bindTexture(textureatlassprite.getAtlasTexture().getTextureLocation());
                blit(matrixStack, i, j, this.getBlitOffset(), 16, 16, textureatlassprite);
                flag1 = true;
            }
        }

        if (!flag1) {
            if (flag) {
                fill(matrixStack, i, j, i + 16, j + 16, -2130706433);
            }

            RenderSystem.enableDepthTest();
            this.itemRenderer.renderItemAndEffectIntoGUI(player, itemstack, i, j);
            this.itemRenderer.renderItemOverlayIntoGUI(this.font, itemstack, i, j, s);
        }

        this.itemRenderer.zLevel = 0.0F;
        this.setBlitOffset(0);
    }

    private void updateDragSplitting()
    {
        PlayerEntity player = this.minecraft.player;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                player = bot.connection.bot;
            }
        }

        ItemStack itemstack = player.inventory.getItemStack();

        if (!itemstack.isEmpty() && this.dragSplitting)
        {
            if (this.dragSplittingLimit == 2)
            {
                this.dragSplittingRemnant = itemstack.getMaxStackSize();
            }
            else
            {
                this.dragSplittingRemnant = itemstack.getCount();

                for (Slot slot : this.dragSplittingSlots)
                {
                    ItemStack itemstack1 = itemstack.copy();
                    ItemStack itemstack2 = slot.getStack();
                    int i = itemstack2.isEmpty() ? 0 : itemstack2.getCount();
                    Container.computeStackSize(this.dragSplittingSlots, this.dragSplittingLimit, itemstack1, i);
                    int j = Math.min(itemstack1.getMaxStackSize(), slot.getItemStackLimit(itemstack1));

                    if (itemstack1.getCount() > j)
                    {
                        itemstack1.setCount(j);
                    }

                    this.dragSplittingRemnant -= itemstack1.getCount() - i;
                }
            }
        }
    }

    @Nullable
    private Slot getSelectedSlot(double mouseX, double mouseY) {
        for (int i = 0; i < this.container.inventorySlots.size(); ++i) {
            Slot slot = this.container.inventorySlots.get(i);

            if (this.isSlotSelected(slot, mouseX, mouseY) && slot.isEnabled()) {
                return slot;
            }
        }

        return null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        PlayerEntity player = this.minecraft.player;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                player = bot.connection.bot;
            }
        }
        if (super.mouseClicked(mouseX, mouseY, button))
        {
            return true;
        }
        else
        {
            boolean flag = this.minecraft.gameSettings.keyBindPickBlock.matchesMouseKey(button);
            Slot slot = this.getSelectedSlot(mouseX, mouseY);
            long i = Util.milliTime();
            this.doubleClick = this.lastClickSlot == slot && i - this.lastClickTime < 250L && this.lastClickButton == button;
            this.ignoreMouseUp = false;

            if (button != 0 && button != 1 && !flag)
            {
                this.hotkeySwapItems(button);
            }
            else
            {
                int j = this.guiLeft;
                int k = this.guiTop;
                boolean flag1 = this.hasClickedOutside(mouseX, mouseY, j, k, button);
                int l = -1;

                if (slot != null)
                {
                    l = slot.slotNumber;
                }

                if (flag1)
                {
                    l = -999;
                }

                if (this.minecraft.gameSettings.touchscreen && flag1 && player.inventory.getItemStack().isEmpty())
                {
                    this.minecraft.displayGuiScreen((Screen)null);
                    return true;
                }

                if (l != -1)
                {
                    if (this.minecraft.gameSettings.touchscreen)
                    {
                        if (slot != null && slot.getHasStack())
                        {
                            this.clickedSlot = slot;
                            this.draggedStack = ItemStack.EMPTY;
                            this.isRightMouseClick = button == 1;
                        }
                        else
                        {
                            this.clickedSlot = null;
                        }
                    }
                    else if (!this.dragSplitting)
                    {
                        if (this.minecraft.player.inventory.getItemStack().isEmpty())
                        {
                            if (this.minecraft.gameSettings.keyBindPickBlock.matchesMouseKey(button))
                            {
                                this.handleMouseClick(slot, l, button, ClickType.CLONE);
                            }
                            else
                            {
                                boolean flag2 = l != -999 && (InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), 340) || InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), 344));
                                ClickType clicktype = ClickType.PICKUP;

                                if (flag2)
                                {
                                    this.shiftClickedSlot = slot != null && slot.getHasStack() ? slot.getStack().copy() : ItemStack.EMPTY;
                                    clicktype = ClickType.QUICK_MOVE;
                                }
                                else if (l == -999)
                                {
                                    clicktype = ClickType.THROW;
                                }

                                this.handleMouseClick(slot, l, button, clicktype);
                            }

                            this.ignoreMouseUp = true;
                        }
                        else
                        {
                            this.dragSplitting = true;
                            this.dragSplittingButton = button;
                            this.dragSplittingSlots.clear();

                            if (button == 0)
                            {
                                this.dragSplittingLimit = 0;
                            }
                            else if (button == 1)
                            {
                                this.dragSplittingLimit = 1;
                            }
                            else if (this.minecraft.gameSettings.keyBindPickBlock.matchesMouseKey(button))
                            {
                                this.dragSplittingLimit = 2;
                            }
                        }
                    }
                }
            }

            this.lastClickSlot = slot;
            this.lastClickTime = i;
            this.lastClickButton = button;
            return true;
        }
    }

    private void hotkeySwapItems(int keyCode)
    {
        PlayerEntity player = this.minecraft.player;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                player = bot.connection.bot;
            }
        }
        if (this.hoveredSlot != null && player.inventory.getItemStack().isEmpty())
        {
            if (this.minecraft.gameSettings.keyBindSwapHands.matchesMouseKey(keyCode))
            {
                this.handleMouseClick(this.hoveredSlot, this.hoveredSlot.slotNumber, 40, ClickType.SWAP);
                return;
            }

            for (int i = 0; i < 9; ++i)
            {
                if (this.minecraft.gameSettings.keyBindsHotbar[i].matchesMouseKey(keyCode))
                {
                    this.handleMouseClick(this.hoveredSlot, this.hoveredSlot.slotNumber, i, ClickType.SWAP);
                }
            }
        }
    }

    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
        return mouseX < (double) guiLeftIn || mouseY < (double) guiTopIn || mouseX >= (double) (guiLeftIn + this.xSize) || mouseY >= (double) (guiTopIn + this.ySize);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        PlayerEntity player = this.minecraft.player;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                player = bot.connection.bot;
            }
        }
        Slot slot = this.getSelectedSlot(mouseX, mouseY);
        ItemStack itemstack = player.inventory.getItemStack();

        if (this.clickedSlot != null && this.minecraft.gameSettings.touchscreen)
        {
            if (button == 0 || button == 1)
            {
                if (this.draggedStack.isEmpty())
                {
                    if (slot != this.clickedSlot && !this.clickedSlot.getStack().isEmpty())
                    {
                        this.draggedStack = this.clickedSlot.getStack().copy();
                    }
                }
                else if (this.draggedStack.getCount() > 1 && slot != null && Container.canAddItemToSlot(slot, this.draggedStack, false))
                {
                    long i = Util.milliTime();

                    if (this.currentDragTargetSlot == slot)
                    {
                        if (i - this.dragItemDropDelay > 500L)
                        {
                            this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, 0, ClickType.PICKUP);
                            this.handleMouseClick(slot, slot.slotNumber, 1, ClickType.PICKUP);
                            this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, 0, ClickType.PICKUP);
                            this.dragItemDropDelay = i + 750L;
                            this.draggedStack.shrink(1);
                        }
                    }
                    else
                    {
                        this.currentDragTargetSlot = slot;
                        this.dragItemDropDelay = i;
                    }
                }
            }
        }
        else if (this.dragSplitting && slot != null && !itemstack.isEmpty() && (itemstack.getCount() > this.dragSplittingSlots.size() || this.dragSplittingLimit == 2) && Container.canAddItemToSlot(slot, itemstack, true) && slot.isItemValid(itemstack) && this.container.canDragIntoSlot(slot))
        {
            this.dragSplittingSlots.add(slot);
            this.updateDragSplitting();
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        PlayerEntity player = this.minecraft.player;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                player = bot.connection.bot;
            }
        }
        Slot slot = this.getSelectedSlot(mouseX, mouseY);
        int i = this.guiLeft;
        int j = this.guiTop;
        boolean flag = this.hasClickedOutside(mouseX, mouseY, i, j, button);
        int k = -1;

        if (slot != null)
        {
            k = slot.slotNumber;
        }

        if (flag)
        {
            k = -999;
        }

        if (this.doubleClick && slot != null && button == 0 && this.container.canMergeSlot(ItemStack.EMPTY, slot))
        {
            if (hasShiftDown())
            {
                if (!this.shiftClickedSlot.isEmpty())
                {
                    for (Slot slot2 : this.container.inventorySlots)
                    {
                        if (slot2 != null && slot2.canTakeStack(player) && slot2.getHasStack() && slot2.inventory == slot.inventory && Container.canAddItemToSlot(slot2, this.shiftClickedSlot, true))
                        {
                            this.handleMouseClick(slot2, slot2.slotNumber, button, ClickType.QUICK_MOVE);
                        }
                    }
                }
            }
            else
            {
                this.handleMouseClick(slot, k, button, ClickType.PICKUP_ALL);
            }

            this.doubleClick = false;
            this.lastClickTime = 0L;
        }
        else
        {
            if (this.dragSplitting && this.dragSplittingButton != button)
            {
                this.dragSplitting = false;
                this.dragSplittingSlots.clear();
                this.ignoreMouseUp = true;
                return true;
            }

            if (this.ignoreMouseUp)
            {
                this.ignoreMouseUp = false;
                return true;
            }

            if (this.clickedSlot != null && this.minecraft.gameSettings.touchscreen)
            {
                if (button == 0 || button == 1)
                {
                    if (this.draggedStack.isEmpty() && slot != this.clickedSlot)
                    {
                        this.draggedStack = this.clickedSlot.getStack();
                    }

                    boolean flag2 = Container.canAddItemToSlot(slot, this.draggedStack, false);

                    if (k != -1 && !this.draggedStack.isEmpty() && flag2)
                    {
                        this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, button, ClickType.PICKUP);
                        this.handleMouseClick(slot, k, 0, ClickType.PICKUP);

                        if (this.minecraft.player.inventory.getItemStack().isEmpty())
                        {
                            this.returningStack = ItemStack.EMPTY;
                        }
                        else
                        {
                            this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, button, ClickType.PICKUP);
                            this.touchUpX = MathHelper.floor(mouseX - (double)i);
                            this.touchUpY = MathHelper.floor(mouseY - (double)j);
                            this.returningStackDestSlot = this.clickedSlot;
                            this.returningStack = this.draggedStack;
                            this.returningStackTime = Util.milliTime();
                        }
                    }
                    else if (!this.draggedStack.isEmpty())
                    {
                        this.touchUpX = MathHelper.floor(mouseX - (double)i);
                        this.touchUpY = MathHelper.floor(mouseY - (double)j);
                        this.returningStackDestSlot = this.clickedSlot;
                        this.returningStack = this.draggedStack;
                        this.returningStackTime = Util.milliTime();
                    }

                    this.draggedStack = ItemStack.EMPTY;
                    this.clickedSlot = null;
                }
            }
            else if (this.dragSplitting && !this.dragSplittingSlots.isEmpty())
            {
                this.handleMouseClick((Slot)null, -999, Container.getQuickcraftMask(0, this.dragSplittingLimit), ClickType.QUICK_CRAFT);

                for (Slot slot1 : this.dragSplittingSlots)
                {
                    this.handleMouseClick(slot1, slot1.slotNumber, Container.getQuickcraftMask(1, this.dragSplittingLimit), ClickType.QUICK_CRAFT);
                }

                this.handleMouseClick((Slot)null, -999, Container.getQuickcraftMask(2, this.dragSplittingLimit), ClickType.QUICK_CRAFT);
            }
            else if (!this.minecraft.player.inventory.getItemStack().isEmpty())
            {
                if (this.minecraft.gameSettings.keyBindPickBlock.matchesMouseKey(button))
                {
                    this.handleMouseClick(slot, k, button, ClickType.CLONE);
                }
                else
                {
                    boolean flag1 = k != -999 && (InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), 340) || InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), 344));

                    if (flag1)
                    {
                        this.shiftClickedSlot = slot != null && slot.getHasStack() ? slot.getStack().copy() : ItemStack.EMPTY;
                    }

                    this.handleMouseClick(slot, k, button, flag1 ? ClickType.QUICK_MOVE : ClickType.PICKUP);
                }
            }
        }

        if (this.minecraft.player.inventory.getItemStack().isEmpty())
        {
            this.lastClickTime = 0L;
        }

        this.dragSplitting = false;
        return true;
    }

    private boolean isSlotSelected(Slot slotIn, double mouseX, double mouseY) {
        return this.isPointInRegion(slotIn.xPos, slotIn.yPos, 16, 16, mouseX, mouseY);
    }

    protected boolean isPointInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {
        int i = this.guiLeft;
        int j = this.guiTop;
        mouseX = mouseX - (double) i;
        mouseY = mouseY - (double) j;
        return mouseX >= (double) (x - 1) && mouseX < (double) (x + width + 1) && mouseY >= (double) (y - 1) && mouseY < (double) (y + height + 1);
    }

    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        EventHandleMouseClick eventHandleMouseClick = new EventHandleMouseClick(slotIn, slotId, mouseButton, type);
        EventManager.call(eventHandleMouseClick);
        if (eventHandleMouseClick.isCancelled()) return;

        if (slotIn != null) {
            slotId = slotIn.slotNumber;
        }
        boolean botsImprovements = false;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                bot.connection.botController.windowClick(this.container.windowId, slotId, mouseButton, type, bot.connection.bot);
                botsImprovements = true;
            }
        }

        if (!botsImprovements) {
            this.minecraft.playerController.windowClick(this.container.windowId, slotId, mouseButton, type, this.minecraft.player);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (this.minecraft.gameSettings.keyBindInventory.matchesKey(keyCode, scanCode)) {
            this.closeScreen();
            return true;
        } else {
            this.itemStackMoved(keyCode, scanCode);

            if (keyCode == GLFW.GLFW_KEY_Q && hasControlDown() && hasShiftDown()) {
                this.dropAllSimilarItems();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                PurchaseHistory.getHistory().clear();
                return true;
            }
            if (this.hoveredSlot != null && this.hoveredSlot.getHasStack()) {
                if (this.minecraft.gameSettings.keyBindPickBlock.matchesKey(keyCode, scanCode)) {
                    this.handleMouseClick(this.hoveredSlot, this.hoveredSlot.slotNumber, 0, ClickType.CLONE);
                } else if (this.minecraft.gameSettings.keyBindDrop.matchesKey(keyCode, scanCode)) {
                    this.handleMouseClick(this.hoveredSlot, this.hoveredSlot.slotNumber, hasControlDown() ? 1 : 0, ClickType.THROW);
                }
            }

            return true;
        }
    }

    protected boolean itemStackMoved(int keyCode, int scanCode)
    {
        PlayerEntity player = null;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                player = bot.connection.bot;
            }
        }

        if (player == null) {
            player = this.minecraft.player;
        }
        if (player.inventory.getItemStack().isEmpty() && this.hoveredSlot != null)
        {
            if (this.minecraft.gameSettings.keyBindSwapHands.matchesKey(keyCode, scanCode))
            {
                this.handleMouseClick(this.hoveredSlot, this.hoveredSlot.slotNumber, 40, ClickType.SWAP);
                return true;
            }

            for (int i = 0; i < 9; ++i)
            {
                if (this.minecraft.gameSettings.keyBindsHotbar[i].matchesKey(keyCode, scanCode))
                {
                    this.handleMouseClick(this.hoveredSlot, this.hoveredSlot.slotNumber, i, ClickType.SWAP);
                    return true;
                }
            }
        }

        return false;
    }
    public void onClose()
    {
        PlayerEntity player = this.minecraft.player;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                player = bot.connection.bot;
            }
        }

        if (player != null) {
            this.container.onContainerClosed(player);
        }

    }
    public boolean isPauseScreen() {
        return false;
    }

    public void tick() {
        super.tick();
        Bot bot1 = null;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
            }
        }

        label31: {
            if (bot1 != null) {
                if (!bot1.connection.bot.isAlive()) {
                    break label31;
                }
            } else if (!this.minecraft.player.isAlive()) {
                break label31;
            }

            if (bot1 != null) {
                if (!bot1.connection.bot.removed) {
                    return;
                }
            } else if (!this.minecraft.player.removed) {
                return;
            }
        }

        if (bot1 == null) {
            this.minecraft.player.closeScreen();
        } else {
            bot1.connection.bot.closeScreen();
        }

    }

    public T getContainer() {
        return this.container;
    }

    private void dropAllSimilarItems() {
        if (this.hoveredSlot == null || !this.hoveredSlot.getHasStack()) {
            return;
        }

        ItemStack targetItem = this.hoveredSlot.getStack().copy();
        if (targetItem.isEmpty()) {
            return;
        }

        for (Slot slot : this.container.inventorySlots) {
            if (slot.getHasStack() && areItemsSimilar(slot.getStack(), targetItem)) {
                this.handleMouseClick(slot, slot.slotNumber, 1, ClickType.THROW);
            }
        }
    }

    private boolean areItemsSimilar(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) {
            return false;
        }

        if (!stack1.getItem().equals(stack2.getItem())) {
            return false;
        }

        if (!stack1.hasTag() && !stack2.hasTag()) {
            return true;
        }

        if (stack1.hasTag() != stack2.hasTag()) {
            return true;
        }

        ItemStack copy1 = stack1.copy();
        ItemStack copy2 = stack2.copy();

        if (copy1.hasTag() && copy1.getTag().contains("Damage")) {
            copy1.getTag().remove("Damage");
        }
        if (copy2.hasTag() && copy2.getTag().contains("Damage")) {
            copy2.getTag().remove("Damage");
        }

        return ItemStack.areItemStackTagsEqual(copy1, copy2);
    }

    private boolean isStorageContainer() {
        return this.container instanceof ChestContainer ||
                this.container instanceof ShulkerBoxContainer ||
                this.container instanceof HopperContainer;
    }

    private void addContainerButtons() {
        if (isStorageContainer()) {
            int buttonWidth = 80;
            int buttonHeight = 20;
            int spacing = 2;
            int startX = this.guiLeft + this.xSize + 4;
            int startY = this.guiTop;

            this.takeAllButton = this.addButton(new Button(
                    startX, startY,
                    buttonWidth, buttonHeight,
                    new StringTextComponent("Взять"),
                    (button) -> this.takeAllFromContainer()
            ));

            this.putAllButton = this.addButton(new Button(
                    startX, startY + buttonHeight + spacing,
                    buttonWidth, buttonHeight,
                    new StringTextComponent("Сложить"),
                    (button) -> this.putAllToContainer()
            ));

            this.throwAllButton = this.addButton(new Button(
                    startX, startY + (buttonHeight + spacing) * 2,
                    buttonWidth, buttonHeight,
                    new StringTextComponent("Выкинуть"),
                    (button) -> this.throwAllFromContainer()
            ));
        }

        String title = this.title.getString().toLowerCase();
        boolean isAuction = title.contains("аукционы") || title.contains("auction") || title.contains("аукцион") ||
                title.contains("поиск") || title.contains("search");

        if (isAuction) {
            this.autoBuyButton = this.addButton(new Button(
                    this.guiLeft + (this.xSize / 2) - 40, this.guiTop - 22,
                    80, 20,
                    new StringTextComponent("AutoBuy: " + (SkyCore.getInstance().getModuleManager().getModule(AutoBuy.class).isEnabled() ? "ВКЛ" : "ВЫКЛ")),
                    (button) -> {
                        AutoBuy autoBuy = (AutoBuy) SkyCore.getInstance().getModuleManager().getModule(AutoBuy.class);
                        autoBuy.settoggled(!autoBuy.isEnabled());
                        updateAutoBuyButton();
                    }
            ));
            updateAutoBuyButton();
        }
    }

    private void updateAutoBuyButton() {
        if (autoBuyButton != null) {
            AutoBuy autoBuy = (AutoBuy) SkyCore.getInstance().getModuleManager().getModule(AutoBuy.class);
            boolean enabled = autoBuy.isEnabled();
            autoBuyButton.setMessage(new StringTextComponent("AutoBuy: " + (enabled ? TextFormatting.GREEN + "ВКЛ" : TextFormatting.RED + "ВЫКЛ")));
        }
    }

    private void updateButtonStates() {
        if (autoBuyButton != null) {
            updateAutoBuyButton();
        }

        if (!isStorageContainer()) return;

        if (takeAllButton != null) {
            takeAllButton.active = hasItemsInContainer();
        }

        if (putAllButton != null) {
            putAllButton.active = hasItemsInPlayerInventory();
        }

        if (throwAllButton != null) {
            throwAllButton.active = hasItemsInContainer();
        }
    }

    private boolean hasItemsInContainer() {
        int containerSlotCount = getContainerSlotCount();
        for (int i = 0; i < containerSlotCount; i++) {
            Slot slot = this.container.inventorySlots.get(i);
            if (slot.getHasStack()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasItemsInPlayerInventory() {
        int containerSlotCount = getContainerSlotCount();
        for (int i = containerSlotCount; i < this.container.inventorySlots.size(); i++) {
            Slot slot = this.container.inventorySlots.get(i);
            if (slot.getHasStack()) {
                return true;
            }
        }
        return false;
    }

    private void takeAllFromContainer() {
        if (!isStorageContainer()) return;

        int containerSlotCount = getContainerSlotCount();

        for (int i = 0; i < containerSlotCount; i++) {
            Slot slot = this.container.inventorySlots.get(i);
            if (slot.getHasStack()) {
                this.handleMouseClick(slot, slot.slotNumber, 0, ClickType.QUICK_MOVE);
            }
        }

    }

    private void putAllToContainer() {
        if (!isStorageContainer()) return;

        int containerSlotCount = getContainerSlotCount();

        for (int i = containerSlotCount; i < this.container.inventorySlots.size(); i++) {
            Slot slot = this.container.inventorySlots.get(i);
            if (slot.getHasStack()) {
                this.handleMouseClick(slot, slot.slotNumber, 0, ClickType.QUICK_MOVE);
            }
        }

    }

    private void throwAllFromContainer() {
        if (!isStorageContainer()) return;

        int containerSlotCount = getContainerSlotCount();

        for (int i = 0; i < containerSlotCount; i++) {
            Slot slot = this.container.inventorySlots.get(i);
            if (slot.getHasStack()) {
                this.handleMouseClick(slot, slot.slotNumber, 1, ClickType.THROW);
            }
        }

    }

    private int getContainerSlotCount() {
        if (this.container instanceof ChestContainer) {
            ChestContainer chest = (ChestContainer) this.container;
            return chest.getNumRows() * 9;
        } else if (this.container instanceof ShulkerBoxContainer) {
            return 27;
        } else if (this.container instanceof HopperContainer) {
            return 5;
        }
        return 0;
    }


    public void closeScreen() {
        Bot bot1 = null;

        for(Bot bot : BotManager.allBots) {
            if (this.minecraft.renderViewEntity == bot.connection.bot) {
                bot1 = bot;
            }
        }

        if (bot1 != null) {
            bot1.connection.bot.closeScreen();
        } else {
            this.minecraft.player.closeScreen();
        }

        super.closeScreen();
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        String titleLower = this.title.getString().toLowerCase();
        if (titleLower.contains("аукцион") || titleLower.contains("auction") || titleLower.contains("поиск")) {
            int panelW = 140;
            int panelX = this.guiLeft - panelW - 8;

            if (mouseX >= panelX && mouseX <= panelX + panelW) {
                targetHistoryScroll += (float) delta * 36;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}