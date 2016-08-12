insert into METL_COMPONENT_SETTING
select random_uuid() as ID
     , id as COMPONENT_ID
     , 'empty.file' as NAME
     , 'false' as VALUE
     , CURRENT_TIMESTAMP() as CREATE_TIME
     , null as CREATE_BY
     , null as LAST_UPDATE_BY
     , CURRENT_TIMESTAMP() as LAST_UPDATE_TIME
from METL_COMPONENT where type = 'Text File Writer';

update METL_COMPONENT set OUTPUT_MODEL_ID=INPUT_MODEL_ID where type='Transformer' and INPUT_MODEL_ID != OUTPUT_MODEL_ID;