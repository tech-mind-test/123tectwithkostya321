package sky.core;

import com.adl.nativeprotect.Native;
import com.darkmagician6.eventapi.EventManager;
import other.party.IRCClient;
import sky.core.cmd.CommandManager;
import sky.core.utils.component.ComponentManager;
import sky.core.handlers.HandlerManager;
import sky.core.utils.managers.impl.*;
import sky.core.utils.managers.impl.dragmanager.DraggingManager;
import sky.core.utils.managers.impl.staffmanager.StaffManager;
import sky.core.modules.ModuleManager;
import sky.core.ui.gui.DropDown;
import sky.core.utils.discord.rpc.DiscordManager;
import lombok.Getter;
import mods.cape.Cape;
import mods.proxy.Config;
import mods.proxy.ProxyServer;
import mods.viaversion.viamcp.ViaMCP;
import net.minecraft.util.text.StringTextComponent;

@Getter
public class SkyCore {

    @Getter
    private static SkyCore instance;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private ThemeManager themeManager;
    private AccountManager accountManager;
    private ClientConfig clientConfig;
    private FriendManager friendManager;
    private StaffManager staffManager;
    private MacroManager macroManager;
    private WaypointManager waypointManager;
    private BlockESPManager blockESPManager;
    private CommandManager commandsManager;
    private IRCClient ircClient;
    private DraggingManager draggingManager;
    private HandlerManager handlerManager;
    public DropDown dropDown;
    private ComponentManager componentManager;
    private Cape cape;
    private DiscordManager discordManager;

    public SkyCore() {
        instance = this;
        init();
    }

    private void init() {
        initMods();
        initManager();

        EventManager.register(this);
    }

    @Native
    private void initManager() {
        draggingManager = new DraggingManager();
        draggingManager.init();

        accountManager = new AccountManager();
        accountManager.init();

        themeManager = new ThemeManager();
        themeManager.init();

        configManager = new ConfigManager();
        configManager.init();

        moduleManager = new ModuleManager();
        moduleManager.init();

        friendManager = new FriendManager();
        friendManager.init();

        staffManager = new StaffManager();
        staffManager.init();

        macroManager = new MacroManager();
        macroManager.init();

        waypointManager = new WaypointManager();
        waypointManager.init();

        blockESPManager = new BlockESPManager();
        blockESPManager.init();

        componentManager = new ComponentManager();
        componentManager.init();

        clientConfig = new ClientConfig();
        clientConfig.init();

        handlerManager = new HandlerManager();
        handlerManager.init();


        clientConfig.applySettings();

        dropDown = new DropDown(new StringTextComponent(""));

        clientConfig.initializeDefaultTheme();

        commandsManager = new CommandManager();
        //discordManager = new DiscordManager().start();


//        ircClient = new IRCClient("ABOBAPASTER");
//        ircClient.connect();
//        discordRPCManager = new DiscordRPCManager();
//        discordRPCManager.update();

    }

    @Native
    private void initMods() {
        new ProxyServer();
        //  new InteractHandler();
        Config.loadConfig();
        cape = new Cape();
        cape.init();
        try {
            ViaMCP.create();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        draggingManager.save();
        if (clientConfig != null && ClientConfig.getCurrentActiveTheme() != null) {
            clientConfig.onThemeApplied(ClientConfig.getCurrentActiveTheme());
        }

        accountManager.updateLastAccountFile();
        clientConfig.saveCurrentSettings();
//        if (discordManager != null) {
//            discordManager.stopRPC();
//        }
    }
}
