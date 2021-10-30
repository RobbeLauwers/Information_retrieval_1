import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

// Only use import below if junit is actually installed, might not be useful
// import static org.junit.Assert.assertEquals;

// https://lucene.apache.org/core/7_3_1/core/index.html
// TODO: split up indexing/reading queries/querying into separate classes/files?
public class mainClass {

    // https://zetcode.com/java/listdirectory/
    // Prints all files in directory
    // This is just to manually check if the directory is correct, should not be used in actual code
    public static void testDirectory(Boolean largeDataset) throws IOException {
        String dirName = "./Datasets/Small";
        Files.list(new File(dirName).toPath())
                .forEach(System.out::println);
    }

    // https://zetcode.com/java/listdirectory/
    // Indexes all documents
    public static void readFiles(IndexWriter iwriter, Boolean smallDataset) throws IOException {
        //Select dataset
        String dirName = "";
        if (smallDataset) {
            dirName = "./Datasets/Small";
        } else {
            dirName = "./Datasets/Large";
        }
        //For each file in dataset
        Files.list(new File(dirName).toPath())
                //Get the path to the file
                .forEach(path -> {
                            //Create a new document to index
                            Document doc = new Document();
                            // https://stackoverflow.com/a/4030936
                            // Get file number from file path, store it in the document
                            doc.add(new Field("file_number", path.toString().replaceAll("\\D+", ""), TextField.TYPE_STORED));
                            // Initialize file contents
                            String content = "";
                            // Exception handling seems to be mandatory?
                            try {
                                // Get file from path
                                File tempFile = path.toFile();
                                // https://stackoverflow.com/a/15253450
                                // Check if file is actually a file containing data, the datasets provided also include non-data files (like write.lock)
                                if (tempFile.length() > 0 && path.toString().substring(path.toString().length() - 3).equals("txt")) {
                                    // https://stackoverflow.com/a/3403112
                                    // Read contents from file
                                    content = new Scanner(tempFile).useDelimiter("\\Z").next();
                                }
                            } catch (FileNotFoundException e) {
                                // Default exception handling
                                e.printStackTrace();
                            }
                            // Actually store file contents to the lucene document
                            doc.add(new Field("file_content", content, TextField.TYPE_STORED));
                            // Exception handling seems to be mandatory?
                            try {
                                // Index the document
                                iwriter.addDocument(doc);
                            } catch (IOException e) {
                                // Default exception handling
                                e.printStackTrace();
                            }
                        }
                );
    }

    // Instances of a class outside of methods need a bunch of keywords (static, final)?
    // Not sure if this method will need any more code than just calling readFiles, leaving it just in case
    // Indexing results are written to the disk/RAM directory that the IndexWriter was told to use, so return
    // value needed.
    public static void indexing(IndexWriter iwriter) throws IOException {
        // Read files and index them
        readFiles(iwriter, true);
    }

    public static void queryProcessing(){

    }

