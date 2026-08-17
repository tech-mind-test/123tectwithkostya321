package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.*;
import sky.core.utils.misc.OtherUtil;
import sky.core.utils.misc.TargetUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;

public class Arrows extends Module {
    private final MultiBooleanSetting rendering = new MultiBooleanSetting("Отображать", new BooleanSetting("Игроков", true), new BooleanSetting("Друзей", true), new BooleanSetting("Голых", false), new BooleanSetting("Предметы", false));
    private final ColorSetting playerColor = new ColorSetting("Цвет игроков", true, -1, () -> rendering.is("Игроков") || rendering.is("Голых"));
    private final ColorSetting friendColor = new ColorSetting("Цвет друзей", true, ColorUtil.getColor(0, 255, 0), () -> rendering.is("Друзей"));
    private final ColorSetting itemColor = new ColorSetting("Цвет предметов", true, ColorUtil.getColor(255, 0, 0), () -> rendering.is("Предметы"));
    private final SliderSetting radius = new SliderSetting("Радиус", 40, 30, 80, 5);
    private final BooleanSetting onlyIfNotVisible = new BooleanSetting("Показывать только если не видишь", false);
    public final ModeSetting type = new ModeSetting("Тип стрелки", "Необычная", "Необычная", "Заливка");

    private static final float ARROW_SIZE = 20;

    private static final ResourceLocation ARROW_UNUSUAL = new ResourceLocation("SkyCore/icons/world_render/triangle.png");
    private static final ResourceLocation ARROW_FILLED = new ResourceLocation("SkyCore/icons/world_render/arrow.png");

    public Arrows() {
        super("Arrows", "Рисует указатели на позиции разных существ", Category.Visuals);
        addSettings(rendering, playerColor, friendColor, itemColor, onlyIfNotVisible, radius, type);
    }

    @EventTarget
    private void onRender(EventRender2D eventRender2D) {
        if (mc.gameSettings.showDebugInfo) {
            return;
        }

        double cos = Math.cos(Math.toRadians(mc.gameRenderer.getActiveRenderInfo().getYaw()));
        double sin = Math.sin(Math.toRadians(mc.gameRenderer.getActiveRenderInfo().getYaw()));
        Vector2f fixed = OtherUtil.getMouse(mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight());
        double baseRadius = radius.get();

        ResourceLocation arrowTexture = type.is("Необычная") ? ARROW_UNUSUAL : ARROW_FILLED;

        for (Entity entity : mc.world.getAllEntities()) {
            if (!isValidEntity(entity)) continue;

            double adjustedRadius = (entity instanceof ItemEntity) ? Math.max(baseRadius - 20, 0) : baseRadius;
            double xOffset = (fixed.getX() / 2F) - adjustedRadius;
            double yOffset = (fixed.getY() / 2F) - adjustedRadius;

            Vector3d vector3d = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
            double xWay = (((entity.getPosX() + (entity.getPosX() - entity.lastTickPosX) * mc.getRenderPartialTicks()) - vector3d.x) * 0.01D);
            double zWay = (((entity.getPosZ() + (entity.getPosZ() - entity.lastTickPosZ) * mc.getRenderPartialTicks()) - vector3d.z) * 0.01D);
            double rotationY = -(zWay * cos - xWay * sin);
            double rotationX = -(xWay * cos + zWay * sin);
            double angle = Math.toDegrees(Math.atan2(rotationY, rotationX));
            double x = ((adjustedRadius * Math.cos(Math.toRadians(angle))) + xOffset + adjustedRadius);
            double y = ((adjustedRadius * Math.sin(Math.toRadians(angle))) + yOffset + adjustedRadius);

            if (isValidRotation(rotationX, rotationY, adjustedRadius)) {
                GL11.glPushMatrix();
                GL11.glTranslated(x, y, 0D);
                GL11.glRotated(angle, 0D, 0D, 1D);

                int selectedColor = determineEntityColor(entity);
                RenderUtil.drawImage2D(arrowTexture, -ARROW_SIZE / 2, -ARROW_SIZE / 2, ARROW_SIZE, ARROW_SIZE, selectedColor);
                GL11.glPopMatrix();
            }
        }
    }

    private boolean isValidEntity(Entity entity) {
        boolean isPlayerOrItem = TargetUtil.isPlayerTarget(entity, rendering, true) || TargetUtil.isItemTarget(entity, rendering);
        if (isPlayerOrItem && entity.isAlive() && entity != mc.player) {
            return !onlyIfNotVisible.get() || !isEntityVisible(entity);
        }
        return false;
    }

    private boolean isEntityVisible(Entity entity) {
        Vector3d start = mc.player.getEyePosition(mc.getRenderPartialTicks());
        Vector3d end = entity.getPositionVec().add(0, entity.getHeight() / 2, 0);
        RayTraceContext context = new RayTraceContext(start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player);
        RayTraceResult result = mc.world.rayTraceBlocks(context);
        return result.getType() == RayTraceResult.Type.MISS;
    }

    private int determineEntityColor(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            boolean isFriend = SkyCore.getInstance().getFriendManager().isFriend(player.getGameProfile().getName());
            boolean isNaked = player.getTotalArmorValue() == 0;
            if (isFriend) {
                return friendColor.get();
            } else if (isNaked) {
                return playerColor.get();
            } else {
                return playerColor.get();
            }
        } else if (entity instanceof ItemEntity) {
            return itemColor.get();
        }
        return playerColor.get();
    }

    private boolean isValidRotation(double rotationX, double rotationY, double radius) {
        final double mrotY = -rotationY;
        final double mrotX = -rotationX;
        return MathHelper.sqrt(mrotX * mrotX + mrotY * mrotY) < radius;
    }
}