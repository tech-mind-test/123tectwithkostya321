//package sky.core.ui.screens;
//
//import com.mojang.blaze3d.matrix.MatrixStack;
//import com.mojang.blaze3d.systems.RenderSystem;
//import net.minecraft.client.gui.screen.Screen;
//import net.minecraft.client.renderer.BufferBuilder;
//import net.minecraft.client.renderer.Tessellator;
//import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
//import net.minecraft.item.ItemStack;
//import net.minecraft.util.math.MathHelper;
//import net.minecraft.util.text.StringTextComponent;
//import org.lwjgl.opengl.GL11;
//import sky.core.modules.impl.combat.AutoSwap;
//import sky.core.utils.Wrapper;
//import sky.core.utils.render.ColorUtil;
//import sky.core.utils.render.font.Fonts;
//
//public class TripleAutoSwapWheelScreen extends Screen implements Wrapper {
//
//    private final AutoSwap module;
//
//    private final float[] hoverProgress = new float[]{0f, 0f, 0f};
//
//    private static final float INNER_RADIUS = 70f * 1.8f;
//    private static final float OUTER_RADIUS = 130f * 1.5f;
//    private static final float START_ANGLE = -150 + 180;
//
//    public TripleAutoSwapWheelScreen(AutoSwap module) {
//        super(new StringTextComponent("TripleAutoSwap"));
//        this.module = module;
//    }
//
//
//    private static float normalizeDeg(float deg) {
//        float d = deg % 360f;
//        if (d < 0) d += 360f;
//        return d;
//    }
//
//    private boolean hasItemInInventory(ItemStack target) {
//        if (target == null || target.isEmpty()) return false;
//
//        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
//            ItemStack inv = mc.player.inventory.getStackInSlot(i);
//            if (!inv.isEmpty() && ItemStack.areItemsEqual(inv, target)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private int getHoveredSectorIndex(double mouseX, double mouseY,
//                                      float cx, float cy, int sectors) {
//        double dx = mouseX - cx;
//        double dy = mouseY - cy;
//
//        double dist = Math.sqrt(dx * dx + dy * dy);
//        if (dist < INNER_RADIUS) return -1;
//
//        float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
//        float rel = normalizeDeg(ang - START_ANGLE);
//
//        float sectorAngle = 360f / sectors;
//        int idx = (int) (rel / sectorAngle);
//
//        return (idx >= 0 && idx < sectors) ? idx : -1;
//    }
//
//
//    @Override
//    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
//        float guiScale = (float) mc.getMainWindow().getGuiScaleFactor() / 4;
//        float mx = mouseX * 2f;
//        float my = mouseY * 2f;
//
//        float cx = mc.getMainWindow().getScaledWidth();
//        float cy = mc.getMainWindow().getScaledHeight();
//
//        int sectors = 3;
//        float sectorAngle = 360f / sectors;
//
//        RenderSystem.pushMatrix();
//        GL11.glEnable(GL11.GL_LINE_SMOOTH);
//        GL11.glScaled(guiScale, guiScale, guiScale);
//
//        try {
//            int hovered = getHoveredSectorIndex(mx, my, cx, cy, sectors);
//
//            for (int i = 0; i < sectors; i++) {
//                float target = i == hovered ? 1f : 0f;
//                hoverProgress[i] += (target - hoverProgress[i]) * 0.18f;
//                hoverProgress[i] = MathHelper.clamp(hoverProgress[i], 0f, 1f);
//            }
//
//
//            for (int i = 0; i < sectors; i++) {
//                float a0 = START_ANGLE + sectorAngle * i;
//                float a1 = a0 + sectorAngle;
//
//                ItemStack stack = module.getSlotStack(i);
//                boolean empty = stack == null || stack.isEmpty();
//                boolean missing = !empty && !hasItemInInventory(stack);
//
//                int base = ColorUtil.getColor(255, 255, 255, 32);
//                int hi;
//
//                if (empty) {
//                    hi = ColorUtil.getColor(255, 214, 0, 100);
//                } else if (missing) {
//                    hi = ColorUtil.getColor(255, 60, 60, 120);
//                } else {
//                    hi = ColorUtil.getColor(60, 255, 60, 100);
//                }
//
//                float maxOffset = 22f;
//                float t = hoverProgress[i];
//                t = t * t * (3f - 2f * t);
//                float offset = t * maxOffset;
//
//                float inner = INNER_RADIUS + offset * 0.6f;
//                float outer = OUTER_RADIUS + offset;
//
//                drawRingSector(
//                        matrixStack,
//                        cx, cy,
//                        inner, outer,
//                        a0, a1,
//                        lerpColorClamped(base, hi, hoverProgress[i])
//                );
//            }
//
//            for (int i = 0; i < sectors; i++) {
//                float mid = START_ANGLE + sectorAngle * i + sectorAngle / 2f;
//                float rad = (float) Math.toRadians(mid);
//
//                float r = (INNER_RADIUS + OUTER_RADIUS) / 2f;
//                float ix = cx + (float) Math.cos(rad) * r;
//                float iy = cy + (float) Math.sin(rad) * r;
//
//                ItemStack stack = module.getSlotStack(i);
//                boolean missing = stack != null && !stack.isEmpty() && !hasItemInInventory(stack);
//
//                GL11.glScaled(2, 2, 2);
//
//                if (stack == null || stack.isEmpty()) {
//                    Fonts.sf_bold[15].drawString(
//                            matrixStack, "+",
//                            (ix) / 2, (iy) / 2,
//                            ColorUtil.getColor(255, 214, 0, 255)
//                    );
//                } else if (missing) {
//                    Fonts.sf_bold[20].drawString(
//                            matrixStack, "X",
//                            (ix) / 2, (iy) / 2,
//                            ColorUtil.getColor(255, 80, 80, 255)
//                    );
//                } else {
//                    mc.getItemRenderer().renderItemAndEffectIntoGUI(
//                            stack, (int) (ix - 14) / 2, (int) (iy - 14) / 2
//                    );
//                }
//
//                GL11.glScaled(0.5, 0.5, 0.5);
//            }
//
//        } finally {
//            GL11.glDisable(GL11.GL_LINE_SMOOTH);
//            RenderSystem.popMatrix();
//        }
//    }
//
//
//    @Override
//    public boolean mouseClicked(double mouseX, double mouseY, int button) {
//        float mx = (float) mouseX * 2;
//        float my = (float) mouseY * 2;
//
//        float cx = mc.getMainWindow().getScaledWidth();
//        float cy = mc.getMainWindow().getScaledHeight();
//
//        int i = getHoveredSectorIndex(mx, my, cx, cy, 3);
//        if (i == -1) return super.mouseClicked(mx, my, button);
//
//        mc.displayGuiScreen(new TripleAutoSwapInventorySelectScreen(module, i, this));
//        return true;
//    }
//
//    public void swap() {
//        double mx = mc.mouseHelper.getMouseX();
//        double my = mc.mouseHelper.getMouseY();
//
//        float cx = mc.getMainWindow().getScaledWidth();
//        float cy = mc.getMainWindow().getScaledHeight();
//
//        int i = getHoveredSectorIndex(mx, my, cx, cy, 3);
//        if (i == -1) return;
//
//        ItemStack stack = module.getSlotStack(i);
//        if (stack == null || stack.isEmpty()) return;
//        if (!hasItemInInventory(stack)) return;
//
//        module.setTarget(stack);
//        module.setSwapRequested(true);
//    }
//
//    @Override
//    public boolean isPauseScreen() {
//        return false;
//    }
//
//
//    private void drawRingSector(MatrixStack ms, float cx, float cy,
//                                float innerR, float outerR,
//                                float startDeg, float endDeg, int color) {
//
//        float[] c = ColorUtil.getColor(color);
//        Tessellator t = Tessellator.getInstance();
//        BufferBuilder b = t.getBuffer();
//
//        RenderSystem.enableBlend();
//        RenderSystem.disableTexture();
//
//        b.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
//        int steps = 64;
//        float step = (endDeg - startDeg) / steps;
//
//        for (int i = 0; i <= steps; i++) {
//            float deg = startDeg + step * i;
//            float rad = (float) Math.toRadians(deg);
//
//            float cos = (float) Math.cos(rad);
//            float sin = (float) Math.sin(rad);
//
//            b.pos(ms.getLast().getMatrix(), cx + cos * outerR, cy + sin * outerR, 0)
//                    .color(c[0], c[1], c[2], c[3]).endVertex();
//            b.pos(ms.getLast().getMatrix(), cx + cos * innerR, cy + sin * innerR, 0)
//                    .color(c[0], c[1], c[2], c[3]).endVertex();
//        }
//        t.draw();
//
//        RenderSystem.enableTexture();
//        RenderSystem.disableBlend();
//    }
//
//    private static int lerpColorClamped(int c1, int c2, float t) {
//        t = MathHelper.clamp(t, 0f, 1f);
//        int a = (int) ((c1 >> 24 & 255) + ((c2 >> 24 & 255) - (c1 >> 24 & 255)) * t);
//        int r = (int) ((c1 >> 16 & 255) + ((c2 >> 16 & 255) - (c1 >> 16 & 255)) * t);
//        int g = (int) ((c1 >> 8 & 255) + ((c2 >> 8 & 255) - (c1 >> 8 & 255)) * t);
//        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
//        return ColorUtil.getColor(r, g, b, a);
//    }
//}
