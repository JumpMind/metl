/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
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
                        hostName = IOUtils.toString(Runtime.getRuntime().exec("hostname").getInputStream(), Charset.defaultCharset());
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
            	BouncyCastleHelper helper = new BouncyCastleHelper();
                helper.checkProviderInstalled();
                KeyPair pair = helper.generateRSAKeyPair();
                String certString = String.format("CN=%s, OU=Metl, O=Jumpmind", hostName);
                SubjectPublicKeyInfo publicKeyInfo = helper.getInstance(pair.getPublic());
                X509v1CertificateBuilder builder = new X509v1CertificateBuilder(new X500Name(certString), BigInteger.valueOf(System.currentTimeMillis()),
                        new Date(System.currentTimeMillis() - 86400000), new Date(System.currentTimeMillis() + 788400000000l), new X500Name(certString),
                        publicKeyInfo);
                AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WithRSAEncryption");
                AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
                ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(helper.createKey(pair.getPrivate()));
                X509CertificateHolder holder = builder.build(signer);
                X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);
                X509Certificate[] serverChain = new X509Certificate[] { cert };
                PrivateKeyEntry privateEntry = new PrivateKeyEntry(pair.getPrivate(), serverChain);
                keyStore.setEntry(alias, privateEntry, param);
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
