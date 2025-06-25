import java.io.File;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

public class CoverRemover {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("❌ Usage: java -jar CoverRemover.jar <path-to-music-root>");
            return;
        }

        File rootDir = new File(args[0]);

        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.err.println("❌ Invalid directory: " + rootDir.getAbsolutePath());
            return;
        }

        File[] albumDirs = rootDir.listFiles(File::isDirectory);

        if (albumDirs == null || albumDirs.length == 0) {
            System.out.println("📭 No album folders found in: " + rootDir.getAbsolutePath());
            return;
        }

        for (File album : albumDirs) {
            File[] tracks = album.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
            if (tracks == null || tracks.length == 0) {
                System.out.println("ℹ️ No tracks found in: " + album.getName());
                continue;
            }

            for (File track : tracks) {
                try {
                    AudioFile audioFile = AudioFileIO.read(track);
                    Tag tag = audioFile.getTag();

                    if (tag != null) {
                        Artwork artwork = tag.getFirstArtwork();
                        if (artwork != null) {
                            tag.deleteArtworkField();
                            audioFile.commit();
                            System.out.println("🧹 Removed artwork from: " + track.getName());
                        } else {
                            System.out.println("ℹ️ No artwork in: " + track.getName());
                        }
                    } else {
                        System.out.println("⚠️ No tag found in: " + track.getName());
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error processing " + track.getName() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("✅ Finished removing album art from all tracks.");
    }
}
