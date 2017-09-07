/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/
import java.net.Socket;
import java.util.LinkedList;

public class P2PRoutings {
    
    private final RoutingTable rt;
    private final ForwardedMessageList forwards;
    private final FloodList floodList;
    
    public static final int MAX_FORWARD_RECORDS = 200;    // these MAX values are adjustments
    public static final int MAX_ROUTINGS_RECORDS = 200;
    public static final int MAX_FLOOD_LIST = 20;
    
    private final P2PNode p2pNode;
    
    public P2PRoutings(P2PNode p2pNode) {
        rt = new RoutingTable(p2pNode);
        forwards = new ForwardedMessageList(p2pNode);
        floodList = new FloodList(p2pNode, forwards);
        this.p2pNode = p2pNode;
    }

    public synchronized void addFloodSocket(Socket s) {
        floodList.add(s);
    }
    
    // new PING (this node created it)
    public synchronized void floodNewPing(P2PMessage msg) {
        floodList.doFloodPing(msg, null);
    }
    
    // received PING
    // 1 - flood the PING
    // 2 - send a PONG back to the sender on the same socket
    public synchronized void RoutePing(P2PMessage msgPing, Socket sockRead) {
        PongMessage msgPong = new PongMessage(0x7f000001, (short) p2pNode.get_listen_port());   // TODO: hardcoded IP
        P2PMessage.clone_msgID(msgPong, msgPing);
        floodList.doFloodPing(msgPing, sockRead);
        P2PMessageIO.send(msgPong, sockRead, p2pNode);
    }

    // received PONG
    // 1 - harvest the ipv4:port this msgPong is advertising and add (ipv4,port,sockRead) to routing table
    // 2 - find out the route a matching PING possibly took (so that it can be returned exactly on that path)
    // 3 - if we previously received (and forwarded) the matching PING, then return this PONG along the same path
    // 4 - finally, flood this node advertisement in all cases
    public synchronized void RoutePong(P2PMessage msg, Socket sockRead) {   
        
        rt.add(((PongMessage)msg).get_ipv4(), ((PongMessage)msg).get_port(), sockRead);
        
        ForwardedMsgEntry e = forwards.find(msg, sockRead); 
        Socket sockFrom = ( e == null ) ? null : e.sockFrom;
        
        if ( sockFrom != null ) {
            P2PMessageIO.send(msg, sockFrom, p2pNode);
        }
        
        floodList.doFloodPong(msg, sockFrom, sockRead);
    }
               
    // send a new REQUEST, rc.readComplete() will be called when IO is complete
    public synchronized void makeRequest(int ipv4, short port, P2PRequestEventI rc) {
        P2PMessage msgReq = new RequestMessage(ipv4, port);
        P2PLogger.log("RequestRoutings: %s NEW REQUEST\n", p2pNode.getname());
        RouteRequest(msgReq, null, rc);
    }   
    
    // when a REQUEST is received:
    // - If this REQUEST is for this node, then make a new REPLY and send it back on the socket we read it on
    // - Otherwise lookup the routing from the routing table
    // - If we find a forward route, route it, otherwise send back an error response saying "node not found"
    public synchronized void RouteRequest(P2PMessage msg, Socket sockRead, P2PRequestEventI rc) {
     
        if ( forThisNode(msg) ) {
            ReplyMessage msgResponse = new ReplyMessage(p2pNode.getname());
            P2PMessage.clone_msgID(msgResponse, msg);
            send_reply(msgResponse, sockRead, rc);
            return;
        }
       
        Socket sockTo = rt.findRoute(msg);
        
        if ( sockTo != null ) {
            P2PLogger.log("RequestRoutings: %s forwarding REQUEST\n", p2pNode.getname());
            forwards.add(msg, sockTo, sockRead, rc);
            P2PMessageIO.send(msg, sockTo, p2pNode);
       
        } else {
            P2PLogger.log("RequestRoutings(): %s cannot find a route to %s:%d\n", p2pNode.getname(), ((RequestMessage) msg).get_ipv4(), ((RequestMessage) msg).get_port());
            P2PMessage msgResponse = new ReplyMessage("ERROR");
            P2PMessage.clone_msgID(msgResponse, msg);
            send_reply(msgResponse, sockRead, rc);
        }
    }

    private void send_reply(P2PMessage msg, Socket sockRead, P2PRequestEventI rc) {
        if (sockRead != null) {
            P2PMessageIO.send(msg, sockRead, p2pNode);
        }
        if (rc != null) {
            rc.readComplete(msg.getPayload());
        }
    }
    
