package sky.core.utils.animations.advanced;

import lombok.Getter;
import sky.core.utils.timer.Timer;

@Getter
public class Animation {
    private final Timer timer = new Timer();
    private int speed;
    private double size = 1;
    private boolean forward;
    private Easing easing;

    public boolean finished(boolean forward) {
        return timer.finished(speed) && (forward ? this.forward : !this.forward);
    }

    public boolean finished() {
        return timer.finished(speed) && this.forward;
    }

    public Animation setForward(boolean forward) {
        if (this.forward != forward) {
            this.forward = forward;
            timer.setMillis((long) (System.currentTimeMillis() - (size - Math.min(size, timer.getElapsedTime()))));
        }
        return this;
    }

    public Animation finish() {
        timer.setMillis((long) (System.currentTimeMillis() - speed));
        return this;
    }

    public Animation setEasing(Easing easing) {
        this.easing = easing;
        return this;
    }

    public Animation setSpeed(int speed) {
        this.speed = speed;
        return this;
    }

    public Animation setSize(float size) {
        this.size = size;
        return this;
    }

    public float getLinear() {
        if (forward) {
            if (timer.finished(speed)) return (float) size;
            return (float) (timer.getElapsedTime() / (double) speed * size);
        } else {
            if (timer.finished(speed)) return 0.0f;
            return (float) ((1 - timer.getElapsedTime() / (double) speed) * size);
        }
    }

    public float get() {
        if (forward) {
            if (timer.finished(speed)) return (float) size;
            return (float) (easing.apply(timer.getElapsedTime() / (double) speed) * size);
        } else {
            if (timer.finished(speed)) return 0.0f;
            return (float) ((1 - easing.apply(timer.getElapsedTime() / (double) speed)) * size);
        }
    }

    public float reversed() {
        return 1 - get();
    }

    public void reset() {
        timer.reset();
    }
}

