package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TableConfig {
	String name;
	Map<String, String> mapping;
	List<String> key;
	Map<String, String> adds;
	public Map<String, String> getAdds() {
		return adds;
	}
	public void setAdds(Map<String, String> adds) {
		this.adds = adds;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Map<String, String> getMapping() {
		return mapping;
	}
	public void setMapping(Map<String, String> mapping) {
		this.mapping = mapping;
	}
	public List<String> getKey() {
		return key;
	}
	public void setKey(List<String> key) {
		this.key = key;
	}
	
	/***
	 * 删除sql
	 * @return
	 */
	public String getDeleteSQL() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("delete from ");
		sb.append(this.name);
		sb.append(" where ");
		
		for (String column:this.key) {
			sb.append(column);
			sb.append("=");
			if(adds != null && adds.containsKey(column)) {
				sb.append(adds.get(column));
			}else {
				sb.append(":");
				sb.append(column);
			}
			
			sb.append(" and ");
		}
		sb.append(" 1=1 ");
		
		return sb.toString();
	}
	/***
	 *  插入sql
	 * @return
	 */
	public String getInsertSQL() {
		List<String> columns = new ArrayList<String>(mapping.keySet());
		List<String> addColumns = null;
		if(adds !=null & adds.size()>0) {
			addColumns = new ArrayList<String>(adds.keySet());
		}
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ");
		sb.append(this.name);
		sb.append(" (");
		for (int i = 0; i < columns.size(); i++) {
			sb.append(columns.get(i));
			if(i<columns.size()-1) {
				sb.append(",");
			}
		}
		if(addColumns != null && addColumns.size() > 0) {
			for (int i = 0; i < addColumns.size(); i++) {
				sb.append(",");
				sb.append(addColumns.get(i));
			}
		}
		sb.append(" ) values (");
		for (int i = 0; i < columns.size(); i++) {
			if(i>0) {
				sb.append(",");
			}
			sb.append(":");
			sb.append(columns.get(i));
			
		}
		if(addColumns != null && addColumns.size() > 0) {
			for (int i = 0; i < addColumns.size(); i++) {
				sb.append(",'");
				sb.append(adds.get(addColumns.get(i)));
				sb.append("'");
			}
		}
		sb.append(")");
		return sb.toString();
	}
}
