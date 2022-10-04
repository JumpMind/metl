/* adapted for Vaadin 8 from com.cybercom:spring-ui-scope:0.0.2 */
package org.jumpmind.metl.vaadin.spring;

import com.vaadin.server.ClientConnector;
import com.vaadin.shared.Registration;
import com.vaadin.ui.UI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;

/* this is a stop-gap measure; not intended to exist beyond upgrade activities */
@Deprecated(forRemoval = true)
public class UIScope implements Scope, ClientConnector.DetachListener, BeanFactoryPostProcessor {
    private static final Object MUTEX = new Object();

    private final Map<Integer, Map<String, Object>> objectMap = new HashMap<>();

    private final Map<Integer, Registration> regMap = new HashMap<>();

    private final Map<Integer, Map<String, Runnable>> destructionCallbackMap = new HashMap<>();

    @Override
    public Object get(final String name, final ObjectFactory<?> objectFactory) {
	UI ui = (UI) resolveContextualObject("ui");
	Integer uiId = ui.getUIId();

        Map<String, Object> uiSpace;
        synchronized (MUTEX) {
            uiSpace = objectMap.get(uiId);
            if (uiSpace == null) {
                Registration reg = ui.addDetachListener(this);
                regMap.put(uiId, reg);

                uiSpace = new HashMap<String, Object>();
                objectMap.put(uiId, uiSpace);
            }
        }

        synchronized (MUTEX) {
            Object bean = uiSpace.get(name);
            if (bean == null) {
                bean = objectFactory.getObject();
                uiSpace.put(name, bean);
            }

            return bean;
        }
    }

    @Override
    public String getConversationId() {
        UI ui = (UI) resolveContextualObject("ui");
        if (ui != null) {
            Integer uiId = ui.getUIId();
            return (uiId != null) ? uiId.toString() : null;
        }
        return null;
    }

    @Override
    public void registerDestructionCallback(final String name, final Runnable callback) {
	UI ui = (UI) resolveContextualObject("ui");
	Integer uiId = ui.getUIId();

        synchronized (MUTEX) {
            Map<String, Runnable> destructionSpace = destructionCallbackMap.get(uiId);
            if (destructionSpace == null) {
                destructionSpace = new LinkedHashMap<>();
                destructionCallbackMap.put(uiId, destructionSpace);
            }
            destructionSpace.put(name, callback);
        }
    }

    @Override
    public Object remove(final String name) {
	UI ui = (UI) resolveContextualObject("ui");
	Integer uiId = ui.getUIId();

        synchronized (MUTEX) {
            Map<String, Runnable> destructionSpace = destructionCallbackMap.get(uiId);
            if (destructionSpace != null) {
                destructionSpace.remove(name);
            }

            Map<String, Object> uiSpace = objectMap.get(uiId);
            return (uiSpace != null) ? uiSpace.remove(name) : null;
        }
    }

    @Override
    public Object resolveContextualObject(final String key) {
        switch (key) {
        case "ui":
            return UI.getCurrent();
        default:
            return null;
        }
    }

    @Override
    public void detach(final ClientConnector.DetachEvent event) {
	UI ui = (UI) event.getSource();
	Integer uiId = ui.getUIId();

        Map<String, Runnable> destructionSpace;
        synchronized (MUTEX) {
            destructionSpace = destructionCallbackMap.remove(uiId);

            objectMap.remove(uiId);

            Registration reg = regMap.remove(uiId);
            if (reg != null) {
                reg.remove();
            }
        }

        if (destructionSpace != null) {
            for (Runnable destructor : destructionSpace.values()) {
                destructor.run();
            }
        }
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.registerScope("ui", this);
    }
}

