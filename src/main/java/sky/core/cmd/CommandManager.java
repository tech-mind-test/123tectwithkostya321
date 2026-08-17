package sky.core.cmd;

import com.mojang.brigadier.CommandDispatcher;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.command.ISuggestionProvider;
import sky.core.cmd.impl.*;
import sky.core.utils.Wrapper;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class CommandManager implements Wrapper {
    private String prefix = ".";

    private final CommandDispatcher<ISuggestionProvider> dispatcher = new CommandDispatcher<>();
    private final List<Command> commands = new ArrayList<>();

    public ISuggestionProvider getSource() {
        ClientPlayNetHandler conn = mc.getConnection();
        if (conn != null) {
            return conn.getSuggestionProvider();
        }
        return new ClientSuggestionProvider(null, mc);
    }


    private void initialize() {
        add(new ConfigCommand());
        add(new FriendCommand());
        add(new GpsCommand());
        add(new BindCommand());
     //   add(new PartyCommand());
        add(new StaffCommand());
        add(new FakePlayerCommand());
        add(new BotCommand());
        add(new MacroCommand());
        //   add(new PrefixCommand());
      //  add(new HelpCommand());
        add(new WaypointCommand());
        add(new BlockESPCommand());
    //    add(new NukerCommand());
        add(new RctCommand());
        add(new AutoContractCommand());
    }

    public CommandManager() {
        initialize();
    }

    private void add(Command command) {
        command.add(dispatcher);
        commands.add(command);
    }
}