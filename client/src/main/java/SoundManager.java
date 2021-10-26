import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;

public class SoundManager {

    private static final Media soundClick = new Media(new File("client/src/main/resources/sounds/ClickH3.mp3").toURI().toString());
    private static double volume = 0.3;

    public void playFirst() {
        MediaPlayer mediaPlayer = new MediaPlayer(soundClick);
        mediaPlayer.setVolume(volume);
        mediaPlayer.play();
    }

    public void setVolume(double volume) {
        SoundManager.volume = volume;
    }

    public double getVolume() {
        return volume;
    }
}
