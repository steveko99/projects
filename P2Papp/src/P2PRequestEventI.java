/*-------------------------------------------------------------------------------------------------
    P2PApp - Peer to Peer app

    Author: Steve Koscho

    Date: Jan 19, 2017

    See design document checked in with the source code
-------------------------------------------------------------------------------------------------*/

public interface P2PRequestEventI {
    public void readComplete(byte[] payload);
}
