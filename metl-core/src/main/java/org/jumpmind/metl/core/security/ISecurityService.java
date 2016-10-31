package org.jumpmind.metl.core.security;

import java.security.KeyStore;

/**
 * Pluggable Service API that is responsible for encrypting and decrypting data.
 */
public interface ISecurityService {
    
    public String nextSecureHexString(int len);

    public String encrypt(String plainText);
    
    public String decrypt(String encText);
    
    public String obfuscate(String plainText);
    
    public String unobfuscate(String obfText);
    
    public KeyStore getKeyStore();
    
    public String hash(String salt, String password);
    
    public void setConfigDir(String dir);
    
}