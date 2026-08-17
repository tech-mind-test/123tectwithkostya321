package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.item.ItemStack;
import sky.core.events.EventRender2D;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.Wrapper;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.ProjectUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;

public class Prediction extends Module implements Wrapper {

    private final MultiBooleanSetting projectiles = new MultiBooleanSetting("Снаряды", new BooleanSetting("Эндер Пёрл", true), new BooleanSetting("Стрела", true), new BooleanSetting("Трезубец", true));
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.0");

    public Prediction() {
        super("Prediction", "Предугадывает куда и за сколько времени упадет предмет", Category.Visuals);
        addSettings(projectiles);
    }

    @EventTarget
    public void onEvent(EventRender2D event) {
        final int rectBgColor = ColorUtil.getColor(0, 0, 0, 85);
        final float rectHeight = 11f;
        final float iconSize = rectHeight;
        final float gap = 0.5f;
        final float padding = 3f;
        final float fontHeight = Fonts.sf_medium[13].getHeight();

        for (Entity entity : mc.world.getAllEntities()) {
            if (!validEntity(entity) || !noMove(entity)) continue;

            Item item = entity instanceof EnderPearlEntity ? Items.ENDER_PEARL
                    : entity instanceof ArrowEntity ? Items.ARROW
                    : Items.TRIDENT;

            Vector3d pearlPosition = entity.getPositionVec();
            Vector3d pearlMotion = entity.getMotion();
            Vector3d lastPosition = pearlPosition;
            int steps = 0;
            for (int i = 0; i <= 300; i++) {
                steps++;
                lastPosition = pearlPosition;
                pearlPosition = pearlPosition.add(pearlMotion);
                pearlMotion = updatePearlMotion(entity, pearlMotion, pearlPosition);
                if (shouldEntityHit(pearlPosition, lastPosition) || pearlPosition.y <= 0) break;
            }

            Vector2f position = ProjectUtil.project2D(lastPosition.x, lastPosition.y, lastPosition.z);
            if (position.x == Float.MAX_VALUE && position.y == Float.MAX_VALUE) continue;

            float timeInSeconds = steps * 0.05f;
            String timeText = TIME_FORMAT.format(timeInSeconds) + " сек.";
            float timeBlockWidth = Fonts.sf_medium[13].getWidth(timeText) + padding * 2;

            float totalWidth = iconSize + gap + timeBlockWidth;
            float tagX = position.x - totalWidth / 2f;
            float tagY = position.y - rectHeight / 2f;

            float currentX = tagX;

            RenderUtil.drawRoundedRectangle(currentX, tagY, iconSize, rectHeight, 2, rectBgColor);
            RenderUtil.drawStack(new ItemStack(item), currentX + 1.5f, tagY + 1.5f, 0.5f);
            currentX += iconSize + gap;

            RenderUtil.drawRoundedRectangle(currentX, tagY, timeBlockWidth, rectHeight, 2, rectBgColor);
            float timeTextX = currentX + padding;
            float timeTextY = tagY + (rectHeight - fontHeight) / 2f + 1f;
            Fonts.sf_medium[13].drawString(event.getStack(), timeText, timeTextX, timeTextY, -1);
        }
    }

    @EventTarget
    public void onEvent(EventRender3D event) {
        MatrixStack matrix = new MatrixStack();
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrix.getLast().getMatrix());
        RenderSystem.translated(-mc.getRenderManager().renderPosX(), -mc.getRenderManager().renderPosY(), -mc.getRenderManager().renderPosZ());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(1.5F);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        BUFFER.begin(1, DefaultVertexFormats.POSITION_COLOR);
        for (Entity entity : mc.world.getAllEntities()) {
            if (validEntity(entity) && noMove(entity)) renderLine(entity);
        }
        TESSELLATOR.draw();
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.translated(mc.getRenderManager().renderPosX(), mc.getRenderManager().renderPosY(), mc.getRenderManager().renderPosZ());
        RenderSystem.popMatrix();
    }

    private void renderLine(Entity pearl) {
        Vector3d pearlPosition = pearl.getPositionVec().add(0, 0, 0);
        Vector3d pearlMotion = pearl.getMotion();
        Vector3d lastPosition;
        for (int i = 0; i <= 300; i++) {
            lastPosition = pearlPosition;
            pearlPosition = pearlPosition.add(pearlMotion);
            pearlMotion = updatePearlMotion(pearl, pearlMotion, lastPosition);

            if (shouldEntityHit(pearlPosition, lastPosition) || pearlPosition.y <= 0) {
                break;
            }

            int color = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
            BUFFER.pos(lastPosition.x, lastPosition.y, lastPosition.z).color(color).endVertex();
            BUFFER.pos(pearlPosition.x, pearlPosition.y, pearlPosition.z).color(color).endVertex();
        }
    }

    public Vector3d updatePearlMotion(Entity entity, Vector3d originalPearlMotion, Vector3d pearlPosition) {
        Vector3d pearlMotion = originalPearlMotion;

        if ((entity.isInWater() || mc.world.getBlockState(new BlockPos(pearlPosition)).getBlock() == Blocks.WATER) && !(entity instanceof TridentEntity)) {
            float scale = entity instanceof EnderPearlEntity ? 0.8f : 0.6f;
            pearlMotion = pearlMotion.scale(scale);
        } else {
            pearlMotion = pearlMotion.scale(0.99f);
        }

        if (!entity.hasNoGravity()) pearlMotion.y -= entity instanceof EnderPearlEntity ? 0.03 : 0.05;

        return pearlMotion;
    }

    public boolean shouldEntityHit(Vector3d pearlPosition, Vector3d lastPosition) {
        final RayTraceContext rayTraceContext = new RayTraceContext(lastPosition, pearlPosition, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player);
        final BlockRayTraceResult blockHitResult = mc.world.rayTraceBlocks(rayTraceContext);

        return blockHitResult.getType() == RayTraceResult.Type.BLOCK;
    }
    public static void drawItemStack(ItemStack stack, float x, float y, float width, float height) {
        RenderSystem.translated(x, y, 0.0F);
        float scaleX = width / 16.0F;
        float scaleY = height / 16.0F;
        RenderSystem.scaled(scaleX, scaleY, 1.0F);
        mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        RenderSystem.scaled(1.0F / scaleX, 1.0F / scaleY, 1.0F);
        RenderSystem.translated(-x, -y, 0.0F);
    }

    boolean noMove(Entity entity) {
        return entity.prevPosY != entity.getPosY() || entity.prevPosX != entity.getPosX() || entity.prevPosZ != entity.getPosZ();
    }

    boolean validEntity(Entity entity) {
        return (entity instanceof EnderPearlEntity && projectiles.is("Эндер Пёрл")) || (entity instanceof ArrowEntity && projectiles.is("Стрела")) || (entity instanceof TridentEntity && projectiles.is("Трезубец"));
    }
}
