package p2p;

import java.util.*;
import java.io.*;
import java.text.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class PeerLogger {

    private String peerID;
    private String fileName;
    private FileHandler fileHandler;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd-yyyy hh:mm:ss a");
    private Logger logger;

    public PeerLogger(String peerId) {
        this.peerID = peerId;
        this.fileName = "peer_" + this.peerID + ".log";
        try {
            this.fileHandler = new FileHandler(this.fileName, false);
            System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s %n");
            this.fileHandler.setFormatter(new SimpleFormatter());
            this.logger = Logger.getLogger("P2PLogger");
            this.logger.setUseParentHandlers(false);
            this.logger.addHandler(this.fileHandler);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void receivedHandshakeMessage(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] has received a handshake message from Peer " + "[" + neighborPeerID + "].");
    }

    public synchronized void initiatedconnectionEstablishment(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] has initiated a TCP connection to Peer " + "[" + neighborPeerID + "].");
    }

    public synchronized void connectionEstablished(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] is connected to Peer " + "[" + neighborPeerID + "].");
    }

    public synchronized void sentBitField(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] has sent its bitfield to Peer " + "[" + neighborPeerID + "].");
    }

    public synchronized void receivedBitField(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] has received a bitfield message from Peer " + "[" + neighborPeerID + "].");
    }

    public synchronized void receivedRequestMessage(String neighborPeerID, String pieceIndex) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] has received a request message for piece [" + pieceIndex + "] from Peer " + "[" + neighborPeerID + "].");
    }

    public synchronized void sentPieceMessage(String neighborPeerID, String pieceIndex) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] has sent piece [" + pieceIndex + "] Peer " + "[" + neighborPeerID + "].");
    }

    public synchronized void preferredNeighborsChanged(List<String> neigbors) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] has the following preferred neighbors [" + neigbors.toString() + "].");
    }

    public synchronized void optimisticallyUnchokedNeighborChanged(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] has the following peer as optimistically unchoked neighbor [" + neighborPeerID + "].");
    }

    public synchronized void unchokedNeighbor(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] is unchoked by [" + neighborPeerID + "].");
    }

    public synchronized void chokedNeighbor(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] is choked by [" + neighborPeerID + "].");
    }

    public synchronized void receivedHaveMessage(String neighborPeerID, int index) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] received a ‘have’ message from [" + neighborPeerID + "] for the piece [" + String.valueOf(index) + "].");
    }

    public synchronized void sendInterestedMessage(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] sent an ‘interested’ message to [" + neighborPeerID + "].");
    }

    public synchronized void receivedInterestedMessage(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID + "] received an ‘interested’ message from [" + neighborPeerID + "].");
    }

    public synchronized void sendNotInterestedMessage(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID
                + "] sent a ‘not interested’ message to [" + neighborPeerID + "].");
    }

    public synchronized void receivedNotInterestedMessage(String neighborPeerID) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerID
                + "] received the ‘not interested’ message from [" + neighborPeerID + "].");
    }

    public synchronized void downloadedPiece(String neighborPeerID, int ind, int pieces) {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO,
                "[" + currTime + "]: Peer [" + this.peerID + "] has downloaded the piece [" + String.valueOf(ind)
                        + "] from [" + neighborPeerID + "]. The number of pieces it has is [" + String.valueOf(pieces) + "].");
    }

    public synchronized void downloadCompleted() {
        Calendar calendar = Calendar.getInstance();
        String currTime = this.dateFormat.format(calendar.getTime());
        this.logger.log(Level.INFO,
                "[" + currTime + "]: Peer [" + this.peerID + "] has downloaded the complete file.");
    }

    public void deconstructLogger() {
        try {
            if (this.fileHandler != null) {
                this.fileHandler.close();
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}