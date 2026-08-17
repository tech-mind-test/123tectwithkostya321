//package sky.core.utils.discord;
//
//import com.jagrosh.discordipc.IPCClient;
//import com.jagrosh.discordipc.IPCListener;
//import com.jagrosh.discordipc.entities.RichPresence;
//import ru.kotopushka.compiler.sdk.classes.Profile;
//
//import java.time.OffsetDateTime;
//
//public class DiscordRPCManager {
//
//    private RichPresence.Builder getBuilder() {
//        RichPresence.Builder builder = new RichPresence.Builder();
//        return builder.setDetails("Build: 1.16.5")
//                .setButton1Text("Discord")
//                .setButton2Text("Telegram")
//                .setButton1Url("https://discord.gg/KE3fbQeFqR")
//                .setButton2Url("https://t.me/Divinecorp")
//                .setState("UID: " + Profile.getUid())
//                .setStartTimestamp(OffsetDateTime.now())
//                .setLargeImage("https://r2.e-z.host/9745b31e-2dd8-48a2-8fce-784ce213928d/dxeuvc93.png", Profile.getUsername());
//    }
//
//    public void update() {
//        try {
//            IPCClient client = new IPCClient(1417532770892251338L);
//            client.setListener(new IPCListener() {
//                @EventTarget
//                public void onReady(IPCClient client) {
//                    client.sendRichPresence(getBuilder().build());
//                }
//            });
//            client.connect();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}