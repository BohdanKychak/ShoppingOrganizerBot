package org.example.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static org.example.util.Constants.ZERO;

@Slf4j
@Component
public class EncryptionService {
    private static final String ALGORITHM = "AES";

    private static final int MIN_LENGTH = 16; // Minimum length for an AES key
    private static final int MED_LENGTH = 24; // Medium length for an AES key
    private static final int MAX_LENGTH = 32; // Maximum length for an AES key

    public static String encrypt(String text, String key) throws Exception {
        log.debug("Encrypting text: {}", text);
        String keyWithRightLength = getKeyWithRightLength(key);
        if (keyWithRightLength == null) {
            return text;
        }

        SecretKeySpec secretKey = new SecretKeySpec(
                keyWithRightLength.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(text.getBytes());
        String result = bytesToHex(encryptedBytes);

        log.debug("Encrypted text (HEX): {}", result);
        return result; // Generate HEX
    }

    public static String decrypt(String encryptedText, String key) throws Exception {
        log.debug("Decrypting text: {}", encryptedText);
        String keyWithRightLength = getKeyWithRightLength(key);
        if (keyWithRightLength == null) {

            return encryptedText;
        }

        SecretKeySpec secretKey = new SecretKeySpec(
                keyWithRightLength.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(hexToBytes(encryptedText));
        String result = new String(decryptedBytes);

        log.debug("Decrypted text: {}", result);
        return result;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b)); // HEX without `_`
        }
        return hexString.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static String getKeyWithRightLength(String key) {
        if (key.length() <= MIN_LENGTH) {
            return adjustKeyLength(key, MIN_LENGTH);
        }
        if (key.length() <= MED_LENGTH) {
            return adjustKeyLength(key, MED_LENGTH);
        }
        if (key.length() <= MAX_LENGTH) {
            return adjustKeyLength(key, MAX_LENGTH);
        }
        log.error("Key length is not valid. Key: {}", key);
        return null;
    }

    private static String adjustKeyLength(String input, int targetLength) {
        if (input.length() >= targetLength) {
            return input; // if the input is already long enough, return it as is
        }
        StringBuilder paddedString = new StringBuilder();
        while (paddedString.length() + input.length() < targetLength) {
            paddedString.append(ZERO); // adding zeros to the beginning
        }
        paddedString.append(input); // adding the original string
        return paddedString.toString();
    }
}