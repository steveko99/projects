package Encrypt1;

import java.io.File;

public class PathNameKeeper {
    
    private static String srcRoot_s = "";     // input arg if restore or init - read from config file if encrypting
    private static String dbRoot_s = "";      // input arg always required
    private static String destRoot_s = "";    // input arg required if restore specified
    private static String restoreMeta_s = ""; // input arg from -restore:metafile

    public static String get_srcRoot() {
        return srcRoot_s;
    }
    
    public static void set_srcRoot(String s) {
        srcRoot_s = s;
    }

    public static boolean check_srcRoot() {
        return does_folder_exist(srcRoot_s);
    }

    public static String get_dbRoot() {
        return dbRoot_s;
    }

    public static void set_dbRoot(String s) {
        dbRoot_s = s;
    }

    public static boolean check_dbRoot() {
        return does_folder_exist(dbRoot_s);
    }

    public static String get_destRoot() {
        return destRoot_s;
    }

    public static void set_destRoot(String s) {
        destRoot_s = s;
    }

    public static boolean check_destRoot() {
        return does_folder_exist(destRoot_s);
    }

    public static String get_config_file() {
        return cat_path_s(dbRoot_s, "config");
    }

    public static String get_meta_file_write() {    // TreeWalker writes to meta file
        String s;
        s = cat_path_s(dbRoot_s, "META");
        s = cat_path_s(s, "TreeWalker.out");
        return s;
    }

    public static String get_meta_file_read() {     // -restore:metafile reads from meta file
        return restoreMeta_s;
    }

    public static void set_meta_file_read(String s) {
        restoreMeta_s = s;
    }

    public static boolean check_meta_file_read() {
        return does_file_exist(restoreMeta_s);
    }
    
    public static String get_dat_folder() {
        return cat_path_s(dbRoot_s, "DAT");
    }

    public static String get_key_folder() {
        return cat_path_s(dbRoot_s, "KEY");
    }
    
    public static boolean does_file_exist(String filename) {
        File f = new File(filename);
        return f.exists() && f.isFile();
    }

    public static boolean does_folder_exist(String filename) {
        File f = new File(filename);
        return f.exists() && f.isDirectory();
    }

    // On Windows C:\ has trailing slash for root only (C:\foo\bar has none)
    // r must be fully qualified path name, with or without trailing slash
    // d must be a sub-folder of r, taken verbatim with no leading slash
    public static String cat_path_s(String r, String d) {
        int len = r.length();
        if ( "\\".equals(r.substring(len-1, len)) )
            return r + d;
        return r + "\\" + d;
    }
}
