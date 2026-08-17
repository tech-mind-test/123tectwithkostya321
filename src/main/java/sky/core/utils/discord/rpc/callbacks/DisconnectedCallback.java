package sky.core.utils.discord.rpc.callbacks;

import com.sun.jna.Callback;

public interface DisconnectedCallback extends Callback {
    void apply(int errorCode, String message);
}
