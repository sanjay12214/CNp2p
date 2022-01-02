package p2p;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class PeerServer implements Runnable {
	private String peerID;
	private ServerSocket listener;
	private PeerAdmin peerAdmin;
	private boolean isFinished;

	public PeerServer(String peerID, ServerSocket listener, PeerAdmin admin) {
		this.peerID = peerID;
		this.listener = listener;
		this.peerAdmin = admin;
		this.isFinished = false;
	}

	public void run() {
		while (!this.isFinished) {
			try {
				Socket neighborConnection = this.listener.accept();
				PeerUtils neighborPeerUtils = new PeerUtils(neighborConnection	, this.peerAdmin);
				new Thread(neighborPeerUtils).start();
			} 
			catch (SocketException e) {
				break;
			} 
			catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
	}
}
