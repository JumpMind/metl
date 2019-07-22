package org.jumpmind.metl.ui.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


import org.jumpmind.vaadin.ui.common.IDataProvider;

import com.vaadin.v7.ui.AbstractField;
import com.vaadin.v7.ui.Table.ColumnHeaderMode;

public class TableV7DataProvider implements IDataProvider {
	
	private com.vaadin.v7.ui.Table table;
	
	public TableV7DataProvider(com.vaadin.v7.ui.Table table) {
		this.table = table;
	}

	@Override
	public Collection<?> getRowItems() {
		return table.getItemIds();
	}

	@Override
	public List<Object> getColumns() {
		return Arrays.asList(table.getVisibleColumns());
	}

	@Override
	public Object getCellValue(Object item, Object column) {
		return ((AbstractField<String>) table.getContainerProperty(item, column).getValue()).getValue();
	}

	@Override
	public String getHeaderValue(Object column) {
		return table.getColumnHeader(column);
	}

	@Override
	public boolean isHeaderVisible() {
		return ! ColumnHeaderMode.HIDDEN.equals(table.getColumnHeaderMode());
	}

}
