/* Decompiler 4ms, total 645ms, lines 27 */
package other.bot;



import other.bot.connection.BotClientPlayNetHandler;
import other.bot.connection.BotNetwork;
import other.bot.player.BotController;
import other.bot.player.BotPlayer;
import other.bot.world.BotWorld;

public class Bot {
    public BotNetwork networkManager;
    public BotWorld botWorld;
    public BotPlayer botPlayer;
    public BotController botController;
    public BotClientPlayNetHandler connection;
    public boolean collected;
    public boolean codesCollected;
    public long lastTimeCollected = 0L;

    public Bot(BotNetwork botNetwork, BotWorld botWorld, BotPlayer botPlayer, BotController botController, BotClientPlayNetHandler botClientPlayNetHandler) {
        this.networkManager = botNetwork;
        this.botWorld = botWorld;
        this.botPlayer = botPlayer;
        this.botController = botController;
        this.connection = botClientPlayNetHandler;
    }
}