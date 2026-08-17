package sky.core.modules.impl.visuals;

import sky.core.modules.Category;
import sky.core.modules.Module;

public class ArmorDurabilityView extends Module {

    private static final float[][] COLOR_TABLE = {{0.6f, 0.0f, 0.0f}, {0.8f, 0.0f, 0.0f}, {1.0f, 0.0f, 0.0f}, {1.0f, 0.2f, 0.0f}, {1.0f, 0.4f, 0.0f}, {1.0f, 0.6f, 0.0f}, {1.0f, 0.8f, 0.0f}, {1.0f, 1.0f, 0.0f}, {0.9f, 1.0f, 0.0f}, {0.7f, 1.0f, 0.0f}, {0.5f, 1.0f, 0.0f}, {0.0f, 0.6f, 0.0f}, {0.0f, 0.7f, 0.0f}, {0.0f, 0.8f, 0.0f}, {0.0f, 0.9f, 0.0f}, {0.0f, 1.0f, 0.0f}, {0.2f, 1.0f, 0.2f}, {0.4f, 1.0f, 0.4f}, {0.6f, 1.0f, 0.6f}, {0.8f, 1.0f, 0.8f}, {1.0f, 1.0f, 1.0f}};

    public ArmorDurabilityView() {
        super("ArmorDurabilityView", "Красит броню в зависимости от прочности", Category.Visuals);
    }

    public static float[] getDurabilityColor(float durability) {
        if (durability < 0 || durability > 1) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }

        int interval = (int) (durability * 100 / 5);
        return COLOR_TABLE[Math.min(interval, COLOR_TABLE.length - 1)];
    }
}