package p2p;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class HandshakeMessage {
    private String handshakeHeader;
    private String peerID;
    
    public HandshakeMessage(String peerID) {
        this.handshakeHeader = "P2PFILESHARINGPROJ";
        this.peerID = peerID;
    }

    public String getPeerID(){
        return this.peerID;
    }

    public String getHandshakeHeader() {
        return handshakeHeader;
    }

    public void setHandshakeHeader(String handshakeHeader) {
        this.handshakeHeader = handshakeHeader;
    }

    public void setPeerID(String peerID) {
        this.peerID = peerID;
    }

    public byte[] constructHandshakeMessage() {
        /*
            This method is responsible for constructing the handshake message
            A handshake message contains the handshakeHeader + zero bits + peerID
        */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(this.handshakeHeader.getBytes(StandardCharsets.UTF_8));
            baos.write(new byte[10]);
            baos.write(this.peerID.getBytes(StandardCharsets.UTF_8));
        } 
        catch(Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    public void readHandshakeMessage(byte[] messageBytes){
        /*
            This message is responsible for reading the peer ID from
            the handshake message.
         */
        String handshakeMessage = new String(messageBytes,StandardCharsets.UTF_8);
        this.peerID = handshakeMessage.substring(28,32);
    }
}
