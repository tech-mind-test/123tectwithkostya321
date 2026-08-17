package sky.core.utils.discord.rpc.callbacks;

import com.sun.jna.Callback;

public interface JoinGameCallback extends Callback {
    void apply(String secret);
}
