package other.party;

import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;

public class PartyFormatter {

    public static final String IRC_GRADIENT_PREFIX = makeGradientIRC();

    private static String makeGradientIRC() {
        String text = "[IRC]";
        int[] startRGB = {85, 255, 85};
        int[] endRGB = {0, 170, 0};

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            float ratio = text.length() > 1 ? (float) i / (text.length() - 1) : 0;
            int r = (int) (startRGB[0] + (endRGB[0] - startRGB[0]) * ratio);
            int g = (int) (startRGB[1] + (endRGB[1] - startRGB[1]) * ratio);
            int b = (int) (startRGB[2] + (endRGB[2] - startRGB[2]) * ratio);
            sb.append(String.format("§x§%s§%s§%s§%s§%s§%s%c",
                    hexChar(r >> 4), hexChar(r & 0xF),
                    hexChar(g >> 4), hexChar(g & 0xF),
                    hexChar(b >> 4), hexChar(b & 0xF),
                    text.charAt(i)));
        }
        return sb.toString();
    }

    private static char hexChar(int v) {
        return "0123456789abcdef".charAt(v & 0xF);
    }

    public static IFormattableTextComponent createGradientIRCPrefix() {
        String messageText = "[IRC]";
        IFormattableTextComponent finalComponent = new StringTextComponent("");


        for (int i = 0; i < messageText.length(); i++) {

            char c = messageText.charAt(i);

            float progress = (float) i / (float) Math.max(1, messageText.length() - 1);

            int startColor = ColorUtil.getColorWithDarkness(ThemeEditor.getColor(ThemeSettings.MAIN), 2);
            int endColor = ThemeEditor.getColor(ThemeSettings.MAIN);
            int color = ColorUtil.interpolate(endColor, startColor, progress);


            IFormattableTextComponent charComponent = new StringTextComponent(String.valueOf(c));
            Style charStyle = Style.EMPTY
                    .setColor(Color.fromInt(color))
                    .setBold(true);

            charComponent.setStyle(charStyle);
            finalComponent.append(charComponent);
        }

        return finalComponent;
    }

    public static String formatError(String message) {
        return IRC_GRADIENT_PREFIX + " §c" + message;
    }

    public static String formatSuccess(String message) {
        return IRC_GRADIENT_PREFIX + " §a" + message;
    }

    public static String formatInfo(String message) {
        return IRC_GRADIENT_PREFIX + " §7" + message;
    }

    public static ITextComponent formatPartyCreated(String code) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Группа успешно создана, код группы: §f" + code
                + "§7, все кто знают этот код, могут присоединиться к группе"));
        return msg;
    }

    public static ITextComponent formatMemberJoined(String ircName, int count) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7" + ircName + " присоединился к группе (" + count + "/10)"));
        return msg;
    }

    public static ITextComponent formatMemberLeft(String ircName) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7" + ircName + " покинул группу"));
        return msg;
    }

    public static ITextComponent formatNewLeader(String newLeader) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Лидер группы сменился, теперь лидер вашей группы " + newLeader));
        return msg;
    }

    public static ITextComponent formatYouAreLeader() {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Теперь вы новый лидер группы"));
        return msg;
    }

    public static ITextComponent formatPlayerKicked(String ircName) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Участник " + ircName + " исключен из группы"));
        return msg;
    }

    public static ITextComponent formatYouKicked() {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Вас исключили из группы"));
        return msg;
    }

    public static ITextComponent formatDisbanded(String leaderName) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Лидер " + leaderName + " расформировал группу"));
        return msg;
    }

    public static ITextComponent formatYouLeft() {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Вы покинули группу, "));

        IFormattableTextComponent createBtn = new StringTextComponent("§7§nнажмите для создания новой");
        createBtn.setStyle(createBtn.getStyle()
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".party create"))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new StringTextComponent("§7Нажми чтобы создать новую группу"))));

        msg.append(createBtn);
        return msg;
    }

    public static ITextComponent formatYouDisbanded() {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Вы расформировали группу"));
        return msg;
    }

    public static ITextComponent formatPlayerInvited(String ircName) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7" + ircName + " приглашен в вашу группу"));
        return msg;
    }

    public static ITextComponent formatInviteReceived(String fromIrcName) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Вас пригласили в группу §f" + fromIrcName + " "));

        IFormattableTextComponent acceptBtn = new StringTextComponent("§aПринять");
        acceptBtn.setStyle(acceptBtn.getStyle()
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".party join"))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new StringTextComponent("§aПринять приглашение"))));

        IFormattableTextComponent declineBtn = new StringTextComponent("§cОтклонить");
        declineBtn.setStyle(declineBtn.getStyle()
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".party dismiss"))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new StringTextComponent("§cОтклонить приглашение"))));

        msg.append(acceptBtn);
        msg.append(new StringTextComponent(" "));
        msg.append(declineBtn);
        return msg;
    }

    public static ITextComponent formatInviteDismissed(String ircName) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7" + ircName + " отклонил приглашение"));
        return msg;
    }

    public static ITextComponent formatYouDismissed() {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Вы отклонили приглашение"));
        return msg;
    }

    public static ITextComponent formatPartyInfoHeader(String code) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Код: §f" + code));
        return msg;
    }

    public static ITextComponent formatPartyInfoLeader(String ircName, String gameName) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Лидер: §f" + ircName + " §7игровое имя: §f" + gameName));
        return msg;
    }

    public static ITextComponent formatPartyInfoMember(String ircName, String gameName) {
        IFormattableTextComponent msg = createGradientIRCPrefix();
        msg.append(new StringTextComponent(" §7Участник: §f" + ircName + " §7игровое имя: §f" + gameName));
        return msg;
    }

    public static ITextComponent formatPartyError(String errorCode) {
        IFormattableTextComponent msg = createGradientIRCPrefix();

        switch (errorCode) {
            case "already_in_party":
                msg.append(new StringTextComponent(" §7Вы уже состоите в группе, для начала покиньте текущую группу"));
                break;
            case "not_in_party": {
                msg.append(new StringTextComponent(" §7Вы не состоите в группе, "));
                IFormattableTextComponent createLink = new StringTextComponent("§7§nсоздайте свою группу");
                createLink.setStyle(createLink.getStyle()
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".party create"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new StringTextComponent("§7Нажми чтобы создать группу"))));
                msg.append(createLink);
                break;
            }
            case "party_not_found":
                msg.append(new StringTextComponent(" §cГруппа не найдена"));
                break;
            case "party_full":
                msg.append(new StringTextComponent(" §cГруппа заполнена"));
                break;
            case "not_leader":
                msg.append(new StringTextComponent(" §cВы не лидер группы"));
                break;
            case "already_member":
                msg.append(new StringTextComponent(" §cИгрок уже в группе"));
                break;
            case "already_invited":
                msg.append(new StringTextComponent(" §7Игрок уже приглашен в группу"));
                break;
            case "not_member":
                msg.append(new StringTextComponent(" §7Игрок не состоит в вашей группе"));
                break;
            case "cant_kick_self":
                msg.append(new StringTextComponent(" §cНельзя кикнуть себя"));
                break;
            case "no_invite":
                msg.append(new StringTextComponent(" §7У вас нет активных приглашений"));
                break;
            case "invite_expired":
                msg.append(new StringTextComponent(" §cПриглашение истекло"));
                break;
            default:
                msg.append(new StringTextComponent(" §c" + errorCode));
                break;
        }
        return msg;
    }
}