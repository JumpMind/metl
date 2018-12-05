insert into metl_component_setting
select
   random_uuid()
   , id
   , 'send.control.messages.to.all.nodes'
   , 'false'
   , CURRENT_TIMESTAMP()
   , 'upgrade'
   , 'upgrade'
   , CURRENT_TIMESTAMP()
from
   metl_component
where
      type='Content Router'
      and deleted=0;