package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventPacket;
import sky.core.events.EventRender2D;
import sky.core.events.EventSwapWorld;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.ProjectUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Getter;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import org.joml.Vector2f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FireworkESP extends Module {
    private final BooleanSetting time = new BooleanSetting("Отображать время", true);
    private final List<Firework> fireworks = new CopyOnWriteArrayList<>();

    public FireworkESP() {
        super("Firework ESP", "Отображает где был использован фейерверк", Category.Visuals);
        addSettings(time);
    }

    @EventTarget
    public void onPacket(EventPacket eventPacket) {
        if (eventPacket.getPacket() instanceof SPlaySoundEffectPacket packet) {
            if (packet.getCategory() == SoundCategory.AMBIENT && packet.getSound() == SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH) {
                Vector3d newPosition = new Vector3d(packet.getX(), packet.getY(), packet.getZ());
                if (!hasActiveFireworkInRadius(newPosition, 5)) {
                    fireworks.add(new Firework(newPosition));
                }
            }
        }
    }

    @EventTarget
    public void onEvent(EventRender2D event) {
        if (fireworks.isEmpty()) return;

        updateAndCleanFireworks();

        for (Firework firework : fireworks) {
            firework.updateAlpha();

            Vector3d pos = firework.getPosition();
            Vector2f screenPos = ProjectUtil.project2D(pos.x, pos.y, pos.z);

            if (!(screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE)) continue;

            renderFirework(event, firework, screenPos, time.get());
        }
    }

    private void renderFirework(EventRender2D event, Firework firework, Vector2f screenPos, boolean showTime) {
        float posX = screenPos.x - 20;
        float posY = screenPos.y;

        RenderUtil.drawMinecraftRectangle(event.getStack(), posX, posY, showTime ? Fonts.sf_medium[14].getWidth(String.format("%.1f сек.", Math.max(0.0, (firework.getLifetimeMs() - firework.timer.getTimePassed()) / 1000.0))) + 14 : 10, 11, ColorUtil.getColor(0, 0, 0, (int) (120 * firework.getAlpha())));
        RenderUtil.drawImage2D(new ResourceLocation("textures/item/firework_rocket.png"), posX + 0.5f, posY + 1.5f, 8, 8, ColorUtil.getColor(255, 255, 255, (int) (255 * firework.getAlpha())));

        if (showTime) Fonts.sf_medium[14].drawString(event.getStack(), String.format("%.1f сек.", Math.max(0.0, (firework.getLifetimeMs() - firework.timer.getTimePassed()) / 1000.0)), posX + 11, posY + 4, ColorUtil.getColor(255, 255, 255, (int) (255 * firework.getAlpha())));
    }

    private boolean hasActiveFireworkInRadius(Vector3d position, double radius) {
        final double radiusSquared = radius * radius;

        return fireworks.stream().filter(Firework::isActive).anyMatch(firework -> {
            double distanceSquared = position.squareDistanceTo(firework.getPosition());
            return distanceSquared <= radiusSquared;
        });
    }

    private void updateAndCleanFireworks() {
        fireworks.removeIf(firework -> {
            if (firework.timer.getTimePassed() >= firework.getLifetimeMs()) {
                if (!firework.isFading()) {
                    firework.startFade();
                }
                return firework.shouldRemove();
            }
            return false;
        });
    }

    @EventTarget
    public void onEvent(EventSwapWorld event) {
        fireworks.clear();
    }

    @java.lang.Override
    public void onDisable() {
        fireworks.clear();
        super.onDisable();
    }

    @Getter
    public static class Firework {
        private final Vector3d position;
        private final TimeUtil timer = new TimeUtil();
        private final long lifetimeMs = 5000;
        private boolean fading = false;
        private final AnimationUtil alpha = new AnimationUtil(1.0f, 6f, Easings.LINEAR);

        public Firework(Vector3d position) {
            this.position = position;
        }

        public void startFade() {
            if (!fading) {
                fading = true;
                alpha.update(0.0f);
            }
        }

        public boolean shouldRemove() {
            return fading && alpha.isDone();
        }

        public float getAlpha() {
            return alpha.getValue();
        }

        public void updateAlpha() {
            if (fading) alpha.update(0.0f);
        }

        public boolean isActive() {
            return !fading || getAlpha() > 0.1f;
        }
    }
}