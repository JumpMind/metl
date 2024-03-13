package org.jumpmind.metl;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = { "org.jumpmind.metl", "com.jumpmind.metl" })
public class MetlBoot {
    @Bean
    public ServletWebServerFactory servletWebServerFactory() {
        return new JettyServletWebServerFactory();
    }
    
    @Bean
    ServletRegistrationBean<DefaultServlet> docsServlet() {
        ServletRegistrationBean<DefaultServlet> bean = new ServletRegistrationBean<DefaultServlet>(new DefaultServlet(),
                "/api.html", "/ws-api.html", "/doc/*", "/ace/*");
        bean.setLoadOnStartup(1);
        return bean;
    }
    
    @Bean
    FilterRegistrationBean<HttpMethodFilter> httpMethodFilter() {
        FilterRegistrationBean<HttpMethodFilter> bean = new FilterRegistrationBean<HttpMethodFilter>();
        bean.setFilter(new HttpMethodFilter());
        bean.setAsyncSupported(true);
        bean.addUrlPatterns("/*");
        Map<String, String> param = new HashMap<String, String>();
        param.put(ServerConstants.SERVER_DISALLOW_HTTP_METHODS,
                System.getProperty(ServerConstants.SERVER_DISALLOW_HTTP_METHODS, "OPTIONS"));
        bean.setInitParameters(param);
        bean.setOrder(1);
        return bean;
    }
    
    public static ConfigurableApplicationContext run(String[] args) {
        if (System.getProperty(ServerConstants.HTTPS_ENABLE, "true").equals("true")) {
            String keyStorePassword = System.getProperty(ServerConstants.SSL_KEYSTORE_PASSWORD, "changeit");
            installSslCertIfNecessary(args, keyStorePassword);
        }
        return new SpringApplicationBuilder().registerShutdownHook(false)
                .listeners(new MetlBootPropertySetupListener(), new MetlBootStartedListener())
                .bannerMode(Banner.Mode.OFF).sources(MetlBoot.class).run(args);
    }
    
    private static File getKeyStoreFile(String[] args) {
        return new File(System.getProperty(ServerConstants.SSL_KEYSTORE_FILE, Wrapper.getConfigDir(args, false) + "/security/keystore"));
    }

    private static String getHostName(String property) {
        final String UNKNOWN = "unknown";
        String hostName = System.getProperty(property, UNKNOWN);
        if (UNKNOWN.equals(hostName)) {
            try {
                hostName = System.getenv("HOSTNAME");

                if (isBlank(hostName)) {
                    hostName = System.getenv("COMPUTERNAME");
                }

                if (isBlank(hostName)) {
                    try {
                        hostName = IOUtils.toString(Runtime.getRuntime().exec("hostname").getInputStream());
                    } catch (Exception ex) {
                    }
                }

                if (isBlank(hostName)) {
                    hostName = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()).getHostName();
                }

                if (isNotBlank(hostName)) {
                    hostName = hostName.trim();
                }

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return hostName;
    }
    
    private static KeyStore getKeyStore(String[] args, String keyPass) {
        try {
            String keyStoreType = System.getProperty(ServerConstants.SSL_KEYSTORE_TYPE, ServerConstants.SSL_DEFAULT_KEYSTORE_TYPE);
            KeyStore ks = KeyStore.getInstance(keyStoreType);
            File keyStoreFile = getKeyStoreFile(args);
            if (keyStoreFile.exists()) {
                try (FileInputStream is = new FileInputStream(keyStoreFile)) {
                    ks.load(is, keyPass.toCharArray());
                }
            } else {
                ks.load(null, keyPass.toCharArray());
            }
            return ks;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void installSslCertIfNecessary(String[] args, String keyPass) {
        try {
            String hostName = getHostName(ServerConstants.HTTPS_HOST_BIND_NAME);
            KeyStore keyStore = getKeyStore(args, keyPass);
            String alias = System.getProperty(ServerConstants.SSL_KEYSTORE_CERT_ALIAS, ServerConstants.SSL_DEFAULT_ALIAS_PRIVATE_KEY);
            KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(keyPass.toCharArray());
            Entry entry = keyStore.getEntry(alias, param);
            if (entry == null) {
                Class<?> keyPairClazz = Class.forName("sun.security.tools.keytool.CertAndKeyGen");
                Constructor<?> constructor = keyPairClazz.getConstructor(String.class, String.class);
                Object keypair = constructor.newInstance("RSA", "SHA1WithRSA");

                Class<?> x500NameClazz = Class.forName("sun.security.x509.X500Name");
                constructor = x500NameClazz.getConstructor(String.class, String.class, String.class, String.class, String.class,
                        String.class);
                Object x500Name = constructor.newInstance(hostName, "Metl", "JumpMind", "Unknown", "Unknown", "Unknown");

                keyPairClazz.getMethod("generate", Integer.TYPE).invoke(keypair, 1024);

                PrivateKey privKey = (PrivateKey) keyPairClazz.getMethod("getPrivateKey").invoke(keypair);

                X509Certificate[] chain = new X509Certificate[1];

                Date startDate = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24));
                long validTimeInMs = 100 * 365 * 24 * 60 * 60;
                chain[0] = (X509Certificate) keyPairClazz.getMethod("getSelfCertificate", x500NameClazz, Date.class, Long.TYPE)
                        .invoke(keypair, x500Name, startDate, validTimeInMs);

                keyStore.setKeyEntry(alias, privKey, keyPass.toCharArray(), chain);

                File keyStoreFile = getKeyStoreFile(args);
                keyStoreFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
                    keyStore.store(fos, keyPass.toCharArray());
                }
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void main(String[] args) {
        run(args);
    }
}
