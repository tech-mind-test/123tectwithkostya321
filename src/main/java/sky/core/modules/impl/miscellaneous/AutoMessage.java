package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.text.TextFormatting;
import com.adl.nativeprotect.Native;
import sky.core.events.EventPacket;
import sky.core.modules.Category;
import sky.core.modules.Module;

import java.util.Random;

public class AutoMessage extends Module {
    private static final String[] INSULT_MESSAGES = new String[]{
            "але шлюха? да-да нексус айм пастер айм COCKLEAR DLC",
            "Удаляй нахуй свою пасточку на коклеаре и покупай скайкор нах",
            "Please buy SkyCore, not chatgptclear.!",
            "Опять упал в колени и скушал мою сперму потому что ты не был с лучшим читом SkyCore",
            "Веревочка + мыло = суицыд, ну а если не хочешь гетай skycore",
            "Соси мне дальше уёбок с коклеаром купи лучше скайкор дура",
            "Ты будешь сосать если выбрал пастаклир, а не SkyCore!",
            "Пора в пастель пока бабка не отпиздила тебя как субо",
            "Пиздуй оправдоваться якобы у тебя не нупокакал длц",
            "Придумал тебе кличку (сосуякин) как тебе? если заебок напиши выпиши чот дура",
            "Пойми сын дуры SkyCore непобедить.",
            "Выебан лучшим читом SkyCore!",
            "get good SkyCore!",
            "Хочешь чтобы тебя не ебали как сейчас я тебя? - Покупай SkyCore",
            "полетел на спавн хуесос",
            "Пойми что SkyCore убил тебя в 0",
            "У тебя есть сиська у меня пиписька и если мы будем рядом то нам будет заебисько!"
    };

    private final Random random = new Random();

    public AutoMessage() {
        super("AutoMessage", "Автоматические оскорбления после убийства", Category.Miscellaneous);
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null || !event.isReceive()) return;
        if (!(event.getPacket() instanceof SChatPacket chatPacket)) return;

        String rawChat = chatPacket.getChatComponent().getString();
        String message = TextFormatting.getTextWithoutFormattingCodes(rawChat);
        if (message == null) return;

        if (message.contains("Вы убили")) {
            String victimName = extractPlayerNameFromKillMessage(message);
            if (victimName != null && !victimName.isEmpty()) {
                String insult = INSULT_MESSAGES[random.nextInt(INSULT_MESSAGES.length)];
                mc.player.sendChatMessage("! -ezz " + victimName + " " + insult);
            }
        }
    }

    @Native
    private String extractPlayerNameFromKillMessage(String message) {
        int killIndex = message.toLowerCase().indexOf("вы убили");
        if (killIndex == -1) return null;

        String afterKill = message.substring(killIndex + "Вы убили".length()).trim();
        if (afterKill.isEmpty()) return null;

        afterKill = afterKill.replaceAll("^[!.,:;\\s]+", "");
        String[] parts = afterKill.split("\\s+");
        if (parts.length == 0) return null;

        String name = parts[0].trim().replaceAll("[!.,:;]+$", "");
        return name.isEmpty() ? null : name;
    }
}

