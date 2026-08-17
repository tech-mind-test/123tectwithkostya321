package sky.core.utils.player;

import lombok.Getter;
import lombok.experimental.Accessors;
import sky.core.utils.math.TimeUtil;


@Getter
@Accessors(fluent = true)
public class PerfectDelay {
    private long delay = 0;
    private final TimeUtil time = new TimeUtil();

    public boolean cooldownComplete() {
        return cooldownComplete(this.delay);
    }

    public boolean cooldownComplete(long delay) {
        return time.finished(delay);
    }

    public void reset(long delay) {
        this.time.reset();
        this.delay = delay;
    }

}