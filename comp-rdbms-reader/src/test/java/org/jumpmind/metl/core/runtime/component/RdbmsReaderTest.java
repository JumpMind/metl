/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.jumpmind.metl.core.runtime.MisconfiguredException;
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
        String sql = "select\r\n ISNULL(a,ISNULL(z,'')) /*COLA*/, 'lastName, firstName'/*COLB*/, c/*  COLC */, d /*  \"COL D\" */ from test;";
        Map<Integer, String> hints = RdbmsReader.getSqlColumnEntityHints(sql);
        assertEquals(hints.get(1), "COLA");
        assertEquals(hints.get(2), "COLB");
        assertEquals(hints.get(3), "COLC");
        assertEquals(hints.get(4), "\"COL D\"");
        
        
        // Do not allow duplicate entity attribute combinations.
        String dupeAtribSQL = 
                "select" + 
                "    a   /* test.a */" + 
                "    , b /* test.b */" + 
                "    , c /* test.a */" + 
                "from test;";
        boolean errorFound = false;
        try {
            RdbmsReader.getSqlColumnEntityHints(dupeAtribSQL);
        } catch(MisconfiguredException e) {
            errorFound = true;
        }
        assertEquals(errorFound,true);
        
        
        // Allow duplicate entity names
        String dupeEntitySQL = 
                "select" + 
                "    a   /* test */" + 
                "    , b /* test */" + 
                "    , c /* test */" + 
                "from test;";
        errorFound = false;
        try {
            RdbmsReader.getSqlColumnEntityHints(dupeEntitySQL);
        } catch(MisconfiguredException e) {
            errorFound = true;
        }
        assertEquals(errorFound,false);
    }
    

}
