package com.example.demo.util;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class CryptoUtil {


    /**
     * Encryption algorithm
     */
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    /**
     * Key algorithm
     */
    private static final String KEY_ALGORITHM = "AES";
    /**
     * AES key length
     */
    private static final int SECRET_KEY_LEN = 128;

    public static String sign(String data, String privateKeyStr) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(getPrivateKey(privateKeyStr));
        signature.update(data.getBytes());
        byte[] signData = signature.sign();
        return java.util.Base64.getEncoder().encodeToString(signData);
    }


    public static boolean verify(String data, String sign, PublicKey publicKey) throws Exception {
        return verify(data.getBytes(), java.util.Base64.getDecoder().decode(sign), publicKey);
    }


    public static boolean verify(byte[] data, byte[] sign, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(sign);
    }

    public static PublicKey getPublicKey(String publicKeyStr) throws Exception {
        X509EncodedKeySpec x509KeySpec =
                new X509EncodedKeySpec(org.apache.commons.codec.binary.Base64.decodeBase64(publicKeyStr));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(x509KeySpec);
    }

    public static PrivateKey getPrivateKey(String privateKeyStr) throws Exception {
        PKCS8EncodedKeySpec pkcs8KeySpec =
                new PKCS8EncodedKeySpec(org.apache.commons.codec.binary.Base64.decodeBase64(privateKeyStr));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(pkcs8KeySpec);
    }

    public static String encryptByPublicKey(String aesKey, String publicKey) throws Exception {
        X509EncodedKeySpec x509KeySpec =
                new X509EncodedKeySpec(org.apache.commons.codec.binary.Base64.decodeBase64(publicKey));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Key pubKey = keyFactory.generatePublic(x509KeySpec);
        //encrypt
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return org.apache.commons.codec.binary.Base64.encodeBase64String(cipher.doFinal((aesKey.getBytes())));
    }


    public static String getSortedData(SortedMap<String, String> input) {
        List<String> keyList = new ArrayList<>(input.keySet());
        keyList.sort(Comparator.naturalOrder());
        return sortData(input, keyList);
    }



    private static String sortData(Map<String, String> input, List<String> keyList) {
        //  key1=value1&key2=value
        StringBuilder sb = new StringBuilder();
        for (String key : keyList) {
            if (StringUtils.isBlank(key)) {
                continue;
            }
            String value = input.get(key);
            if (StringUtils.isBlank(value)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(key.trim()).append("=").append(value);
        }
        return sb.toString();
    }


    public static String decryptByPrivateRSA(String data, String privateKey) {
        try {
            PKCS8EncodedKeySpec pkcs8KeySpec =
                    new PKCS8EncodedKeySpec(org.apache.commons.codec.binary.Base64.decodeBase64(privateKey));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            Key priKey = keyFactory.generatePrivate(pkcs8KeySpec);
            //decrypt
            Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            return new String(cipher.doFinal(org.apache.commons.codec.binary.Base64.decodeBase64(data)),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encrypt(String plainText, String key) throws Exception {
        String iv = key;
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes("UTF-8"));
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));
        return java.util.Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decrypt(String cipherText, String key) throws Exception {
        String iv = key;
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes("UTF-8"));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(plainText, "UTF-8");
    }
}