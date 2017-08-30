update metl_project_version set version_type='MASTER', version_label='master'
where id in
(
   select 
      pv.id
   from 
      metl_project_version pv
      inner join 
      (
         select
            p.id as project_id
            , max(pv.last_update_time) as max_pv_last_update_time
         from
            metl_project p
            inner join metl_project_version pv
               on p.id = pv.project_id
         group by 
            p.id
      ) pvx
         on pv.project_id = pvx.project_id
         and pv.last_update_time = pvx.max_pv_last_update_time
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
         
delete from metl_agent_deployment_parameter;
