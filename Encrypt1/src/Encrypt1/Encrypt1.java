package Encrypt1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Encrypt1 {

    private static void usage(String msg) {
        System.err.println("usage: encrypt1 -db:dbRoot -src:srcRoot -restore:manifest -dest:destRoot\n" + msg);
        System.exit(-1);
    }

    private static final Password MasterPw = new Password();
    
    public static Password GetMasterPw() {
        return MasterPw;
    } 
    
    private static String MasterPwHash = "";

    public static String GetMasterPwHash() {
        return MasterPwHash;
    }
    
    private static final String OPTION_DB = "-db:";
    private static final String OPTION_SRC = "-src:";
    private static final String OPTION_RSTOR = "-restore:";
    private static final String OPTION_DEST = "-dest:";
    private static final String OPTION_INIT = "-init:";

    // return a new string that is the prefix of s up to max chars long without
    // throwing bounds check exception in cases s.length < max
    private static String get_prefix_str(String s, int max) {
        String substring = s.substring(0, Math.min(s.length(), max));
        return substring;
    }
    
    public static void main(String[] args) {

        try {
            
            boolean doRestore = false;
            boolean doEncrypt = false;
            boolean doInit = false;
            
            for (String arg : args) {
                
                if (OPTION_DB.equals(get_prefix_str(arg, OPTION_DB.length()))) {  // -db:dbRoot
                    
                    String s = arg.substring(OPTION_DB.length());
                    PathNameKeeper.set_dbRoot(s);
                    
                } else if (OPTION_SRC.equals(get_prefix_str(arg, OPTION_SRC.length()))) {  // -src:srcRoot
                    
                    String s = arg.substring(OPTION_SRC.length());
                    PathNameKeeper.set_srcRoot(s);
                    
                } else if (OPTION_RSTOR.equals(get_prefix_str(arg, OPTION_RSTOR.length()))) {  // -restore:metafile
                    
                    doRestore = true;
                    String s = arg.substring(OPTION_RSTOR.length());
                    PathNameKeeper.set_meta_file_read(s);
                    
                } else if (OPTION_DEST.equals(get_prefix_str(arg, OPTION_DEST.length()))) {  // -dest:destRoot
                    
                    String s = arg.substring(OPTION_DEST.length());
                    PathNameKeeper.set_destRoot(s);
                    
                } else if (OPTION_INIT.equals(get_prefix_str(arg, OPTION_INIT.length()))) {  // -init
                    
                    doInit = true;
                    String s = arg.substring(OPTION_INIT.length());
                    PathNameKeeper.set_srcRoot(s);
                    
                } else {
                    usage("unrecognized command line argument");
                }
            }
            if (!doRestore && !doInit) {
                doEncrypt = true;
            }
            read_config(PathNameKeeper.get_config_file());
            
            if ( !MasterPw.Verify() ) {
                System.exit(-1);
            }
            if (!PathNameKeeper.check_dbRoot()) {
                usage("dbRoot not found -db:dbRoot");
            }
            try {
                if (doRestore) {
                    doRestore();
                }
                if (doEncrypt) {
                    doEncrypt();
                }
                if (doInit) {
                    doInit();
                }
            } catch (Exception ex) {
                Logger.getLogger(Encrypt1.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(Encrypt1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // srcRoot is read from dbRoot\config and not specified by user at command line
    // sample input file ...
    // ROOT,C:\\users\\user0\\Documents
    private static void read_config(String filename) throws FileNotFoundException, IOException {
        System.out.println("read_config: start");
        String line;
        BufferedReader br = new BufferedReader(new FileReader(filename));
        while ((line = br.readLine()) != null) {
            String[] lines = line.split(",");
            System.out.println("read_config: " + line);
            if (lines.length < 2) {
                return;
            }
            if ( "MASTER".equals(lines[0]) ) {
                MasterPwHash = lines[1];
            }
        }
    }

    private static void doEncrypt()  {
    
        try {
            if (!PathNameKeeper.check_srcRoot()) {
                usage("srcRoot not found");
            }
            
            // walk the tree at srcRoot and
            // 1 - encrypt each file
            // 2 - create a manifest of encrypted files that should be restored when user runs restore
            Path startingDir = Paths.get(PathNameKeeper.get_srcRoot());
            TreeWalker walker = new TreeWalker(startingDir);
            Files.walkFileTree(startingDir, walker);
            walker.close();

        } catch (IOException ex) {
            Logger.getLogger(Encrypt1.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Encrypt1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void doRestore() {

        if (!PathNameKeeper.check_meta_file_read()) {
            usage("manifest to restore not found -restore:manifest");
        }
        if (!PathNameKeeper.check_destRoot()) {
            usage("destRoot is not found");
        }
        
        try {
            RestoreTree rt = new RestoreTree(PathNameKeeper.get_meta_file_read());
            rt.Start();
        } catch (Exception ex) {
            Logger.getLogger(Encrypt1.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void doInit() {
    }
}
