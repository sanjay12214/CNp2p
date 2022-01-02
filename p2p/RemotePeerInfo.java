package p2p;

public class RemotePeerInfo {
	public String peerID;
	public String peerAddress;
	public int peerPort;
	public int containsFile;

	public RemotePeerInfo(String peerID, String hostname, String portNumber, String isFileAvailable) {
		this.peerID = peerID;
		this.peerAddress = hostname;
		this.peerPort = Integer.parseInt(portNumber);
		this.containsFile = Integer.parseInt(isFileAvailable);
	}
	
}