    private boolean forThisNode(P2PMessage msg) {
        if ( ((RequestMessage) msg).get_ipv4() == p2pNode.get_advertised_ipv4() && ((RequestMessage) msg).get_port() ==  p2pNode.get_advertised_port() )
            return true;
        return false;
    }

    // When we receive a REPLY, find a return route, if one is found, send it there
    // Otherwise, we sent the REQUEST, so notify the app that called us
    public synchronized void RouteReply(P2PMessage msg, Socket sockRead) {
        
        ForwardedMsgEntry e = forwards.find(msg);
      
        if ( e == null ) {
            P2PLogger.log("ReplyRoutings(): %s received a REPLY but have no record of matching REQUEST - dropping! Should not happen.\n", p2pNode.getname());
        } else if ( e.sockFrom  == null ) {
            P2PLogger.log("ReplyRoutings(): %s REPLY has returned!\n", p2pNode.getname());
            signalIOComplete(msg, e);
        } else {
            P2PLogger.log("ReplyRoutings: %s matching REQUEST - route back to %s\n", p2pNode.getname(), e.sockFrom);
            P2PMessageIO.send(msg, e.sockFrom, p2pNode);
        }
    }
    
    private void signalIOComplete(P2PMessage msg, ForwardedMsgEntry e) {
        if ( e.rc != null ) {
            e.rc.readComplete(msg.getPayload());
        }
    } 
           
    public synchronized void reportStats() {
        rt.print();
        forwards.print();
        floodList.print();
    }
}

// class RoutingTable
// binds (ipv4, port, socket)
// lookup on (ipv4, port)
class RoutingTable {
    
    private final LinkedList<RoutingEntry> routings;
    private int numRoutingsRecords = 0;
    private final P2PNode p2pNode;
    
    public RoutingTable(P2PNode p2pNode) {
        routings = new LinkedList<>();
        this.p2pNode = p2pNode;
    }

    // Policy on this list is most recently seen advertisement.
    // So if the (ipv4,port) is already on the list, just udpate it in place.
    public void add(int ipv4, short port, Socket sockTo) {
        for ( RoutingEntry e: routings ) {
            if ( e.ipv4 == ipv4 && e.port == port ) {
                e.update(sockTo);
                return;
            }
        }
        RoutingEntry route = new RoutingEntry(ipv4, port, sockTo);
        routings.addFirst(route);
        if ( numRoutingsRecords++ >= P2PRoutings.MAX_ROUTINGS_RECORDS ) {
            routings.removeLast();
            numRoutingsRecords = P2PRoutings.MAX_ROUTINGS_RECORDS;
        }
        System.out.printf("addRoutingEntry(): %s %d %d %s\n", p2pNode.getname(), ipv4, port, sockTo);
    }

    // match (ipv4,port) and return the socket to route it on
    public Socket findRoute(P2PMessage msg) {
        for ( RoutingEntry e: routings ) {
            if ( e.ipv4 == ((RequestMessage) msg).get_ipv4() && e.port == ((RequestMessage) msg).get_port() )
                return e.sockTo;
        }
        return null;
    }  

    public void print() {
        for (RoutingEntry e : routings) {
           P2PLogger.log("ROUTING: %s %d %d %s\n", p2pNode.getname(), e.ipv4, e.port, e.sockTo);
        }
    }
}

class RoutingEntry {
    int ipv4;       // unique key
    short port;     // unique key
    Socket sockTo;
    public RoutingEntry(int ipv4, short port, Socket sockTo) {
        this.ipv4 = ipv4;
        this.port = port;
        this.sockTo = sockTo;
    }
    public void update(Socket sockTo) {
        this.sockTo = sockTo;
    }
} 

// class ForwardedMessageList
//
// Keeps a rolling history of PING and REQUEST messages sent.
// Each entry in the list binds (msgId,sockTo,sockFrom)
// Duplicates do not matter and are not checked.
// Only PING and REQUEST are put on this list

class ForwardedMessageList {
    
    private final LinkedList<ForwardedMsgEntry> forwarded;
    private int numForwardRecords = 0;
    private final P2PNode p2pNode;
    
    public ForwardedMessageList(P2PNode p2pNode) {
        this.forwarded = new LinkedList<>();
        this.p2pNode = p2pNode;
    }

