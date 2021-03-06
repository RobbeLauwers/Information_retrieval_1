import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.xml.builders.SpanNearBuilder;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


// Only use import below if junit is actually installed, might not be useful
// import static org.junit.Assert.assertEquals;

// Starting point for this class was from https://lucene.apache.org/core/7_3_1/core/index.html,
// by Apache software foundation
public class mainClass {
    // If you want the path to be relative to root directory, remember to use ./ at the start
    static String DATASET_DIRECTORY_PATH = "./Datasets/Small";
    //Path to file containing queries
    static String QUERY_FILE_PATH = "./Queries/dev_small_queries.tsv";
    //Is the query file csv? tsv is assumed if false
    static Boolean QUERY_IS_CSV = false;
    //Store index in ram if true, on disk if false. Recommended true for small, false for large dataset
    static Boolean INDEX_LOCATION_IN_RAM = true;
    //Where should index be stored if on disk?
    static String INDEX_LOCATION_IF_ON_DISK = "./tmp/testindex2";
    //Max results that a query can return
    static int LIMIT_SEARCH_RESULT_PER_QUERY = 1;
    //Is there a file containing example results?
    static Boolean COMPARE_RESULTS_TO_EXAMPLE = true;
    //If above is true, where is it?
    static String EXAMPLE_PATH = "./Queries/dev_query_results_small.csv";
    //Where should query results be written?
    static String TEST_OUTPUT_PATH = "./result.csv";
    //Should time to index be stored in results?
    static Boolean STORE_TIME = false;


    //Set above variables to be suitable for big dataset + comparison with example results
    public static void set_big_test(){
        DATASET_DIRECTORY_PATH = "./Datasets/Large";
        QUERY_FILE_PATH = "./Queries/dev_queries.tsv";
        INDEX_LOCATION_IN_RAM = false;
        INDEX_LOCATION_IF_ON_DISK = "./tmp/index";
        LIMIT_SEARCH_RESULT_PER_QUERY = 30;
        EXAMPLE_PATH = "./Queries/dev_query_results.csv";
        STORE_TIME = true;
    }

    public static void set_big_real(){
        DATASET_DIRECTORY_PATH = "./Datasets/Large";
        QUERY_FILE_PATH = "./Queries/queries.csv";
        INDEX_LOCATION_IN_RAM = false;
        INDEX_LOCATION_IF_ON_DISK = "./tmp/indexReal";
        LIMIT_SEARCH_RESULT_PER_QUERY = 10;
        QUERY_IS_CSV = false;
        STORE_TIME = true;
        COMPARE_RESULTS_TO_EXAMPLE = false;
    }

    // Timers used to give progress updates, initial values set at start of indexing
    //Set at start of indexing, never changes
    public static long startOfProgram;
    //Set after every 10000 documents indexed
    public static long TimeSincePrevIndex;

    //Keep stats about all tested
    public static ArrayList<String[]> allResults = new ArrayList<>();

    // https://www.baeldung.com/java-csv by multiple authors
    // Helper function to write output
    public static String convertToCSV(String[] data) {
        return String.join(",", data);
    }

