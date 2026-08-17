package sky.core.utils.animation;

@FunctionalInterface
public interface Easing {
    double ease(double value);
}