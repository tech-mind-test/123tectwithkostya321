package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

@Getter
@Setter
public class EventMoving implements Event {
    public Vector3d from, to, motion;
    private final boolean toGround;
    private final AxisAlignedBB aabbFrom;
    @Getter
    public boolean ignoreHorizontal, ignoreVertical, collidedHorizontal, collidedVertical;

    public EventMoving(Vector3d from, Vector3d to, Vector3d motion, boolean toGround, boolean isCollidedHorizontal, boolean isCollidedVertical, AxisAlignedBB aabbFrom) {
        this.from = from;
        this.to = to;
        this.motion = motion;
        this.toGround = toGround;
        this.collidedHorizontal = isCollidedHorizontal;
        this.collidedVertical = isCollidedVertical;
        this.aabbFrom = aabbFrom;
    }

    public Vector3d motion() {
        return this.motion;
    }

}
