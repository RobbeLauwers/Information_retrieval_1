//Source: https://zetcode.com/java/listdirectory/ by Jan Bodnar
package com.zetcode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class JavaFilesList {

    public static void main(String[] args) throws IOException {

        String dirName = "/home/janbodnar/prog/";

        Files.list(new File(dirName).toPath())
                .limit(10)
                .forEach(path -> {
                    System.out.println(path);
                });
    }
}