/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PListenerThread implements Runnable {

    private short listen_port;
    private ServerSocket listen_socket = null;
    private static final int MAX_OPEN_RETRIES = 1000;
    private P2PNode p2pNode;

    public P2PListenerThread(P2PNode p2pNode, short listen_port) {
        this.p2pNode = p2pNode;
        this.listen_port = listen_port;
    }
    
    @Override
    public void run() {
        System.out.println("START ListenThread nodeNname=" + p2pNode.getname());
        listen_socket = open_listen_socket();
        while (true) {     
            Socket s = accept_connection(listen_socket);
            Thread connectionThread = new Thread(new P2PConnectionReadThread(p2pNode, s));
            connectionThread.start();
        }
    }

    // There is possible contention for listener ports
    // So if you can't open listen_port because it is already bound, then try
    // a different one
    
    private ServerSocket open_listen_socket() {
        ServerSocket s;
        for (int i=0; i<MAX_OPEN_RETRIES; i++ ) {
            try {
                s = new ServerSocket(listen_port);
                System.out.println(String.format("PORTOPEN: listenport=%d nodeName=%s", listen_port, p2pNode.getname()));
                return s;
            } catch ( java.net.BindException ex ) {
                listen_port++;
                continue;
            } catch (IOException ex) {
                Logger.getLogger(P2PListenerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                Thread.sleep(0);
            } catch (InterruptedException ex) {
                Logger.getLogger(P2PListenerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println(String.format("PORTOPEN: failed, too many retries, give up"));
        return null;
    }
    
    private Socket accept_connection(ServerSocket s) {
        Socket s2 = null;
        try {
            s2 = s.accept();
        } catch (IOException ex) {
            Logger.getLogger(P2PListenerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(String.format("ACCEPT: accept_connection nodeName=%s s=%s s2=%s", p2pNode.getname(), s, s2));
        return s2;
    }
 }
