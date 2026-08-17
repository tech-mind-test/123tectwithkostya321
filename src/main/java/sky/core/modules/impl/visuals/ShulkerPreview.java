package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import sky.core.events.EventRender2D;
import sky.core.events.EventRenderTooltip;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ProjectUtil;
import sky.core.utils.render.RenderUtil;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

public class ShulkerPreview extends Module {
    private final BooleanSetting world = new BooleanSetting("Отображать в мире", true);

    public ShulkerPreview() {
        super("Shulker Preview", "Позволяет посмотреть что находится в шалкере (Зажать CTRL при наведении на шалкер)", Category.Visuals);
        addSettings(world);
    }

    @EventTarget
    public void onRenderTooltip(EventRenderTooltip e) {
        if (shouldShowPreview(e.stack)) {
            e.setCancelled(true);
            showPreview(e.matrixStack, e.stack, e.mouseX, e.mouseY);
        }
    }

    @EventTarget
    public void onRender2DWorld(EventRender2D e) {
        if (world.get()) {
            List<ItemEntity> shulkerDrops = new ArrayList<>();
            for (Entity ent : mc.world.getAllEntities()) {
                if (ent instanceof ItemEntity itemEnt) {
                    ItemStack drop = itemEnt.getItem();
                    if (drop.getItem() instanceof BlockItem
                        && ((BlockItem) drop.getItem()).getBlock() instanceof ShulkerBoxBlock) {
                        shulkerDrops.add(itemEnt);
                    }
                }
            }
            shulkerDrops.sort(Comparator.comparingDouble((ItemEntity ie) -> ie.getDistanceSq(mc.player.getPositionVec())).reversed());
            for (ItemEntity itemEnt : shulkerDrops) {
                ItemStack drop = itemEnt.getItem();
                CompoundNBT rootTag = drop.getTag();
                if (rootTag == null || !rootTag.contains("BlockEntityTag")) continue;

                Vector2f screen = ProjectUtil.project2D(MathUtil.interpolate(itemEnt, e.getPartialTicks()));
                if (screen.x == Float.MAX_VALUE || screen.y == Float.MAX_VALUE) continue;

                int baseOffsetX = 14;
                float panelX = screen.x + baseOffsetX;

                int baseOffset = 20;
                float panelY = screen.y - baseOffset;

                DyeColor dropColor = ShulkerBoxBlock.getColorFromItem(drop.getItem());
                int tint = 0xFF000000 | (dropColor != null ? dropColor.getColorValue() : DyeColor.PURPLE.getColorValue());

                RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/shulker_view/shulker_box_tooltip.png"), panelX, panelY, 129, 129, tint);

                CompoundNBT blockTag = rootTag.getCompound("BlockEntityTag");
                NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
                ItemStackHelper.loadAllItems(blockTag, items);
                for (int i = 0; i < items.size(); i++) {
                    ItemStack content = items.get(i);
                    if (content.isEmpty()) continue;
                    int col = i % 9;
                    int row = i / 9;
                    float slotX = panelX + 4 + col * 9;
                    float slotY = panelY + 4 + row * 9;
                    RenderUtil.drawStack(content, slotX, slotY, 0.5f);
                }
            }
        }
    }

    public static boolean shouldShowPreview(ItemStack stack) {
        return Screen.hasControlDown() && stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock;
    }

    public static void showPreview(MatrixStack matrixStack, ItemStack stack, int mouseX, int mouseY) {
        List<ITextComponent> texts = new ArrayList<>();
        texts.add(stack.getDisplayName().deepCopy().mergeStyle(TextFormatting.WHITE));
        texts.add(new StringTextComponent("Содержит 27 предметов").mergeStyle(TextFormatting.GRAY));
        List<IReorderingProcessor> procs = texts.stream().map(ITextComponent::func_241878_f).collect(Collectors.toList());
        mc.currentScreen.renderTooltip(matrixStack, procs, mouseX, mouseY);
        int maxWidth = procs.stream().mapToInt(mc.fontRenderer::func_243245_a).max().orElse(0);
        int x = mouseX + 12;
        int y = mouseY - 12;
        int k = 8;
        if (procs.size() > 1) {
            k += 2 + (procs.size() - 1) * 10;
        }
        if (x + maxWidth > mc.currentScreen.width) {
            x -= 28 + maxWidth;
        }
        if (y + k + 6 > mc.currentScreen.height) {
            y = mc.currentScreen.height - k - 6;
        }
        DyeColor dye = ShulkerBoxBlock.getColorFromItem(stack.getItem());
        int tint = 0xFF000000 | (dye != null ? dye.getColorValue() : DyeColor.PURPLE.getColorValue());
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/shulker_view/shulker_box_tooltip.png"), x - 4, y + k + 5, 256, 256, tint);

        CompoundNBT rootTag = stack.getTag();
        if (rootTag != null && rootTag.contains("BlockEntityTag")) {
            CompoundNBT blockTag = rootTag.getCompound("BlockEntityTag");
            NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
            ItemStackHelper.loadAllItems(blockTag, items);
            int startX = x + 4;
            int startY = y + k + 13;
            for (int i = 0; i < items.size(); i++) {
                ItemStack content = items.get(i);
                if (!content.isEmpty()) {
                    int col = i % 9;
                    int row = i / 9;
                    int slotX = startX + col * 18;
                    int slotY = startY + row * 18;
                    RenderSystem.pushMatrix();
                    RenderSystem.translatef(0.0F, 0.0F, 32.0F);
                    mc.currentScreen.setBlitOffset(200);
                    mc.getItemRenderer().zLevel = 200.0F;
                    RenderUtil.drawStack(content, slotX, slotY, 1.0f);
                    mc.currentScreen.setBlitOffset(0);
                    mc.getItemRenderer().zLevel = 0.0F;
                    RenderSystem.popMatrix();
                }
            }
        }
    }
}