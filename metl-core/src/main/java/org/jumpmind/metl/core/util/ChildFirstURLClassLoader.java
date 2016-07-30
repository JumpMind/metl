package org.jumpmind.metl.core.util;

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

}