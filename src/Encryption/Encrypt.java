package Encryption;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
public class Encrypt {
    private static  Key key ;

    public static SecretKey getKey( String keyAsString ) {
        try{
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] aesKey = messageDigest.digest(keyAsString.getBytes(UTF_8));
            aesKey = Arrays.copyOf(aesKey, 16);
            return new SecretKeySpec(aesKey, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null ;
    }

    public static SecretKey generateSecretKey() {
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert keyGenerator != null;
        keyGenerator.init(128);
        return keyGenerator.generateKey();
    }

    public static String encryptSecretKey(SecretKey secretKey , PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance( "RSA" );
            cipher.init( Cipher.ENCRYPT_MODE,publicKey );
            return Base64.getEncoder().encodeToString(cipher.doFinal( secretKey.getEncoded() )) ;
        } catch (NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return "" ;
    }

    public static SecretKey decryptSecretKey(byte[] encryptedSecretKey, PrivateKey privateKey ) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init( Cipher.DECRYPT_MODE,privateKey );
            byte[] decryptedSecretKey = cipher.doFinal( encryptedSecretKey );
            return new SecretKeySpec(decryptedSecretKey,"AES");
        } catch (NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null  ;
    }

    public static String encryptMessage( Key key, Object message ) {
        String encryptedMessage = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, (SecretKey) key);
            encryptedMessage = Base64.getEncoder().encodeToString(cipher.doFinal(String.valueOf(message).getBytes(UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedMessage;
    }

    public static String decryptMessage(Key key, String encryptedMessage) {
        byte[] byteData = null;
        try {
            byteData = Base64.getDecoder().decode(encryptedMessage);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, (SecretKey) key);
            return new String(cipher.doFinal(byteData), UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String sign(String message , PrivateKey privateKey) {
        try {
            Signature privateSignature = Signature.getInstance("SHA256withRSA");
            privateSignature.initSign(privateKey);
            privateSignature.update(message.getBytes(UTF_8));
            byte[] signature = privateSignature.sign();
            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        return "" ;
    }

    public static boolean verify(String message, String signature, PublicKey publicKey){
        try {
            Signature publicSignature = Signature.getInstance("SHA256withRSA");
            publicSignature.initVerify(publicKey);
            publicSignature.update(message.getBytes(UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return publicSignature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        return false ;
    }
}
