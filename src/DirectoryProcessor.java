import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import org.json.*;

public class DirectoryProcessor 
{
    private String path; // path to the directory containing albums (String)
    private File albumDirectory; // file containing albums (File)
    private File[] indivisualAlbums; // array of all album files (File[])
    private ArrayList<File> albumsMissingArtwork= new ArrayList<>(); // arraylist containing all the album files that dont have artwork attached to it (ArrayList<File>)
    private static String apiKey; // api key thats being read from config.properties

    // constructor
    public DirectoryProcessor(String path)
    {
        this.albumDirectory = new File(path);   
        this.path = path;
        this.indivisualAlbums = albumDirectory.listFiles();
        try{
            this.apiKey = new java.util.Properties() {{ load(new java.io.FileInputStream("config.properties")); }}.getProperty("api.key");
        }
        catch(Exception e)
        {
            System.out.println("Could not read api key...");
        }
        
    }

    // prints all the albums
    public void printAllAlbums()
    {
        // System.out.println(apiKey);
        System.out.println("All albums found in your directory: ");
        sleep(2000);
        for(File file : indivisualAlbums)
        {
            System.out.println(file.getName());
        }
        sleep(2000);
    }

    // checks if the first track of the album has a image attached to it or not, if it does not, it adds the title of that album to the arraylist albumsMissingArtwork
    public void checkIndvisualAlbums()
    {
        for(File file : indivisualAlbums)
        {
            File current_file = file;
            File[] album_tracks = current_file.listFiles();
            System.out.println();
            System.out.println("Checking album for embedded images: "+file.getName());
            System.out.println();
            if (album_tracks == null) continue;

            int i = 0;
            for(File track : album_tracks)
            {
                if (track.getName().toLowerCase().endsWith(".mp3") ||
                track.getName().toLowerCase().endsWith(".flac") ||
                track.getName().toLowerCase().endsWith(".wav") ||
                track.getName().toLowerCase().endsWith(".aac") ||
                track.getName().toLowerCase().endsWith(".ogg") ||
                track.getName().toLowerCase().endsWith(".opus") ||
                track.getName().toLowerCase().endsWith(".m4a"))
                {
                    if(hasEmbeddedArtwork(track))
                    {
                        System.out.println("✅ " + track.getName());
                    }
                    else
                    {
                        if(i == 0)
                        {
                            albumsMissingArtwork.add(file);
                            i++;
                        }
                        System.out.println("❌ " + track.getName());
                    }
                }
            }
        }
        System.out.println();
        System.out.println("Albums missing the artwork are: " + albumsMissingArtwork);
    }

    // This method checks all albums in albumsMissingArtwork
    // If a cover image is present (cover.jpg/png), it embeds that into all tracks and removes the album from the list
    public void embedIfCoverImageExists() 
    {
        List<File> toRemove = new ArrayList<>();

        for (File album : albumsMissingArtwork) {
            File[] files = album.listFiles();
            if (files == null) continue;

            File coverImage = null;
            for (File f : files) {
                String name = f.getName().toLowerCase();
                if (name.equals("cover.jpg") || name.equals("folder.jpg") ||
                    name.equals("cover.png") || name.equals("folder.png")) {
                    coverImage = f;
                    break;
                }
            }

            if (coverImage == null) continue;

            System.out.println("📁 Found existing artwork in: " + album.getName());

            for (File track : files) {
                if (track.getName().toLowerCase().matches(".*\\.(mp3|flac|wav|m4a|aac|ogg|opus)")) {
                    try {
                        AudioFile audioFile = AudioFileIO.read(track);
                        Tag tag = audioFile.getTagOrCreateAndSetDefault();
                        Artwork artwork = ArtworkFactory.createArtworkFromFile(coverImage);
                        tag.deleteArtworkField();
                        tag.setField(artwork);
                        audioFile.commit();

                        System.out.println("✅ Embedded artwork in " + track.getName());
                    } catch (Exception e) {
                        System.out.println("❌ Failed to embed artwork into " + track.getName());
                        e.printStackTrace();
                    }
                }
            }

            toRemove.add(album);
        }

        albumsMissingArtwork.removeAll(toRemove);
    }




