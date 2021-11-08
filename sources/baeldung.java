//Source: https://www.baeldung.com/java-csv, by multiple authors
// (https://www.baeldung.com/author/baeldung/: "This is the standard author on the site. Most articles are published
// by individual authors, with their own profiles, but when multiple people have a strong contribution,
// we publish collectively here.")
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class baeldung {
    // https://www.baeldung.com/java-csv
    // Helper function to write output
    public static String convertToCSV(String[] data) {
        return String.join(",", data);
    }

    // https://www.baeldung.com/java-csv
    // prints output to csv
    public static void givenDataArray_whenConvertToCSV_thenOutputCreated(String CSV_FILE_NAME, ArrayList<String[]> dataLines) throws IOException {
        File csvOutputFile = new File(CSV_FILE_NAME);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            dataLines.stream()
                    .map(mainClass::convertToCSV)
                    .forEach(pw::println);
        }
    }
}
