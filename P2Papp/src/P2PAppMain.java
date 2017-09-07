/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PAppMain {

    public static void main(String[] args) {
        P2PLogger.log("P2Papp: start\n");
              
        if ( args.length == 0 ) {
            TestLaunch t = new TestLaunch();
            t.LaunchTestNetwork();        
            return;
        }

        if ( args.length < 3 ) {
            System.out.println("Usage: P2Papp listen_port advertised_ip:port remote_ip:port");
        }

        int listen_port = Integer.parseInt(args[0]);    // TODO: have to check for exceptions and bad input
        String[] remote_ip_split = args[2].split(":");
        String[] advertised_ip_split = args[1].split(":");
        
        System.out.println(listen_port);
        for ( String s: remote_ip_split ) System.out.println(s);
        for ( String s: advertised_ip_split ) System.out.println(s);
    }
    
    static int parseip(String s) {
        try {
            InetAddress addr = InetAddress.getByName(s);
            System.out.println(addr);
        } catch (UnknownHostException ex) {
            Logger.getLogger(P2PAppMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }
    
    short parseport(String s) {
        return (short)0;
    }
}

class TestNode {

    public String node_name;
    public int remote_ipv4;
    public short remote_port;
    public short listen_port;

    TestNode(String node_name, int remote_ipv4, short remote_port, short listen_port) {
        this.node_name = node_name;
        this.remote_ipv4 = remote_ipv4;
        this.remote_port = remote_port;
        this.listen_port = listen_port;
    }
}

class TestLaunch implements P2PRequestEventI {

    public TestLaunch() {
    }
    
    // criss-cross
    TestNode[] nodes1 = {
                   // name     remote_ip   remote_port  listen_port
        new TestNode( "NODE0", 0x7f000001, (short)4001, (short)4000),
        new TestNode( "NODE1", 0x7f000001, (short)4000, (short)4001)
    };
    
    // star
    TestNode[] nodes2 = {
                   // name     remote_ip   remote_port  listen_port
        new TestNode( "NODE0", 0x7f000001, (short)4000, (short)4000),
        new TestNode( "NODE1", 0x7f000001, (short)4000, (short)4001),
        new TestNode( "NODE2", 0x7f000001, (short)4000, (short)4002),
        new TestNode( "NODE3", 0x7f000001, (short)4000, (short)4003),
        new TestNode( "NODE4", 0x7f000001, (short)4000, (short)4004),
        new TestNode( "NODE5", 0x7f000001, (short)4000, (short)4005),
        new TestNode( "NODE6", 0x7f000001, (short)4000, (short)4006) 
    };

    // circle
    TestNode[] nodes3 = {
                   // name     remote_ip   remote_port  listen_port
        new TestNode( "NODE0", 0x7f000001, (short)4006, (short)4000),
        new TestNode( "NODE1", 0x7f000001, (short)4000, (short)4001),
        new TestNode( "NODE2", 0x7f000001, (short)4001, (short)4002),
        new TestNode( "NODE3", 0x7f000001, (short)4002, (short)4003),
        new TestNode( "NODE4", 0x7f000001, (short)4003, (short)4004),
        new TestNode( "NODE5", 0x7f000001, (short)4004, (short)4005),
        new TestNode( "NODE6", 0x7f000001, (short)4005, (short)4006) 
    };

    // node[0] never pings another, but node[1] opens a connection to it
    TestNode[] nodes4 = {
                   // name     remote_ip   remote_port  listen_port
        new TestNode( "NODE0", 0x7f000001, (short)4000, (short)4000),
        new TestNode( "NODE1", 0x7f000001, (short)4000, (short)4001),
        new TestNode( "NODE2", 0x7f000001, (short)4001, (short)4002)
    };
    
    public void LaunchTestNetwork() {
        LaunchNetwork(nodes2);
    }
    
    private void LaunchNetwork(TestNode[] nodes) {
        
        P2PNode[] nodeList = new P2PNode[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            TestNode n = nodes[i];
            nodeList[i] = new P2PNode(n.node_name, n.remote_ipv4, n.remote_port, n.listen_port);
        }

        for (int i = 0; i < nodes.length; i++) {
            nodeList[i].start();
        }

        while (true) {
            for (int i = 0; i < nodes.length; i++) {
                //for (int j = 0; j < nodes.length; j++) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TestLaunch.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    nodeList[2].makeRequest(0x7f000001, nodeList[i].get_listen_port(), this);
                //}
            }
        }
    }

    @Override
    public void readComplete(byte[] payload) {
        System.out.println("RESPONSE: " + Arrays.toString(payload));
    }
}
