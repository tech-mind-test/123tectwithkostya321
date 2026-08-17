package sky.core.utils.misc;

import sky.core.SkyCore;
import sky.core.modules.impl.miscellaneous.ToggleSounds;
import sky.core.utils.Wrapper;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SoundUtil implements Wrapper {

    public static void playSound(final String location) {
        if (!SkyCore.getInstance().getModuleManager().getModule(ToggleSounds.class).isEnabled()) return;

        new Thread(() -> {
            try {
                InputStream resource = SoundUtil.class.getResourceAsStream("/assets/minecraft/SkyCore/sounds/" + location + ".wav");
                if (resource == null) return;

                try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(resource))) {
                    AudioFormat baseFormat = in.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.getSampleRate(),
                            16,
                            baseFormat.getChannels(),
                            baseFormat.getChannels() * 2,
                            baseFormat.getSampleRate(),
                            false
                    );

                    try (AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in)) {
                        Clip clip = AudioSystem.getClip();
                        clip.open(din);

                        setVolume(clip, ToggleSounds.volume.get() / 100.0);

                        clip.start();

                    }
                }
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void setVolume(Clip clip, double volume) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

                float min = volumeControl.getMinimum();
                float max = volumeControl.getMaximum();

                // Ограничиваем громкость
                volume = Math.max(0.0, Math.min(1.0, volume));

                if (volume == 0) {
                    volumeControl.setValue(min);
                } else {
                    // Формула для логарифмического изменения громкости
                    float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
                    volumeControl.setValue(Math.max(min, Math.min(max, dB)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}