package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import com.adl.nativeprotect.Native;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.impl.combat.AttackAura;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class KillMessage extends Module {

    private final BooleanSetting soft = new BooleanSetting("Упоминать чит", true);

    private static final String[] killMessages = {
            "але шлюха? да-да нексус айм пастер айм NUCLEAR DLC", "Удаляй нахуй свою пасточку на нуклеаре и покупай скайкор нах", "Please buy SkyCore, not chatgptclear.!", "Опять упал в колени и скушал мою сперму потому что ты не был с лучшим читом SkyCore", "Веревочка + мыло = суицыд, ну а если не хочешь гетай skycore", "Соси мне дальше уёбок с нуклеаром купи лучше скайкор дура", "Ты будешь сосать если выбрал пастаклир, а не SkyCore!", "Пора в пастель пока бабка не отпиздила тебя как субо", "Пиздуй оправдоваться якобы у тебя не нупокакал длц", "Придумал тебе кличку (сосуякин) как тебе? если заебок напиши выпиши чот дура", "Пойми сын дуры SkyCore непобедить.", "Выебан лучшим читом SkyCore!", "get good SkyCore!", "Хочешь чтобы тебя не ебали как сейчас я тебя? - Покупай SkyCore", "полетел на спавн хуесос", "Пойми что SkyCore убил тебя в 0", "У тебя есть сиська у меня пиписька и если мы будем рядом то нам будет заебисько!"};

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

    private LivingEntity lastTrackedTarget = null;
    private float lastTargetHealth = -1.0f;
    private boolean killMessageSent = false;
    private int lastMessageIndex = -1;
    private long lastMessageTime = 0;
    private String pendingKillMessage = null;
    private long pendingKillMessageTime = 0;
    private long lastTotemMessageTime = 0;

    public KillMessage() {
        super("KillMessage", Category.Miscellaneous);
        addSettings(soft);
    }

    @Native
    private String replaceRandomLetter(String name) {
        Map<Character, Character> letterMap = new HashMap<>();
        letterMap.put('p', 'р');
        letterMap.put('a', 'а');
        letterMap.put('e', 'е');
        letterMap.put('o', 'о');
        letterMap.put('k', 'к');
        letterMap.put('c', 'с');
        letterMap.put('x', 'х');
        letterMap.put('y', 'у');
        letterMap.put('P', 'Р');
        letterMap.put('A', 'А');
        letterMap.put('E', 'Е');
        letterMap.put('O', 'О');
        letterMap.put('K', 'К');
        letterMap.put('T', 'Т');
        letterMap.put('H', 'Н');
        letterMap.put('B', 'В');
        letterMap.put('M', 'М');
        letterMap.put('C', 'С');
        letterMap.put('X', 'Х');
        letterMap.put('Y', 'У');
        letterMap.put('r', 'г');
        letterMap.put('n', 'п');
        letterMap.put('N', 'И');
        letterMap.put('m', 'м');
        letterMap.put('b', 'ь');
        letterMap.put('w', 'ш');
        letterMap.put('W', 'Ш');
        letterMap.put('0', 'O');

        List<Integer> replaceablePositions = new ArrayList<>();
        for (int i = 0; i < name.length(); i++) {
            if (letterMap.containsKey(name.charAt(i))) {
                replaceablePositions.add(i);
            }
        }

        if (replaceablePositions.isEmpty()) {
            return name;
        }

        int randomPosition = replaceablePositions.get(random.nextInt(replaceablePositions.size()));
        char russianChar = letterMap.get(name.charAt(randomPosition));

        StringBuilder modified = new StringBuilder(name);
        modified.setCharAt(randomPosition, russianChar);
        return modified.toString();
    }

    private String pickSpecialOrDefault(String[] specialMessages) {
        boolean useSpecial = random.nextBoolean();
        String[] messagesToUse = useSpecial ? specialMessages : killMessages;

        List<Integer> availableIndices = new ArrayList<>();
        for (int i = 0; i < messagesToUse.length; i++) {
            if (!soft.get() && messagesToUse[i].contains("SkyCore") && !messagesToUse[i].contains("by SkyCore нахуй")) {
                continue;
            }
            availableIndices.add(i);
        }

        if (availableIndices.isEmpty()) return null;

        int messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
        String msg = messagesToUse[messageIndex];
        if (!soft.get() && msg.endsWith(" by SkyCore нахуй")) {
            msg = msg.substring(0, msg.length() - " by SkyCore нахуй".length());
        }
        return msg;
    }

    @Native
    private void sendKillMessage(LivingEntity deadTarget) {
        if (mc.player == null) return;
        if (!(deadTarget instanceof PlayerEntity)) return;

        long currentTime = System.currentTimeMillis();
        String targetName = deadTarget.getName().getString();
        String displayName = deadTarget.getDisplayName().getString().toLowerCase();
        String modifiedName = replaceRandomLetter(targetName);
        String randomMessage;

        {
            List<Integer> availableIndices = new ArrayList<>();
            for (int i = 0; i < killMessages.length; i++) {
                if (!soft.get() && killMessages[i].contains("SkyCore") && !killMessages[i].contains("by SkyCore нахуй")) {
                    continue;
                }
                availableIndices.add(i);
            }
            if (availableIndices.isEmpty()) return;

            int messageIndex;
            do {
                messageIndex = availableIndices.get(random.nextInt(availableIndices.size()));
            } while (messageIndex == lastMessageIndex && availableIndices.size() > 1);

            lastMessageIndex = messageIndex;
            randomMessage = killMessages[messageIndex];
            if (!soft.get() && randomMessage.endsWith(" by SkyCore нахуй")) {
                randomMessage = randomMessage.substring(0, randomMessage.length() - " by SkyCore нахуй".length());
            }
        }

        if (randomMessage == null) return;

        String message = "!-" + modifiedName + randomMessage;

        if (currentTime - lastMessageTime >= 3000) {
            mc.player.sendChatMessage(message);
            lastMessageTime = currentTime;
        } else {
            pendingKillMessage = message;
            pendingKillMessageTime = currentTime;
        }
    }

    @Native
    private void sendTotemMessage(LivingEntity totemTarget) {
        if (mc.player == null) return;
        if (!(totemTarget instanceof PlayerEntity)) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTotemMessageTime < 15000) return;
        if (currentTime - lastMessageTime < 3000) return;

        String targetName = totemTarget.getName().getString();
        String modifiedName = replaceRandomLetter(targetName);
        lastTotemMessageTime = currentTime;
        lastMessageTime = currentTime;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        long currentTime = System.currentTimeMillis();

        if (pendingKillMessage != null && currentTime - pendingKillMessageTime >= 3000) {
            mc.player.sendChatMessage(pendingKillMessage);
            lastMessageTime = currentTime;
            pendingKillMessage = null;
            pendingKillMessageTime = 0;
        }

        LivingEntity currentTarget = AttackAura.getTarget();

        if (currentTarget != null && currentTarget instanceof PlayerEntity) {
            float currentHealth = currentTarget.getHealth();

            if (lastTrackedTarget != null && lastTrackedTarget == currentTarget) {
                if (lastTargetHealth > 0.0f && lastTargetHealth <= 3.0f && currentHealth > lastTargetHealth + 10.0f) {
                    sendTotemMessage(currentTarget);
                }

                if (!killMessageSent && lastTargetHealth > 0.0f && (currentHealth <= 0.0f || currentTarget.deathTime > 0 || !currentTarget.isAlive())) {
                    sendKillMessage(currentTarget);
                    killMessageSent = true;
                }
            }

            lastTrackedTarget = currentTarget;
            lastTargetHealth = currentHealth;

            if (currentTarget.isAlive() && currentHealth > 0.0f) {
                killMessageSent = false;
            }

        } else if (lastTrackedTarget != null && lastTrackedTarget instanceof PlayerEntity) {
            if (!killMessageSent && (lastTrackedTarget.deathTime > 0 || !lastTrackedTarget.isAlive() || lastTrackedTarget.getHealth() <= 0.0f)) {
                sendKillMessage(lastTrackedTarget);
                killMessageSent = true;
            }

            if (currentTarget == null) {
                lastTrackedTarget = null;
                lastTargetHealth = -1.0f;
                killMessageSent = false;
            }
        }
    }

    @java.lang.Override
    public void onDisable() {
        super.onDisable();
        lastTrackedTarget = null;
        lastTargetHealth = -1.0f;
        killMessageSent = false;
        pendingKillMessage = null;
        lastMessageIndex = -1;
    }
}