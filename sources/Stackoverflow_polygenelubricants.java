//From https://stackoverflow.com/a/3403112 by stackoverflow user polygenelubricants
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Stackoverflow_polygenelubricants {
    public void FileToString() throws FileNotFoundException {
        String content = new Scanner(new File("filename")).useDelimiter("\\Z").next();
        System.out.println(content);
    }
}
