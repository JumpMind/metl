package org.jumpmind.metl.ui.common;

import java.io.Serializable;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.symmetric.ui.sqlexplorer.IDb;

public class DbResource implements IDb, Serializable {

        private static final long serialVersionUID = 1L;

        IResourceRuntime resource;

        Agent agent;

        IDatabasePlatform platform;

        public DbResource(Agent agent, IResourceRuntime resource) {
            this.resource = resource;
            this.agent = agent;
        }

        @Override
        public String getName() {
            return agent.getName() + " > " + resource.getResource().getName();
        }

        @Override
        public IDatabasePlatform getPlatform() {
            if (platform == null) {                
                DataSource dataSource = resource.reference();
                platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource,
                        new SqlTemplateSettings(), false, false);
            }
            return platform;
        }
        
        public void close() {
            if (platform != null) {
                BasicDataSource ds = (BasicDataSource)platform.getDataSource();
                if (ds != null) {
                    try {
                        ds.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }
        
        public Agent getAgent() {
            return agent;
        }

        public IResourceRuntime getResource() {
            return resource;
        }

        @Override
        public int hashCode() {
            return resource.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DbResource) {
                return resource.equals(((DbResource) obj).getResource());
            } else {
                return super.equals(obj);
            }
        }

    }