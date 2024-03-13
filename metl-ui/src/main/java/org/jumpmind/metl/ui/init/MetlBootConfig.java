package org.jumpmind.metl.ui.init;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.spring.annotation.EnableVaadin;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

@Configuration
@EnableVaadin(value = {"org.jumpmind.metl", "com.jumpmind.metl"})
public class MetlBootConfig {
    @Bean
    ServletContextInitializer uiServletContextInitializer() {
        return new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                servletContext.setInitParameter("productionMode", "false");
            }
        };
    }
    
    @Bean
    AppInitializer appInitializer() {
        return new AppInitializer();
    }
    
    @Bean
    ServletRegistrationBean<VaadinServlet> vaadinServlet() {
        ServletRegistrationBean<VaadinServlet> bean = new ServletRegistrationBean<VaadinServlet>(new VaadinServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void servletInitialized() throws ServletException {
                super.servletInitialized();
                getService().addSessionInitListener(new AppSessionInitListener());
            }
        });
        bean.addInitParameter("widgetset", "org.jumpmind.metl.ui.AppWidgetSet");
        bean.addInitParameter("org.atmosphere.cpr.asyncSupport", "org.atmosphere.container.JSR356AsyncSupport");
        bean.addUrlMappings("/*");
        bean.setAsyncSupported(true);
        return bean;
    }
}
