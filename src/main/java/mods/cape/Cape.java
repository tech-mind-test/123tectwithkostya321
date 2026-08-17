package mods.cape;

public class Cape {
    public static Cape INSTANCE;
    public static Config config;

    public void init() {
        INSTANCE = this;
        config = new Config();
    }
}
