//------------------------------------------------------------------------------
// RestoreTree:
//
// Read each line of the manifest and decrypt each file listed.  Restore it to
// srcRoot\pathname.
//
// pathname is extracted from the header of the encrypted file.
//------------------------------------------------------------------------------

package Encrypt1;

import static Encrypt1.PathNameKeeper.cat_path_s;
import static Encrypt1.PathNameKeeper.get_destRoot;
import static Encrypt1.TreeWalker.hexStringToByteArray;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RestoreTree  {
    
    private final String m_input;
    
    public RestoreTree(String metaFile) {
        m_input = metaFile;
    }
    
    public void Start() throws Exception {
        int lineNum = 0;
        String line; 
        BufferedReader br = new BufferedReader(new FileReader(m_input));
        while ((line = br.readLine()) != null) {
            decrypt_one(line);
        }
    }

    private void decrypt_one(String name_s ) throws Exception {
        
        // Build fully qualified pathnames
        String encryptedFile;
        String keyfile;

        // TODO: Reversed infile and outfile confusing names
        //infile  = PathNameKeeper.cat_path_s(PathNameKeeper.get_destRoot(), name_s);
        encryptedFile = PathNameKeeper.cat_path_s(PathNameKeeper.get_dat_folder(), name_s);
        keyfile = PathNameKeeper.cat_path_s(PathNameKeeper.get_key_folder(), name_s);

        // decrypt
        byte[] aesKey = readKeyFromFile(keyfile);
        FileMeta filemeta = EncryptLib.decryptAES(encryptedFile, aesKey);
        
        String fullPath = cat_path_s(get_destRoot(), filemeta.GetPathName());
        File f = new File(fullPath);
        boolean setLastModified = f.setLastModified(filemeta.GetTimeStamp());
    }
    
    private byte[] readKeyFromFile(String keyfile_s) throws FileNotFoundException, IOException, Exception {
        byte[] key_plain = null;
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(keyfile_s));
            String key_s = "";
            if ((line = br.readLine()) != null) {
                key_s = line;
            }
            
            if ( "".equals(key_s) ) {
                throw new Exception();
            }
            // key_s was previously encrypted with the MasterPw key
            // Because of padding on encrypt, the size will be infalted to even blocksize
            // So we must truncate key_plain to its correct plaintext size
            // WARNING: This code assumes 128-bit keysize ( 16 bytes )
            Password master_pw = Encrypt1.GetMasterPw();
            key_plain = master_pw.doDecrypt(hexStringToByteArray(key_s));
            key_plain = Arrays.copyOf(key_plain, 16);
            
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            Logger.getLogger(RestoreTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        return key_plain;
    }
}
