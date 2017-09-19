package chat.server;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

public class AsymetricKey_ServerSide {

    private final int iterations = 16384;
/*
    public static void createLogger(String name) {
        try {
            Logger logger = Logger.getLogger(name);
            FileHandler fh;
            fh = new FileHandler("Logs/" + name + ".log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.setLevel(logger.getLevel());
            System.out.println(name + " file was created");
        } catch (IOException | SecurityException ex) {
            System.out.println(ex);
        }
    }*/
    public byte[] iv;//Remember to send this with every message

    public String getUserKey(String password, String encodedKey1, String verifyKey3, String saltStr, String ivStr) {
        try {
            byte[] salt = getDecoder().decode(saltStr);
            byte[] iv = getDecoder().decode(ivStr);
            //Creates key2 and 3
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            SecretKey secretKey = skf.generateSecret(spec);
            byte[] key = secretKey.getEncoded();

            //Splits key2 and3
            byte[] key2 = Arrays.copyOfRange(key, 0, key.length / 2);
            byte[] key3 = Arrays.copyOfRange(key, key.length / 2, key.length);
            String key3Str = getEncoder().encodeToString(key3);
            if (!key3Str.equals(verifyKey3)) {
                System.out.println("Invalid Password");
                return null;
            }

            byte[] bytesKey1 = getDecoder().decode(encodedKey1);
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKey SessionKey = new SecretKeySpec(key2, 0, key2.length, "AES");

            // System.out.println("Data decoded");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");//Creates cipher
            cipher.init(Cipher.DECRYPT_MODE, SessionKey, ivspec);

            byte[] cipherText = cipher.doFinal(bytesKey1);//Encrypts message

            return getEncoder().encodeToString(cipherText);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(AsymetricKey_ServerSide.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }


    public String genAdminPassword(char[] password,byte[]salt){
        try {

            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 128);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            SecretKey secretKey = skf.generateSecret(spec);
            spec.clearPassword();
            for (int x = 0; x < password.length; x++) {
                password[x] = 0;
            }
            byte[] key=secretKey.getEncoded();

            return getEncoder().encodeToString(key);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


    public byte[][] genPasswords(char[] password, byte[] salt) {
        //Creates key2 and 3
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            SecretKey secretKey = skf.generateSecret(spec);
            spec.clearPassword();
            for (int x = 0; x < password.length; x++) {
                password[x] = 0;
            }
            byte[] key=secretKey.getEncoded();
//            secretKey.destroy();
            //Splits key2 and3
            byte[] key2 = Arrays.copyOfRange(key, 0, key.length / 2);
            byte[] key3 = Arrays.copyOfRange(key, key.length / 2, key.length);
            for (int x = 0; x <key.length ; x++) {
                key[x] = 0;
            }
            return new byte[][]{key2, key3};
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] createDBKey(char[] password) {
        // https://security.stackexchange.com/a/38854
        try {
            //Creates raw key1
            KeyGenerator KeyGen = KeyGenerator.getInstance("AES");
            KeyGen.init(128);
            byte[] key1 = KeyGen.generateKey().getEncoded();
            System.out.println("The message key is: "+getEncoder().encodeToString(key1));
            //Creates iv and salt
            SecureRandom rand = new SecureRandom();

            byte[] iv = new byte[16];
            rand.nextBytes(iv);
            String ivStr = getEncoder().encodeToString(iv);

            byte[] salt = new byte[16];
            rand.nextBytes(salt);
            String saltStr = getEncoder().encodeToString(salt);

            byte[][] keys = genPasswords(password, salt);
            for (int x = 0; x < password.length; x++) {
                password[x] = 0;
            }
            //Creates encoded key1
            String[] messageKey=encryptMessage(getEncoder().encodeToString(key1),getEncoder().encodeToString(keys[0]));


            SecretKey secretKey2 = new SecretKeySpec(keys[0], 0, keys[0].length, "AES");

           // SecretKey SessionKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");//Creates cipher
            cipher.init(Cipher.ENCRYPT_MODE, secretKey2, ivSpec);
            byte[] cipherText = cipher.doFinal(key1);//Encrypts message
            String key1Encoded = getEncoder().encodeToString(cipherText);//Converts message to string

            //Returns data to be saved
            return new String[]{messageKey[0],messageKey[1], saltStr, getEncoder().encodeToString(keys[1]) };

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(AsymetricKey_ServerSide.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    String keyGen() {
        SecretKey SessionKey;
        KeyGenerator KeyGen = null;
        try {
            //Generates session key
            KeyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException ex) {
            System.out.println(ex);
        }
        KeyGen.init(128);
        SessionKey = KeyGen.generateKey();
        return getEncoder().encodeToString(SessionKey.getEncoded());

    }

    public String encryptKey(String message, String StrPubKey) {
        // System.out.println("Encrypting keys for sending");
        PublicKey pubKey = null;
        try {
            byte[] bytePubKey = getDecoder().decode(StrPubKey);
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(bytePubKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubKey = keyFactory.generatePublic(pubKeySpec);

        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            System.out.println(ex);
        }
        //System.out.println("Data converted");
        String encoded = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");//Creates cipher
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            // System.out.println("Original Length: " + (message.getBytes()).length);
            byte[] cipherText = cipher.doFinal(message.getBytes());//Encrypts message
            // System.out.println("Encrypted Length: " + cipherText.length);
            encoded = getEncoder().encodeToString(cipherText);//Converts message to string
            // System.out.println("Cipher completed");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            System.out.println(ex);
        }
        //System.out.println("Keys encrypted");
        //  System.out.println("Keys encrypted");
        return encoded;
    }

    public String DecryptKey(String message, String StrPrivKey) {
        //  System.out.println("Decrypting recieved keys");
        PrivateKey privKey = null;
        try {

            byte[] bytePrivKey = getDecoder().decode(StrPrivKey);
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(bytePrivKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privKey = keyFactory.generatePrivate(privKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            System.out.println(ex);
        }
        // System.out.println("Decoded Data");
        String decoded = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");//Creates cipher
            cipher.init(Cipher.DECRYPT_MODE, privKey);
            byte[] decodedMsg = getDecoder().decode(message);
            //  System.out.println("Encrypted recieved Length: " + decodedMsg.length);
            byte[] plainText = cipher.doFinal(decodedMsg);//Decrypting Message
            //   System.out.println("Decrypted Length: " + plainText.length);
            decoded = new String(plainText);//Converts to String
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            System.out.println(ex);
        }
        // System.out.println("Keys decrypted");
        return (decoded);
    }

    public String[] encryptMessage(String message, String StrSessionKey) {
        // System.out.println("Encrypting Message");
        try {
            SecureRandom rand = new SecureRandom();
            byte[] iv = new byte[16];
            rand.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            // System.out.println("The iv is: " + Arrays.toString(iv));
            //   SecretKey SessionKey = new SecretKeySpec(StrSessionKey.getBytes(), "AES");
            byte[] decodedKey = getDecoder().decode(StrSessionKey);
            SecretKey SessionKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            //   System.out.println("Key converted");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");//Creates cipher            
            cipher.init(Cipher.ENCRYPT_MODE, SessionKey, ivspec);
            byte[] cipherText = cipher.doFinal(message.getBytes());//Encrypts message       
            String encoded = getEncoder().encodeToString(cipherText);//Converts message to string
            //     System.out.println("Message encrypted and base 64");
            //     System.out.println("The iv spec is" + Arrays.toString(iv) + "\nand the encrypted message is: " + encoded);
            //     System.out.println("Encryption complete");
            String ivStr = getEncoder().encodeToString(iv);
            System.out.println("Straight decoding returns: "+decryptMessage(encoded,StrSessionKey,ivStr));

            return new String[]{ivStr, encoded};
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            System.out.println(ex);
            System.out.println("MAY NEED TO KILL PROGRAM HERE");
            System.exit(1);
            return null;
        }
    }

    public String decryptMessage(String message, String StrSessionKey, String iv) {
        //  System.out.println("Decrypting Message");
        byte[] messagebytes = getDecoder().decode(message);
        IvParameterSpec ivspec = new IvParameterSpec(getDecoder().decode(iv));

        byte[] ByteKey = getDecoder().decode(StrSessionKey);
        SecretKey SessionKey = new SecretKeySpec(ByteKey, 0, ByteKey.length, "AES");
        String decrypted = null;
        //  System.out.println("Data decoded");
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");//Creates cipher            
            cipher.init(Cipher.DECRYPT_MODE, SessionKey, ivspec);

            byte[] cipherText = cipher.doFinal(messagebytes);//Encrypts message       
            decrypted = new String(cipherText);//Converts message to string  
            //   System.out.println("Decryption successful");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            System.out.println(ex);
        }
        return decrypted;
    }
}
