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

import static org.junit.Assert.assertEquals;

// https://lucene.apache.org/core/7_3_1/core/index.html
public class mainClass {

    // https://zetcode.com/java/listdirectory/
    // Prints all files in directory
    public static void testDirectory(Boolean largeDataset) throws IOException {
        String dirName = "./Datasets/Small";
        Files.list(new File(dirName).toPath())
                .forEach(System.out::println);
    }

    // https://zetcode.com/java/listdirectory/
    // Indexes all documents
    public static void readFiles(IndexWriter iwriter, Boolean smallDataset) throws IOException {
        String dirName = "";
        if (smallDataset){
            dirName = "./Datasets/Small";
        }else{
            dirName = "./Datasets/Large";
        }
        Files.list(new File(dirName).toPath())
                .forEach(path->{
                    Document doc = new Document();
                    // https://stackoverflow.com/a/4030936
                    doc.add(new Field("file_number", path.toString().replaceAll("\\D+",""), TextField.TYPE_STORED));
                    String content = "";
                    try {
                        // https://stackoverflow.com/a/3403112
                        File tempFile = path.toFile();
                        // https://stackoverflow.com/a/15253450
                        if (tempFile.length() > 0 && path.toString().substring(path.toString().length() - 3).equals("txt")){
                            // System.out.print("Indexing file: " + path.toString() + "\n");
                            content = new Scanner(tempFile).useDelimiter("\\Z").next();
                        }else{
                            // System.out.print("Empty file: " + path.toString());
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    doc.add(new Field("file_content", content, TextField.TYPE_STORED));
                    try {
                        iwriter.addDocument(doc);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

        );
    }

    // Instances of a class outside of methods need a bunch of keywords (static, final)?
    // private static final Analyzer analyzer = new StandardAnalyzer();
    public static void indexing(IndexWriter iwriter) throws IOException {
        readFiles(iwriter, true);
    }

    public static void main(String[] args) throws IOException, ParseException {
        Analyzer analyzer = new StandardAnalyzer();
        //testDirectory();

        // Store the index in memory:
        Directory directory = new RAMDirectory();
        // To store an index on disk, use this instead:
        //Directory directory = FSDirectory.open("/tmp/testindex");

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);
        indexing(iwriter);
        iwriter.close();

        // Now search the index:
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        // Parse a simple query that searches for "text":
        QueryParser parser = new QueryParser("fieldname", analyzer);
        Query query = parser.parse("text");
        ScoreDoc[] hits = isearcher.search(query, 1000, Sort.RELEVANCE).scoreDocs;
        assertEquals(1, hits.length);
        // Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            /* get field example:
            assertEquals("This is the text to be indexed.", hitDoc.get("fieldname")); */
        }

        ireader.close();
        directory.close();

    }

    /*
    // https://www.baeldung.com/lucene-file-search
    public void addFileToIndex(String filepath) throws IOException {

        Path path = Paths.get(filepath);
        File file = path.toFile();
        IndexWriterConfig indexWriterConfig
                = new IndexWriterConfig(analyzer);
        Directory indexDirectory = FSDirectory
                .open(Paths.get(""));
        IndexWriter indexWriter = new IndexWriter(
                indexDirectory, indexWriterConfig);
        Document document = new Document();

        FileReader fileReader = new FileReader(file);
        document.add(
                new TextField("contents", fileReader));
        document.add(
                new StringField("path", file.getPath(), Field.Store.YES));
        document.add(
                new StringField("filename", file.getName(), Field.Store.YES));

        indexWriter.addDocument(document);
        indexWriter.close();
    }
*/
}