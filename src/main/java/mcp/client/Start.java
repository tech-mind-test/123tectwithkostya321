package mcp.client;


import net.minecraft.client.main.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Start
{
    public static void main(String[] args)
    {
        /*
         * start minecraft game application
         * --version is just used as 'launched version' in snoop data and is required
         * Working directory is used as gameDir if not provided
         */
        String assets = System.getenv().containsKey("assetDirectory") ? System.getenv("assetDirectory") : "assets";
        Main.main(withMissingDefaults(args, assets));
    }

    public static <T> T[] concat(T[] first, T[] second)
    {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static String[] withMissingDefaults(String[] args, String assetsDir)
    {
        List<String> result = new ArrayList<>(Arrays.asList(args));
        addIfMissing(result, "--version", "mcp");
        addIfMissing(result, "--accessToken", "0");
        addIfMissing(result, "--assetsDir", assetsDir);
        addIfMissing(result, "--assetIndex", "1.16");
        addIfMissing(result, "--userProperties", "{}");
        return result.toArray(new String[0]);
    }

    private static void addIfMissing(List<String> args, String key, String value)
    {
        if (!args.contains(key))
        {
            args.add(key);
            args.add(value);
        }
    }
}
