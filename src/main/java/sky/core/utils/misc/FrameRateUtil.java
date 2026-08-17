package sky.core.utils.misc;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class FrameRateUtil {
    private static final List<Long> records = new ArrayList<>();
    @Getter
    private static int fps = 5;

    public static void recordFrame() {
        long c = System.currentTimeMillis();
        records.add(c);
        records.removeIf(aLong -> aLong + 1000 < System.currentTimeMillis());
        fps = Math.max(records.size(), 4);
    }
}
