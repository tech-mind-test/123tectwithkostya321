package net.minecraft.network;

import net.minecraft.util.text.ITextComponent;
import other.bot.connection.BotNetwork;

public interface INetHandler
{
    /**
     * Invoked when disconnecting, the parameter is a ChatComponent describing the reason for termination
     */
    void onDisconnect(ITextComponent reason);

    /**
     * Returns this the NetworkManager instance registered with this NetworkHandlerPlayClient
     */
    NetworkManager getNetworkManager();
    BotNetwork getBotNetwork();

}
