/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/
import java.net.*;

/*
    P2PNode is the root object, all shared data is allocated here
    Launch > 1 node for testing
    A Node spawns 2 threads (ListenerThread, PingerThread)
    The Listener spawns a Reader thread for each incoming connection
    The Pinger spawns another thread to do the reads
*/

public class P2PNode {

    private final String nodeName;
    private final int remote_ipv4;
    private final short remote_port;
    private final short listen_port;
    private final int advertised_ipv4;
    private final short advertised_port;    // TODO
    private final P2PReceivedMessageCache msgCache;
    private final P2PRoutings routings;
    
    public P2PNode(String name, int remote_ip, short remote_port, short listen_port) {
        this.nodeName = name;
        this.remote_ipv4 = remote_ip;
        this.remote_port = remote_port;
        this.listen_port = listen_port;
        this.advertised_ipv4 = 0x7f000001;      // TODO:
        this.advertised_port = listen_port;     // TODO: 
        this.msgCache = new P2PReceivedMessageCache();
        this.routings = new P2PRoutings(this);
    }
    
    public void addFloodSocket(Socket s) {
        routings.addFloodSocket(s);
    }
                
    public void RoutePing(P2PMessage msgPing, Socket sockRead) {
        routings.RoutePing(msgPing, sockRead);
    }
    
    public void RoutePong(P2PMessage msgPong, Socket sockRead) {
        routings.RoutePong(msgPong, sockRead);     
    }

    public void RouteRequest(P2PMessage msgPing, Socket sockRead) {
        routings.RouteRequest(msgPing, sockRead, null);
    }
    
    public void RouteReply(P2PMessage msgPong, Socket sockRead) {
        routings.RouteReply(msgPong, sockRead);     
    }

    public void floodNewPing(P2PMessage msg) {
        routings.floodNewPing(msg);
    }
    
    public void makeRequest(int ipv4, short port, P2PRequestEventI rc) {
        routings.makeRequest(ipv4, port, rc);
    }

    public void reportStats() {
        routings.reportStats();
    }

    public String getname() { return nodeName; }
    
    public int   get_remote_ipv4() { return this.remote_ipv4; }
    public short get_remote_port() { return this.remote_port; }
    public short get_listen_port() { return this.listen_port; }
    public int   get_advertised_ipv4() { return this.advertised_ipv4; }
    public short get_advertised_port() { return this.advertised_port; }
    
    public void add_received_message(P2PMessage msg) {
        msgCache.add(msg, 0, (short)0); // TODO ip and port
    }
    
    public boolean lookup_received_message(P2PMessage msg) {
        return msgCache.lookup(msg);
    }
    
    public void start() {
        Thread pThread = new Thread(new P2PListenerThread(this, listen_port));
        Thread lThread = new Thread(new P2PPingerThread(this));
        
        pThread.start();
        lThread.start();
    }
}
