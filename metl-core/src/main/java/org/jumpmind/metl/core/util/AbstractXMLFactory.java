package org.jumpmind.metl.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.jumpmind.exception.IoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractXMLFactory {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    public AbstractXMLFactory() {
        refresh();
    }
    
    synchronized public void refresh() {
        reset();
        loadComponentsForClassloader(getClass().getClassLoader());
        // TODO in the future load from other resources
    }
    
    abstract protected void reset();

    abstract protected void loadComponentsForClassloader(ClassLoader classLoader);
    
    protected List<InputStream> loadResources(final String name, final ClassLoader classLoader) {
        try {
            final List<InputStream> list = new ArrayList<InputStream>();
            final Enumeration<URL> systemResources = (classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader)
                    .getResources(name);
            while (systemResources.hasMoreElements()) {
                list.add(systemResources.nextElement().openStream());
            }
            return list;
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

}
