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

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class SerializerTest {

    @Test
    public void testBuildObjectTree() throws JsonProcessingException {  //if the root is an object
    		ObjectMapper mapper = new ObjectMapper();	
    		ObjectNode root = mapper.createObjectNode();
    		
    		ObjectNode child = root.putObject("Store");   //entity  -- put object if its off the root
    		child.put("ID", "001");   //attributes
    		child.put("Name", "MyName");
    		System.out.println(mapper.writeValueAsString(root));
    		assertEquals("{\"Store\":{\"ID\":\"001\",\"Name\":\"MyName\"}}", 
    				mapper.writeValueAsString(root));    		
    }

    @Test
    public void testBuildObjectTreeNoRoot() throws JsonProcessingException {  //if the root is an object
    		ObjectMapper mapper = new ObjectMapper();	
    		ObjectNode root = mapper.createObjectNode();
    		
    		root.put("ID", "001");   //attributes
    		root.put("Name", "MyName");
    		System.out.println(mapper.writeValueAsString(root));
    		assertEquals("{\"ID\":\"001\",\"Name\":\"MyName\"}", 
    				mapper.writeValueAsString(root));    		
    }    

    @Test
    public void testBuildXmlObjectTreeNoRoot() throws JsonProcessingException {  //if the root is an object
    		XmlMapper mapper = new XmlMapper();   		
    		ObjectNode root = mapper.createObjectNode();    		
    		root.put("ID", "001");   //attributes
    		root.put("Name", "MyName");    		
    		ObjectWriter writer = mapper.writer();
    		System.out.println(writer.withRootName("Person").writeValueAsString(root));
    }    
    
    
    @Test
    public void testBuildArrayTree() throws JsonProcessingException {  // if the root is an array
    		ObjectMapper mapper = new ObjectMapper();	
    		ArrayNode root = mapper.createArrayNode();
    		    		
    		ObjectNode child1 = mapper.createObjectNode();  //entity   --  create object if its not off the root
    		child1.put("id", "001");  //attribute
    		child1.put("name", "Store 001");

    		ObjectNode store1 = mapper.createObjectNode();
    		store1.set("Store", child1);
    		
    		ObjectNode child2 = mapper.createObjectNode();
    		child2.put("id", "002");
    		child2.put("name", "Store 002");
    		
    		ObjectNode store2 = mapper.createObjectNode();
    		store2.set("Store", child2);
    		
    		root.add(store1);
    		root.add(store2);
   
    		System.out.println(mapper.writeValueAsString(root));
    		
    		assertEquals("[{\"Store\":{\"id\":\"001\",\"name\":\"Store 001\"}},{\"Store\":{\"id\":\"002\",\"name\":\"Store 002\"}}]", 
    				mapper.writeValueAsString(root));    		
    }
    
    @Test
    public void testSimpleEntityPayload() throws JsonProcessingException {


    }
    
}
