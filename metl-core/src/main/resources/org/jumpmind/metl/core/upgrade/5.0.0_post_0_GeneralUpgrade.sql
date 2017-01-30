update metl_project_version set version_type='TRUNK', version_label='trunk'
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
         group by
            project_id
      ) mct
         on mpv.project_id = mct.project_id
         and mpv.create_time = mct.max_create_time
);

update metl_project_version set version_type='RELEASE', released=1
where version_type is null;
