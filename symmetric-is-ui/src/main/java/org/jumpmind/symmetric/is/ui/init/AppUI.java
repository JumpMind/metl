package org.jumpmind.symmetric.is.ui.init;

import org.jumpmind.symmetric.is.ui.common.DesignAgentSelect;
import org.jumpmind.symmetric.is.ui.common.TopBar;
import org.jumpmind.symmetric.is.ui.common.ViewManager;
import org.jumpmind.symmetric.ui.common.AbstractSpringUI;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Theme("apptheme")
@Title("SymmetricIS")
@PreserveOnRefresh
public class AppUI extends AbstractSpringUI {

    private static final long serialVersionUID = 1L;

    ViewManager viewManager;
    
    BackgroundRefresherService backgroundRefresherService;

    @SuppressWarnings("serial")
	@Override
    protected void init(VaadinRequest request) {

        super.init(request);
        
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
                doDefault(event);
            } 
        });
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
    		Label labelMessage = new Label(message, ContentMode.HTML);
    		addComponent(labelMessage);
    		content.setExpandRatio(labelMessage, 1.0f);
    		addComponent(buildButtonFooter(buildCloseButton()));
    	}
    }

}
