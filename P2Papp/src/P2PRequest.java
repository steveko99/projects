/*
public abstract class P2PRequest implements ReadCompleteEventI {
    
    private P2PNode p2pNode;
    private P2PRequestEventI rc;
    
    public void P2PRequest(P2PNode p2pNode, P2PRequestEventI rc) {
        this.p2pNode = p2pNode;
        this.rc = rc;
    }
    
    public void makeRequest(int ipv4, short port) {
        p2pNode.makeRequest(ipv4, port, rc);
    }
}


class ReadCompleteEvent implements ReadCompleteEventI {
    
    public ReadCompleteEvent() {   
    }
    
    @Override
    public void readComplete(byte[] response) {
        System.out.println("WOOHOO");
    }
}
*/