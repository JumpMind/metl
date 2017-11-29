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
package org.jumpmind.metl.core.model;

public class ModelRelationMapping extends AbstractNamedObject implements IAuditable {

	private static final long serialVersionUID = 1L;
	
	private String name;
	
	private String modelRelationId;
	
	private ModelAttrib sourceAttribute;
	
	private ModelAttrib targetAttribute;

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getModelRelationId() {
		return modelRelationId;
	}

	public void setModelRelationId(String modelRelationId) {
		this.modelRelationId = modelRelationId;
	}

	public ModelAttrib getSourceAttribute() {
		return sourceAttribute;
	}

	public void setSourceAttribute(ModelAttrib sourceAttribute) {
		this.sourceAttribute = sourceAttribute;
	}

	public ModelAttrib getTargetAttribute() {
		return targetAttribute;
	}

	public String getTargetAttribId() {
		return targetAttribute.getId();
	}
	
	public String getSourceAttribId() {
		return sourceAttribute.getId();
	}
	
	public void setTargetAttribute(ModelAttrib targetAttribute) {
		this.targetAttribute = targetAttribute;
	}
	
    public void setSourceAttribId(String sourceAttributeId) {
        if (sourceAttributeId != null) {
            sourceAttribute = new ModelAttrib();
            sourceAttribute.setId(sourceAttributeId);
        } else {
            sourceAttribute = null;
        }
    }

    public void setTargetAttribId(String targetAttributeId) {
        if (targetAttributeId != null) {
            targetAttribute = new ModelAttrib();
            targetAttribute.setId(targetAttributeId);
        } else {
            targetAttribute = null;
        }
    }
    
}
