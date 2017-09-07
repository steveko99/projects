/*------------------------------------------------------------------------------
EncryptLib
encryption methods for this app

Author: Steve Koscho
Date: 2017-04-02

This implementation is for AES 128-bit only.

It uses CBC encoding which requires that the IV is random but does not need to
be kept secret.  This matters as this solution saves the IV plaintext in the
encrypted output file.

Encrypted file format is as follows:
    -------------- START HEADER (plaintext values) --------------------
        16 bytes : IV
        8 bytes  : original file size
        8 bytes  : length of pathname (includes padding bytes)
        8 bytes  : last modify timestamp
    -------------- END OF HEADER ---------------------------------------
    Encrypted pathname
    Encrypted file contents
    -------------- EOF --------------------------------

Lengths are written as 64-bit longs to maintain 64-bit alignment in the header
and throughout the entire file.

When data is sent through the cipher it is always done in BLOCKSIZE bytes at a
time.  BLOCKSIZE is fixed and is 128-bits (16 bytes).
------------------------------------------------------------------------------*/
package Encrypt1;

import static Encrypt1.PathNameKeeper.cat_path_s;
import static Encrypt1.PathNameKeeper.get_destRoot;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptLib {

    // these constants cannot be changed
    private static final String AESNAME = "AES/CBC/PKCS5Padding";
    private static final int KEYSIZE = 16;
    private static final int LONGSIZE = 8;
    private static final int BLOCKSIZE = 16;

    private static int CountBlocks(int len) {
        int nBlocks = len / BLOCKSIZE;
        if (len % BLOCKSIZE != 0) {
            nBlocks++;
        }
        return nBlocks;
    }
    
    // The IV is written plaintext as the first KEYSIZE bytes of the encrypted file
    private static byte[] readIV(FileInputStream fis) throws IOException, Exception {
        byte[] key = new byte[KEYSIZE];
        int nRead = fis.read(key, 0, KEYSIZE);
        if (nRead != KEYSIZE) {
            throw new Exception(); // TODO: invalid file, throw specific exception
        }
        return key;
    }
    
    // 64-bit size of the file before encryption, next field of the header
    private static long readFileSize(FileInputStream fis) throws IOException, Exception {
        byte[] l = new byte[LONGSIZE];
        int nRead = fis.read(l, 0, LONGSIZE);
        if ( nRead != LONGSIZE ) {
            throw new Exception(); // TODO: invalid file, throw specific exception
        }
        long filesize = readLongFromBuffer(l);
        return filesize;
    }

    // 64-bit length for length of pathname size, next field of the header
    private static long readPathSize(FileInputStream fis) throws IOException, Exception {
        byte[] l = new byte[LONGSIZE];
        int nRead = fis.read(l, 0, LONGSIZE);
        if ( nRead != LONGSIZE ) {
            throw new Exception(); // TODO: invalid file, throw specific exception
        }
        long pathsize = readLongFromBuffer(l);
        return pathsize;
    }

    // 64-bit timestamp of lastModifiedTime, next field of the header
    private static long readTimeStamp(FileInputStream fis) throws IOException, Exception {
        byte[] l = new byte[LONGSIZE];
        int nRead = fis.read(l, 0, LONGSIZE);
        if ( nRead != LONGSIZE ) {
            throw new Exception(); // TODO: invalid file, throw specific exception
        }
        long timestamp = readLongFromBuffer(l);
        return timestamp;
    }

    // TODO: timestamp and restore last modify
    
    // encode the String UTF-8 as bytes and pad to BLOCKSIZE alignment with zeroes
    // this is used at encrypt time to write pathname into encrypted output file
    private static byte[] GetPathBytes(String pathname) {
        try {
            byte[] pathnameBytes = pathname.getBytes("UTF-8");
            int nBytes = pathnameBytes.length;
            int nBlocks = CountBlocks(pathnameBytes.length);
            byte[] paddedBytes = new byte[nBlocks * BLOCKSIZE];
            System.arraycopy(pathnameBytes, 0, paddedBytes, 0, nBytes);
            return paddedBytes;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(EncryptLib.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    // remove the trailing zeroes and give back the String with original length
    // this is used at decrypt time
    private static String GetPathString(byte[] pathname_b) {
        
        // scan the byte array until find a zero byte (if any) and that
        // is the real end of the string
        // note: this works because there is no zero byte in a UTF-8
        // encoded string even if multi-byte chars are in it
        // also it is a better idea to figure out where the padding starts
        // before converting to String because possibility of multi-byte chars
        String pathname = "";
        int len = pathname_b.length;
        for (int i = 0; i < len; i++) {
            if (pathname_b[i] == (byte) 0) {
                len = i;
                break;
            }
        }
        if (len != 0) {
            try {
                byte[] temp_b = new byte[len];
                System.arraycopy(pathname_b, 0, temp_b, 0, len);
                pathname = new String(temp_b, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(EncryptLib.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return pathname;
    }

    private static Cipher GetNewCipher(int mode, SecretKey aesKey) {
        try {
            Cipher aesCipher;
            aesCipher = Cipher.getInstance(AESNAME);
            aesCipher.init(mode, aesKey);
            return aesCipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {
            Logger.getLogger(EncryptLib.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    private static Cipher GetNewCipher(int mode, SecretKey aesKey, byte[] ivBytes) {
        try {
            Cipher aesCipher;
            aesCipher = Cipher.getInstance(AESNAME);
            aesCipher.init(mode, aesKey, new IvParameterSpec(ivBytes));
            return aesCipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            Logger.getLogger(EncryptLib.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    // TODO: review this and there is a place in TreeWalker doing similar function
    public static SecretKey getKey() throws NoSuchAlgorithmException {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        SecretKey aesKey = keygen.generateKey();
        return aesKey;
    }

    // EncryptLib.encryptAES()
    // infile - full path to input file within srcRoot
    // filemeta.pathname - MUST be relative pathname, written into encrypted outfile
    // outfile - full path to encrypted file, will overwite if existing
    // aesKey - new key (TODO: should this be moved inside)
    public static void encryptAES(String infile, FileMeta filemeta, String outfile, SecretKey aesKey) throws FileNotFoundException, IOException {
  
        System.out.println("Encrypt infile " + infile);
        System.out.println("        outfile " + outfile);
                
        // create output file
        FileOutputStream fos = new FileOutputStream(outfile);
        
        try (FileInputStream fis = new FileInputStream(infile)) {
           
            Cipher aesCipher = GetNewCipher(Cipher.ENCRYPT_MODE, aesKey);
            
            // convert the pathname to byte[] and pad it out to BLOCKSIZE
            byte[] pathname_b = GetPathBytes(filemeta.GetPathName());
            
            long fileSize = filemeta.GetFileSize();
            long pathSize = (long) pathname_b.length;
            long timestamp = filemeta.GetTimeStamp();
            
            // write header to output
            // 16 bytes IV
            // 8 bytes file size
            // 8 bytes pathname size
            // 8 bytes timestamp (lastModifiedTime)
            byte[] aesIV_b =  aesCipher.getIV();
            byte[] fileSize_b = writeLongToBuffer(fileSize);
            byte[] pathSize_b = writeLongToBuffer(pathSize);
            byte[] timestamp_b = writeLongToBuffer(timestamp);
            
            fos.write(aesIV_b);
            fos.write(fileSize_b);
            fos.write(pathSize_b);
            fos.write(timestamp_b);
 
            // write pathname to file encrypted
            byte[] block = new byte[BLOCKSIZE];
            int nBlocks = CountBlocks(pathname_b.length);
            for ( int i=0; i < nBlocks; i++ ) {
                System.arraycopy(pathname_b, i*BLOCKSIZE, block, 0, BLOCKSIZE);
                fos.write(aesCipher.update(block));
            }

            // read the input file in chunks
            // encrypt each trunk and write to the encrypted file
            byte[] ctext;
            int nRead = fis.read(block);
            while(nRead > 0) {
                ctext = aesCipher.update(block);
                fos.write(ctext);
                nRead = fis.read(block);
            }
            ctext = aesCipher.doFinal(block);
            fos.write(ctext);
            
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(EncryptLib.class.getName()).log(Level.SEVERE, null, ex);
        } 
        finally {
            fos.close();
        }
    }
    
    // TODO: Do I need 2 of these?
    public static FileMeta decryptAES(String infile, byte[] key_b) throws FileNotFoundException, IOException, Exception {
        SecretKeySpec aesKey = new SecretKeySpec(key_b, "AES");
        return decryptAES(infile, /*sha1,*/ aesKey);
    }

    public static FileMeta decryptAES(String infile, SecretKeySpec aesKey) throws FileNotFoundException, IOException, Exception {

        System.out.println("Decrypt infile: " + infile);
        
        FileOutputStream fos = null;
        
        try (FileInputStream fis = new FileInputStream(infile)) {
 
            // Read the plaintext header of the encrypted file
            byte[] aesIV = readIV(fis);          // reads 16 bytes
            long file_size = readFileSize(fis);  // reads 8 bytes
            long path_size = readPathSize(fis);  // reads 8 bytes
            long timestamp = readTimeStamp(fis); // reads 8 bytes

            Cipher dCipher = GetNewCipher(Cipher.DECRYPT_MODE, aesKey, aesIV);
            
            // read pathname, Note calls to Cipher.update() don't always
            // return data.  So only stop this loop after we get path_size bytes
            // out of the Cipher, as opposed to how many we have read from the file.
            byte[] block = new byte[BLOCKSIZE];
            byte[] pathname_b = new byte[(int) path_size];  // TODO: need to put a practical limit on path_size
            int nCopied = 0;
            int nRead = fis.read(block);
            byte[] text = dCipher.update(block);
            while (nRead > 0 && nCopied < path_size) {
                if ( text.length > 0 ) {
                    System.arraycopy(text, 0, pathname_b, nCopied, text.length);
                    nCopied += text.length;
                }
                nRead = fis.read(block);
                text = dCipher.update(block);
            }

            // return byte[] pathname_b back into a String of the original length
            String pathname = GetPathString(pathname_b);
            if ( "".equals(pathname) ) {
                throw new Exception();  // TODO make more specific
            }
            
            String fullRestorePath = cat_path_s(get_destRoot(), pathname);
            File f = new File(fullRestorePath);
            
            if (f.lastModified() == timestamp) {
                System.out.println("SKIP - file to restore already exists");
            } else {
                System.out.println("        outfile: " + fullRestorePath);

                // now read the file contents and write them as they are decrypted
                fos = CreateRestoreFile(fullRestorePath);
                long nWrote = 0;

                while (nWrote < file_size) {
                    if (nWrote + text.length < file_size) {
                        fos.write(text);
                        nWrote += text.length;
                    } else {
                        int n = (int) (file_size - nWrote);
                        fos.write(text, 0, n);
                        nWrote += n;
                        break;      // correct place for this loop to end, when we have all the file data
                    }
                    if (nRead == 0) {
                        break;      // something is wrong with the input file if this happens
                    }
                    nRead = fis.read(block);
                    text = dCipher.update(block);
                }
            }
            // done
            FileMeta filemeta = new FileMeta(pathname, timestamp, file_size);
            return filemeta;

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(EncryptLib.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            if (fos != null) {
                fos.close();
            }
        }
        return null;
    }
            
    // pathname is relative name that was read out of the encrypted data
    // concat destRoot with pathname for the fully qualified name
    public static FileOutputStream CreateRestoreFile(String pathname) throws FileNotFoundException {

        String destRoot = PathNameKeeper.get_destRoot();
        String outfile = pathname;

        File f;
        f = new File(outfile).getParentFile();
        boolean mkdirs = f.mkdirs();
        
        FileOutputStream fos = new FileOutputStream(outfile);
        return fos;
    }

    // to network byte order
    public static byte[] writeLongToBuffer(long l) {
        byte[] b = new byte[8];
        b[0] =(byte) (l >> 56);
        b[1] =(byte) (l >> 48);
        b[2] =(byte) (l >> 40);
        b[3] =(byte) (l >> 32);
        b[4] =(byte) (l >> 24);
        b[5] =(byte) (l >> 16);
        b[6] =(byte) (l >> 8);
        b[7] =(byte) l;
        return b;
    }
    
    // from network byte order
    public static long readLongFromBuffer(byte[] b) {
        long l = b[0];
        for ( int i=1; i<8; i++ ) {
            l = l << 8;
            l = (0xFF & b[i]) | l;
        }
        return l;
    }
}
