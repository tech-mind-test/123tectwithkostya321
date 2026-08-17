package mods.proxy;


import net.minecraft.client.gui.widget.button.Button;

public class ProxyServer {
    public static boolean proxyEnabled = false;
    public static Proxy proxy = new Proxy();
    public static Proxy lastUsedProxy = new Proxy();
    public static Button proxyMenuButton;

    public static String getLastUsedProxyIp() {
        return lastUsedProxy.ipPort.isEmpty() ? "none" : lastUsedProxy.getIp();
    }

//    @EventTarget
//    public void onInitialize() {
//        Config.loadConfig();
//    }
}
