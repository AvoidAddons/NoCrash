package pl.olafcio.nocrash.crashpad;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class NoCrashAmbient {
    private static final String[] songs
                    = new String[]{ "Recovery.wav", "Exfiltration.wav" };

    public void play() {
        try {
            InputStream byteStream;

            try (var stream = this.getClass().getResourceAsStream("/music/" + songs[(int) Math.round(Math.random() * (songs.length - 1))])) {  // I made dis! somewhat inspired by BOTW
                byteStream = new ByteArrayInputStream(stream.readAllBytes());
            }

            var audioInputStream = AudioSystem.getAudioInputStream(byteStream);

            // create clip reference
            var clip = AudioSystem.getClip();

            // open audioInputStream to the clip
            clip.open(audioInputStream);

            // volume
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

                // dB!! -80.0f <> 0.0f
                volumeControl.setValue(-20F);
            } else {
                // well fuck it

                clip.close();
                return;
            }

            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            IO.println("\n[NoCrashAmbient/WARN] Unable to play audio");
            e.printStackTrace();
        }
    }
}
