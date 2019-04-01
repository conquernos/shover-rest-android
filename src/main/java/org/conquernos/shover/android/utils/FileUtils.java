package org.conquernos.shover.android.utils;


import java.io.File;


public class FileUtils {

    public static boolean isFile(String filePath) {
        return new File(filePath).isFile();
    }

}
