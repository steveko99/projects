/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/
import java.util.LinkedList;

class P2PReceivedMessageCache {
 
    private final LinkedList<P2PReceivedMessageCacheEntry> msgList;
    private static final int MAX_RECENT = 200;
    private int nEntries = 0;

    public P2PReceivedMessageCache() {
        msgList = new LinkedList<>();
    }   

    public synchronized void add(P2PMessage msg, int ip, short port) {
        //P2PLogger.log("P2PReceiveMessageCache.add(): nEntries=%d, id=%s\n", nEntries, Arrays.toString(msg.get_msgId()));
        P2PReceivedMessageCacheEntry msgEntry = new P2PReceivedMessageCacheEntry(msg, ip, port);
        msgList.addFirst(msgEntry);
        if ( nEntries++ >= MAX_RECENT ) {
            msgList.removeLast();
            nEntries = MAX_RECENT;
        }
    }

    // keyed on (msgId,msgType)
    public synchronized boolean lookup(P2PMessage msg) {
        boolean result = msgList.stream().anyMatch((e) -> ( P2PMessage.compare_uuid(e.msg.get_msgId(), msg.get_msgId()) && e.msg.get_msgType() == msg.get_msgType() ));
        //P2PLogger.log("P2PReceiveMessageCache.lookup(): nEntries=%d, result=%s\n", nEntries, result);
        return result;
    }
}

class P2PReceivedMessageCacheEntry {
    P2PMessage msg;
    int sender_ipv4;
    short sender_port;

    public P2PReceivedMessageCacheEntry(P2PMessage msg, int ip, short port) {
        this.msg = msg;
        this.sender_ipv4 = ip;
        this.sender_port = port;
    }
}
