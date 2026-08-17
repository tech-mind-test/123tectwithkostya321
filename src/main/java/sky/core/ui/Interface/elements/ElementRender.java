package sky.core.ui.Interface.elements;


import sky.core.events.EventRender2D;
import sky.core.utils.Wrapper;

public interface ElementRender extends Wrapper {
    void render(EventRender2D.Post eventRender);
}
