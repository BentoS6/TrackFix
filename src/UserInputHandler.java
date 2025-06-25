import java.util.Scanner;
import java.io.File;

// classes function: to make sure user gives the right input and using that path, we can return it back to the main class, so that the main class can pass it to the album processor class, that scans each album indivisually
public class UserInputHandler 
{
    public String getAlbumDirectory()
    {
        Scanner sc = new Scanner(System.in);
        while(true)
        {
            System.out.println("Enter the directory to the albums: ");
            // String directory = "/home/keys/me_meow/media/music/";
            String directory = sc.nextLine();
            File albumDirectory = new File(directory);

            if(albumDirectory != null &&albumDirectory.exists() && albumDirectory.isDirectory())
            {
                sc.close();
                System.out.println("Directory exists...");
                try{
                    Thread.sleep(2000);
                }
                catch (Exception e)
                {
                }
                return directory;
            }
            else
            {
                System.out.println();
                System.out.println("Given path is either not a valid location, is not a directory, or is null, try again...");
                try{
                    Thread.sleep(1000);
                }
                catch (Exception e)
                {
                }
            }
        }
    }
}
