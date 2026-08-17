package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import sky.core.events.EventRender3D;
import sky.core.events.EventSwapWorld;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.misc.TargetUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.ProjectUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Trails extends Module {
    private final MultiBooleanSetting rendering = new MultiBooleanSetting("Отображать", new BooleanSetting("Игроков", false), new BooleanSetting("Друзей", true), new BooleanSetting("Себя", true), new BooleanSetting("Невидимых", false));
    private final SliderSetting size = new SliderSetting("Длина", 2.5f, 2, 4, 0.5f);
    private final Map<Integer, List<Trail>> trailsByEntity = new HashMap<>();

    public Trails() {
        super("Trails", "Рисует шлейф по последним позициям существ", Category.Visuals);
        addSettings(rendering, size);
    }

    @EventTarget
    public void onEvent(EventRender3D event) {
        RenderSystem.pushMatrix();
        RenderSystem.shadeModel(7425);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(770, 771);
        RenderSystem.disableAlphaTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableTexture();
        
        double camX = mc.getRenderManager().info.getProjectedView().x;
        double camY = mc.getRenderManager().info.getProjectedView().y;
        double camZ = mc.getRenderManager().info.getProjectedView().z;
        RenderSystem.translated(-camX, -camY, -camZ);
        
        float partialTicks = event.getPartialTicks();
        float sizeValue = size.get() * 100;
        
        BUFFER.begin(7, DefaultVertexFormats.POSITION_COLOR);
        
        for (Entity entity : mc.world.getAllEntities()) {
            if (!isValidTarget(entity)) continue;
            
            if (entity instanceof LivingEntity living) {
                double posX = living.lastTickPosX + (living.getPosX() - living.lastTickPosX) * partialTicks;
                double posY = living.lastTickPosY + (living.getPosY() - living.lastTickPosY) * partialTicks + 0.05 - (living.isElytraFlying() ? 0.15 : 0);
                double posZ = living.lastTickPosZ + (living.getPosZ() - living.lastTickPosZ) * partialTicks;

                boolean isFirstPerson = living == mc.player && mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON;
                if (!isFirstPerson) {
                    List<Trail> list = trailsByEntity.computeIfAbsent(living.getEntityId(), id -> new ArrayList<>());
                    list.add(new Trail(posX, posY, posZ));
                }
                
                float height = living.getHeight() * (living.isSneaking() ? 0.8f : 1.0f);
                float heightOffset = height * 0.01f;
                
                List<Trail> list = trailsByEntity.get(living.getEntityId());
                if (list == null) continue;

                Trail prevTrail = null;
                boolean isFirst = true;
                int prevColor = 0;

                Iterator<Trail> it = list.iterator();
                while (it.hasNext()) {
                    Trail trail = it.next();
                    
                    if (System.currentTimeMillis() - trail.time > sizeValue) {
                        it.remove();
                        continue;
                    }
                    
                    trail.alpha = MathHelper.clamp((System.currentTimeMillis() - trail.time) / sizeValue, 0.0f, 1.0f) * 255.0f;
                    int color = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), (int)((255 * (255 - Math.round(trail.alpha))) / 220.0F));
                    
                    if (isFirst) {
                        BUFFER.pos(trail.x, trail.y, trail.z).color(color).endVertex();
                        BUFFER.pos(trail.x, trail.y + height, trail.z).color(color).endVertex();
                    } else {
                        BUFFER.pos(prevTrail.x, prevTrail.y, prevTrail.z).color(prevColor).endVertex();
                        BUFFER.pos(prevTrail.x, prevTrail.y + height, prevTrail.z).color(prevColor).endVertex();
                    }
                    BUFFER.pos(trail.x, trail.y + height, trail.z).color(color).endVertex();
                    BUFFER.pos(trail.x, trail.y, trail.z).color(color).endVertex();
                    
                    if (isFirst) {
                        BUFFER.pos(trail.x, trail.y, trail.z).color(color).endVertex();
                        BUFFER.pos(trail.x, trail.y + heightOffset, trail.z).color(color).endVertex();
                    } else {
                        BUFFER.pos(prevTrail.x, prevTrail.y, prevTrail.z).color(prevColor).endVertex();
                        BUFFER.pos(prevTrail.x, prevTrail.y + heightOffset, prevTrail.z).color(prevColor).endVertex();
                    }
                    BUFFER.pos(trail.x, trail.y + heightOffset, trail.z).color(color).endVertex();
                    BUFFER.pos(trail.x, trail.y, trail.z).color(color).endVertex();
                    
                    if (isFirst) {
                        BUFFER.pos(trail.x, trail.y + height, trail.z).color(color).endVertex();
                        BUFFER.pos(trail.x, trail.y + height - heightOffset, trail.z).color(color).endVertex();
                    } else {
                        BUFFER.pos(prevTrail.x, prevTrail.y + height, prevTrail.z).color(prevColor).endVertex();
                        BUFFER.pos(prevTrail.x, prevTrail.y + height - heightOffset, prevTrail.z).color(prevColor).endVertex();
                    }
                    BUFFER.pos(trail.x, trail.y + height - heightOffset, trail.z).color(color).endVertex();
                    BUFFER.pos(trail.x, trail.y + height, trail.z).color(color).endVertex();
                    
                    isFirst = false;
                    prevTrail = trail;
                    prevColor = color;
                }

                if (list.isEmpty()) {
                    trailsByEntity.remove(living.getEntityId());
                }
            }
        }
        
        TESSELLATOR.draw();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.shadeModel(7424);
        RenderSystem.popMatrix();
        RenderSystem.clearCurrentColor();
    }

    private boolean isValidTarget(Entity entity) {
        if (!entity.isAlive() || !ProjectUtil.isInView(entity)) return false;
        if (entity.isInvisible() && !rendering.is("Невидимых")) return false;
        if (entity instanceof PlayerEntity) {
            if (entity == mc.player) return TargetUtil.isSelfTarget(entity, rendering);
            return TargetUtil.isPlayerTarget(entity, rendering, false);
        }
        return false;
    }

    @EventTarget
    public void onEvent(EventSwapWorld e) {
        trailsByEntity.clear();
    }

    @java.lang.Override
    public void onDisable() {
        trailsByEntity.clear();
        super.onDisable();
    }

    public static final class Trail {
        final double x, y, z;
        float alpha;
        final long time;

        public Trail(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = System.currentTimeMillis();
        }
    }
}