DELETE FROM METL_FLOW_STEP_LINK WHERE SOURCE_STEP_ID IN (SELECT ID FROM METL_FLOW_STEP WHERE FLOW_ID IN (SELECT ID FROM METL_FLOW WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' ));
DELETE FROM METL_FLOW_STEP WHERE FLOW_ID IN (SELECT ID FROM METL_FLOW WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' );
DELETE FROM METL_FLOW_PARAMETER WHERE FLOW_ID IN (SELECT ID FROM METL_FLOW WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' );
DELETE FROM METL_FLOW WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' ;
DELETE FROM METL_COMPONENT_ATTRIB_SETTING WHERE COMPONENT_ID IN (SELECT ID FROM METL_COMPONENT WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' );
DELETE FROM METL_COMPONENT_ENTITY_SETTING WHERE COMPONENT_ID IN (SELECT ID FROM METL_COMPONENT WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' );
DELETE FROM METL_COMPONENT_SETTING WHERE COMPONENT_ID IN (SELECT ID FROM METL_COMPONENT WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' );
DELETE FROM METL_COMPONENT WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' ;
DELETE FROM METL_RESOURCE_SETTING WHERE RESOURCE_ID IN (SELECT ID FROM METL_RESOURCE WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' );
DELETE FROM METL_RESOURCE WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' ;
DELETE FROM METL_MODEL_ATTRIB WHERE ENTITY_ID IN (SELECT ID FROM METL_MODEL_ENTITY WHERE MODEL_ID in (SELECT ID FROM METL_RELATIONAL_MODEL WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' ));
DELETE FROM METL_MODEL_ENTITY WHERE MODEL_ID in (SELECT ID FROM METL_RELATIONAL_MODEL WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' );
DELETE FROM METL_RELATIONAL_MODEL WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad' ;
DELETE FROM METL_FOLDER WHERE PROJECT_VERSION_ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad';
DELETE FROM METL_PROJECT_VERSION WHERE ID='c1de9907-a913-41ff-8c7f-7bd67dae90ad';
DELETE FROM METL_PROJECT WHERE ID='bcbd73f9-ea7a-49f0-abf4-d150320e8887';
insert into METL_PROJECT (ID, NAME, DESCRIPTION, DELETED, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('bcbd73f9-ea7a-49f0-abf4-d150320e8887','Deduper Flow Test',null,0,{ts '2016-02-09 14:16:28.181'},null,null,{ts '2016-02-09 14:17:18.652'});
insert into METL_PROJECT_VERSION (ID, VERSION_LABEL, PROJECT_ID, ORIG_VERSION_ID, DESCRIPTION, ARCHIVED, DELETED, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('c1de9907-a913-41ff-8c7f-7bd67dae90ad','1.0','bcbd73f9-ea7a-49f0-abf4-d150320e8887',null,'',0,0,{ts '2016-02-09 14:16:28.181'},null,null,{ts '2016-02-09 14:17:18.644'});
insert into METL_RELATIONAL_MODEL (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, SHARED, FOLDER_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('bda155f9-a28c-432e-b7de-0d0b8366d934','69708c0e-b37c-4e4f-a8cb-e326d246f48c','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Deduper Model',0,null,{ts '2016-02-09 16:01:07.217'},null,null,{ts '2016-02-09 16:01:13.505'});
insert into METL_MODEL_ENTITY (ID, MODEL_ID, NAME, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('a36ef883-c427-4fc5-ba63-8bf99f6578a0','bda155f9-a28c-432e-b7de-0d0b8366d934','Deduper_Entity',{ts '2016-02-09 16:01:18.943'},null,null,{ts '2016-02-09 16:28:13.821'});
insert into METL_MODEL_ATTRIB (ID, ENTITY_ID, NAME, TYPE, TYPE_ENTITY_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME, PK) values ('5bc78280-10c0-43cf-8ff0-b4144aef5746','a36ef883-c427-4fc5-ba63-8bf99f6578a0','deduper_value','VARCHAR',null,{ts '2016-02-09 16:01:44.291'},null,null,{ts '2016-02-09 16:01:48.529'},0);
insert into METL_MODEL_ATTRIB (ID, ENTITY_ID, NAME, TYPE, TYPE_ENTITY_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME, PK) values ('8895e148-7ebf-46a3-8262-61e2bb41886b','a36ef883-c427-4fc5-ba63-8bf99f6578a0','deduper_cde','VARCHAR',null,{ts '2016-02-09 16:01:39.453'},null,null,{ts '2016-02-09 16:01:42.696'},0);
insert into METL_MODEL_ATTRIB (ID, ENTITY_ID, NAME, TYPE, TYPE_ENTITY_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME, PK) values ('93e1475b-a822-4c7d-b3e2-e98164b97a3c','a36ef883-c427-4fc5-ba63-8bf99f6578a0','deduper_id','INTEGER',null,{ts '2016-02-09 16:01:26.925'},null,null,{ts '2016-02-09 16:01:38.194'},1);
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('104e43fc-a8bd-431b-ba34-246978333e60','cc72b5da-7753-4226-a03d-98abd53edbdd','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Assert','Assert',null,0,'bda155f9-a28c-432e-b7de-0d0b8366d934','bda155f9-a28c-432e-b7de-0d0b8366d934',null,{ts '2016-02-10 08:25:38.877'},null,null,{ts '2016-02-10 08:27:13.914'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('16fa33e2-8ffd-411b-a738-ce4d115abb3f','b970f880-8d51-43c2-b82e-6bacbc4209f5','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Dedupe Attribute First Record','Deduper',null,0,'bda155f9-a28c-432e-b7de-0d0b8366d934',null,null,{ts '2016-02-09 16:10:56.223'},null,null,{ts '2016-02-09 16:22:36.470'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('1c9a8fbc-e203-4286-a17f-7f194fc10d8e','402fe671-d187-48c8-a544-6f45b75ad546','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Parse Delimited','Parse Delimited',null,0,null,'bda155f9-a28c-432e-b7de-0d0b8366d934',null,{ts '2016-02-09 15:59:04.737'},null,null,{ts '2016-02-09 16:04:26.468'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('281eb9a9-30ca-4795-9ca4-5181a57b4462','50b82441-f43d-4a20-9c2c-a4264c66e725','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Deduper','Deduper',null,0,'bda155f9-a28c-432e-b7de-0d0b8366d934','bda155f9-a28c-432e-b7de-0d0b8366d934',null,{ts '2016-02-09 16:02:14.576'},null,null,{ts '2016-02-09 16:04:26.473'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('49a84a54-d6a1-4d36-9d7c-fc87d2f210a5','9b007c60-b2c9-4289-9610-850906f66b09','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Check Number of Rows and Validate First Record','Script',null,0,'bda155f9-a28c-432e-b7de-0d0b8366d934',null,null,{ts '2016-02-09 16:11:08.551'},null,null,{ts '2016-02-10 08:02:13.146'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('57ad7508-90ca-473b-b809-f4b55ea513ce','d43293c4-b708-4a31-a9be-eb75b1a1da70','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Text Constant','Text Constant',null,0,null,null,null,{ts '2016-02-09 15:58:55.460'},null,null,{ts '2016-02-09 16:04:26.473'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('61829ef4-fae2-48b6-868c-ff409e4b881c','283cabb4-2cb0-400e-bc37-d101662c7c3b','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Parse Delimited','Parse Delimited',null,0,null,'bda155f9-a28c-432e-b7de-0d0b8366d934',null,{ts '2016-02-09 16:10:46.052'},null,null,{ts '2016-02-10 08:03:47.604'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('64b54607-0ad1-4f39-868b-c5c5fbe0590b','6a5ce1f6-1da6-4c70-a1ea-6ec2cec403ae','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Text Constant','Text Constant',null,0,null,null,null,{ts '2016-02-10 08:25:22.286'},null,null,{ts '2016-02-10 08:27:09.891'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('722c55c5-4081-4073-b5e6-b9a7cbde5d35','564318fa-8f89-41f9-8a29-b7fff8487c66','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Check Number of Rows','Script',null,0,null,null,null,{ts '2016-02-09 16:04:24.155'},null,null,{ts '2016-02-09 16:11:39.057'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('779d930c-cc60-410a-8b1b-334c02753928','03ed513f-a7e5-4c2f-b041-6b64309080f3','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Parse Delimited','Parse Delimited',null,0,null,'bda155f9-a28c-432e-b7de-0d0b8366d934',null,{ts '2016-02-10 08:25:29.475'},null,null,{ts '2016-02-10 08:27:09.892'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('913e8222-6105-4806-912c-17cfd04e0258','013731d5-ee31-491a-b3d5-2a5fa2d36818','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Check Number of Rows and Validate Last Record','Script',null,0,'bda155f9-a28c-432e-b7de-0d0b8366d934',null,null,{ts '2016-02-10 08:25:43.923'},null,null,{ts '2016-02-10 08:27:30.916'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('bad67043-fc64-46e5-a3a6-c05af1b23cf9','218f6ad3-50e0-490a-8849-a08d8d6ef0dd','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Assert','Assert',null,0,'bda155f9-a28c-432e-b7de-0d0b8366d934',null,null,{ts '2016-02-09 16:02:22.637'},null,null,{ts '2016-02-09 16:11:51.068'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('c4f49bb4-2c43-431a-a290-82aa5fd8e0ed','3eb09f10-c589-4414-b17a-8d27ab9d51ae','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Text Constant','Text Constant',null,0,null,null,null,{ts '2016-02-09 16:10:33.425'},null,null,{ts '2016-02-10 08:25:51.212'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('d22b8569-cc8f-4cd8-bbd1-45531fce19f9','2a45a2f1-f9f9-4449-8290-91499a197a3c','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Assert','Assert',null,0,'bda155f9-a28c-432e-b7de-0d0b8366d934','bda155f9-a28c-432e-b7de-0d0b8366d934',null,{ts '2016-02-09 16:11:04.020'},null,null,{ts '2016-02-10 08:02:16.830'});
insert into METL_COMPONENT (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, TYPE, FOLDER_ID, SHARED, INPUT_MODEL_ID, OUTPUT_MODEL_ID, RESOURCE_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('fff09270-8e66-4e94-81e6-d6dff7a1bd43','152adbd5-ca21-4290-8c74-93a909a3019f','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Dedupe Attribute Last Record','Deduper',null,0,'bda155f9-a28c-432e-b7de-0d0b8366d934','bda155f9-a28c-432e-b7de-0d0b8366d934',null,{ts '2016-02-10 08:25:36.550'},null,null,{ts '2016-02-10 08:27:09.894'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('139f3d14-66c7-4843-b985-5389d82bdcf8','bad67043-fc64-46e5-a3a6-c05af1b23cf9','expected.control.messages.count','1',{ts '2016-02-09 16:02:43.029'},null,null,{ts '2016-02-09 16:04:26.473'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('18a3ae34-a87e-4960-8a5d-9d2fb2b4b7f2','c4f49bb4-2c43-431a-a290-82aa5fd8e0ed','text','1,ABC,record1a
2,DEF,record2
1,GHI,record1b
3,JKL,record3a
3,MNO,record3b
4,PQR,record4
1,STU,record1c',{ts '2016-02-09 16:12:43.544'},null,null,{ts '2016-02-10 08:25:51.216'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('24c87033-7e4e-4b82-8e4d-bba691325461','49a84a54-d6a1-4d36-9d7c-fc87d2f210a5','init.script','ArrayList<EntityData> entityList = new ArrayList<EntityData>();

scriptContext.put("entityList", entityList);',{ts '2016-02-09 16:30:36.471'},null,null,{ts '2016-02-09 16:30:36.471'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('356fad1c-a5a5-4613-938e-49838b57677e','49a84a54-d6a1-4d36-9d7c-fc87d2f210a5','handle.msg.script','ArrayList<EntityData> list = scriptContext.get("entityList");
if (!(inputMessage instanceof ControlMessage)) {
    // put all messages into one single message array 
    ArrayList<EntityData> payload = ((EntityDataMessage)inputMessage).getPayload();
    list.addAll(payload);
    recInCount = (inputMessage.getPayload()).size();
} else if (inputMessage instanceof ControlMessage) {
    // check the count of records 
    if (recInCount != recExpectedCount) {
        throw new AssertException("Expected ${recExpectedCount} rows but received ${recInCount}.")    
    } else {
        info("Row counts match")
    }
    
    ArrayList<String> resultList = new ArrayList<String>();
    ArrayList<String> compareToList = new ArrayList<String>();
    compareToList.add("1,ABC,record1a");
    compareToList.add("2,DEF,record2");
    compareToList.add("3,JKL,record3a");
    compareToList.add("4,PQR,record4");

    info("compare to list: " + compareToList.toString());
    // check the actual content of the data received
    RelationalModel inputModel = flowStep.getComponent().getInputModel();

    if (inputModel != null) {
        ModelEntity deduperEntity = inputModel.getEntityByName("Deduper_Entity");
        ModelAttrib deduperIdAttribute = deduperEntity.getModelAttributeByName("deduper_id");
        ModelAttrib deduperCdeAttribute = deduperEntity.getModelAttributeByName("deduper_cde");
        ModelAttrib deduperValueAttribute = deduperEntity.getModelAttributeByName("deduper_value");
        
        entityDataIterator = list.iterator();

        while (entityDataIterator.hasNext()) {
            EntityData data = entityDataIterator.next();

            String deduperId = (String) data.get(deduperIdAttribute.getId());
            String deduperCde = (String) data.get(deduperCdeAttribute.getId());
            String deduperValue = (String) data.get(deduperValueAttribute.getId());
            
            resultList.add(deduperId + "," + deduperCde + "," + deduperValue);
        }
    }
    info("result list: " + resultList.toString());
    
    for (String entry : compareToList) {
        if (!resultList.contains(entry)) {
            throw new AssertException("Resulting list doesn''t match expected output. Entry: " + entry + " not in list.")    
        } 
    }
}',{ts '2016-02-09 16:21:01.125'},null,null,{ts '2016-02-10 08:16:37.080'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('3da386ef-ca56-4991-a023-68a8a754d0d7','722c55c5-4081-4073-b5e6-b9a7cbde5d35','init.script','',{ts '2016-02-09 16:05:09.419'},null,null,{ts '2016-02-09 16:08:01.688'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('45fbe17a-1ddd-416b-998d-f73b0c552bd6','913e8222-6105-4806-912c-17cfd04e0258','init.script','ArrayList<EntityData> entityList = new ArrayList<EntityData>();

scriptContext.put("entityList", entityList);',{ts '2016-02-10 08:27:59.713'},null,null,{ts '2016-02-10 08:27:59.713'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('48a30b8d-0cf1-430e-ad68-4d9bfe538de5','64b54607-0ad1-4f39-868b-c5c5fbe0590b','split.on.line.feed','true',{ts '2016-02-10 08:26:00.849'},null,null,{ts '2016-02-10 08:27:09.892'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('4d3afa2b-7501-4d8e-bb29-b72de6ab29f2','57ad7508-90ca-473b-b809-f4b55ea513ce','split.on.line.feed','true',{ts '2016-02-09 15:59:22.040'},null,null,{ts '2016-02-09 16:04:26.473'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('56d72407-91bc-4979-a775-380284abdc96','16fa33e2-8ffd-411b-a738-ce4d115abb3f','dedupe.type','ATTRIBUTE',{ts '2016-02-09 16:12:16.561'},null,null,{ts '2016-02-09 16:22:36.470'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('5814fe4d-00b2-41cf-ae2e-d69c8485cadb','104e43fc-a8bd-431b-ba34-246978333e60','expected.entity.messages.count','1',{ts '2016-02-10 08:26:57.612'},null,null,{ts '2016-02-10 08:27:09.890'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('5b71ad84-9e16-45bf-8768-2122142c2e15','722c55c5-4081-4073-b5e6-b9a7cbde5d35','imports','int recInCount = 0;
int recExpectedCount = 5;
',{ts '2016-02-09 16:08:04.858'},null,null,{ts '2016-02-09 16:08:04.858'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('70744516-4104-4c05-a36d-4cdedb996d9e','64b54607-0ad1-4f39-868b-c5c5fbe0590b','text','1,ABC,record1a
2,DEF,record2
1,GHI,record1b
3,JKL,record3a
3,MNO,record3b
2,DEF,record2
4,PQR,record4
1,STU,record1c',{ts '2016-02-10 08:26:00.044'},null,null,{ts '2016-02-10 08:28:57.464'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('742fceda-3899-41c5-85bb-c927b7952e3c','722c55c5-4081-4073-b5e6-b9a7cbde5d35','handle.msg.script','if (!(inputMessage instanceof ControlMessage)) {
    recInCount = (inputMessage.getPayload()).size();
} else if (inputMessage instanceof ControlMessage) {
    if (recInCount != recExpectedCount) {
        throw new AssertException("Expected ${recExpectedCount} rows but received ${recInCount}.")    
    } else {
        info("Row counts match")
    }
}',{ts '2016-02-09 16:04:52.290'},null,null,{ts '2016-02-09 16:09:16.658'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('7a948817-f49e-4913-a36c-991b360681af','104e43fc-a8bd-431b-ba34-246978333e60','expected.control.messages.count','1',{ts '2016-02-10 08:26:58.937'},null,null,{ts '2016-02-10 08:27:09.891'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('7ac29167-66c0-4eb3-bb3c-0e5cb358e161','49a84a54-d6a1-4d36-9d7c-fc87d2f210a5','imports','import org.jumpmind.metl.core.model.*;

import java.util.ArrayList;

int recInCount = 0;
int recExpectedCount = 4;
',{ts '2016-02-09 16:21:11.371'},null,null,{ts '2016-02-09 16:31:28.706'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('7cd86b15-60eb-43ef-83f2-35fe15c75787','bad67043-fc64-46e5-a3a6-c05af1b23cf9','expected.entity.messages.count','1',{ts '2016-02-09 16:02:41.437'},null,null,{ts '2016-02-09 16:04:26.473'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('7eed2e63-bcf4-4c73-a4f6-836a9443c4db','c4f49bb4-2c43-431a-a290-82aa5fd8e0ed','split.on.line.feed','true',{ts '2016-02-09 16:24:34.427'},null,null,{ts '2016-02-10 08:25:51.213'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('9b1dc7b9-c0aa-4072-be13-bca471ad59c7','57ad7508-90ca-473b-b809-f4b55ea513ce','text','1,ABC,TestRow1
2,DEF,TestRow2
3,GHI,TestRow3
1,ABC,TestRow1
1,ABC,TestSimilarRow1
2,DEF,TestRow2
4,JKL,TestRow4',{ts '2016-02-09 15:59:24.654'},null,null,{ts '2016-02-09 16:04:26.473'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('9f6e00bc-d5e2-4139-8bdf-86852c9004fa','d22b8569-cc8f-4cd8-bbd1-45531fce19f9','expected.control.messages.count','1',{ts '2016-02-09 16:20:47.388'},null,null,{ts '2016-02-09 16:20:47.388'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('a1b375a0-596c-4295-b465-52da75d84faa','d22b8569-cc8f-4cd8-bbd1-45531fce19f9','expected.entity.messages.count','1',{ts '2016-02-09 16:20:45.673'},null,null,{ts '2016-02-09 16:20:45.673'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('a24ff3a8-529f-4d9c-9b99-0a97582f5f5f','fff09270-8e66-4e94-81e6-d6dff7a1bd43','preserve.record','Last Record',{ts '2016-02-10 08:26:48.681'},null,null,{ts '2016-02-10 08:27:09.898'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('bba4b840-dd55-4a0b-8bf8-e2de8b5e0615','fff09270-8e66-4e94-81e6-d6dff7a1bd43','dedupe.type','ATTRIBUTE',{ts '2016-02-10 08:26:46.179'},null,null,{ts '2016-02-10 08:27:09.898'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('c6eb51fc-ca3e-4a91-984f-ccc010002f7d','16fa33e2-8ffd-411b-a738-ce4d115abb3f','preserve.record','First Record',{ts '2016-02-10 08:16:45.812'},null,null,{ts '2016-02-10 08:17:00.732'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('ea058315-55c8-4589-9d16-799bd6297c42','913e8222-6105-4806-912c-17cfd04e0258','imports','import org.jumpmind.metl.core.model.*;

import java.util.ArrayList;

int recInCount = 0;
int recExpectedCount = 4;
',{ts '2016-02-10 08:27:50.788'},null,null,{ts '2016-02-10 08:27:50.788'});
insert into METL_COMPONENT_SETTING (ID, COMPONENT_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('ff33b3f9-980a-46b1-bb7b-9e64864c3b98','913e8222-6105-4806-912c-17cfd04e0258','handle.msg.script','ArrayList<EntityData> list = scriptContext.get("entityList");
if (!(inputMessage instanceof ControlMessage)) {
    // put all messages into one single message array 
    ArrayList<EntityData> payload = ((EntityDataMessage)inputMessage).getPayload();
    list.addAll(payload);
    recInCount = (inputMessage.getPayload()).size();
} else if (inputMessage instanceof ControlMessage) {
    // check the count of records 
    if (recInCount != recExpectedCount) {
        throw new AssertException("Expected ${recExpectedCount} rows but received ${recInCount}.")    
    } else {
        info("Row counts match")
    }
    
    ArrayList<String> resultList = new ArrayList<String>();
    ArrayList<String> compareToList = new ArrayList<String>();
    compareToList.add("1,STU,record1c");
    compareToList.add("2,DEF,record2");
    compareToList.add("3,MNO,record3b");
    compareToList.add("4,PQR,record4");

    info("compare to list: " + compareToList.toString());
    // check the actual content of the data received
    RelationalModel inputModel = flowStep.getComponent().getInputModel();

    if (inputModel != null) {
        ModelEntity deduperEntity = inputModel.getEntityByName("Deduper_Entity");
        ModelAttrib deduperIdAttribute = deduperEntity.getModelAttributeByName("deduper_id");
        ModelAttrib deduperCdeAttribute = deduperEntity.getModelAttributeByName("deduper_cde");
        ModelAttrib deduperValueAttribute = deduperEntity.getModelAttributeByName("deduper_value");
        
        entityDataIterator = list.iterator();

        while (entityDataIterator.hasNext()) {
            EntityData data = entityDataIterator.next();

            String deduperId = (String) data.get(deduperIdAttribute.getId());
            String deduperCde = (String) data.get(deduperCdeAttribute.getId());
            String deduperValue = (String) data.get(deduperValueAttribute.getId());
            
            resultList.add(deduperId + "," + deduperCde + "," + deduperValue);
        }
    }
    info("result list: " + resultList.toString());
    
    for (String entry : compareToList) {
        if (!resultList.contains(entry)) {
            throw new AssertException("Resulting list doesn''t match expected output. Entry: " + entry + " not in list.")    
        } 
    }
}',{ts '2016-02-10 08:27:41.211'},null,null,{ts '2016-02-10 08:28:34.686'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('07e0221c-82c1-405c-bb24-f539639f00c6','61829ef4-fae2-48b6-868c-ff409e4b881c','8895e148-7ebf-46a3-8262-61e2bb41886b','delimited.formatter.attribute.ordinal','2',{ts '2016-02-10 08:13:46.947'},null,null,{ts '2016-02-10 08:13:46.947'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('1190d2ae-127c-433d-b95a-4f8bb10ec588','16fa33e2-8ffd-411b-a738-ce4d115abb3f','93e1475b-a822-4c7d-b3e2-e98164b97a3c','dedupe.enabled','true',{ts '2016-02-09 16:21:42.148'},null,null,{ts '2016-02-09 16:22:36.470'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('190e22c0-9962-4ec2-8548-6e1b194e0545','61829ef4-fae2-48b6-868c-ff409e4b881c','93e1475b-a822-4c7d-b3e2-e98164b97a3c','delimited.formatter.attribute.ordinal','1',{ts '2016-02-10 08:13:46.947'},null,null,{ts '2016-02-10 08:13:46.947'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('1bb8edd4-e7b3-4865-b22a-372968d744a9','779d930c-cc60-410a-8b1b-334c02753928','8895e148-7ebf-46a3-8262-61e2bb41886b','delimited.formatter.attribute.format.function','',{ts '2016-02-10 08:26:16.275'},null,null,{ts '2016-02-10 08:27:09.893'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('2abe1d1d-9463-45fd-8773-3b1f324cd417','61829ef4-fae2-48b6-868c-ff409e4b881c','5bc78280-10c0-43cf-8ff0-b4144aef5746','delimited.formatter.attribute.format.function','',{ts '2016-02-10 08:13:46.948'},null,null,{ts '2016-02-10 08:13:46.948'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('34f699d1-399b-4326-859c-9f9fb1ee4b5f','779d930c-cc60-410a-8b1b-334c02753928','93e1475b-a822-4c7d-b3e2-e98164b97a3c','delimited.formatter.attribute.format.function','',{ts '2016-02-10 08:26:16.275'},null,null,{ts '2016-02-10 08:27:09.893'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('38bbef8d-2b0d-4a3e-b393-28b946be7fc5','61829ef4-fae2-48b6-868c-ff409e4b881c','5bc78280-10c0-43cf-8ff0-b4144aef5746','delimited.formatter.attribute.ordinal','3',{ts '2016-02-10 08:13:46.948'},null,null,{ts '2016-02-10 08:13:46.948'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('3cfbaf24-2728-4c57-96c0-0389efccb417','1c9a8fbc-e203-4286-a17f-7f194fc10d8e','93e1475b-a822-4c7d-b3e2-e98164b97a3c','delimited.formatter.attribute.format.function','',{ts '2016-02-09 16:02:51.860'},null,null,{ts '2016-02-09 16:04:26.471'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('43aca1d4-a963-4dec-ae57-46b37d1cd91d','779d930c-cc60-410a-8b1b-334c02753928','5bc78280-10c0-43cf-8ff0-b4144aef5746','delimited.formatter.attribute.ordinal','3',{ts '2016-02-10 08:26:16.274'},null,null,{ts '2016-02-10 08:27:09.893'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('5f9a7d35-6879-40a5-8fef-8cf09275c368','1c9a8fbc-e203-4286-a17f-7f194fc10d8e','8895e148-7ebf-46a3-8262-61e2bb41886b','delimited.formatter.attribute.ordinal','2',{ts '2016-02-09 16:02:51.856'},null,null,{ts '2016-02-09 16:04:26.472'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('649797f7-d47e-46c0-8d40-11185fe6c7b0','779d930c-cc60-410a-8b1b-334c02753928','8895e148-7ebf-46a3-8262-61e2bb41886b','delimited.formatter.attribute.ordinal','2',{ts '2016-02-10 08:26:16.274'},null,null,{ts '2016-02-10 08:27:09.893'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('67cbd658-0c6a-438e-8ab7-e0a53c642e34','779d930c-cc60-410a-8b1b-334c02753928','5bc78280-10c0-43cf-8ff0-b4144aef5746','delimited.formatter.attribute.format.function','',{ts '2016-02-10 08:26:16.275'},null,null,{ts '2016-02-10 08:27:09.892'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('887e5bd3-17a6-4541-8a0e-d3616e614b88','779d930c-cc60-410a-8b1b-334c02753928','93e1475b-a822-4c7d-b3e2-e98164b97a3c','delimited.formatter.attribute.ordinal','1',{ts '2016-02-10 08:26:16.269'},null,null,{ts '2016-02-10 08:27:09.893'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('a09dc2cd-1a4a-4329-834e-a0e542345150','1c9a8fbc-e203-4286-a17f-7f194fc10d8e','5bc78280-10c0-43cf-8ff0-b4144aef5746','delimited.formatter.attribute.ordinal','3',{ts '2016-02-09 16:02:51.860'},null,null,{ts '2016-02-09 16:04:26.471'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('b4dd4e44-7747-4099-9c13-0e0263eedfc7','1c9a8fbc-e203-4286-a17f-7f194fc10d8e','5bc78280-10c0-43cf-8ff0-b4144aef5746','delimited.formatter.attribute.format.function','',{ts '2016-02-09 16:02:51.864'},null,null,{ts '2016-02-09 16:04:26.470'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('c8174817-bd8f-4bd0-8d12-eef4f6700ba8','1c9a8fbc-e203-4286-a17f-7f194fc10d8e','8895e148-7ebf-46a3-8262-61e2bb41886b','delimited.formatter.attribute.format.function','',{ts '2016-02-09 16:02:51.864'},null,null,{ts '2016-02-09 16:04:26.468'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('e6208804-6c2d-49d1-b65d-e774e0577cb5','61829ef4-fae2-48b6-868c-ff409e4b881c','8895e148-7ebf-46a3-8262-61e2bb41886b','delimited.formatter.attribute.format.function','',{ts '2016-02-10 08:13:46.948'},null,null,{ts '2016-02-10 08:13:46.948'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('eda37adc-d817-48ac-a60c-436a95680126','1c9a8fbc-e203-4286-a17f-7f194fc10d8e','93e1475b-a822-4c7d-b3e2-e98164b97a3c','delimited.formatter.attribute.ordinal','1',{ts '2016-02-09 16:02:51.855'},null,null,{ts '2016-02-09 16:04:26.472'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('f3585dc9-f910-40c7-8c32-98ce79f89a0a','61829ef4-fae2-48b6-868c-ff409e4b881c','93e1475b-a822-4c7d-b3e2-e98164b97a3c','delimited.formatter.attribute.format.function','',{ts '2016-02-10 08:13:46.948'},null,null,{ts '2016-02-10 08:13:46.948'});
insert into METL_COMPONENT_ATTRIB_SETTING (ID, COMPONENT_ID, ATTRIBUTE_ID, NAME, VALUE, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('f6ee73bb-5571-4852-ab0e-29d5477b24b0','fff09270-8e66-4e94-81e6-d6dff7a1bd43','93e1475b-a822-4c7d-b3e2-e98164b97a3c','dedupe.enabled','true',{ts '2016-02-10 08:27:02.985'},null,null,{ts '2016-02-10 08:27:09.895'});
insert into METL_FLOW (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, FOLDER_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME, TEST) values ('8125504a-6a51-4c05-8115-a2fac6d68df5','7a9e9574-70dc-4170-bcff-df58a4a7a5a5','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Deduper - Compare Entity',null,{ts '2016-02-09 15:56:33.506'},null,null,{ts '2016-02-09 16:04:26.468'},1);
insert into METL_FLOW (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, FOLDER_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME, TEST) values ('db0d203a-6d32-4e64-9a22-0be793bf5a0e','341829c5-0e0d-4dd1-8c88-649d287ea285','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Deduper - Compare Attribute choose last match',null,{ts '2016-02-09 15:57:17.055'},null,null,{ts '2016-02-10 08:27:09.889'},1);
insert into METL_FLOW (ID, ROW_ID, PROJECT_VERSION_ID, DELETED, NAME, FOLDER_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME, TEST) values ('ebf27a4d-cfa5-4ac7-b8ea-b771bad0db02','5b3c132f-78b9-4dbd-a3c4-367083c78760','c1de9907-a913-41ff-8c7f-7bd67dae90ad',0,'Deduper - Compare Attribute on PK',null,{ts '2016-02-09 15:56:49.963'},null,null,{ts '2016-02-09 16:11:12.210'},1);
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('091f307c-7fbb-4531-8da7-fc0168fbf99f','ebf27a4d-cfa5-4ac7-b8ea-b771bad0db02','c4f49bb4-2c43-431a-a290-82aa5fd8e0ed',70,120,0,{ts '2016-02-09 16:10:33.425'},null,null,{ts '2016-02-09 16:11:12.211'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('25517245-f18c-454c-b6c0-6fba0d58c06e','ebf27a4d-cfa5-4ac7-b8ea-b771bad0db02','49a84a54-d6a1-4d36-9d7c-fc87d2f210a5',750,120,4,{ts '2016-02-09 16:11:08.551'},null,null,{ts '2016-02-09 16:24:00.616'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('331f04e9-eeeb-4a7f-a6c2-a4b61cefd623','8125504a-6a51-4c05-8115-a2fac6d68df5','722c55c5-4081-4073-b5e6-b9a7cbde5d35',760,110,4,{ts '2016-02-09 16:04:24.156'},null,null,{ts '2016-02-09 16:04:29.194'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('33b857b0-fb48-4657-9a6e-fe035426a7ab','ebf27a4d-cfa5-4ac7-b8ea-b771bad0db02','16fa33e2-8ffd-411b-a738-ce4d115abb3f',410,120,2,{ts '2016-02-09 16:10:56.223'},null,null,{ts '2016-02-09 16:11:12.211'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('3fab95a6-5034-4288-a65d-1a0d03580e07','db0d203a-6d32-4e64-9a22-0be793bf5a0e','fff09270-8e66-4e94-81e6-d6dff7a1bd43',390,120,3,{ts '2016-02-10 08:25:36.550'},null,null,{ts '2016-02-10 08:27:09.898'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('67d04fa5-7d94-4a08-a747-ab1c768982a7','ebf27a4d-cfa5-4ac7-b8ea-b771bad0db02','61829ef4-fae2-48b6-868c-ff409e4b881c',250,120,1,{ts '2016-02-09 16:10:46.052'},null,null,{ts '2016-02-10 08:03:47.605'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('6f5d5ca9-5e54-45b7-81fb-692fdf9a5173','8125504a-6a51-4c05-8115-a2fac6d68df5','57ad7508-90ca-473b-b809-f4b55ea513ce',80,110,0,{ts '2016-02-09 15:58:55.460'},null,null,{ts '2016-02-09 16:04:26.473'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('7da4acc3-0519-451f-b496-68fa4b5e32b0','8125504a-6a51-4c05-8115-a2fac6d68df5','281eb9a9-30ca-4795-9ca4-5181a57b4462',420,110,2,{ts '2016-02-09 16:02:14.577'},null,null,{ts '2016-02-09 16:04:26.473'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('94877a76-4881-40ff-8e50-da5ae5d2daa9','ebf27a4d-cfa5-4ac7-b8ea-b771bad0db02','d22b8569-cc8f-4cd8-bbd1-45531fce19f9',580,120,3,{ts '2016-02-09 16:11:04.020'},null,null,{ts '2016-02-09 16:11:13.730'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('a2c45165-f1fc-4942-99a9-a6e916dafb70','8125504a-6a51-4c05-8115-a2fac6d68df5','bad67043-fc64-46e5-a3a6-c05af1b23cf9',590,110,3,{ts '2016-02-09 16:02:22.637'},null,null,{ts '2016-02-09 16:04:26.473'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('dddbc53d-d5dd-4ab4-a672-474304a22dff','db0d203a-6d32-4e64-9a22-0be793bf5a0e','64b54607-0ad1-4f39-868b-c5c5fbe0590b',60,120,1,{ts '2016-02-10 08:25:22.286'},null,null,{ts '2016-02-10 08:27:09.892'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('e8297534-cce8-4feb-9a38-eec66e226cb2','db0d203a-6d32-4e64-9a22-0be793bf5a0e','913e8222-6105-4806-912c-17cfd04e0258',720,120,4,{ts '2016-02-10 08:25:43.923'},null,null,{ts '2016-02-10 08:27:10.669'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('eb6e2846-51ea-49d4-82eb-71518902cffc','8125504a-6a51-4c05-8115-a2fac6d68df5','1c9a8fbc-e203-4286-a17f-7f194fc10d8e',250,110,1,{ts '2016-02-09 15:59:04.737'},null,null,{ts '2016-02-09 16:04:26.472'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('f2f2728b-2a1a-49b8-9816-b0eeca277625','db0d203a-6d32-4e64-9a22-0be793bf5a0e','104e43fc-a8bd-431b-ba34-246978333e60',560,120,4,{ts '2016-02-10 08:25:38.877'},null,null,{ts '2016-02-10 08:27:09.891'});
insert into METL_FLOW_STEP (ID, FLOW_ID, COMPONENT_ID, X, Y, APPROXIMATE_ORDER, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('f8288c74-d28c-4d9a-a077-d3d6b009bd05','db0d203a-6d32-4e64-9a22-0be793bf5a0e','779d930c-cc60-410a-8b1b-334c02753928',220,120,2,{ts '2016-02-10 08:25:29.475'},null,null,{ts '2016-02-10 08:27:09.894'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('091f307c-7fbb-4531-8da7-fc0168fbf99f','67d04fa5-7d94-4a08-a747-ab1c768982a7',{ts '2016-02-09 16:10:50.593'},null,null,{ts '2016-02-09 16:11:12.212'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('33b857b0-fb48-4657-9a6e-fe035426a7ab','94877a76-4881-40ff-8e50-da5ae5d2daa9',{ts '2016-02-09 16:11:10.179'},null,null,{ts '2016-02-09 16:11:12.212'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('3fab95a6-5034-4288-a65d-1a0d03580e07','f2f2728b-2a1a-49b8-9816-b0eeca277625',{ts '2016-02-10 08:26:51.450'},null,null,{ts '2016-02-10 08:27:09.899'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('67d04fa5-7d94-4a08-a747-ab1c768982a7','33b857b0-fb48-4657-9a6e-fe035426a7ab',{ts '2016-02-09 16:10:58.719'},null,null,{ts '2016-02-09 16:11:12.212'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('6f5d5ca9-5e54-45b7-81fb-692fdf9a5173','eb6e2846-51ea-49d4-82eb-71518902cffc',{ts '2016-02-09 15:59:09.749'},null,null,{ts '2016-02-09 16:04:26.475'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('7da4acc3-0519-451f-b496-68fa4b5e32b0','a2c45165-f1fc-4942-99a9-a6e916dafb70',{ts '2016-02-09 16:02:24.916'},null,null,{ts '2016-02-09 16:04:26.475'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('94877a76-4881-40ff-8e50-da5ae5d2daa9','25517245-f18c-454c-b6c0-6fba0d58c06e',{ts '2016-02-09 16:11:12.210'},null,null,{ts '2016-02-09 16:11:12.213'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('a2c45165-f1fc-4942-99a9-a6e916dafb70','331f04e9-eeeb-4a7f-a6c2-a4b61cefd623',{ts '2016-02-09 16:04:26.468'},null,null,{ts '2016-02-09 16:04:26.475'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('dddbc53d-d5dd-4ab4-a672-474304a22dff','f8288c74-d28c-4d9a-a077-d3d6b009bd05',{ts '2016-02-10 08:26:09.430'},null,null,{ts '2016-02-10 08:27:09.898'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('eb6e2846-51ea-49d4-82eb-71518902cffc','7da4acc3-0519-451f-b496-68fa4b5e32b0',{ts '2016-02-09 16:02:17.934'},null,null,{ts '2016-02-09 16:04:26.475'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('f2f2728b-2a1a-49b8-9816-b0eeca277625','e8297534-cce8-4feb-9a38-eec66e226cb2',{ts '2016-02-10 08:27:09.889'},null,null,{ts '2016-02-10 08:27:09.900'});
insert into METL_FLOW_STEP_LINK (SOURCE_STEP_ID, TARGET_STEP_ID, CREATE_TIME, CREATE_BY, LAST_UPDATE_BY, LAST_UPDATE_TIME) values ('f8288c74-d28c-4d9a-a077-d3d6b009bd05','3fab95a6-5034-4288-a65d-1a0d03580e07',{ts '2016-02-10 08:26:26.915'},null,null,{ts '2016-02-10 08:27:09.899'});