    public static void main(String[] args) throws IOException, ParseException {
        // Store the index in memory:
        Directory directory = new RAMDirectory();
        // To store an index on disk, use this instead:
        //Directory directory = FSDirectory.open("/tmp/testindex");

        // TODO: move the analyzer/index definitions below to the indexing() method?

        // https://lucene.apache.org/core/7_3_1/core/index.html
        // "Filters StandardTokenizer with StandardFilter, LowerCaseFilter and StopFilter, using a list of English stop words."
        Analyzer analyzer = new StandardAnalyzer();

        // We want the index to be constructed using the analyzer defined above
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        // We tell the index writer where to write to (directory) and give it additional settings like ignoring stop wrods (config)
        IndexWriter iwriter = new IndexWriter(directory, config);
        // Actually run the indexer
        indexing(iwriter);
        // Indexing finished -> close index writer
        iwriter.close();

        // Now search the index:
        // Open the directory where we stored the index
        DirectoryReader ireader = DirectoryReader.open(directory);
        // We want to search in the index we just read
        IndexSearcher isearcher = new IndexSearcher(ireader);

        // full contents of file 1537
        String fileContent1537 = "\"Forced air vs central air: a comparison guideTags: HVAC, install central A/CCentral air, often confused as an alternative to forced-air systems, is actually an additional component that can be added to them.Do you like this article? TweetQuality HVAC ServicesWhere do you need HVAC services?GoWhere do you need HVAC services?GoFeatured inForced air is an HVAC system designed to push air through the ductwork of a home or an office building. Central air applies only to air conditioning and is often, but not always, included within forced-air systems. Read on for the basics of forced-air vs. central air systems.Get free quotes on forced air versus central air today!Forced airForced-air systems can be found in most residential and corporate buildings. These systems use ducting to efficiently deliver heating and cooling to where it is needed. A forced AC unit is one of the most popular types as nearly all contractors are familiar with this system.When it comes to cost, forced-air vs. central air conditioning depends largely on the size of the house, the quality of the ducting and the AC unit itself. Be sure to hire a contractor who is well known in your area to provide you with a forced-air system estimate.Before making a purchase, consider the following aspects of forced-air AC units:Price range from $711 to $1,192.Easy to install.Comes with energy-efficient options.Comes with heating and cooling elements.The question of forced air vs. central air is really about whether a basic system is sufficient or whether the convenience of central air justifies the additional costs.Central airCentral air, often confused as an alternative to forced-air systems, is actually an additional component that can be added to the existing forced-air system. The central air conditioning unit is for cooling only. It uses a condenser and a complex series of coils to send blasts of cold air throughout the entire building.The condenser portion of a central air unit is located outside the home or office building. It converts air from the environment into cold air.You should hire a contractor to install an AC unit, such as central air.Central air units range in price from $3,500 to $4,000 for an average-sized home of around 2,000 square feet. So, it is important to remember that this cost is in addition to the initial cost of a forced-air system.One of the primary benefits of a central air system is that, unlike other HVAC systems, it delivers cool air to the entire house almost instantly. For a lot of homeowners, this convenience is worth the cost. Central air is an especially popular addition to homes located in parts of the country with warm and humid climates.WarrantyDue to their complex system of coils, central air units do tend to require maintenance as time passes. For this reason, it is recommended you purchase a central air system that comes with a lifetime warranty. Although such warranties may add to the upfront cost of a unit, they are a great way to save money on replacements and repairs over the lifetime of your product.The forced air vs. central air debate is really more a question about cost and convenience. Central air is expensive, but the convenience of instantly cooling your home may compensate for the additional cost.Photo credit: mira_foto via Compfight CC.Share This Article\"\n";

        // Full contents of file 1. This crashes if actually used as a query if not escaped (See "Query query = ..." below)
        String fileContent1 = "Science & Mathematics PhysicsThe hot glowing surfaces of stars emit energy in the form of electromagnetic radiation.?It is a good approximation to assume that the emissivity e is equal to 1 for these surfaces.  Find the radius of the star Rigel, the bright blue star in the constellation Orion that radiates energy at a rate of 2.7 x 1032 W and has a surface temperature of 11,000 K. Assume that the star is spheriw moreFollow 3 answersAnswersRelevanceRatingNewestOldestBest Answer: Stefan-Boltzmann law states that the energy flux by radiation is proportional to the forth power of the temperdecade ago0 18 CommentSchmiso, you forgot a 4 in your answer. Your link even says iosity, as the energy in this problem, you can find the radius R by doingope this helps everyone.Caroline · 4 years ago4 1 Comment (Stefan-Boltng in your values you should get: ou would like to learn more about one of these?Want to build a free website? Interested in dating sites?Need a Home Security Safe? How to order contacts online?\n";

        // Query the contents of the first file
        // "file_content" is the name of the field we want to search. It is defined like this during index creation (see method readFiles)
        // Currently, we use the same analyzer that was used to create the index (remove/keep stop words from query, etc), probably possible to use a different one
        QueryParser parser = new QueryParser("file_content", analyzer);

        // Apply analyzer to query, removing stop words etc depending on analyzer settings
        // QueryParser.escape Changes special characters so that they do not crash the query
        // https://stackoverflow.com/a/10259944
        // TODO: using QueryParser.escape strongly increases the amount of results, should this happen? Is it even
        //  needed to do this, do any queries from the assignment crash if not escaped?
        Query query = parser.parse(QueryParser.escape(fileContent1537));

        // Actually execute the query, limit shown results to 10
        ScoreDoc[] hits = isearcher.search(query, 10, Sort.RELEVANCE).scoreDocs;

        // example of how junit can be used to test results (needs junit jar to be added to project)
        //assertEquals(1, hits.length);

        // Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            // Get one result
            Document hitDoc = isearcher.doc(hits[i].doc);
            System.out.print("Rank number: " + String.valueOf(i + 1) + ", Doc number: " + hitDoc.getField("file_number") + "\n");
            //System.out.print("Doc content: " + hitDoc.getField("file_content") + "\n");
        }

        ireader.close();
        directory.close();

    }
}