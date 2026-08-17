package sky.core.utils.managers.impl.staffmanager;

import lombok.Getter;
import net.minecraft.util.text.ITextComponent;
import java.util.Objects;

@Getter
public class Staff {
    private final String name;
    private final ITextComponent prefix;
    private final boolean vanished;

    public Staff(String name, ITextComponent prefix, boolean vanished) {
        this.name = name;
        this.prefix = prefix;
        this.vanished = vanished;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Staff staff)) return false;
        return Objects.equals(name, staff.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}