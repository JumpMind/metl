
insert into metl_component_model_setting (id, type, component_id, model_object_id, name, value, create_time, create_by)
select id, 'ATTRIBUTE', component_id, attribute_id, 'mapping.processor.model.object.maps.to', value, create_time, create_by 
from metl_component_attrib_setting where name='mapping.processor.attribute.maps.to';

delete from metl_component_attrib_setting where name='mapping.processor.attribute.maps.to';
