/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/
import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PLogger {

    private P2PLogger() {};     // no instances
    
    static PrintWriter pw = open_logfile();
    
    public static PrintWriter open_logfile() {
        try {
            String f = "./P2PApp.out.txt";
            return (new PrintWriter(f));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(P2PLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static void log(String format, Object...o) {
        String s = String.format(format, o);
        String s2 = String.format("%08d: %s", System.currentTimeMillis(), s);
        System.out.print(s2);
    }

    /*
     public static void LogString(String s) {
        pw.printf("%08d: %s", System.currentTimeMillis(), s);
        System.out.printf("%08d: %s", System.currentTimeMillis(), s);
    } */
}
