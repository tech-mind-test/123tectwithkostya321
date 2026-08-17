package sky.core.utils.other;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AsyncManager {

    public Thread run(Runnable runnable) {
        Thread thread = new Thread(() -> runnable.run());
        thread.start();
        return thread;
    }

}