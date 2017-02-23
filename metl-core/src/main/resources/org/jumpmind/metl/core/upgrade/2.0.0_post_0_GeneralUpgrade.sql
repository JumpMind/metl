update metl_project_version set version_type='MASTER', version_label='master'
where id in
(
   select
      mpv.id
   from
      metl_project_version mpv
      inner join 
      (   
         select 
            project_id, max(create_time) max_create_time
         from 
            metl_project_version
         where
            deleted=0
         group by
            project_id
      ) mct
         on mpv.project_id = mct.project_id
         and mpv.create_time = mct.max_create_time
   where 
      mpv.deleted=0
);

update metl_project_version set version_type='RELEASE', release_date='2017-01-01'
where version_type is null;

insert into metl_agent_flow_deployment_parameter
select 
   f.id
   , adp.agent_deployment_id
   , adp.name
   , adp.value
   , adp.create_time
   , adp.create_by
   , adp.last_update_by
   , adp.last_update_time
from
   metl_agent_deployment_parameter adp
      inner join metl_flow_parameter fp
         on adp.flow_parameter_id = fp.id
      inner join metl_flow f
         on f.id = fp.flow_id;
         