    // embeds all the artwork to the tracks of the albums in the arraylist albumsMissingArtwork
    public void artworkEmbedder() {
        for (File album : albumsMissingArtwork) {
            System.out.println();
            System.out.println("Embedding artwork for: " + album.getName());

            File[] tracks = album.listFiles();
            if (tracks == null || tracks.length == 0) continue;

            String imageUrl = null;
            String artistName = null;
            String albumName = null;
            File coverFile = null;

            try {
                // Use the first valid track to get metadata
                for (File track : tracks) {
                    if (track.getName().toLowerCase().matches(".*\\.(mp3|flac|m4a|wav|ogg|opus|aac)$")) {
                        artistName = getArtistName(track);
                        albumName = getAlbumName(track);

                        if (artistName == null || albumName == null || artistName.isEmpty() || albumName.isEmpty()) {
                            String[] result = getArtistNameAndAlbumNameIfNull(track);
                            artistName = result[0];
                            albumName = result[1];
                        }

                        imageUrl = getAlbumArtUrlFromLastFM(artistName, albumName);
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            System.out.println("❌Could not fetch artwork URL for: " + album.getName());
                            break;
                        }

                        // Download image to file
                        coverFile = new File("covers/" + albumName.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg");
                        coverFile.getParentFile().mkdirs();
                        downloadImage(imageUrl, coverFile);
                        break; // only need to do this once per album
                    }
                }

                if (coverFile == null || !coverFile.exists()) continue;

                // Embed artwork into all valid tracks
                for (File track : tracks) {
                    if (track.getName().toLowerCase().matches(".*\\.(mp3|flac|m4a|wav|ogg|opus|aac)$")) {
                        try {
                            AudioFile audioFile = AudioFileIO.read(track);
                            Tag tag = audioFile.getTagOrCreateAndSetDefault();

                            Artwork artwork = ArtworkFactory.createArtworkFromFile(coverFile);
                            tag.deleteArtworkField(); // remove old artwork if any
                            tag.setField(artwork);    // set new artwork
                            audioFile.commit();       // save changes

                            System.out.println("✅Embedded artwork into " + track.getName());
                        } catch (Exception e) {
                            System.out.println("❌Failed to embed artwork into " + track.getName());
                            e.printStackTrace();
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("❌Error processing album: " + album.getName());
                e.printStackTrace();
            }
        }
    }



    public static void downloadImage(String imageUrl, File outputFile) throws IOException {
        try (InputStream in = new URL(imageUrl).openStream()) 
        {
            Files.copy(in, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }


    public static String getAlbumArtUrlFromLastFM(String artist, String album) {
    try 
    {
        String encodedArtist = URLEncoder.encode(artist, "UTF-8");
        String encodedAlbum = URLEncoder.encode(album, "UTF-8");
        String urlStr = "https://ws.audioscrobbler.com/2.0/?method=album.getinfo"
                + "&api_key=" + apiKey
                + "&artist=" + encodedArtist
                + "&album=" + encodedAlbum
                + "&format=json";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) jsonBuilder.append(line);
        reader.close();

        JSONObject json = new JSONObject(jsonBuilder.toString());
        JSONArray images = json.getJSONObject("album").getJSONArray("image");
        return images.getJSONObject(images.length() - 1).getString("#text"); // get largest
    } 
    catch (Exception e) {
        System.out.println("❌ Failed to fetch artwork from Last.fm");
        return null;
    }
}


    // uses the lastfm api to search the original albums file name to return the artists name and the album name
    public static String[] getArtistNameAndAlbumNameIfNull(File file)
    {
        String parentFileName  = file.getParentFile().getName();
        try{
            String encodedAlbum = URLEncoder.encode(parentFileName, "UTF-8");
            String urlStr = "https://ws.audioscrobbler.com/2.0/"
            + "?method=album.search"                 // Method: album search
            + "&album=" + encodedAlbum               // Query: encoded album name
            + "&api_key=" + apiKey                  // Your API key
            + "&format=json";                        // Response format: JSON

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET"); // HTTP GET request

            BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream()) // Convert InputStream -> Reader
            );

            // Step 5: Read all lines of the response into a single string
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close(); // Always close your resources

            JSONObject json = new JSONObject(response.toString());

            JSONArray albums = json.getJSONObject("results")            // get "results" object
                           .getJSONObject("albummatches")       // get "albummatches" object
                           .getJSONArray("album");              // get the array of matching albums

            if (albums.length() == 0) {
                System.out.println("No matches found.");
                return new String[] { "Unknown artist", "Unknown album" };
            }

            // Step 9: Get the first album match from the array
            JSONObject first = albums.getJSONObject(0);

            // Step 10: Extract artist name, album name
            String artist = first.getString("artist");
            String album = first.getString("name");

            return new String[] { artist, album };
        }
        catch (Exception e)
        {
            System.out.println("Encoding didnt work....");
            return new String[] { "Unknown artist", "Unknown album" };
        }
    }

    // return artists name from metadata
    public static String getArtistName(File file) throws Exception 
    {
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTag();
        return (tag != null) ? tag.getFirst("ARTIST") : "";
    }

    // return albums name from metadata
    public static String getAlbumName(File file) throws Exception 
    {
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTag();
        return (tag != null) ? tag.getFirst("ALBUM") : "";
    }   

    // helper method to find if the given track has embedded artwork or not
    public static boolean hasEmbeddedArtwork(File track)
    {
        try 
        {
            AudioFile f = AudioFileIO.read(track);
            Tag tag = f.getTag();
            Artwork artwork = null;

            if (tag != null) 
            {
                artwork = tag.getFirstArtwork();
            }

            return artwork != null;
        }
        
        catch (Exception e) {
            System.out.println("Error occurred while reading file: " + track.getName());
        }
        return false;
    }

    public static boolean getPermission(String request)
    {
        Scanner sc = new Scanner(System.in);
        System.out.println();
        System.out.println(request + "\nEnter y to move onto the next step or n to stop process...");
        String choice = sc.nextLine();
        if(choice == "y")
        {
            return true;
        }
        if(choice == "n")
        {
            System.exit(0);
            return false;
        }
        else
        {
            System.out.println("Incorrect input, stopping process...");
            System.exit(0);
            return false;
        }
    }

    public static void sleep(int time)
    {
        try{
            Thread.sleep(time);
        }
        catch (Exception e)
        {

        }
    }
}



