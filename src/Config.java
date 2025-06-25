import java.io.FileInputStream;
import java.util.Properties;

public class Config {
    public static String LASTFM_API_KEY;

    static {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("config.properties"));
            LASTFM_API_KEY = props.getProperty("LASTFM_API_KEY");
        } catch (Exception e) {
            System.out.println("Failed to load config.");
        }
    }
}
