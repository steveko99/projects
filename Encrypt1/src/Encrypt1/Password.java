package Encrypt1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class Password {

    PBEKeySpec pbeKeySpec;
    PBEParameterSpec pbeParamSpec;
    SecretKey pbeKey;

    // NOTE:
    // We want the salt to be fixed for this app (unlike typical server login app)
    // Don't ever change the value; 64-bit salt="BAADFOOD" when encoded in ASCII
    byte[] salt = {
                (byte) 0x42, (byte) 0x41, (byte) 0x41, (byte) 0x44,
                (byte) 0x46, (byte) 0x4f, (byte) 0x4f, (byte) 0x44
    };
           
    // Iteration count
    // NOTE:
    // This is more arbitrarily chosen, but it cannot change: count=50000
    int count = 50000;
    
    // NOTE: prompting user problems with console
    // - recommended method of calling readPassword() only works in the IDE
    // - recommended method also echoes the password anyway in the IDE
    // - recommended method causes compiler warning
    //
    // Tried to open System.console() instead but it returns null
    //
    // However, the recommended method (calling readPassword) works when I
    // make a standalone test app and buld it and run it on command line
    //
    // I suspect it has to do with differences with compile time or run time
    // switches
    //
    // For now, I call readLine() and it not too bad - it echoes password
    // but works in IDE and on command line
    private char[] PromptUser() {
        try {
            System.out.print("Enter encryption password:  ");
            System.out.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String pw_s = reader.readLine();
            return pw_s.toCharArray();
        } catch (IOException ex) {
            Logger.getLogger(Password.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public Password() {
    }

    public boolean Verify() {

        try {
            pbeParamSpec = new PBEParameterSpec(salt, count);

            char[] pw = PromptUser();
            pbeKeySpec = new PBEKeySpec(pw);

            SecretKeyFactory keyFac;
            keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            pbeKey = keyFac.generateSecret(pbeKeySpec);
            
            String newHash = generatePasswordHash(pw);
            System.out.println("Password.Verify: " + newHash);
            
            String oldHash = Encrypt1.GetMasterPwHash();

            if ( !oldHash.equals(newHash) ) {
                System.err.println("ERROR: Password does not match");
                return false;
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(Password.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }
    
    public byte[] doEncrypt(byte[] cleartext) throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher c;
        c = Cipher.getInstance("PBEWithMD5AndDES");
        c.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
        byte[] ciphertext = c.doFinal(cleartext);
        return ciphertext;
    }

    public byte[] doDecrypt(byte[] ciphertext) throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException {
        Cipher c;
        c = Cipher.getInstance("PBEWithMD5AndDES");
        c.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
        byte[] plaintext = c.doFinal(ciphertext);
        return plaintext;
    }

    //--------------------------------------------------------------------------
    // Generates a large hash of the given password
    // NOTE: This hash is not the same hash used for PBE encryption
    // This hash is saved in the config file when DB is created
    // Then, it can be verified for "login"
    // In the context of this app, "login" means it will proceed
    // If these hashes match, then the master key will work
    //--------------------------------------------------------------------------
    public String generatePasswordHash(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //public static String generatePasswordHash(String password, String salt)
        //char[] chars = password.toCharArray();
        //byte[] saltBytes = salt.getBytes(StandardCharsets.US_ASCII);
        char[] chars = password;
        byte[] saltBytes = salt;
        
        PBEKeySpec spec = new PBEKeySpec(chars, saltBytes, 10000, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        BigInteger bi = new BigInteger(1, hash);
        return String.format("%064x", bi);
    }
} 
