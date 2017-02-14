package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

public class RdbmsReaderTest {

    @Test
    public void testCountColumnSeparatingCommas() {
        
        int count = RdbmsReader.countColumnSeparatingCommas("ISNULL(a,''), b, *");
        assertEquals(count, 2);
        
        count = RdbmsReader.countColumnSeparatingCommas("ISNULL(a,('')), b, 'a,b', *");
        assertEquals(count, 3);
    }

    @Test
    public void testGetSqlColumnEntityHints() throws Exception {
        String sql = "select\r\n ISNULL(a,ISNULL(z,'')) /*COLA*/, 'lastName, firstName'/*COLB*/, c/*  COLC */ from test;";
        Map<Integer, String> hints = RdbmsReader.getSqlColumnEntityHints(sql);
        assertEquals(hints.get(1), "COLA");
        assertEquals(hints.get(2), "COLB");
        assertEquals(hints.get(3), "COLC");
    }
}