    // https://www.baeldung.com/java-csv by multiple authors
    // prints output to csv
    public static void givenDataArray_whenConvertToCSV_thenOutputCreated(String CSV_FILE_NAME, ArrayList<String[]> dataLines) throws IOException {
        File csvOutputFile = new File(CSV_FILE_NAME);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            dataLines.stream()
                    .map(mainClass::convertToCSV)
                    .forEach(pw::println);
        }
    }

    // https://zetcode.com/java/listdirectory/ by Jan Bodnar
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
                            // https://stackoverflow.com/a/4030936 by stackoverflow user codaddict
                            // Get file number from file path, store it in the document
                            doc.add(new Field("file_number", path.toString().replaceAll("\\D+", ""), TextField.TYPE_STORED));
                            // Initialize file contents
                            String content = "";

                            if (iwriter.numDocs()%10000 == 0){
                                System.out.print("Indexed " + iwriter.numDocs() + " files, time since last update: " + (-TimeSincePrevIndex +System.currentTimeMillis()) + "\n");
                                TimeSincePrevIndex = System.currentTimeMillis();
                            }

                            // Exception handling seems to be mandatory?
                            try {
                                // Get file from path
                                File tempFile = path.toFile();
                                // https://stackoverflow.com/a/15253450 by Stackoverflow user Egor
                                // Check if file is actually a file containing data, the datasets provided also include non-data files (like write.lock)
                                if (tempFile.length() > 0 && path.toString().substring(path.toString().length() - 3).equals("txt")) {
                                    // https://stackoverflow.com/a/3403112 by STackoverflow user polygenelubricants
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
    // https://stackoverflow.com/a/61443651, by Stackoverflow user Germano Mosconi
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

    public static int compareResults(ArrayList<String[]> results, Boolean resultNotInExample){
        //Reads example results from csv
        ArrayList<String[]> example = tsvr(new File(EXAMPLE_PATH),true);

        int errors = 0;

        if(resultNotInExample){
            //Key: query number, value: document numbers
            HashMap<String, ArrayList<String>> mapExample = new HashMap<>();
            //For each row from table [querynumber,documentnumber]
            for (String[] row : example){
                //If key (query number) is not yet present
                if(!mapExample.containsKey(row[0])){
                    //Add query number and document number
                    mapExample.put(row[0], new ArrayList<String>(Collections.singleton(row[1])));
                }else{
                    //If key did exist, add document
                    ArrayList<String> temp = mapExample.get(row[0]);
                    temp.add(row[1]);
                    mapExample.put(row[0],temp);
                }
            }
            int error2 = 0;
            //For each query/document combination in our results
            for (String[] row : results){
                if(!mapExample.containsKey(row[0])){
                    error2 += 1;
                    continue;
                }
                //Check if same query in example has the same result
                if(!mapExample.get(row[0]).contains(row[1])){
                    //If not, count as error
                    error2 += 1;
                }
            }
            errors = error2;
            System.out.print("There are " + error2 + " actual query results that do not show up in the examples\n");
        }else{
            //same as above, but example and actual results swapped
            HashMap<String, ArrayList<String>> mapResult = new HashMap<>();
            for (String[] row : results){
                if(!mapResult.containsKey(row[0])){
                    mapResult.put(row[0], new ArrayList<String>(Collections.singleton(row[1])));
                }else{
                    ArrayList<String> temp = mapResult.get(row[0]);
                    temp.add(row[1]);
                    mapResult.put(row[0],temp );
                }
            }
            int error = 0;
            for (String[] row : example){
                if(!mapResult.containsKey(row[0])){
                    error += 1;
                    continue;
                }
                if(!mapResult.get(row[0]).contains(row[1])){
                    error += 1;
                }
            }
            errors = error;
            System.out.print("There are " + error + " example query results that do not show up in the results\n");
        }
        return errors;
    }

    //Run all implemented tf_idf variations
    public static void runAllTF_IDF() throws IOException, ParseException {
        //Instantiate object that contains tf_idf functions
        tf_idf TF_TEMP = new tf_idf();
        // https://www.techiedelight.com/iterate-over-characters-string-java/
        //For each idf implementation
        for (int idf_int = 0; idf_int < TF_TEMP.validIDF.length(); idf_int++) {
            TF_TEMP.idf = String.valueOf(TF_TEMP.validIDF.charAt(idf_int));
            //For each tf implementation
            for (int tf_int = 0; tf_int < TF_TEMP.validTF.length(); tf_int++) {
                TF_TEMP.tf = String.valueOf(TF_TEMP.validTF.charAt(tf_int));
                //for each norm implementation
                for (int norm_int = 0; norm_int < TF_TEMP.validNorm.length(); norm_int++) {
                    TF_TEMP.norm = String.valueOf(TF_TEMP.validNorm.charAt(norm_int));
                    String name =  TF_TEMP.tf + TF_TEMP.idf + TF_TEMP.norm;
                    //create similarity with functions set above
                    Similarity similarity = new TFIDFSimilarity() {
                        @Override
                        public float tf(float v) {
                            return TF_TEMP.tf(v);
                        }

                        @Override
                        public float idf(long l, long l1) {
                            return TF_TEMP.idf(l,l1);
                        }

                        @Override
                        public float lengthNorm(int i) {
                            return TF_TEMP.norm(i);
                        }

                        @Override
                        public float sloppyFreq(int i) {
                            return 0;
                        }

                        @Override
                        public float scorePayload(int i, int i1, int i2, BytesRef bytesRef) {
                            return 0;
                        }
                    };
                    System.out.print("tf_idf using " + name + "." + name + "\n");
                    //Actually do search
                    fullSearch(similarity,similarity,"" + name + "." + name);
                }
            }
        }
    }

    //Run Okapi25BM with several parameters
    public static void runAllOkapi25(float stepsize) throws IOException, ParseException {
        for (int k1int = 100; k1int < 250; k1int += stepsize) {
            float k1 = 1.0f * k1int / 100;
            for (int bint = 20; bint < 100; bint += stepsize) {
                    float  b = 1.0f * bint / 100;
                    Similarity similarity = new BM25Similarity(k1, b);
                    System.out.print("Okapi 25 using k1=" + k1 + " and b=" + b + "\n");
                    fullSearch(similarity,similarity,"Okapi_k" + k1 + "_b" + b);
            }
        }
    }

    //Run Language model with Jelinek-Mercer smoothing with several parameters
    public static void runAllLM(float stepsize) throws IOException, ParseException {
        for (float i =0; i <= 1; i+=stepsize){
            Similarity similarity = new LMJelinekMercerSimilarity(i);
            System.out.print("Language model using Jelinek-Mercer smoothing, using lambda=" + i +  "\n");
            fullSearch(similarity,similarity,"LM_i" + i);
        }
    }

    //Runs all implemented Similarities on the small dataset.
    public static void testSmall() throws IOException, ParseException {
        System.out.print("Using BM25\n");
        Similarity default_sim = new BM25Similarity();

        fullSearch(default_sim,default_sim,"BM25");
        runAllTF_IDF();
        runAllOkapi25(10);
        runAllLM((float) 0.2);

        givenDataArray_whenConvertToCSV_thenOutputCreated("./SmallResultsCSV.csv",allResults);
    }

    //Runs best results from small dataset on large dataset.
    public static void run_best_of_each() throws IOException, ParseException {
        set_big_test();
        System.out.print("Using BM25\n");
        Similarity default_sim = new BM25Similarity();
        fullSearch(default_sim,default_sim,"BM25");

        INDEX_LOCATION_IF_ON_DISK = "./tmp/index2";

        Similarity similarity = new BM25Similarity((float)1.9, (float)0.5);
        System.out.print("Okapi 25 using k1=" + 1.9 + " and b=" + 0.5 + "\n");
        fullSearch(similarity,similarity,"Okapi_k" + 1.9 + "_b" + 0.5);

        INDEX_LOCATION_IF_ON_DISK = "./tmp/index3";

        Similarity similarity2 = new LMJelinekMercerSimilarity((float)0.4);
        System.out.print("Language model using Jelinek-Mercer smoothing, using lambda=" + 0.4 +  "\n");
        fullSearch(similarity2,similarity2,"LM_i" + 0.4);

        INDEX_LOCATION_IF_ON_DISK = "./tmp/index4";

        tf_idf TF_TEMP = new tf_idf();
        TF_TEMP.norm = "n";
        TF_TEMP.idf = "p";
        TF_TEMP.tf = "l";
        Similarity similarity3 = new TFIDFSimilarity() {
            @Override
            public float tf(float v) {
                return TF_TEMP.tf(v);
            }

            @Override
            public float idf(long l, long l1) {
                return TF_TEMP.idf(l,l1);
            }

            @Override
            public float lengthNorm(int i) {
                return TF_TEMP.norm(i);
            }

            @Override
            public float sloppyFreq(int i) {
                return 0;
            }

            @Override
            public float scorePayload(int i, int i1, int i2, BytesRef bytesRef) {
                return 0;
            }
        };
        System.out.print("tf_idf using " + "npl" + "." + "npl" + "\n");
        fullSearch(similarity3,similarity3,"" + "npl" + "." + "npl");
        givenDataArray_whenConvertToCSV_thenOutputCreated("./comparisons.csv",allResults);

    }

    //Run main set of queries, do not compare to results.
    public static void run_main_queries() throws IOException, ParseException {
        set_big_real();
        Similarity similarity = new BM25Similarity((float)1.9, (float)0.5);
        System.out.print("Okapi 25 using k1=" + 1.9 + " and b=" + 0.5 + "\n");
        fullSearch(similarity,similarity,"Okapi_k" + 1.9 + "_b" + 0.5);
    }

    public static void main(String[] args) throws IOException, ParseException {
        //testSmall();
        //run_best_of_each();
        run_main_queries();
    }

    public static void fullSearch(Similarity similarityIndex,Similarity similarityQuery,String name) throws IOException, ParseException {

        Directory directory;
        // Store the index in memory:
        if(INDEX_LOCATION_IN_RAM){
            directory = new RAMDirectory();
        }else{
            //Or store on disk
            directory = FSDirectory.open(Paths.get(INDEX_LOCATION_IF_ON_DISK));
        }

        // https://lucene.apache.org/core/7_3_1/core/index.html
        // "Filters StandardTokenizer with StandardFilter, LowerCaseFilter and StopFilter, using a list of English stop words."
        Analyzer analyzer = new EnglishAnalyzer();

        // We want the index to be constructed using the analyzer defined above
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        config.setSimilarity(similarityIndex);

        // We tell the index writer where to write to (directory) and give it additional settings like ignoring stop words (config)
        IndexWriter iwriter = new IndexWriter(directory, config);
        // Actually run the indexer
        indexing(iwriter);
        // Indexing finished -> close index writer
        iwriter.close();

        long indexingTime = -startOfProgram + System.currentTimeMillis();
        System.out.print("Indexing took " + (-startOfProgram + System.currentTimeMillis() + "\n"));

        // Now search the index:
        // Open the directory where we stored the index
        DirectoryReader ireader = DirectoryReader.open(directory);
        // We want to search in the index we just read
        IndexSearcher isearcher = new IndexSearcher(ireader);

        isearcher.setSimilarity(similarityQuery);

        // read query file from path
        File queryFile = new File(QUERY_FILE_PATH);
        // Parse queries: queries.get(x) gets row x, queries.get(x)[0] gets query number, queries.get(x)[1] gets query
        ArrayList<String[]> queries = tsvr(queryFile,QUERY_IS_CSV);

        // "file_content" is the name of the field we want to search. It is defined like this during index creation (see method readFiles)
        // Currently, we use the same analyzer that was used to create the index (remove/keep stop words from query, etc), probably possible to use a different one
        QueryParser parser = new QueryParser("file_content", analyzer);
        parser.setSplitOnWhitespace(true);

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
            // https://stackoverflow.com/a/10259944 ,by Stackoverflow user Pau Kiat Wee
            Query query;

            query = parser.parse(QueryParser.escape(queryRow[1]));


            // Show progress
//            counter += 1;
//            if ((counter % 100) == 0){
//                System.out.print("Running query " + counter + "\n");
//            }

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

        int error = 0;
        if(COMPARE_RESULTS_TO_EXAMPLE){
            error = compareResults(QueryNrResultNr,false);
        }

        // Write output
        //System.out.print("Output written to ./result.csv\n");
        System.out.print("\n");
        givenDataArray_whenConvertToCSV_thenOutputCreated(TEST_OUTPUT_PATH,QueryNrResultNr);

        ireader.close();
        directory.close();

        if(STORE_TIME){
            allResults.add(new String[]{name, String.valueOf(error), String.valueOf(indexingTime)});
        }else{
            allResults.add(new String[]{name, String.valueOf(error)});
        }

    }
}