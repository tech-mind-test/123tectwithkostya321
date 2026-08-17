package sky.core.utils.managers.impl.notificationmanager;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.math.TimeUtil;
import lombok.Getter;
import net.minecraft.util.text.ITextComponent;

@Getter
public abstract class AbstractNotification {
    protected final ITextComponent message;
    protected final long duration = 1500;
    protected final TimeUtil timer = new TimeUtil();
    protected final AnimationUtil yAnimation = new AnimationUtil(0.0f, 20);
    protected final AnimationUtil alphaAnimation = new AnimationUtil(0.0f, 10);
    protected boolean forceExpire = false;
    protected boolean initializedY = false;

    public AbstractNotification(ITextComponent message) {
        this.message = message;
    }

    public abstract void render(float x, float y, MatrixStack matrixStack);

    public boolean isExpired() {
        return forceExpire || timer.hasTimeElapsed(duration);
    }

    public void expireNow() {
        this.forceExpire = true;
    }

    protected void initYIfNeeded(float y) {
        if (!initializedY) {
            yAnimation.setValue(y);
            initializedY = true;
        }
    }
}