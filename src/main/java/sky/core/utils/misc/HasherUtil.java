package sky.core.utils.misc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HasherUtil {
    private static final String SECRET_KEY = "kotopesprot";
    private static final byte[] MAGIC_BYTES = {(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE};

    public static String encrypt(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        try {
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = new byte[dataBytes.length];
            
            for (int i = 0; i < dataBytes.length; i++) {
                encrypted[i] = (byte) (dataBytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            
            byte[] finalData = new byte[MAGIC_BYTES.length + encrypted.length];
            System.arraycopy(MAGIC_BYTES, 0, finalData, 0, MAGIC_BYTES.length);
            System.arraycopy(encrypted, 0, finalData, MAGIC_BYTES.length, encrypted.length);
            
            return Base64.getEncoder().encodeToString(finalData);
            
        } catch (Exception e) {
            return data;
        }
    }

    public static String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        
        try {
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            
            if (decodedData.length < MAGIC_BYTES.length) {
                return encryptedData;
            }
            
            boolean hasMagic = true;
            for (int i = 0; i < MAGIC_BYTES.length; i++) {
                if (decodedData[i] != MAGIC_BYTES[i]) {
                    hasMagic = false;
                    break;
                }
            }
            
            if (!hasMagic) {
                return encryptedData;
            }
            
            byte[] encrypted = new byte[decodedData.length - MAGIC_BYTES.length];
            System.arraycopy(decodedData, MAGIC_BYTES.length, encrypted, 0, encrypted.length);
            
            byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] decrypted = new byte[encrypted.length];
            
            for (int i = 0; i < encrypted.length; i++) {
                decrypted[i] = (byte) (encrypted[i] ^ keyBytes[i % keyBytes.length]);
            }
            
            return new String(decrypted, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            return encryptedData;
        }
    }
}