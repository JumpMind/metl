package org.jumpmind.symmetric.is.core.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AbstractObjectNameBasedSorter implements Comparator<AbstractObject> {

    @Override
    public int compare(AbstractObject o1, AbstractObject o2) {
        return o1.getName().compareTo(o2.getName());
    }
    
    public static void sort(List<? extends AbstractObject> list) {
        Collections.sort(list, new AbstractObjectNameBasedSorter());
    }

}
