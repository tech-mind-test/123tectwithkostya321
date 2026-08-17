package sky.core.utils.discord.rpc.utils;

import java.io.Serializable;

public class RPCButton implements Serializable {
    private final String url;
    private final String label;

    public static RPCButton create(String label, String url) {
        String safeLabel = label.substring(0, Math.min(label.length(), 31));
        return new RPCButton(safeLabel, url);
    }

    protected RPCButton(String label, String url) {
        this.label = label;
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    public String getLabel() {
        return this.label;
    }
}
