package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.SkyCore;
import sky.core.events.EventUpdate;
import sky.core.utils.managers.impl.staffmanager.StaffManager;
import sky.core.utils.managers.impl.staffmanager.Staff;
import sky.core.modules.impl.player.AutoLeave;
import sky.core.modules.impl.visuals.Interface;
import sky.core.utils.Wrapper;
import lombok.Getter;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.text.TextFormatting;

import java.util.regex.Pattern;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class StaffHandler implements Wrapper {
    @Getter
    private static StaffHandler instance;
    private static final Locale LOCALE = Locale.ROOT;
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
    private final List<Staff> staff = new ArrayList<>();

    public StaffHandler() {
        instance = this;
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if ((mc.player.ticksExisted & 9) != 0) return;
        if ((SkyCore.getInstance().getModuleManager().getModule(Interface.class).isEnabled() && Interface.elements.is("Администрация онлайн")) || SkyCore.getInstance().getModuleManager().getModule(AutoLeave.class).isEnabled())
            update();
    }

    public void update() {
        List<Staff> online = getOnlineStaff();
        List<Staff> vanished = getVanishedStaff();

        online.sort(Comparator.comparing(Staff::getName, String.CASE_INSENSITIVE_ORDER));
        vanished.sort(Comparator.comparing(Staff::getName, String.CASE_INSENSITIVE_ORDER));
        synchronized (staff) {
            staff.clear();
            staff.addAll(online);
            staff.addAll(vanished);
        }
    }

    private List<Staff> getOnlineStaff() {
        if (mc.world == null || mc.getConnection() == null) return Collections.emptyList();
        StaffManager staffManager = SkyCore.getInstance().getStaffManager();
        Set<String> staffNamesLower = staffManager != null ? staffManager.getStaff().stream().map(StaffManager.StaffEntry::getName).filter(Objects::nonNull).map(name -> name.toLowerCase(LOCALE)).collect(Collectors.toSet()) : Collections.emptySet();

        List<Staff> list = new ArrayList<>();
        for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
            if (info == null) continue;
            String username = info.getGameProfile().getName();
            if (username == null) continue;

            ITextComponent rawPrefix = StringTextComponent.EMPTY;
            for (ScorePlayerTeam team : mc.world.getScoreboard().getTeams()) {
                if (team.getMembershipCollection().contains(username)) {
                    rawPrefix = team.getPrefix();
                    break;
                }
            }

            if (staffNamesLower.contains(username.toLowerCase(LOCALE)) || isStaffPrefix(rawPrefix.getString())) {
                list.add(new Staff(username, rawPrefix, false));
            }
        }
        return list;
    }

    public static List<Staff> getVanishedStaff() {
        if (mc.world == null || mc.getConnection() == null) return Collections.emptyList();

        Set<String> onlineNames = mc.getConnection().getPlayerInfoMap().stream().filter(Objects::nonNull).map(info -> info.getGameProfile().getName()).filter(Objects::nonNull).collect(Collectors.toSet());

        Scoreboard scoreboard = mc.world.getScoreboard();
        List<Staff> list = new ArrayList<>();

        for (ScorePlayerTeam team : scoreboard.getTeams().stream().sorted(Comparator.comparing(Team::getName)).toList()) {
            String name = team.getMembershipCollection().stream().findFirst().orElse(null);
            if (name == null || !NAME_PATTERN.matcher(name).matches() || onlineNames.contains(name)) continue;
            if (team.getDisplayName().getString().trim().startsWith("npc") || team.getDisplayName().getString().trim().startsWith("collideRule_") || team.getDisplayName().getString().trim().startsWith("FS_") || team.getDisplayName().getString().trim().startsWith("STAFF-") || team.getDisplayName().getString().trim().startsWith("HELPER-") || team.getDisplayName().getString().trim().startsWith("ADMIN-") || team.getDisplayName().getString().trim().startsWith("MODER-"))
                continue;
            ITextComponent rawPrefix = team.getPrefix();

            list.add(new Staff(name, rawPrefix, true));
        }
        return list;
    }

    private static boolean isStaffPrefix(String rawPrefix) {
        String p = TextFormatting.getTextWithoutFormattingCodes(rawPrefix);
        if (p == null || p.isEmpty()) return false;
        p = p.toLowerCase(LOCALE);
        return p.contains("helper") || p.contains("moder") || p.contains("admin") || p.contains("curator") || p.contains("builder") || p.contains("yt") || p.contains("кур") || p.contains("тех.админ") || p.contains("s.kurator") || p.contains("поддержка") || p.contains("сотруд") || p.contains("ꔓ") || p.contains("ꔉ") || p.contains("ꔅ") || p.contains("ꔁ") || p.contains("ꔷ") || p.contains("ꔳ") || p.contains("ꔩ") || p.contains("ꔥ") || p.contains("ꔡ") || p.contains("ꔗ");
    }
}