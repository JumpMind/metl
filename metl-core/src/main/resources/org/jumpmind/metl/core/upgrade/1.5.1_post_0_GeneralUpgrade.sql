insert into METL_COMPONENT_SETTING
select random_uuid() as ID
     , id as COMPONENT_ID
     , 'pass.along.control.messages' as NAME
     , 'true' as VALUE
     , CURRENT_TIMESTAMP() as CREATE_TIME
     , null as CREATE_BY
     , null as LAST_UPDATE_BY
     , CURRENT_TIMESTAMP() as LAST_UPDATE_TIME
from METL_COMPONENT where type = 'Transformer';