package org.jumpmind.symmetric.is.core.persist;

import java.io.File;

import net.sourceforge.schemaspy.Main;

import org.apache.commons.io.FileUtils;

public class SchemaSpy {

    public static void main(String[] args) throws Exception {
        FileUtils.deleteDirectory(new File("build/schemaspy"));
        SchemaTest test  = new SchemaTest();
        test.setup();
        test.createSchema();
        Main.main(new String[] { "-t","src/test/resources/schemaspy.properties","-u","jumpmind","-p","jumpmind","-o","build/schemaspy","-s","PUBLIC"});
    }

}
