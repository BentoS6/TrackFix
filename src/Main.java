import java.util.Scanner;
import java.io.File;
import java.util.logging.*;

class Main
{
    public static void main(String[] args)
    {
        // to disable JAudiotagger logs
        LogManager.getLogManager().reset();
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);

        // gets the file location of the folder containing the albums
        UserInputHandler inputHandler = new UserInputHandler();
        String path = inputHandler.getAlbumDirectory();
        System.out.println();

        // embedding process
        DirectoryProcessor dir = new DirectoryProcessor(path);
        dir.printAllAlbums();
        System.out.println();
        dir.checkIndvisualAlbums();
        dir.embedIfCoverImageExists();
        dir.artworkEmbedder();
    }
}