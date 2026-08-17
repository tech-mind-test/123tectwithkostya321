//package sky.core.modules.impl.miscellaneous;
//
//import com.darkmagician6.eventapi.eventapinew.EventTarget;
//import com.mojang.blaze3d.matrix.MatrixStack;
//import net.minecraft.client.gui.screen.inventory.ContainerScreen;
//import net.minecraft.inventory.container.Slot;
//import net.minecraft.item.ItemStack;
//import net.minecraft.nbt.CompoundNBT;
//import net.minecraft.nbt.ListNBT;
//import net.minecraft.util.text.ITextComponent;
//import events.sky.core.EventContainerRenderer;
//import modules.sky.core.Category;
//import modules.sky.core.Module;
//import render.utils.sky.core.RenderUtil;
//
//import java.util.List;
//
//public class AhHelper extends Module {
//
//    private static final int COLOR1 = 0xFF00FF00;
//    private static final int COLOR2 = 0xFFFF0000;
//
//    private Slot color1;
//    private Slot color2;
//
//    public AhHelper() {
//        super("Auc Helper", "Отображает самые дешёвые и выгодные предметы", Category.Miscellaneous);
//    }
//
//    @EventTarget
//    public void onEventContainerRenderer(EventContainerRenderer e) {
//        if (!(mc.currentScreen instanceof ContainerScreen)) return;
//
//        ContainerScreen<?> screen = (ContainerScreen<?>) mc.currentScreen;
//        int offsetX = (screen.width - screen.getXSize()) / 2;
//        int offsetY = (screen.height - screen.getYSize()) / 2;
//
//        List<Slot> slots = screen.getContainer().inventorySlots;
//        findSlots(slots);
//
//        MatrixStack ms = e.getMatrixStack();
//        renderHighlight(ms, color1, getBlinkingColor(COLOR1), offsetX, offsetY);
//        renderHighlight(ms, color2, getBlinkingColor(COLOR2), offsetX, offsetY);
//    }
//
//    private void findSlots(List<Slot> slots) {
//        color1 = null;
//        color2 = null;
//
//        int lowestPrice = Integer.MAX_VALUE;
//        double bestPricePerItem = Double.MAX_VALUE;
//
//        for (Slot slot : slots) {
//            ItemStack stack = slot.getStack();
//            int price = getPrice(stack);
//
//            if (price < 0) continue;
//
//            if (price < lowestPrice) {
//                lowestPrice = price;
//                color1 = slot;
//            }
//
//            if (stack.getCount() > 1) {
//                double pricePerItem = (double) price / stack.getCount();
//                if (pricePerItem < bestPricePerItem) {
//                    bestPricePerItem = pricePerItem;
//                    color2 = slot;
//                }
//            }
//        }
//    }
//
//    private int getPrice(ItemStack stack) {
//        CompoundNBT tag = stack.getTag();
//        if (tag == null || !tag.contains("display", 10)) return -1;
//
//        CompoundNBT display = tag.getCompound("display");
//        if (!display.contains("Lore", 9)) return -1;
//
//        ListNBT lore = display.getList("Lore", 8);
//        for (int i = 0; i < lore.size(); i++) {
//            String line = lore.getString(i);
//
//            try {
//                ITextComponent component = ITextComponent.Serializer.getComponentFromJson(line);
//                if (component == null) continue;
//
//                String plainText = component.getString();
//                if (!plainText.contains("Цена:")) continue;
//
//                String priceStr = plainText
//                        .substring(plainText.indexOf("Цена:") + 5)
//                        .replace("$", "")
//                        .replace(",", "")
//                        .replace(" ", "")
//                        .trim();
//
//                return Integer.parseInt(priceStr);
//            } catch (Exception ignored) {}
//        }
//        return -1;
//    }
//
//    private int getBlinkingColor(int color) {
//        long time = System.currentTimeMillis() / 10;
//        int alpha = (int) (Math.abs(Math.sin(time * Math.PI / 180)) * 170);
//        return (alpha << 24) | (color & 0x00FFFFFF);
//    }
//
//    private void renderHighlight(MatrixStack ms, Slot slot, int color, int offsetX, int offsetY) {
//        if (slot != null) {
//            RenderUtil.drawMinecraftRectangle(ms, slot.xPos + offsetX, slot.yPos + offsetY, 16, 16, color);
//        }
//    }
//}