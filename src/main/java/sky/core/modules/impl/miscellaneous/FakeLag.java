package sky.core.modules.impl.miscellaneous;


import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.util.math.vector.Vector3d;
import com.adl.nativeprotect.Native;
import sky.core.events.*;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.StopWatch;
import sky.core.utils.math.TimeUtil;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FakeLag extends Module {

    private final BooleanSetting пульс = new BooleanSetting("Пульс", false);
    private final BooleanSetting рендер = new BooleanSetting("Рендер позиции", false);
    private final SliderSetting лаг = new SliderSetting("Пинг", 100F, 100F, 2000F, 25F, пульс::get);
    private final BooleanSetting атака = new BooleanSetting("Обновлять при атаке", false);

    public FakeLag() {
        super("Fake Lag", "Создаёт фейковый пинг на сервере", Category.Combat);
        addSettings(пульс, лаг, атака);
    }

    TimeUtil asd = new TimeUtil();
    public final StopWatch часы = new StopWatch();
    private final CopyOnWriteArrayList<IPacket> пакеты = new CopyOnWriteArrayList<>();
    boolean стоп = false;
    boolean цвет = false;
    private long начался;
    final StopWatch стопер = new StopWatch();
    Vector3d прошлаяПозиция = new Vector3d(0, 0, 0);

    // Новые поля
    private IPacket отложенныйПакет = null;
    private boolean ждётАтаку = false;
    private final ScheduledExecutorService планировщик =
            Executors.newSingleThreadScheduledExecutor();

    @Native
    @EventTarget
    public void приОбновленииИгры(EventGameUpdate e) {
        if (mc.player == null || mc.world == null && this.isEnabled())
            toggle();
    }

    @Native
    @EventTarget
    public void приПакете(EventPacket e) {
        if (mc.player != null && mc.world != null && !mc.isSingleplayer() && !mc.player.getShouldBeDead()) {

            // Если ждём отправки атаки — этот пакет блокируем
            if (ждётАтаку && e.getPacket() instanceof CUseEntityPacket) {
                e.setCancelled(true);
                return;
            }

            boolean возврат = !цвет && e.getPacket() instanceof CUseEntityPacket && e.isSend();
            if (e.isSend()) {
                if (!возврат || !атака.get()) пакеты.add(e.getPacket());
                e.setCancelled(true);
            }

        } else toggle();
    }

    @Native
    @EventTarget
    public void приОбновлении(EventMotion e) {
        if ((System.currentTimeMillis() - начался) >= 29900) {
            toggle();
        }
        if ((часы.finished(лаг.get()) && пульс.get())
                || (стоп && атака.get() && стопер.finished(20))) {
            прошлаяПозиция = mc.player.getPositionVec();
            for (IPacket пакет : пакеты) {
                mc.player.connection.getNetworkManager().sendPacketWithoutEvent(пакет);
            }

            стоп = false;
            пакеты.clear();
            начался = System.currentTimeMillis();
            часы.reset();
        }
    }

    @Native
    // --- ВАЖНО: полностью изменённый обработчик атаки ---
    @EventTarget
    public void приАтаке(EventAttack e) {
        if (!атака.get()) return;

        double dist = e.getTarget().getDistance(
                mc.player
        );

        // Если ближе — обычное поведение FakeLag
        if (dist < 4.5) {
            стопер.reset();
            стоп = true;
            цвет = true;
            return;
        }

        // ДАЛЬШАЯ АТАКА → перехватываем
        e.setCancelled(true);
        цвет = false;

        ждётАтаку = true;
        отложенныйПакет = new CUseEntityPacket(e.getTarget(), mc.player.isSneaking());

        boolean былоВключено = this.isEnabled();
        if (былоВключено) this.toggle(); // временно выключаем FakeLag

        // Отправляем через 1 миллисекунду
        планировщик.schedule(() -> {

            if (отложенныйПакет != null) {
                mc.player.connection.getNetworkManager().sendPacketWithoutEvent(отложенныйПакет);
            }

            отложенныйПакет = null;
            ждётАтаку = false;

            if (!this.isEnabled() && былоВключено)
                this.toggle(); // включаем FakeLag обратно

        }, 1, TimeUnit.MILLISECONDS);
    }


    @EventTarget
    public void приРендере(EventRender3D e) {
//        if (рендер.get()) {
//            float halfSize = 0.3f;
//            VoxelShape форма = VoxelShapes.create(
//                    прошлаяПозиция.x - halfSize,
//                    прошлаяПозиция.y - halfSize + 0.3,
//                    прошлаяПозиция.z - halfSize,
//                    прошлаяПозиция.x + halfSize,
//                    прошлаяПозиция.y + halfSize + 1.5,
//                    прошлаяПозиция.z + halfSize
//            );
//
//            int цвет = атака.get()
//                    ? this.цвет ? ColorUtil.getColor(0, 255, 0) : ColorUtil.getColor(255, 0, 0)
//                    : ColorUtil.getColor(255, 255, 255);
//
//            RenderUtil3D.drawChams(e.getMatrix(), форма, прошлаяПозиция, цвет, 0.1f, true);
//        }
    }

    @Native
    @Override
    public void onEnable() {
        super.onEnable();
        начался = System.currentTimeMillis();
        прошлаяПозиция = mc.player.getPositionVec();
        часы.reset();
        стоп = false;
        стопер.reset();
    }

    @Native
    @Override
    public void onDisable() {
        super.onDisable();

        for (IPacket пакет : пакеты) {
            mc.player.connection.getNetworkManager().sendPacketWithoutEvent(пакет);
        }

        часы.reset();
        пакеты.clear();
        стоп = false;
        стопер.reset();
    }
}

