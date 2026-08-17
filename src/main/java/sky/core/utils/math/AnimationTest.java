//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sky.core.utils.math;


public class AnimationTest {
    private Easing easing;
    private long duration;
    private long millis;
    private long startTime;
    private double startValue;
    private double destinationValue;
    private double value;
    private boolean finished;

    public AnimationTest(Easing easing, long duration) {
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
    }

    public void run(double destinationValue) {
        this.millis = System.currentTimeMillis();
        if (this.destinationValue != destinationValue) {
            this.destinationValue = destinationValue;
            this.reset();
        } else {
            this.finished = this.millis - this.duration > this.startTime;
            if (this.finished) {
                this.value = destinationValue;
                return;
            }
        }

        double result = (Double)this.easing.getFunction().apply(this.getProgress());
        if (this.value > destinationValue) {
            this.value = this.startValue - (this.startValue - destinationValue) * result;
        } else {
            this.value = this.startValue + (destinationValue - this.startValue) * result;
        }

    }

    public void run(boolean value) {
        this.run(value ? (double)1.0F : (double)0.0F);
    }

    public double getProgress() {
        return (double)(System.currentTimeMillis() - this.startTime) / (double)this.duration;
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
        this.startValue = this.value;
        this.finished = false;
    }

    public Easing getEasing() {
        return this.easing;
    }

    public long getDuration() {
        return this.duration;
    }

    public long getMillis() {
        return this.millis;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public double getStartValue() {
        return this.startValue;
    }

    public double getDestinationValue() {
        return this.destinationValue;
    }

    public double getValue() {
        return this.value;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setStartValue(double startValue) {
        this.startValue = startValue;
    }

    public void setDestinationValue(double destinationValue) {
        this.destinationValue = destinationValue;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof AnimationTest)) {
            return false;
        } else {
            AnimationTest other = (AnimationTest)o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.getDuration() != other.getDuration()) {
                return false;
            } else if (this.getMillis() != other.getMillis()) {
                return false;
            } else if (this.getStartTime() != other.getStartTime()) {
                return false;
            } else if (Double.compare(this.getStartValue(), other.getStartValue()) != 0) {
                return false;
            } else if (Double.compare(this.getDestinationValue(), other.getDestinationValue()) != 0) {
                return false;
            } else if (Double.compare(this.getValue(), other.getValue()) != 0) {
                return false;
            } else if (this.isFinished() != other.isFinished()) {
                return false;
            } else {
                Object this$easing = this.getEasing();
                Object other$easing = other.getEasing();
                if (this$easing == null) {
                    if (other$easing != null) {
                        return false;
                    }
                } else if (!this$easing.equals(other$easing)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof AnimationTest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $duration = this.getDuration();
        result = result * 59 + (int)($duration >>> 32 ^ $duration);
        long $millis = this.getMillis();
        result = result * 59 + (int)($millis >>> 32 ^ $millis);
        long $startTime = this.getStartTime();
        result = result * 59 + (int)($startTime >>> 32 ^ $startTime);
        long $startValue = Double.doubleToLongBits(this.getStartValue());
        result = result * 59 + (int)($startValue >>> 32 ^ $startValue);
        long $destinationValue = Double.doubleToLongBits(this.getDestinationValue());
        result = result * 59 + (int)($destinationValue >>> 32 ^ $destinationValue);
        long $value = Double.doubleToLongBits(this.getValue());
        result = result * 59 + (int)($value >>> 32 ^ $value);
        result = result * 59 + (this.isFinished() ? 79 : 97);
        Object $easing = this.getEasing();
        result = result * 59 + ($easing == null ? 43 : $easing.hashCode());
        return result;
    }

    public String toString() {
        String var10000 = String.valueOf(this.getEasing());
        return "Animation(easing=" + var10000 + ", duration=" + this.getDuration() + ", millis=" + this.getMillis() + ", startTime=" + this.getStartTime() + ", startValue=" + this.getStartValue() + ", destinationValue=" + this.getDestinationValue() + ", value=" + this.getValue() + ", finished=" + this.isFinished() + ")";
    }
}
