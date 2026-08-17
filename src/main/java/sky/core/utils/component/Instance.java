package sky.core.utils.component;

import lombok.experimental.UtilityClass;
import sky.core.SkyCore;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@UtilityClass
public class Instance {
    private final ConcurrentMap<Class<? extends Component>, Component> componentInstances = new ConcurrentHashMap<>();

    public <T extends Component> T getComponent(Class<T> clazz) {
        return clazz.cast(componentInstances.computeIfAbsent(clazz, instance -> SkyCore.getInstance().getComponentManager().get(instance)));
    }
}
