package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.util.ResourceLocation;
import sky.core.events.EventAttack;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Random;

public class KillSound extends Module {

    private final SliderSetting volume = new SliderSetting("Volume", 100.0f, 1.0f, 300.0f, 1.0f);
    private LivingEntity target;
    private long time;
    private Clip currentClip;
    private final Random random = new Random();

    public KillSound() {
        super("KillSound", "Plays sounds on kill", Category.Visuals);
        addSettings(volume);
    }

    @EventTarget
    public void onAttack(EventAttack e) {
        if (e.getTarget() instanceof LivingEntity) {
            target = (LivingEntity) e.getTarget();
            time = System.currentTimeMillis();
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.world == null) return;

        if (e.getPacket() instanceof SEntityStatusPacket) {
            SEntityStatusPacket packet = (SEntityStatusPacket) e.getPacket();
            if (packet.getOpCode() == 3 && target != null && packet.getEntity(mc.world) == target) {
                if (System.currentTimeMillis() - time <= 1000) {
                    onKill();
                    target = null;
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (target != null) {
            if (target.getHealth() <= 0.0f || !target.isAlive()) {
                if (System.currentTimeMillis() - time <= 1000) {
                    onKill();
                }
                target = null;
            }
        }
    }

    private void onKill() {
        stopCurrentSound();
        String[] soundFiles = {"girl_1.wav", "girl_2.wav", "girl_3.wav", "girl_4.wav", "girl_5.wav"};
        int index = random.nextInt(soundFiles.length);
        playSound("SkyCore/sounds/" + soundFiles[index]);
    }

    private void playSound(String path) {
        new Thread(() -> {
            try {
                Clip clip = AudioSystem.getClip();
                InputStream is = mc.getResourceManager().getResource(new ResourceLocation(path)).getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis);

                if (audioInputStream == null) return;

                clip.open(audioInputStream);
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float vol = volume.get();
                float gain = (float) (20.0f * Math.log10(vol / 100.0f));
                gainControl.setValue(Math.max(-80.0f, Math.min(gain, 6.0f)));

                clip.start();
                currentClip = clip;
            } catch (Exception ignored) {}
        }).start();
    }

    private void stopCurrentSound() {
        if (currentClip != null) {
            if (currentClip.isRunning()) {
                currentClip.stop();
            }
            currentClip.close();
            currentClip = null;
        }
    }
}