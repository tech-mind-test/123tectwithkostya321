package sky.core.utils.discord.rpc.callbacks;

import com.sun.jna.Callback;
import sky.core.utils.discord.rpc.utils.DiscordUser;

public interface JoinRequestCallback extends Callback {
    void apply(DiscordUser user);
}
