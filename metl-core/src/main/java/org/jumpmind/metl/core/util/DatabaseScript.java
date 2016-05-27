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
package org.jumpmind.metl.core.util;

import org.apache.commons.fileupload.InvalidFileNameException;
import org.springframework.core.io.Resource;

public class DatabaseScript implements Comparable<DatabaseScript> {
	private int major;
	private int minor;
	private int build;
	
	private int when;
	private int order;
	private String description;
	
	private Resource resource;
	
	public final static String DELIMITER_MAIN = "_";
	public final static String DELIMITER_VERSION = ".";
	public final static String DELIMITER_VERSION_ESCAPED = "\\.";
	
	public final static int WHEN_PREINSTALL = 1;
	public final static int WHEN_POSTINSTALL = 2;
	
	
	public DatabaseScript(String fileName) {
		parse(fileName);
	}
	
	public DatabaseScript() {
	}
	
	public void parse(String fileName) {
		String[] parts = fileName.split(DELIMITER_MAIN);
		if (parts.length != 4) {
			throw new InvalidFileNameException(fileName, "Database scripts must have 3 parts : version_when_order_description.sql");
		}
		parseVersion(parts[0]);
		when = parseWhen(parts[1]);
		order = new Integer(parts[2]);
		parseDescription(parts[3]);
		
	}
	
	public void parseVersion(String version) {
		if (version != null) {
		String[] parts = version.split(DELIMITER_VERSION_ESCAPED);
			if (parts.length < 3) {
				throw new RuntimeException(String.format("An invalid version was provided: %s", version));
			}
			this.major = new Integer(parts[0]).intValue();
			this.minor = new Integer(parts[1]).intValue();
			this.build = new Integer(parts[2]).intValue();
		}
	}
	
	@Override
	public int compareTo(DatabaseScript o) {
		
		// 1.) Compare versions
		if (this.major > o.major){
			return 1;
		}
		else if (this.major < o.major) {
			return -1;
		}
		else if (this.minor > o.minor) {
			return 1;
		}
		else if (this.minor < o.minor) {
			return -1;
		}
		else if (this.build > o.build) {
			return 1;
		}
		else if (this.build < o.build) {
			return -1;
		}
		// 2.) Compare when 
		else if (this.when > o.when) {
			return 1;
		}
		else if (this.when < o.when) {
			return -1;
		}
		// 3.) Compare order
		else if (this.order > o.order) {
			return 1;
		}
		else if (this.order < o.order) {
			return -1;
		}
		return 0;
	}
	
	public int compareVersionTo(DatabaseScript o) {
		if (o == null) {
			return 1;
		}
		
		if (this.major > o.major){
			return 1;
		}
		else if (this.major < o.major) {
			return -1;
		}
		else if (this.minor > o.minor) {
			return 1;
		}
		else if (this.minor < o.minor) {
			return -1;
		}
		else if (this.build > o.build) {
			return 1;
		}
		else if (this.build < o.build) {
			return -1;
		}
		else {
			return 0;
		}
	}
	
	@Override
	public String toString() {
		return resource.getFilename();
	}
	
	public int parseWhen(String when) {
		return when.equals("pre") ? WHEN_PREINSTALL : WHEN_POSTINSTALL;
	}

	public String formatWhen(int when) {
		return when == WHEN_PREINSTALL ? "pre" : "post";
	}
	
	public void parseDescription(String description) {
		if (description.contains(".")) {
			String[] parts = description.split("\\.");
			this.description = parts[0];
		}
	}
	
	public void setResource(Resource resource) {
        this.resource = resource;
    }
	
	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getBuild() {
		return build;
	}
	
	public int getWhen() {
		return when;
	}
	
	public int getOrder() {
		return order;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Resource getResource() {
		return resource;
	}
	
}
