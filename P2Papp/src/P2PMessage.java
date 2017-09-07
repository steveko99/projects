/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/

/*
The payloads in this project have the following layout
Fields are fixed length as noted
The header is 23 bytes long

bytes[23] header
    message_ID[0:15]        // unique for each message
    type[16]
    TTL[17]
    hops[18]
    payload_length[19:22]

type enum
    0: ping
    1: pong
    2: query
    3: reply

----
Ping has no data to encapsulate
    type=0 payload_length=0

----
bytes[6] Pong
    listen_port[0:1]
    ipv4_addr[2:5]

----
Query has no data to encapsulate
    type=2, payload_length=0

----
bytes[] Reply
    listen_port[0:1]
    ipv4_addr[2:5]
    ascii_string[]

*/

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PMessage {
    private byte[] msgID;           // uuid
    private byte msgType;           // { PING=0, PONG, REQUEST, REPLY }
    private byte TTL;
    private byte hops;
    private int payload_length;
    private byte[] payload;         // payload_length == 0 implies payload == null

    protected final static int HEADER_SIZE = 23;
    private final static int OFFSET_MSGID = 0;
    private final static int OFFSET_MSGTYPE = 16;
    private final static int OFFSET_TTL = 17;
    private final static int OFFSET_HOPS = 18;
    private final static int OFFSET_PAYLOAD_LENGTH = 19;
    private final static int OFFSET_PAYLOAD = 23;
    private final static int SIZE_MSGID = 16;           // sizeof(UUID)
    private final static int SIZE_PAYLOAD_LENGTH = 4;   // sizeof(int)
    private final static int DEFAULT_TTL = 10;
    private final static byte MSGTYPE_INVALID = -1;
    public final static byte MSGTYPE_PING = 0;
    public final static byte MSGTYPE_PONG = 1;
    public final static byte MSGTYPE_REQUEST = 2;
    public final static byte MSGTYPE_REPLY = 3;

    // mske sure this is the max possible payload size on the wire
    public final static int MAX_PACKET_SIZE = HEADER_SIZE + PongMessage.HEADER_SIZE;
    
    public P2PMessage() {
        msgID = generate_msgID();
        msgType = MSGTYPE_INVALID;
        TTL = DEFAULT_TTL;
        hops = 0;
        payload_length = 0;
        payload = null;
    }

    public byte[] get_msgId()   { return this.msgID; }
    public byte get_msgType()   { return this.msgType; }
    public byte get_TTL()       { return this.TTL;   }
    public byte get_hops()      { return this.hops; }
    public int payload_length() { return this.payload_length; }
    
    // msgID is a UUID which is 128 bit number encoded at byte[16] big endian
    private byte[] generate_msgID() {
        UUID uuid1 = UUID.randomUUID();
        return msgUtils.toByteArray(uuid1);
    }
    
    // only subclass can set the msgType and the payload
    protected void set_msgType(byte msgType) {
        this.msgType = msgType;
    }
    
    protected void set_payload(byte[] payload) {
        this.payload_length = payload.length;
        this.payload = payload;
    }

    public void updateTTL() {
        this.TTL--;
        this.hops++;
    }

    // this should only be called when a packet is birthed and caller wants to
    // change the TTL instead of leaving the default e.g. a crawler app
    public void resetTTL(byte TTL) {
        this.TTL = TTL;
        this.hops = 0;
    }
    
    public static int parsePayloadSize(byte[] header) {
        return msgUtils.bytesToInt(header, OFFSET_PAYLOAD_LENGTH);
    }

    public static byte parseMsgType(byte[] header) {
        return header[OFFSET_MSGTYPE];
    }
    
    // Receiver calls parseHeader()
    public void parseHeader(byte[] buffer) {
        msgUtils.copyBytes(msgID, OFFSET_MSGID, buffer, SIZE_MSGID);
        msgType = buffer[OFFSET_MSGTYPE];
        TTL  = buffer[OFFSET_TTL];
        hops = buffer[OFFSET_HOPS];
        payload_length = msgUtils.bytesToInt(buffer, OFFSET_PAYLOAD_LENGTH);
        // TODO: payload_length has to be checked
        if ( payload_length > 1 && payload_length < 1500 ) {
            payload = new byte[payload_length];
            payload = java.util.Arrays.copyOfRange(buffer, OFFSET_PAYLOAD, OFFSET_PAYLOAD+payload_length);
        }
    }
    
    // Sender calls encodeHeader()
    public byte[] encodeHeader() {
        byte[] header = new byte[HEADER_SIZE];
        msgUtils.copyBytes(header, OFFSET_MSGID, msgID, SIZE_MSGID);
        header[OFFSET_MSGTYPE] = msgType;
        header[OFFSET_TTL] = TTL;
        header[OFFSET_HOPS] = hops;
        msgUtils.copyBytes(header, OFFSET_PAYLOAD_LENGTH, msgUtils.toByteArray(payload_length), SIZE_PAYLOAD_LENGTH);
        return header;
    }
    
    public byte[] getPayload() {
        return payload;
    }
    
    public static void clone_msgID(P2PMessage msg_dest, P2PMessage msg_src) {
        msg_dest.msgID = msg_src.msgID;
    }

    // compares 2 uuid
    public static boolean compare_uuid(byte[] id1, byte[] id2) {
        if ( id1.length != 16 || id2.length != 16 )
            return false;
        for ( int i=0; i<16; i++ ) {
            if ( id1[i] != id2[i] )
                return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        String s =  String.format("\t1-hdr msgID: %s\n", Arrays.toString(this.get_msgId())) +
                    String.format("\t2-hdr msgType: %d\n", this.get_msgType()) +
                    String.format("\t3-hdr TTL: %d\n", this.get_TTL()) +
                    String.format("\t4-hdr hops: %d\n", this.get_hops()) +
                    String.format("\t5-hdr payload_length: %d\n", this.payload_length());
        return s;
    }
}

class PingMessage extends P2PMessage {
    
    public PingMessage() {
        super.set_msgType((byte)0);
    }
    
    // construct from data just received on the wire
    public PingMessage(byte[] data) {
        super.parseHeader(data);
    }
    
    @Override
    public String toString() {
        return super.toString();
    }
}

class PongMessage extends P2PMessage {
    private final short port;
    private final int ipv4;

    protected static final int PONG_PAYLOAD_SIZE = 6;
    private static final int OFFSET_PONG_PORT = 0;  // bytes [23:24] of header
    private static final int OFFSET_PONG_IPV4 = 2;  // bytes [25:28] of header
    private static final int SIZE_PONG_PORT = 2;    // sizeof(short)
    private static final int SIZE_PONG_IPV4 = 4;    // sizeof(int)
    
    // construct from assigned values (to send)
    public PongMessage(int ipv4, short port) {
        super.set_msgType((byte)1);
        this.ipv4 = ipv4;
        this.port = port;
    
        byte[] pongMsg = new byte[PONG_PAYLOAD_SIZE];
        msgUtils.copyBytes(pongMsg, OFFSET_PONG_PORT, msgUtils.toByteArray(this.port), SIZE_PONG_PORT);
        msgUtils.copyBytes(pongMsg, OFFSET_PONG_IPV4, msgUtils.toByteArray(this.ipv4), SIZE_PONG_IPV4);
        super.set_payload(pongMsg);
    }

    // construct from a received payload
    public PongMessage(byte[] data) {
        super.parseHeader(data);
        port = msgUtils.bytesToShort(data, HEADER_SIZE + OFFSET_PONG_PORT);
        ipv4 = msgUtils.bytesToInt(data, HEADER_SIZE + OFFSET_PONG_IPV4);
    }
    
    public int get_ipv4() { return this.ipv4; };
    public short get_port() { return this.port; };

    @Override
    public String toString() {
        String s =  String.format("\t0-port: %d\n", this.port) +
                    String.format("\t1-ip: %d\n", this.ipv4);
        return super.toString() + s;
    }
}

class RequestMessage extends P2PMessage {
    private final short port;
    private final int ipv4;

    protected static final int REQUEST_PAYLOAD_SIZE = 6;
    private static final int OFFSET_REQUEST_PORT = 0;  // bytes [23:24] of header
    private static final int OFFSET_REQUEST_IPV4 = 2;  // bytes [25:28] of header
    private static final int SIZE_REQUEST_PORT = 2;    // sizeof(short)
    private static final int SIZE_REQUEST_IPV4 = 4;    // sizeof(int)
    
    // construct from assigned values (to send)
    public RequestMessage(int ipv4, short port) {
        super.set_msgType((byte)2);
        this.ipv4 = ipv4;
        this.port = port;
    
        byte[] requestMsg = new byte[REQUEST_PAYLOAD_SIZE];
        msgUtils.copyBytes(requestMsg, OFFSET_REQUEST_PORT, msgUtils.toByteArray(this.port), SIZE_REQUEST_PORT);
        msgUtils.copyBytes(requestMsg, OFFSET_REQUEST_IPV4, msgUtils.toByteArray(this.ipv4), SIZE_REQUEST_IPV4);
        super.set_payload(requestMsg);
    }

    // construct from a received payload
    public RequestMessage(byte[] data) {
        super.parseHeader(data);
        port = msgUtils.bytesToShort(data, HEADER_SIZE + OFFSET_REQUEST_PORT);
        ipv4 = msgUtils.bytesToInt(data, HEADER_SIZE + OFFSET_REQUEST_IPV4);
    }
    
    public int get_ipv4() { return this.ipv4; };
    public short get_port() { return this.port; };
    
    @Override
    public String toString() {
        String s =  String.format("\t0-port: %d\n", this.port) +
                    String.format("\t1-ip: %d\n", this.ipv4);
        return super.toString() + s;
    }
}

class ReplyMessage extends P2PMessage {

    public ReplyMessage() {
        super.set_msgType((byte)3);
    }
    
    // construct from data just received on the wire
    public ReplyMessage(byte[] data) {
        super.parseHeader(data);
    }
    
    public ReplyMessage(String pdata) {
        super.set_msgType((byte)3);
        try {
            super.set_payload(pdata.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ReplyMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
  
    public byte[] getResponse() {
        return this.getPayload();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

//------------------------------------------------------------------------------
// msgUtils: functions that translate between byte arrays and intrinsic data
// types needed to encode and decode packets
// all numbers are encoded bigendian (network byte order)
//------------------------------------------------------------------------------
class msgUtils {
    
    private msgUtils(){
    }
    
    // copies parts of a byte array to another
    // copy from src[] to a starting point in dest[]
    // to illustrate: first byte copied is: dest[start] <= src[0]
    // don't exceed max bytes copied
    // and don't reference past the end of either array
    public static void copyBytes(byte[] dest, int start, byte[] src, int max)
    {
        for ( int i=start, len1=dest.length, j=0, len2=src.length;
              i<len1 && j<max && j<len2;
              i++, j++ )
        {
            dest[i] = src[j];
        }
    }
    
    public static byte[] toByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i);
        return result;
    }
    
    // uuid size is fixed to 16 bytes
    public static byte[] toByteArray(UUID uuid) {
        long ls = uuid.getLeastSignificantBits();
        long ms = uuid.getMostSignificantBits(); 
        
        byte[] result = new byte[16];
        
        result[0] = (byte) (ms >> 56);
        result[1] = (byte) (ms >> 48);
        result[2] = (byte) (ms >> 40);
        result[3] = (byte) (ms >> 32);
        result[4] = (byte) (ms >> 24);
        result[5] = (byte) (ms >> 16);
        result[6] = (byte) (ms >> 8);
        result[7] = (byte) ms;
 
        result[8]  = (byte) (ls >> 56);
        result[9]  = (byte) (ls >> 48);
        result[10] = (byte) (ls >> 40);
        result[11] = (byte) (ls >> 32);
        result[12] = (byte) (ls >> 24);
        result[13] = (byte) (ls >> 16);
        result[14] = (byte) (ls >> 8);
        result[15] = (byte) ls;
        
        return result;
    }

    public static byte[] toByteArray(short i) {
        byte[] result = new byte[2];
        result[0] = (byte) (i >> 8);
        result[1] = (byte) (i);
        return result;
    }

    public static int bytesToInt(byte[] data, int start) {
        if ( start+3 >= data.length )
            return 0;
        int b1 = (data[start]   << 24) & 0xff000000;
        int b2 = (data[start+1] << 16) & 0x00ff0000;
        int b3 = (data[start+2] <<  8) & 0x0000ff00;
        int b4 = (data[start+3])       & 0x000000ff;
        int x = b1 | b2 | b3 | b4;
        return x;
    }
    
    public static short bytesToShort(byte[] header, int start) {
        if ( start+1 >= header.length )
            return (short) 0;
        int b1 = (header[start] << 8) & 0x0000ff00;
        int b2 = (header[start+1])    & 0x000000ff;
        int x = (b1 | b2) & 0x0000ffff;
        return (short) x;
    }
}
