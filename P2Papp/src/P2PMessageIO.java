/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.logging.*;

public class P2PMessageIO {

    private P2PMessageIO() {
    }

    public static void send(P2PMessage msg, Socket sock, P2PNode p2pNode) {
        try {
            byte[] header = msg.encodeHeader();
            byte[] payload = msg.getPayload();

            P2PLogger.log("SEND: %s s=%s\n%s", p2pNode.getname(), sock, msg.toString());
         
            if (msg.get_TTL() <= 0) {
                P2PLogger.log("%s TTL EXPIRED - Will not SEND msgId=%s msgType=%d\n", p2pNode.getname(), Arrays.toString(msg.get_msgId()), msg.get_msgType());
                return;
            }

            OutputStream sock_out = sock.getOutputStream();
            sock_out.write(header);
            
            if ( payload != null ) {
                sock_out.write(payload);
            }
        } catch (IOException ex) {
            Logger.getLogger(PingMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    public static void ReadAndDispatch(Socket s, P2PNode p2pNode) {
        read_and_dispatch_next_message(s, p2pNode);
    }
    
    private static void read_and_dispatch_next_message(Socket s, P2PNode p2pNode) {
        
        byte[] buffer = new byte[P2PMessage.MAX_PACKET_SIZE];   // TODO
        
        try {
            
            InputStream sock_input = s.getInputStream();

            for (int i = 0; i < P2PMessage.HEADER_SIZE; i++) {
                buffer[i] = (byte) sock_input.read();
            }

            // TODO: This will fail on large transmissions because payload_size can be > 1500, we have to loop reading buffers at a time
            // read the payload and append it to buffer[i], do not write past length allocated for buffer[]
            // note that payload_size was read from wire and could be arbitrarily large

            int payload_size = P2PMessage.parsePayloadSize(buffer);

            for (int i = P2PMessage.HEADER_SIZE, j = 0, len = buffer.length; i < len && j < payload_size; i++, j++) {
                buffer[i] = (byte) sock_input.read();
            }

        } catch (IOException ex) {
            Logger.getLogger(P2PConnectionReadThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        parse_and_dispatch_received_message(buffer, s, p2pNode);
    }
 
    // look into the buffer header and figure out the type of message just received then make and dispatch one
    private static void parse_and_dispatch_received_message(byte[] buffer, Socket s, P2PNode p2pNode) {
                
        switch (P2PMessage.parseMsgType(buffer)) {
            
            case P2PMessage.MSGTYPE_PING:
                dispatch_msg_ping(new PingMessage(buffer), s, p2pNode);
                break;
                
            case P2PMessage.MSGTYPE_PONG:
                dispatch_msg_pong(new PongMessage(buffer), s, p2pNode);
                break;
        
            case P2PMessage.MSGTYPE_REQUEST:
                dispatch_msg_request(new RequestMessage(buffer), s, p2pNode);
                break;
            
            case P2PMessage.MSGTYPE_REPLY:
                dispatch_msg_reply(new ReplyMessage(buffer), s, p2pNode);
                break;
            
            default:
                P2PLogger.log("ERROR: BAADDATA unknown msgType nodeName=%s\n", p2pNode.getname());
                break;
        }
    }
    
    private static void dispatch_msg_ping(P2PMessage msg, Socket s, P2PNode p2pNode) {
        update_message_counters(msg, s, p2pNode);
        p2pNode.RoutePing(msg, s);
    }
    
    private static void dispatch_msg_pong(P2PMessage msg, Socket s, P2PNode p2pNode) {
        update_message_counters(msg, s, p2pNode);
        p2pNode.RoutePong(msg, s);
    }
    
    private static void dispatch_msg_request(P2PMessage msg, Socket s, P2PNode p2pNode) {
        update_message_counters(msg, s, p2pNode);
        p2pNode.RouteRequest(msg, s);
    }

    private static void dispatch_msg_reply(P2PMessage msg, Socket s, P2PNode p2pNode) {
        update_message_counters(msg, s, p2pNode);
        p2pNode.RouteReply(msg, s);
    }

    private static void update_message_counters(P2PMessage msg, Socket s, P2PNode p2pNode) {
        P2PLogger.log("RECV: %s s=%s\n%s", p2pNode.getname(), s, msg.toString());

        // increment TTL decrement nHops
        msg.updateTTL();
        
        // check the cache of recently seen messages
        if (p2pNode.lookup_received_message(msg)) {
            P2PLogger.log("%s PACKET ROUTED IN A LOOP - DROPPING msgId=%s msgType=%d\n", p2pNode.getname(), Arrays.toString(msg.get_msgId()), msg.get_msgType());
            return;
        }

        // add this new receive to the recently seen message cache
        p2pNode.add_received_message(msg);
    }
}
