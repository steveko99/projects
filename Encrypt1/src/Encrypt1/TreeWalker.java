package Encrypt1;
import static Encrypt1.PathNameKeeper.cat_path_s;
import static Encrypt1.PathNameKeeper.does_file_exist;
import static Encrypt1.PathNameKeeper.get_dat_folder;
import static Encrypt1.PathNameKeeper.get_key_folder;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import static java.nio.file.FileVisitResult.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

public class TreeWalker extends SimpleFileVisitor<Path> {

    private final Path m_root;

    private final PrintWriter m_writer;

    public TreeWalker(Path root) throws FileNotFoundException, UnsupportedEncodingException {
        m_root = get_full_path(root);
        m_writer = new PrintWriter(PathNameKeeper.get_meta_file_write(), "UTF-8");
    }
    
    public void close() {
        m_writer.close();
    }
    
    private Path get_full_path(Path p) {
        return p.toAbsolutePath();
    }
    
    private Path get_sub_path(Path p) {
        String s = get_sub_path(p.toString());
        return Paths.get(s);
    }
    
    // figure out the sub-path p relative to m_root, for example:
    // if m_root == c:\foo and p == c:\foo\bar\boo then result => bar\foo
    private String get_sub_path(String p) {
        String newpath = p;
        String rootpath = m_root.toString();
        
        // Have to deal with root on Windows as a special case
        // for example C:\ is root, but C:\foobar has no trailing slash
        // If there exists a trailing slash then it is the root
        int idx;
        int len=rootpath.length();
        if ( "\\".equals(rootpath.substring(len-1, len)) )
            idx = len;
        else
            idx = len + 1;
            
        if ( idx >= newpath.length() )   
            return ""; //Paths.get("");       
        
        return newpath.substring(idx);
    }
    
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        Path filePath = get_sub_path(file);
        Path fullPath = get_full_path(file);

        long len = attrs.size();
        long timestamp = attrs.lastModifiedTime().toMillis();

        // compute hash of this file's contents
        String sha1 = hashFileSHA1(fullPath.toString());

        // compute hash of the additional key values, (filepath, timestamp, len)
        String hashThis = String.format("%s_%d_%d", filePath, timestamp, len);
        String sha1_extra = hashStringSHA1(hashThis);

        // now make the final unique identifier to name the encrypted file
        // sha1File_sha1_Path
        String final_name = String.format("%s_%s", sha1, sha1_extra);

        // If we computed the sha1 of the file's contents, then we can read it
        // Only put this file in the manifest if we opened it ok
        if (!"".equals(sha1)) {
            try {
                m_writer.println(final_name);

                // Build fully qualified pathnames
                String infile = fullPath.toString();
                String outfile = cat_path_s(get_dat_folder(), final_name);
                String keyfile = cat_path_s(get_key_folder(), final_name);

                if (does_file_exist(outfile)) {
                    System.out.println("SKIP encryption - output file already exists\n     " + outfile);
                } else {
                    // Get a new encryption key and save it to file
                    // TODO: should be a getKey() function in EncryptLib
                    SecretKey aesKey;
                    String key_s;

                    aesKey = EncryptLib.getKey();
                    key_s = byteToHex(aesKey.getEncoded());
                    saveKeyToFile(key_s, keyfile);

                    // infile in filemeta needs to be relative to the srcRoot
                    // because this string is written into the encrypted file's header
                    String relativeName = get_sub_path(infile);
                    FileMeta filemeta = new FileMeta(relativeName, timestamp, len);

                    // encrypt
                    EncryptLib.encryptAES(infile, filemeta, outfile, aesKey);
                }
            } catch (NoSuchAlgorithmException | IOException ex) {
                Logger.getLogger(TreeWalker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return CONTINUE;
    }

    // Save the encryption key to a file but encrypt it using the master_pw
    private void saveKeyToFile(String key_s, String outfile) throws FileNotFoundException {
        try (PrintWriter pw = new PrintWriter(outfile)) {
            Password master_pw = Encrypt1.GetMasterPw();
            byte[] ciphertext = master_pw.doEncrypt(TreeWalker.hexStringToByteArray(key_s));
            pw.println(TreeWalker.byteToHex(ciphertext));
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            Logger.getLogger(TreeWalker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }

    // compute a SHA1 of the given file's contents
    private static String hashFileSHA1(String filename) {
        try (FileInputStream fis = new FileInputStream(filename)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            byte[] buf = new byte[64];
            int nRead = fis.read(buf);
            while (nRead > 0) {
                digest.update(buf);
                nRead = fis.read(buf);
            }
            String sha1 = byteToHex(digest.digest());
            return sha1;
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | FileNotFoundException ex) {
            Logger.getLogger(TreeWalker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TreeWalker.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    // compute a SHA1 of the given String
    private static String hashStringSHA1(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(s.getBytes("UTF-8"));
            String sha1 = byteToHex(digest.digest());
            return sha1;
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            Logger.getLogger(TreeWalker.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
 
    public static String byteToHex(final byte[] hash) {
        return DatatypeConverter.printHexBinary(hash);
    }
    
    public static byte[] hexStringToByteArray(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }
}
