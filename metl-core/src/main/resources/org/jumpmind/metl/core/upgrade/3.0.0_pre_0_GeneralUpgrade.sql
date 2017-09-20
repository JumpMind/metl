alter table if exists metl_project_version_dependency rename to metl_project_version_depends;
alter table if exists metl_project_version_definition_plugin rename to metl_project_version_plugin;
alter table if exists metl_model_attribute rename to metl_model_attrib;
alter table if exists metl_component_attribute_setting rename to metl_component_attrib_setting;
alter table if exists metl_agent_deployment rename to metl_agent_deploy;
alter table if exists metl_agent_flow_deployment_parameter rename to metl_agent_flow_deploy_parm;
alter table if exists metl_agent_deployment_parameter rename to metl_agent_deploy_parm;
alter table if exists metl_release_package_project_version rename to metl_rppv;
