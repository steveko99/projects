/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PConnectionReadThread implements Runnable {

    private final P2PNode p2pNode;
    private final Socket s;
   
    public P2PConnectionReadThread(P2PNode p2pNode, Socket s) {
        this.p2pNode = p2pNode;
        this.s = s;
    }
    
    @Override
    public void run() {
        P2PLogger.log("START: %s ConnectionHandler.run() - new thread enter\n", p2pNode.getname());
        while (true) {
            p2pNode.addFloodSocket(s);
            try {
                P2PMessageIO.ReadAndDispatch(s, p2pNode);
            } catch (Exception ex) {
                Logger.getLogger(P2PConnectionReadThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
 }
