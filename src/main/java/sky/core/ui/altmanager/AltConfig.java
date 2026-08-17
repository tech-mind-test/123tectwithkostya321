package sky.core.ui.altmanager;

import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class AltConfig {
    private static final File DIR = new File(Minecraft.getInstance().gameDir, "skycore");
    private static final File FILE = new File(DIR, "alts.txt");

    public static void init(List<Account> accounts) {
        try {
            if (!DIR.exists()) {
                DIR.mkdirs();
            }
            if (!FILE.exists()) {
                FILE.createNewFile();
                return;
            }
            read(accounts);
        } catch (Exception ignored) {
        }
    }

    public static void updateFile(List<Account> accounts) {
        try {
            if (!DIR.exists()) {
                DIR.mkdirs();
            }
            StringBuilder builder = new StringBuilder();
            for (Account alt : accounts) {
                builder.append(alt.accountName).append(":").append(alt.dateAdded).append("\n");
            }
            Files.write(FILE.toPath(), builder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static void read(List<Account> accounts) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || !line.contains(":")) {
                    continue;
                }
                String[] parts = line.split(":");
                if (parts.length < 2) {
                    continue;
                }
                String username = parts[0].trim();
                if (username.isEmpty()) {
                    continue;
                }
                long date = System.currentTimeMillis();
                try {
                    date = Long.parseLong(parts[1].trim());
                } catch (Exception ignored) {
                }
                accounts.add(new Account(username, date));
            }
            scheduleSkinLoads(accounts);
        } catch (Exception ignored) {
        }
    }

    private static void scheduleSkinLoads(List<Account> accounts) {
        if (accounts.isEmpty()) return;
        new Thread(() -> {
            for (Account account : accounts) {
                try {
                    Thread.sleep(450L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
                account.loadSkinAsync();
            }
        }, "AltManager-SkinLoader").start();
    }
}
