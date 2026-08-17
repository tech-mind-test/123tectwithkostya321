package sky.core.utils.net;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MicrosoftLogin {
    static ExecutorService executor;
    private static final String CLIENT_SECRET_ENV = "MICROSOFT_CLIENT_SECRET";
    private static final String CLIENT_ID = "9fbc7315-7200-4b2b-a655-bb38c865da17";
    private static HttpServer server;
    private static Consumer<String> callback;
    static Gson gson;

    private static String clientSecretParameter() {
        String secret = System.getenv(CLIENT_SECRET_ENV);
        if (secret == null || secret.trim().isEmpty()) {
            return "";
        }
        return "&client_secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8);
    }

    static void browse(final String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (UnsupportedOperationException | SecurityException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void getRefreshToken(final Consumer<String> callback) {
        MicrosoftLogin.callback = callback;
        startServer();
        String redirect = URLEncoder.encode("http://localhost:8247", StandardCharsets.UTF_8);
        String scope = URLEncoder.encode("XboxLive.signin offline_access", StandardCharsets.UTF_8);
        String authUrl = "https://login.live.com/oauth20_authorize.srf?client_id=" + CLIENT_ID + "&response_type=code&redirect_uri=" + redirect + "&scope=" + scope;
        System.out.println("Microsoft auth URL: " + authUrl);
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(authUrl), null);
        } catch (Exception ignored) {
        }
        browse(authUrl);
    }

    public static LoginData login(String refreshToken) {
        String tokenResp = BrowserUtil.postExternal("https://login.live.com/oauth20_token.srf", "client_id=9fbc7315-7200-4b2b-a655-bb38c865da17" + clientSecretParameter() + "&refresh_token=" + refreshToken + "&grant_type=refresh_token&redirect_uri=http://localhost:" + 8247, false);
        System.out.println("MicrosoftLogin: token endpoint response: " + tokenResp);
        final AuthTokenResponse res = MicrosoftLogin.gson.fromJson(tokenResp, AuthTokenResponse.class);
        if (res == null) {
            System.out.println("MicrosoftLogin: failed to parse auth token response");
            return new LoginData();
        }
        final String accessToken = res.access_token;
        refreshToken = res.refresh_token;

        String xblReq = "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=" + accessToken + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}";
        String xblResp = BrowserUtil.postExternal("https://user.auth.xboxlive.com/user/authenticate", xblReq, true);
        System.out.println("MicrosoftLogin: xbl authenticate response: " + xblResp);
        final XblXstsResponse xblRes = MicrosoftLogin.gson.fromJson(xblResp, XblXstsResponse.class);
        if (xblRes == null) {
            System.out.println("MicrosoftLogin: failed to parse XBL response");
            return new LoginData();
        }

        String xstsReq = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblRes.Token + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";
        String xstsResp = BrowserUtil.postExternal("https://xsts.auth.xboxlive.com/xsts/authorize", xstsReq, true);
        System.out.println("MicrosoftLogin: xsts response: " + xstsResp);
        final XblXstsResponse xstsRes = MicrosoftLogin.gson.fromJson(xstsResp, XblXstsResponse.class);
        if (xstsRes == null) {
            System.out.println("MicrosoftLogin: failed to parse XSTS response");
            return new LoginData();
        }

        String mcReq = "{\"identityToken\":\"XBL3.0 x=" + xblRes.DisplayClaims.xui[0].uhs + ";" + xstsRes.Token + "\"}";
        String mcResp = BrowserUtil.postExternal("https://api.minecraftservices.com/authentication/login_with_xbox", mcReq, true);
        System.out.println("MicrosoftLogin: minecraft login response: " + mcResp);
        final McResponse mcRes = MicrosoftLogin.gson.fromJson(mcResp, McResponse.class);
        if (mcRes == null) {
            System.out.println("MicrosoftLogin: failed to parse Minecraft login response");
            return new LoginData();
        }

        String ownershipResp = BrowserUtil.getBearerResponse("https://api.minecraftservices.com/entitlements/mcstore", mcRes.access_token);
        System.out.println("MicrosoftLogin: ownership response: " + ownershipResp);
        final GameOwnershipResponse gameOwnershipRes = MicrosoftLogin.gson.fromJson(ownershipResp, GameOwnershipResponse.class);
        if (gameOwnershipRes == null || !gameOwnershipRes.hasGameOwnership()) {
            System.out.println("MicrosoftLogin: no game ownership");
            return new LoginData();
        }

        String profileResp = BrowserUtil.getBearerResponse("https://api.minecraftservices.com/minecraft/profile", mcRes.access_token);
        System.out.println("MicrosoftLogin: profile response: " + profileResp);
        final ProfileResponse profileRes = MicrosoftLogin.gson.fromJson(profileResp, ProfileResponse.class);
        if (profileRes == null) {
            System.out.println("MicrosoftLogin: failed to parse profile response");
            return new LoginData();
        }
        return new LoginData(mcRes.access_token, refreshToken, profileRes.id, profileRes.name);
    }

    private static void startServer() {
        if (MicrosoftLogin.server != null) {
            return;
        }
        try {
            (MicrosoftLogin.server = HttpServer.create(new InetSocketAddress("localhost", 8247), 0)).createContext("/", new Handler());
            MicrosoftLogin.server.setExecutor(MicrosoftLogin.executor);
            MicrosoftLogin.server.start();
            System.out.println("MicrosoftLogin: local callback server started on http://localhost:8247/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void stopServer() {
        if (MicrosoftLogin.server == null) {
            return;
        }
        MicrosoftLogin.server.stop(0);
        MicrosoftLogin.server = null;
        MicrosoftLogin.callback = null;
    }

    static {
        MicrosoftLogin.executor = Executors.newSingleThreadExecutor();
        MicrosoftLogin.gson = new Gson();
    }

    public static class LoginData {
        public String mcToken;
        public String newRefreshToken;
        public String uuid;
        public String username;

        public LoginData() {
        }

        public LoginData(final String mcToken, final String newRefreshToken, final String uuid, final String username) {
            this.mcToken = mcToken;
            this.newRefreshToken = newRefreshToken;
            this.uuid = uuid;
            this.username = username;
        }

        public boolean isGood() {
            return this.mcToken != null;
        }
    }

    private static class Handler implements HttpHandler {
        @Override
        public void handle(final HttpExchange req) throws IOException {
            if (req.getRequestMethod().equals("GET")) {
                final List<NameValuePair> query = URLEncodedUtils.parse(req.getRequestURI(), StandardCharsets.UTF_8.name());
                boolean ok = false;
                for (final NameValuePair pair : query) {
                    if (pair.getName().equals("code")) {
                        try {
                            this.handleCode(pair.getValue());
                            this.writeText(req, "<html>You may now close this page.<script>close()</script></html>");
                        } catch (Exception e) {
                            e.printStackTrace();
                            this.writeText(req, "<html>Authentication failed: " + e.getMessage() + "</html>");
                        }
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    String queryStr = req.getRequestURI().getQuery();
                    System.out.println("MicrosoftLogin: callback without code, query=" + queryStr);
                    this.writeText(req, "<html>Cannot authenticate. Query: " + (queryStr == null ? "" : queryStr) + "</html>");
                }
            }
            stopServer();
        }

        private void handleCode(final String code) {
            final String response = BrowserUtil.postExternal("https://login.live.com/oauth20_token.srf", "client_id=9fbc7315-7200-4b2b-a655-bb38c865da17&code=" + code + clientSecretParameter() + "&grant_type=authorization_code&redirect_uri=http://localhost:" + 8247, false);
            final AuthTokenResponse res = MicrosoftLogin.gson.fromJson(response, AuthTokenResponse.class);
            if (res == null) {
                MicrosoftLogin.callback.accept(null);
            } else {
                MicrosoftLogin.callback.accept(res.refresh_token);
            }
        }

        private void writeText(final HttpExchange req, final String text) throws IOException {
            final OutputStream out = req.getResponseBody();
            req.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            req.sendResponseHeaders(200, text.length());
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        }
    }

    private static class AuthTokenResponse {
        @Expose
        @SerializedName("access_token")
        public String access_token;
        @Expose
        @SerializedName("refresh_token")
        public String refresh_token;
    }

    private static class XblXstsResponse {
        @Expose
        @SerializedName("Token")
        public String Token;
        @Expose
        @SerializedName("DisplayClaims")
        public DisplayClaims DisplayClaims;

        private static class DisplayClaims {
            @Expose
            @SerializedName("xui")
            private Claim[] xui;

            private static class Claim {
                @Expose
                @SerializedName("uhs")
                private String uhs;
            }
        }
    }

    private static class McResponse {
        @Expose
        @SerializedName("access_token")
        public String access_token;
    }

    private static class GameOwnershipResponse {
        @Expose
        @SerializedName("items")
        private Item[] items;

        private boolean hasGameOwnership() {
            boolean hasProduct = false;
            boolean hasGame = false;
            for (final Item item : this.items) {
                if (item.name.equals("product_minecraft")) {
                    hasProduct = true;
                } else if (item.name.equals("game_minecraft")) {
                    hasGame = true;
                }
            }
            return hasProduct && hasGame;
        }

        private static class Item {
            @Expose
            @SerializedName("name")
            private String name;
        }
    }

    private static class ProfileResponse {
        @Expose
        @SerializedName("id")
        public String id;
        @Expose
        @SerializedName("name")
        public String name;
    }
}
