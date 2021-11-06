import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
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

public class tf_idf {
    public String validTF = "Snlb";
    public String validIDF = "Sntp";
    public String validNorm = "S";
    // S = standard (see https://github.com/apache/lucene/blob/main/lucene/core/src/java/org/apache/lucene/search/similarities/ClassicSimilarity.java)
    // Else according to table in slides
    public String tf = "S";
    public String idf = "S";
    public String norm = "S";

    public float tf(float freq){
        float temp = 0;
        if (Objects.equals(tf, "S")){
            temp = tf_default(freq);
        }else if(Objects.equals(tf, "n")){
            temp = tf_n(freq);
        }else if(Objects.equals(tf, "l")){
            temp = tf_l(freq);
        }else if(Objects.equals(tf, "a")){
            temp = tf_a(freq);
        }else if(Objects.equals(tf, "b")){
            temp = tf_b(freq);
        }else if(Objects.equals(tf, "L")){
            temp = tf_L(freq);
        }
        return temp;
    }

    public float idf(long docFreq, long docCount){
        float temp = 0;
        if(Objects.equals(idf,"S")){
            temp = idf_default(docFreq,docCount);
        }else if(Objects.equals(idf,"n")){
            temp = idf_n(docFreq,docCount);
        }else if(Objects.equals(idf,"t")){
            temp = idf_t(docFreq,docCount);
        }else if(Objects.equals(idf,"p")){
            temp = idf_p(docFreq,docCount);
        }
        return temp;
    }

    public float norm(int numTerms) {
        float temp = 0;
        if(Objects.equals(norm, "S")){
            temp = norm_default(numTerms);
        }
        return temp;
    }

    public float tf_n(float freq){
        return freq;
    }
    public float tf_l(float freq){
        return (float) (1+Math.log(freq));
    }
    public float tf_a(float freq){
        //TODO
        return 1;
    }
    public float tf_b(float freq){
        float temp = 0;
        if(freq > 0){
            temp = 1;
        }
        return temp;
    }
    public float tf_L(float freq){
        //TODO
        return 1;
    }
    public float tf_default(float freq){
        // https://github.com/apache/lucene/blob/main/lucene/core/src/java/org/apache/lucene/search/similarities/ClassicSimilarity.java
        return (float) Math.sqrt(freq);
    }

    public float idf_n(long docFreq, long docCount){
        return 1;
    }
    public float idf_t(long docFreq, long docCount){
        return (float) Math.log((float)docCount/docFreq);
    }
    public float idf_p(long docFreq, long docCount){
        float temp = (float) Math.log( (float)(docCount-docFreq) / docFreq);
        if (temp < 0){
            temp = 0;
        }
        return temp;
    }
    public float idf_default(long docFreq, long docCount){
        // https://github.com/apache/lucene/blob/main/lucene/core/src/java/org/apache/lucene/search/similarities/ClassicSimilarity.java
        return (float) (Math.log((docCount + 1) / (double) (docFreq + 1)) + 1.0);
    }

    // https://github.com/apache/lucene/blob/main/lucene/core/src/java/org/apache/lucene/search/similarities/ClassicSimilarity.java
    public float norm_default(int numTerms) {
        return (float) (1.0 / Math.sqrt(numTerms));
    }
}
