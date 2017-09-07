/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/
import java.io.*;
import java.net.*;
import java.util.logging.*;

public class P2PPingerThread extends Thread {
    private P2PNode p2pNode;
    private Socket sock = null;
     
    public P2PPingerThread(P2PNode p2pNode) {
        this.p2pNode = p2pNode;
    }
    
    private static int RETRY_OPEN_DELAY = 30000;    // msec
    private static int PING_DELAY = 5000;           // msec

    @Override
    public void run() {
        P2PLogger.log("START PingerThread name=%s\n", p2pNode.getname());

        if ( ! check_config() )
            return;

        get_first_connection();
        
        while (true) {
            try {
                p2pNode.floodNewPing(new PingMessage());
                p2pNode.reportStats();
                Thread.sleep(PING_DELAY);
            } catch (InterruptedException ex) {
                Logger.getLogger(P2PPingerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private boolean check_config() {
        if ( p2pNode.get_listen_port() == p2pNode.get_remote_port() && p2pNode.get_advertised_ipv4() == p2pNode.get_remote_ipv4()) {
            P2PLogger.log("PINGER %s will not run. It is configured for loopback\n", p2pNode.getname());
            return false;
        }
        if ( p2pNode.get_remote_port() == 0 ) {
            P2PLogger.log("PINGER %s will not run (remote_port == 0)", p2pNode.getname());
            return false;
        }
        return true;
    }

    // Get the first connection, pause and retry if it fails
    private void get_first_connection() {
        while (true) {
            try {
                InetAddress addr = InetAddress.getByName("localhost");      // TODO: hardcoded, has to be fixed
                sock = new Socket(addr, p2pNode.get_remote_port());
                P2PLogger.log("PINGER: node=%s s=%s\n", p2pNode.getname(), sock);
                break;
            } catch (IOException ex) {
                Logger.getLogger(P2PPingerThread.class.getName()).log(Level.SEVERE, null, ex);
            }

            P2PLogger.log("PINGER: Cannot open initial connection - will retry\n");

            try {
                Thread.sleep(RETRY_OPEN_DELAY);
            } catch (InterruptedException ex) {
                Logger.getLogger(P2PPingerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Start a new thread that reads on this socket
        Thread connectionThread = new Thread(new P2PConnectionReadThread(p2pNode, sock));
        connectionThread.start();
    }
}
