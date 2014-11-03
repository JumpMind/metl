package org.jumpmind.symmetric.is.ui.support;

import java.util.Set;

import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.Table;

public class MultiSelectTable extends Table {

    private static final long serialVersionUID = 1L;

    private Set<Object> lastSelected;
    
    public MultiSelectTable() {
        setMultiSelect(true);
        setSelectable(true);
        
        addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;
            @SuppressWarnings("unchecked")
            @Override
            public void valueChange(com.vaadin.data.Property.ValueChangeEvent event) {
                lastSelected = (Set<Object>) getValue();
            }
        });
        
        addItemClickListener(new ItemClickListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.getButton() == MouseButton.LEFT) {
                    if (lastSelected != null && lastSelected.contains(event.getItemId())) {
                        unselect(event.getItemId());
                    }
                }
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public <T> Set<T> getSelected() {
        return (Set<T>)getValue();
    }
}