    public void add(P2PMessage msg, Socket sockTo, Socket sockFrom, P2PRequestEventI rc) {
 
        // it is a coding error within this program to put invalid msgType onto this list
        // sockTo must not be null, if that happens, it probably means a coding error
        // I am logging these conditions, to be safe (defensively programmed)
        
        if ( msg.get_msgType() != P2PMessage.MSGTYPE_PING && msg.get_msgType() != P2PMessage.MSGTYPE_REQUEST ) {
            P2PLogger.log("ERROR: ForwardMessageList.add() - trying to put wrong type of message on forwarded list - probably programming error\n");
            return;
        }
        
        if ( sockTo == null ) {
            P2PLogger.log("ERROR: ForwardMessageList.add() - sockTo cannot be null - probably programming error\n");
            return;
        }
            
        ForwardedMsgEntry e = new ForwardedMsgEntry(msg, sockTo, sockFrom, rc);
        forwarded.addFirst(e);
        if ( numForwardRecords++ >= P2PRoutings.MAX_FORWARD_RECORDS ) {
            forwarded.removeLast();
            numForwardRecords = P2PRoutings.MAX_FORWARD_RECORDS;
        }
        
        System.out.printf("ForwardedMessageList.add(): %s %s %s\n", p2pNode.getname(), sockTo, sockFrom);
    }

    // match (msgId, socket)
    public ForwardedMsgEntry find(P2PMessage msg, Socket sockRead) {
        for (ForwardedMsgEntry e : forwarded )
            if ( P2PMessage.compare_uuid(e.msg.get_msgId(), msg.get_msgId()) && e.sockTo == sockRead )
                return e;
        return null;
    }
    
    // match (msgId)
    public ForwardedMsgEntry find(P2PMessage msg) {
        for (ForwardedMsgEntry e : forwarded )
            if ( P2PMessage.compare_uuid(e.msg.get_msgId(), msg.get_msgId()) )
                return e;
        return null;
    }
    
    public void print() {
        for (ForwardedMsgEntry e : forwarded) {
            P2PLogger.log("FORWARDS: %s %s\n", p2pNode.getname(), e);
        }
    }
}

class ForwardedMsgEntry {
    P2PMessage msg;         // msg.msgType must be PING or REQUEST
    Socket sockTo;
    Socket sockFrom;        // null if this node made the message new
    P2PRequestEventI rc;    // is set only for the node that placed the origninal REQUEST
    public ForwardedMsgEntry(P2PMessage msg, Socket sockTo, Socket sockFrom, P2PRequestEventI rc) {
        this.msg = msg;
        this.sockTo = sockTo;
        this.sockFrom = sockFrom;
        this.rc = rc;
    }
}

// class FloodList
// Keeps a list of sockets opened to this node and sockets opened by this node
// Both PING and PONG messages get flooded and each has a specialized flood method to support it

class FloodList {
    private LinkedList<Socket> flist;
    private int numFloodSockets = 0;
    private final P2PNode p2pNode;
    private final ForwardedMessageList forwards;
    
    public FloodList(P2PNode p2pNode, ForwardedMessageList forwards) {
        this.flist = new LinkedList<>();
        this.p2pNode = p2pNode;
        this.forwards = forwards;
    }
 
    // It is safe to call addFloodsocket() for the same socket > 1 time; it will be added to the list only the first time.
    public synchronized void add(Socket s) {      
        for ( Socket e : flist )
            if ( e == s )
                return;
        flist.addFirst(s);
        if ( numFloodSockets++ >= P2PRoutings.MAX_FLOOD_LIST ) {
            flist.removeLast();
            numFloodSockets = P2PRoutings.MAX_FLOOD_LIST;
        }
    }

    public void print() {
        for (Socket s : flist) {
            P2PLogger.log("FLOODLIST: %s %s\n", p2pNode.getname(), s);
        }
    }
    
    // broadcast a PING
    // a new PING has sockFrom == null
    // a PING being forwarded has sockFrom set to the socket the PING arrived on
    // if the socket we just read it from is on the flood list skip sending on that socket
    public void doFloodPing(P2PMessage msg, Socket sockFrom) {
        for (Socket s : flist) {
            if ( s != sockFrom ) {
                forwards.add(msg, s, sockFrom, null);
                P2PMessageIO.send(msg, s, p2pNode);
            }
        }
    }

    // When we broadcast a PONG, we want to avoid sending to 2 particular sockets
    // 1 - the socket we read the PONG on, we never want to echo a message
    // 2 - the socket we might have routed it back on (sockFrom)
    public void doFloodPong(P2PMessage msg, Socket sockFrom, Socket sockRead) {
        for (Socket s : flist) {
            if ( s != sockFrom && s != sockRead ) {
                P2PMessageIO.send(msg, s, p2pNode);
            }
        }
    }
}
