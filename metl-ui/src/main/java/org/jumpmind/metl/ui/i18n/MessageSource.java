package org.jumpmind.metl.ui.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class MessageSource {
	private static String RESOUCE_BUNDLE_BASENAME = "locales.";
	private static String RESOUCE_BUNDLE_DEFAULTNAME = "metl";
	private static volatile MessageSource instance;
	private static final ThreadLocal<Locale> LOCALE = new ThreadLocal<Locale>();

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
	
	public static void setLocale(Locale locale) {
		LOCALE.set(locale);
	}

	public static String messageWithDefault(String bundle, String key, String defaultMessage) {
		String msg = "";
		try {
			msg = MessageSource.getInstance().getMessage(bundle,key, null, LOCALE.get());
		}catch(java.util.MissingResourceException e) {
			msg = defaultMessage==null?"":defaultMessage;
		}
		return msg;
	}

	public static String messageWithDefault(String key, String defaultMessage) {
		String msg = "";
		try {
			msg = MessageSource.getInstance().getMessage(null,key, null, LOCALE.get());
		}catch(java.util.MissingResourceException e) {
			msg = defaultMessage==null?"":defaultMessage;
		}
		return msg;
	}
	
	public static String message(String key) {
		return MessageSource.getInstance().getMessage(null,key, null, LOCALE.get());
	}

	public static String message(String key, Object[] params) {
		return MessageSource.getInstance().getMessage(null, key, params, LOCALE.get());
	}

	public static String message(String key, Locale locale) {
		return MessageSource.getInstance().getMessage(null, key, null, locale);
	}
	
	public static String message(String key, Object[] params, Locale locale) {
		return MessageSource.getInstance().getMessage(null, key, params, locale);
	}

	public static String message(String bundle, String key) {
		return MessageSource.getInstance().getMessage(bundle,key, null, LOCALE.get());
	}

	public static String message(String bundle, String key, Object[] params) {
		return MessageSource.getInstance().getMessage(bundle, key, params, LOCALE.get());
	}

	public static String message(String bundle, String key, Locale locale) {
		return MessageSource.getInstance().getMessage(bundle, key, null, locale);
	}
	
	public static String message(String bundle, String key, Object[] params, Locale locale) {
		return MessageSource.getInstance().getMessage(bundle, key, params, locale);
	}
	public String getMessage(String bundle, String key, Object[] params, Locale locale) {
		ResourceBundle resource;
		bundle = RESOUCE_BUNDLE_BASENAME+(bundle==null?RESOUCE_BUNDLE_DEFAULTNAME:bundle);
		if(locale == null)
			resource = ResourceBundle.getBundle(bundle);
		else
			resource = ResourceBundle.getBundle(bundle,locale);
		return MessageFormat.format(resource.getString(key), params);
	}
}
