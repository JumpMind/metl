package org.jumpmind.metl.ui.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class MessageSource {
	private static String RESOUCE_BUNDLE_BASENAME = "locales.metl";
	private static volatile MessageSource instance;

	private MessageSource() {}

	public static MessageSource getInstance() {
		if (instance == null) {
            synchronized (MessageSource.class) {
            	if (instance == null) {
            		instance = new MessageSource();
            	}
            }
		}
		return instance;
	}
	public static String message(String key, Object[] params, Locale locale) {
		return MessageSource.getInstance().getMessage(key, params, locale);
	}
	public static String message(String key, Locale locale) {
		return MessageSource.getInstance().getMessage(key, null, locale);
	}
	
	public String getMessage(String key, Object[] params, Locale locale) {
		ResourceBundle resource;
		if(locale == null)
			resource = ResourceBundle.getBundle(RESOUCE_BUNDLE_BASENAME);
		else
			resource = ResourceBundle.getBundle(RESOUCE_BUNDLE_BASENAME,locale);
		return MessageFormat.format(resource.getString(key), params);
	}
}
