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
