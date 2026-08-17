package sky.core.utils.discord.rpc.callbacks;

import com.sun.jna.Callback;
import sky.core.utils.discord.rpc.utils.DiscordUser;

public interface ReadyCallback extends Callback {
    void apply(DiscordUser user);
}
