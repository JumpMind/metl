package org.jumpmind.metl.core.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

public class ChildFirstURLClassLoader extends URLClassLoader {

    private ClassLoader realParent;

    public ChildFirstURLClassLoader(URL classpath, ClassLoader parent) {
        this(new URL[] { classpath }, parent);
    }

    public ChildFirstURLClassLoader(URL[] classpath, ClassLoader parent) {
        super(classpath, null);
        this.realParent = parent;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> loaded = super.findLoadedClass(name);
        if (loaded == null) {
            try {
                loaded = super.findClass(name);
            } catch (ClassNotFoundException e) {
                loaded = realParent.loadClass(name);
            }
        }
        return loaded;
    }
    
    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);
        if (url == null) {
            url = realParent.getResource(name);
        }
        return url;
    }
    
    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = super.getResourceAsStream(name);
        if (is == null) {
            is = realParent.getResourceAsStream(name);
        }
        return is;
    }

}