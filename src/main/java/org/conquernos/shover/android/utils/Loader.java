package org.conquernos.shover.android.utils;


import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

public class Loader {

    public static URL getResourceUrl(String resource) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) classLoader = Loader.class.getClassLoader();

            Enumeration<URL> urls = classLoader.getResources(resource);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (FileUtils.isFile(url.getPath())) return url;
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public static InputStream getResourceInputStream(String resource) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) classLoader = Loader.class.getClassLoader();

        return classLoader.getResourceAsStream(resource);
    }

}
