package org.jumpmind.symmetric.is.ui.init;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import org.jumpmind.symmetric.is.ui.common.DesignAgentSelect;
import org.jumpmind.symmetric.is.ui.common.TopBar;
import org.jumpmind.symmetric.is.ui.common.ViewManager;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.DefaultConverterFactory;
import com.vaadin.data.util.converter.StringToBigDecimalConverter;
import com.vaadin.data.util.converter.StringToBooleanConverter;
import com.vaadin.data.util.converter.StringToDateConverter;
import com.vaadin.data.util.converter.StringToDoubleConverter;
import com.vaadin.data.util.converter.StringToFloatConverter;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.util.converter.StringToLongConverter;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Theme("apptheme")
@Title("SymmetricIS")
@PreserveOnRefresh
public class AppUI extends UI {

    private static final long serialVersionUID = 1L;
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    ViewManager viewManager;
    
    BackgroundRefresherService backgroundRefresherService;

    @SuppressWarnings("serial")
	@Override
    protected void init(VaadinRequest request) {
        
        setPollInterval(5000);
        
        VerticalLayout root = new VerticalLayout();
        root.setSizeFull();
        setContent(root);

        VerticalLayout contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        
        WebApplicationContext ctx = getWebApplicationContext();
        
        backgroundRefresherService = ctx.getBean(BackgroundRefresherService.class);
        backgroundRefresherService.init(this);
        
        viewManager = ctx.getBean(ViewManager.class);
        viewManager.init(this, contentArea);
        
        DesignAgentSelect designAgentSelect = ctx.getBean(DesignAgentSelect.class);

        TopBar menu = new TopBar(viewManager, designAgentSelect);

        root.addComponents(menu, contentArea);
        root.setExpandRatio(contentArea, 1);

        UI.getCurrent().setErrorHandler(new DefaultErrorHandler() {
            public void error(com.vaadin.server.ErrorEvent event) {
                String intro = "Exception of type <b>";
                String message = "";
                for (Throwable t = event.getThrowable(); t != null; t = t.getCause()) {
                    if (t.getCause() == null) {
                        intro += t.getClass().getName() + "</b> with the following message:<br/><br/>";
                        message = t.getMessage();
                    }
                }
                ErrorWindow window = new ErrorWindow(intro, message);
                window.show();
                
                Throwable ex = event.getThrowable();
                if (ex != null) {
                    log.error(ex.getMessage(), ex);
                } else {
                    log.error("An unexpected error occurred");
                }
            } 
        });
        
        VaadinSession.getCurrent().setConverterFactory(new DefaultConverterFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            protected Converter<Date, ?> createDateConverter(Class<?> sourceType) {
                return super.createDateConverter(sourceType);
            }
            
            protected Converter<String, ?> createStringConverter(Class<?> sourceType) {
                if (Double.class.isAssignableFrom(sourceType)) {
                    return new StringToDoubleConverter();
                } else if (Float.class.isAssignableFrom(sourceType)) {
                    return new StringToFloatConverter();
                } else if (Integer.class.isAssignableFrom(sourceType)) {
                    return new StringToIntegerConverter() {
                      private static final long serialVersionUID = 1L;
                    @Override
                        protected NumberFormat getFormat(Locale locale) {
                            NumberFormat format = super.getFormat(locale);
                            format.setGroupingUsed(false);
                            return format;
                        }  
                    };
                } else if (Long.class.isAssignableFrom(sourceType)) {
                    return new StringToLongConverter() {
                        private static final long serialVersionUID = 1L;
                      @Override
                          protected NumberFormat getFormat(Locale locale) {
                              NumberFormat format = super.getFormat(locale);
                              format.setGroupingUsed(false);
                              return format;
                          }  
                      };
                } else if (BigDecimal.class.isAssignableFrom(sourceType)) {
                    return new StringToBigDecimalConverter();
                } else if (Boolean.class.isAssignableFrom(sourceType)) {
                    return new StringToBooleanConverter();
                } else if (Date.class.isAssignableFrom(sourceType)) {
                    return new StringToDateConverter();
                } else {
                    return null;
                }
            }

            
        });        

        Responsive.makeResponsive(this);
    }

    public WebApplicationContext getWebApplicationContext() {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(VaadinServlet
                .getCurrent().getServletContext());
    }
    
    @Override
    public void detach() {
        if (backgroundRefresherService != null) {
            backgroundRefresherService.destroy();
        }
        super.detach();

    }
    
    @SuppressWarnings({ "serial" })
    class ErrorWindow extends ResizableWindow {
    	public ErrorWindow(String intro, String message) {
    		super("Error");
    		setWidth(600f, Unit.PIXELS);
    		setHeight(300f, Unit.PIXELS);
    		content.setMargin(true);
    		
    		HorizontalLayout layout = new HorizontalLayout();
    		Label icon = new Label();
    		icon.setIcon(new ThemeResource("images/error.png"));
    		icon.setWidth(70f, Unit.PIXELS);
    		layout.addComponent(icon);
    		
    		Label labelIntro = new Label(intro, ContentMode.HTML);
    		labelIntro.setStyleName("large");
    		labelIntro.setWidth(530f, Unit.PIXELS);
    		layout.addComponent(labelIntro);
    		addComponent(layout);

            TextArea textField = new TextArea();
            textField.setSizeFull();
            textField.setWordwrap(false);
            textField.setValue(message);
    		addComponent(textField);
    		content.setExpandRatio(textField, 1.0f);
    		
    		addComponent(buildButtonFooter(buildCloseButton()));
    	}
    }

}
