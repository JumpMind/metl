package org.jumpmind.metl.core.runtime.component;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;


public class PgpEncrypt extends AbstractComponentRuntime {

    public final static String PUBLIC_KEY_LOCATION = "pgp.public.key.location";
    public final static String KEY_ALGORITHM = "pgp.key.algorithm";
    public final static String COMPRESSION_ALGORITHM = "pgp.compression.algorithm";
    public final static String ARMORED = "pgp.armored";
    
    String publicKeyLocation;
    int keyAlgorithm;
    int compressionAlgorithm;
    boolean armored;
    PGPPublicKey pubKey;
    
    @Override
    public void start() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        TypedProperties properties = getTypedProperties();
        publicKeyLocation = properties.get(PUBLIC_KEY_LOCATION);
        keyAlgorithm = mapKeyAlgorithm(properties.get(KEY_ALGORITHM));
        compressionAlgorithm = mapCompressionAlgorithm(properties.get(COMPRESSION_ALGORITHM));
        armored = properties.is(ARMORED, false);
        try {
            pubKey = readPublicKey();
        } catch (IOException iox) {
            log.error(String.format("Unable to read public key from keyfile.  Error %s",iox.getMessage()));
            throw new IoException(iox);
        } catch (PGPException pgx) {
            log.error(String.format("Unable to read public key from keyfile.  Error %s",pgx.getMessage()));
            throw new IoException(pgx);
        }
    }
    
    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {   
        
        if (inputMessage instanceof BinaryMessage) {
            byte[] inputPayload = ((BinaryMessage) inputMessage).getPayload();
            byte[] outputPayload = encrypt(inputPayload);
            callback.sendBinaryMessage(inputMessage.getHeader(), outputPayload);
        }
        
    }

    private PGPPublicKey readPublicKey() throws IOException, PGPException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(publicKeyLocation));

        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
            PGPUtil.getDecoderStream(inputStream), new JcaKeyFingerprintCalculator());

        Iterator<PGPPublicKeyRing> keyRingIter = pgpPub.getKeyRings();
        while (keyRingIter.hasNext()) {
            PGPPublicKeyRing keyRing = (PGPPublicKeyRing)keyRingIter.next();

            Iterator<PGPPublicKey> keyIter = keyRing.getPublicKeys();
            while (keyIter.hasNext()) {
                PGPPublicKey key = (PGPPublicKey)keyIter.next();
                if (key.isEncryptionKey()) {
                    return key;
                }
            }
        }
        throw new IllegalArgumentException("Unable to find valid key in the key file.");
    }

    private byte[] encrypt(byte[] inData) {        

        byte[] compressedData = compress(inData, PGPLiteralData.CONSOLE, keyAlgorithm);
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();        
        PGPEncryptedDataGenerator encDataGen = new PGPEncryptedDataGenerator(new JcePGPDataEncryptorBuilder(keyAlgorithm).setProvider("BC"));        
        encDataGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(pubKey));
        OutputStream outputStream = baOutputStream;
        if (armored) {
            outputStream = new ArmoredOutputStream(outputStream);
        }
        try {
            OutputStream encoudedOutputStream = encDataGen.open(outputStream, compressedData.length);
            encoudedOutputStream.write(compressedData);
            encoudedOutputStream.close();
            if (armored) {
                outputStream.close();
            }
        } catch(IOException iox) {
            throw new IoException(iox);
        } catch(PGPException pex) {
            throw new IoException(pex);
        }
        return baOutputStream.toByteArray();
    }
    
    private byte[] compress(byte[] inData, String fileName, int algorithm) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PGPCompressedDataGenerator comDataGen = new PGPCompressedDataGenerator(algorithm);
        PGPLiteralDataGenerator litDataGen = new PGPLiteralDataGenerator();
        try {
            OutputStream cos = comDataGen.open(bOut); 
            OutputStream  pOut = litDataGen.open(cos, PGPLiteralData.BINARY, fileName,  
                    inData.length, new Date());
            pOut.write(inData);
            pOut.close();
            comDataGen.close();
        } catch (IOException iox) {
            throw new IoException(iox);
        }
        return bOut.toByteArray();
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    private int mapKeyAlgorithm(String keyAlgorithmName) {
        int keyAlgorithm = -1;
        
        switch(keyAlgorithmName) {
            case "IDEA":
                keyAlgorithm = SymmetricKeyAlgorithmTags.IDEA;
                break;
            case "TRIPLE_DES":
                keyAlgorithm = SymmetricKeyAlgorithmTags.TRIPLE_DES;
                break;
            case "CAST5":
                keyAlgorithm = SymmetricKeyAlgorithmTags.CAST5;
                break;
            case "BLOWFISH":
                keyAlgorithm = SymmetricKeyAlgorithmTags.BLOWFISH;
                break;
            case "SAFER":
                keyAlgorithm = SymmetricKeyAlgorithmTags.SAFER;
                break;
            case "DES":
                keyAlgorithm = SymmetricKeyAlgorithmTags.DES;
                break;
            case "AES_128":
                keyAlgorithm = SymmetricKeyAlgorithmTags.AES_128;
                break;
            case "AES_192":
                keyAlgorithm = SymmetricKeyAlgorithmTags.AES_192;
                break;
            case "AES_256":
                keyAlgorithm = SymmetricKeyAlgorithmTags.AES_256;
                break;
            case "TWOFISH":
                keyAlgorithm = SymmetricKeyAlgorithmTags.TWOFISH;
                break;
            case "CAMELLIA_128":
                keyAlgorithm = SymmetricKeyAlgorithmTags.CAMELLIA_128;
                break;
            case "CAMELLIA_192":
                keyAlgorithm = SymmetricKeyAlgorithmTags.CAMELLIA_192;
                break;
            case "CAMELLIA_256":
                keyAlgorithm = SymmetricKeyAlgorithmTags.CAMELLIA_256;
                break;
        }
        return keyAlgorithm;
    }

    private int mapCompressionAlgorithm(String compressionAlgorithmName) {
        int compressionAlgorithm = -1;
        
        switch(compressionAlgorithmName) {
            case "ZIP":
                compressionAlgorithm = CompressionAlgorithmTags.ZIP;
                break;
            case "ZLIB":
                compressionAlgorithm = CompressionAlgorithmTags.ZLIB;
                break;
            case "BZIP2":
                compressionAlgorithm = CompressionAlgorithmTags.BZIP2;
                break;
        }
        return compressionAlgorithm;
    }

}
