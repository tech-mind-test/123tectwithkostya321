package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.events.EventViewModel;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ClickSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import net.minecraft.util.HandSide;

public class ViewModel extends Module {

    public final SliderSetting right_x = new SliderSetting("Правая рука X", 0.0F, -2, 2, 0.1F);
    public final SliderSetting right_y = new SliderSetting("Правая рука Y", 0.0F, -2, 2, 0.1F);
    public final SliderSetting right_z = new SliderSetting("Правая рука Z", 0.0F, -2, 2, 0.1F);
    public final SliderSetting left_x = new SliderSetting("Левая рука X", 0.0F, -2, 2, 0.1F);
    public final SliderSetting left_y = new SliderSetting("Левая рука Y", 0.0F, -2, 2, 0.1F);
    public final SliderSetting left_z = new SliderSetting("Левая рука Z", 0.0F, -2, 2, 0.1F);
    public final ClickSetting reset = new ClickSetting("Сбросить", () -> {
        right_x.set(0.0F);
        right_y.set(0.0F);
        right_z.set(0.0F);
        left_x.set(0.0F);
        left_y.set(0.0F);
        left_z.set(0.0F);
    });

    public ViewModel() {
        super("View Model", "Позволяет изменить положение рук", Category.Visuals);
        addSettings(right_x, right_y, right_z, left_x, left_y, left_z, reset);
    }

    @EventTarget
    public void onEvent(EventViewModel event) {
        MatrixStack matrixStack = event.getMatrixStack();

        if (event.getHandside() == HandSide.RIGHT) {
            matrixStack.translate(right_x.get(), right_y.get(), right_z.get());
        } else {
            matrixStack.translate(left_x.get(), left_y.get(), left_z.get());
        }
    }
}