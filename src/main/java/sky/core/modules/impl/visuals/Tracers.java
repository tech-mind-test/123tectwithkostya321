package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import sky.core.SkyCore;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.misc.TargetUtil;
import sky.core.utils.render.ColorUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.vector.Vector3d;

import static org.lwjgl.opengl.GL11.*;

public class Tracers extends Module {
    private final MultiBooleanSetting rendering = new MultiBooleanSetting("Отображать", new BooleanSetting("Игроков", true), new BooleanSetting("Монстров", false), new BooleanSetting("Друзей", true), new BooleanSetting("Животных", false), new BooleanSetting("Жителей", false), new BooleanSetting("Голых", false));
    private final ColorSetting color = new ColorSetting("Цвет", true, -1);
    private final ColorSetting friendColor = new ColorSetting("Цвет друзей", true, ColorUtil.getColor(0, 255, 0), () -> rendering.is("Друзей"));
    private final SliderSetting lineWidth = new SliderSetting("Толщина линии", 1.0f, 0.5f, 5.0f, 0.1f);
    private final BooleanSetting onlyNetherite = new BooleanSetting("Онли кто в броне", false, () -> rendering.is("Игроков"));

    public Tracers() {
        super("Tracers", "Рисует линии до существ", Category.Visuals);
        addSettings(onlyNetherite, rendering, lineWidth, color, friendColor);
    }

    @EventTarget
    public void onUpdate(EventRender3D e) {
        glPushMatrix();

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);

        glEnable(GL_BLEND);
        glEnable(GL_LINE_SMOOTH);

        glLineWidth(lineWidth.get());

        Vector3d cam = new Vector3d(0, 0, 150).rotatePitch((float) -(Math.toRadians(mc.getRenderManager().info.getPitch()))).rotateYaw((float) -Math.toRadians(mc.getRenderManager().info.getYaw()));

        for (Entity entity : mc.world.getAllEntities()) {
            if (!isValidEntity(entity)) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (entity == mc.player) continue;

            Vector3d pos = getInterpolatedPositionVec(entity).subtract(mc.getRenderManager().info.getProjectedView());

            boolean isFriend = entity instanceof PlayerEntity && SkyCore.getInstance().getFriendManager().isFriend(((PlayerEntity) entity).getGameProfile().getName());
            int selectedColor = isFriend ? friendColor.get() : color.get();

            float r = ((selectedColor >> 16) & 255) / 255.0f;
            float g = ((selectedColor >> 8) & 255) / 255.0f;
            float b = (selectedColor & 255) / 255.0f;
            float a = ((selectedColor >> 24) & 255) / 255.0f;
            RenderSystem.color4f(r, g, b, a);

            BUFFER.begin(1, DefaultVertexFormats.POSITION);
            BUFFER.pos(cam.x, cam.y, cam.z).endVertex();
            BUFFER.pos(pos.x, pos.y, pos.z).endVertex();
            TESSELLATOR.draw();
        }

        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_DEPTH_TEST);

        glPopMatrix();
    }

    public Vector3d getPrevPositionVec(final Entity entity) {
        return new Vector3d(entity.prevPosX, entity.prevPosY, entity.prevPosZ);
    }

    public Vector3d getInterpolatedPositionVec(final Entity entity) {
        final Vector3d prev = getPrevPositionVec(entity);
        return prev.add(entity.getPositionVec().subtract(prev).scale(mc.getRenderPartialTicks()));
    }

    private boolean isValidEntity(Entity entity) {
        if (entity instanceof LivingEntity living && entity != mc.player) {
            if (TargetUtil.isPlayerTarget(living, rendering, true) && (!onlyNetherite.get() || hasNetheriteArmor((PlayerEntity) living))) {
                return true;
            }
            if (TargetUtil.isVillagerTarget(living, rendering) || TargetUtil.isAnimalTarget(living, rendering) || TargetUtil.isMonsterTarget(living, rendering)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNetheriteArmor(PlayerEntity player) {
        for (ItemStack stack : player.inventory.armorInventory) {
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) {
                return true;
            }
        }
        return false;
    }
}