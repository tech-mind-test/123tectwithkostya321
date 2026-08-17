package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import sky.core.events.EventRenderChunk;
import sky.core.events.EventRenderChunkContainer;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.animation.Easing;
import sky.core.utils.animation.Easings;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Animation extends Module {

    public MultiBooleanSetting mode = new MultiBooleanSetting("Что анимировать", new BooleanSetting("Список игроков", false), new BooleanSetting("Инвентарь", false), new BooleanSetting("Приближение камеры", false), new BooleanSetting("Обновление чанков", false), new BooleanSetting("Изменение перспективы", false));
    public ModeSetting chunkanim = new ModeSetting("Анимация чанков", "Quart", () -> mode.is("Обновление чанков"), "Quart", "Circ", "Sine", "Cubic");
    private final SliderSetting chunkSpeed = new SliderSetting("Скорость", 6.0F, 2.0F, 10.0F, 1.0F, () -> mode.is("Обновление чанков"));

    public Animation() {
        super("Animation", "Анимирует выбранные действия", Category.Visuals);
        addSettings(mode, chunkanim, chunkSpeed);
    }

    private final WeakHashMap<ChunkRenderDispatcher.ChunkRender, AtomicLong> renderChunkMap = new WeakHashMap<>();

    private double applySelectedEasing(double t) {
        String modeName = chunkanim.get();
        Easing easing;
        if ("Circ".equalsIgnoreCase(modeName)) {
            easing = Easings.CIRC_OUT;
        } else if ("Sine".equalsIgnoreCase(modeName)) {
            easing = Easings.SINE_OUT;
        } else if ("Cubic".equalsIgnoreCase(modeName)) {
            easing = Easings.CUBIC_OUT;
        } else {
            easing = Easings.QUART_OUT;
        }
        return easing.ease(t);
    }

    @EventTarget
    private void onEvent(EventRenderChunk event) {
        if (!Boolean.TRUE.equals(mode.is("Обновление чанков"))) {
            return;
        }
        if (mc.player != null && mc.world != null) {
            if (!renderChunkMap.containsKey(event.getChunkRender())) {
                renderChunkMap.put(event.getChunkRender(), new AtomicLong(-1L));
            }
        }
    }

    @EventTarget
    private void onEvent(EventRenderChunkContainer event) {
        if (!Boolean.TRUE.equals(mode.is("Обновление чанков"))) {
            return;
        }
        if (renderChunkMap.containsKey(event.getChunkRender())) {
            AtomicLong timeAlive = renderChunkMap.get(event.getChunkRender());
            long timeClone = timeAlive.get();
            if (timeClone == -1L) {
                timeClone = System.currentTimeMillis();
                timeAlive.set(timeClone);
            }

            long timeDifference = System.currentTimeMillis() - timeClone;
            double durationMs = chunkSpeed.get() * 100;
            if (timeDifference <= durationMs) {
                double chunkY = event.getChunkRender().getPosition().getY();
                double t = timeDifference / durationMs;
                double offsetY = chunkY * applySelectedEasing(t);
                RenderSystem.translated(0.0D, -chunkY + offsetY, 0.0D);
            }
        }
    }

}
