import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Only use import below if junit is actually installed, might not be useful
// import static org.junit.Assert.assertEquals;

// https://lucene.apache.org/core/7_3_1/core/index.html
// TODO: split up indexing/reading queries/querying into separate classes/files?
public class mainClass {

    // If you want the path to be relative to root directory, remember to use ./ at the start
    //TODO: give these as actual arguments rather than hardcoded?
    static String DATASET_DIRECTORY_PATH = "./Datasets/Small";
    static String QUERY_FILE_PATH = "./Queries/dev_small_queries.tsv";
    static Boolean QUERY_IS_CSV = false;
    static Boolean INDEX_LOCATION_IN_RAM = true;
    static String INDEX_LOCATION_IF_ON_DISK = "./tmp/testindex2";
    static int LIMIT_SEARCH_RESULT_PER_QUERY = 1;

    // Timers used to give progress updates, initial values set at start of indexing
    public static long startOfProgram;
    public static long TimeSincePrevIndex;

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

    // https://zetcode.com/java/listdirectory/
    // Indexes all documents
    public static void readFiles(IndexWriter iwriter, String datasetDir) throws IOException {

        // Timers to give progress updates
        startOfProgram = System.currentTimeMillis();
        TimeSincePrevIndex = System.currentTimeMillis();

        //For each file in dataset
        Files.list(new File(datasetDir).toPath())
                //Get the path to the file
                .forEach(path -> {
                            //Create a new document to index
                            Document doc = new Document();
                            // https://stackoverflow.com/a/4030936
                            // Get file number from file path, store it in the document
                            doc.add(new Field("file_number", path.toString().replaceAll("\\D+", ""), TextField.TYPE_STORED));
                            // Initialize file contents
                            String content = "";

                            //TODO: verwijder
                            if (iwriter.numDocs()%10000 == 0){
                                System.out.print("Indexed " + iwriter.numDocs() + " files, time since last update: " + (-TimeSincePrevIndex +System.currentTimeMillis()) + "\n");
                                TimeSincePrevIndex = System.currentTimeMillis();
                            }

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
        readFiles(iwriter, DATASET_DIRECTORY_PATH);
    }

    // Parse .tsv files (queries)
    // Return format: data.get(x) gets row x, data.get(x)[0] gets query number, data.get(x)[1] gets query
    // https://stackoverflow.com/a/61443651
    // TODO: make tsvr itself check the file extension instead of giving as argument
    public static ArrayList<String[]> tsvr(File test2, Boolean csv) {
        ArrayList<String[]> Data = new ArrayList<>(); //initializing a new ArrayList out of String[]'s
        try (BufferedReader TSVReader = new BufferedReader(new FileReader(test2))) {
            String line = null;
            while ((line = TSVReader.readLine()) != null) {
                String[] lineItems;
                //splitting the line and adding its items in String[]
                if (csv){
                    lineItems = line.split(",");
                }else{
                    lineItems = line.split("\t");
                }
                Data.add(lineItems); //adding the splitted line array to the ArrayList
            }
            // Remove header row
            Data.remove(0);
        } catch (Exception e) {
            System.out.println("Query file could not be parsed");
        }
        return Data;
    }

    public static void main(String[] args) throws IOException, ParseException {

        Directory directory;
        // Store the index in memory:
        if(INDEX_LOCATION_IN_RAM){
            directory = new RAMDirectory();
        }else{
            directory = FSDirectory.open(Paths.get(INDEX_LOCATION_IF_ON_DISK));
        }

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

        System.out.print("Indexing took " + (-startOfProgram + System.currentTimeMillis() + "\n"));

        // Now search the index:
        // Open the directory where we stored the index
        DirectoryReader ireader = DirectoryReader.open(directory);
        // We want to search in the index we just read
        IndexSearcher isearcher = new IndexSearcher(ireader);

        // read query file from path
        File queryFile = new File(QUERY_FILE_PATH);
        // Parse queries: queries.get(x) gets row x, queries.get(x)[0] gets query number, queries.get(x)[1] gets query
        //TODO: make tsvr itself check the file extension
        ArrayList<String[]> queries = tsvr(queryFile,QUERY_IS_CSV);

        // "file_content" is the name of the field we want to search. It is defined like this during index creation (see method readFiles)
        // Currently, we use the same analyzer that was used to create the index (remove/keep stop words from query, etc), probably possible to use a different one
        QueryParser parser = new QueryParser("file_content", analyzer);

        //Used to give progress updates during querying
        int counter = 0;

        //Results will be here
        ArrayList<String[]> QueryNrResultNr = new ArrayList<String[]>();
        // Header required in output
        QueryNrResultNr.add(new String[]{"Query_number", "Document_number"});

        // Reminder: queryRow[0] gets query number, queryRow[1] gets query
        for ( String[] queryRow : queries){
            // Apply analyzer to query, removing stop words etc depending on analyzer settings
            // QueryParser.escape Changes special characters so that they do not crash the query
            // https://stackoverflow.com/a/10259944
            // TODO: using QueryParser.escape strongly increases the amount of results, should this happen? Is it even
            //  needed to do this, do any queries from the assignment crash if not escaped?
            Query query = parser.parse(QueryParser.escape(queryRow[1]));

            // Show progress
            counter += 1;
            if ((counter % 100) == 0){
                System.out.print("Running query " + counter + "\n");
            }

            // Actually run the query, limit shown results
            ScoreDoc[] hits = isearcher.search(query, LIMIT_SEARCH_RESULT_PER_QUERY, Sort.RELEVANCE).scoreDocs;

            // Iterate through the results:
            for (int i = 0; i < hits.length; i++) {
                // Get one result
                Document hitDoc = isearcher.doc(hits[i].doc);

                //Add to "csv" to be written to output
                QueryNrResultNr.add(new String[]{queryRow[0], hitDoc.getField("file_number").stringValue()});

                // Examples of ScoreDoc usage
                //System.out.print("Rank number: " + String.valueOf(i + 1) + ", Doc number: " + hitDoc.getField("file_number") + "\n");
                //System.out.print("Doc content: " + hitDoc.getField("file_content") + "\n");
            }
        }

        // Write output
        System.out.print("Output written to ./result.csv\n");
        givenDataArray_whenConvertToCSV_thenOutputCreated("./result.csv",QueryNrResultNr);

        ireader.close();
        directory.close();

    }
}