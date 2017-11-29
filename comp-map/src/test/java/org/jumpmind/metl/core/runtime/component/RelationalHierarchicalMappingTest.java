package org.jumpmind.metl.core.runtime.component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.jumpmind.metl.core.model.DataType;
import org.junit.Test;

public class RelationalHierarchicalMappingTest {

    @Test
    public void testTreeSetIterator() {
    		Set<Relationship> rels = new HashSet<Relationship>();
    		rels.add(new Relationship("xyz","store",DataType.REF));
    		rels.add(new Relationship("store","dma",DataType.REF));
    		rels.add(new Relationship("store","location",DataType.ARRAY));
    		TreeSet<Relationship> relTree = new TreeSet(rels);
    		Iterator<Relationship> itr = relTree.descendingIterator();
    		while (itr.hasNext()) {
    			Relationship rel = itr.next();
    			System.out.println(rel.parent + "," + rel.child);
    		}
    		
    }
        
	class Relationship implements Comparable<Relationship> {
		public String parent;
		public String child;
		public DataType type;
		
		public Relationship(String parent, String child, DataType type) {
			this.parent = parent;
			this.child = child;
			this.type = type;
		}

		@Override
		public int compareTo(Relationship o) {
			if (!o.child.equalsIgnoreCase(this.parent)) {
				return 1;
			} else {
				return 0;
			}
		}
	}

}
