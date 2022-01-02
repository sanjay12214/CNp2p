package p2p;

import java.io.*;
import java.lang.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

public class PeerAdmin {
	private int pieceCount;
	private String peerID;
	private volatile Boolean destroyPeer;
	private volatile String optimisticUnchokedPeerID;
	private PeerServer server;
	private CommonConfiguration commonConfiguration;
	private PeerInfoConfiguration peerInfoConfiguration;
	private RemotePeerInfo currentPeerConfig;
	private Thread serverThread;
	private volatile ServerSocket listener;
	private volatile PeerLogger logger;
	private volatile RandomAccessFile randomFileAccessor;
	private volatile PreferredNeighborScheduler preferredNeighborScheduler;
	private volatile OptimisticNeighborScheduler optimisticNeighborScheduler;
	private volatile TerminationScheduler terminationScheduler;
	private volatile String[] requestedPeerPieces;
	private ArrayList<String> currentPeerList;
	private volatile HashSet<String> unchokedPeerSet;
	private volatile HashSet<String> interestedPeerSet;
	private HashMap<String, RemotePeerInfo> remotePeerInfoMap;
	private volatile HashMap<String, PeerUtils> connectedPeers;
	private volatile HashMap<String, Thread> connectedThreads;
	private volatile HashMap<String, BitSet> availablePieces;
	private volatile HashMap<String, Integer> downloadRate;

	public PeerAdmin(String peerID) {
		/*
			This is the constructor for PeerAdmin class
		 */
		this.peerID = peerID;
		this.logger = new PeerLogger(this.peerID);
		this.destroyPeer = false;

		this.commonConfiguration = new CommonConfiguration();
		this.commonConfiguration.unpackCommonConfiguration();

		this.peerInfoConfiguration = new PeerInfoConfiguration();
		this.peerInfoConfiguration.unpackConfigurationFile();

		this.pieceCount = this.calculateNumberOfPieces();
		this.requestedPeerPieces = new String[this.pieceCount];

		this.currentPeerConfig = this.peerInfoConfiguration.getremotePeerInfo(this.peerID);
		this.remotePeerInfoMap = this.peerInfoConfiguration.getRemotePeerInfoMap();
		this.currentPeerList = this.peerInfoConfiguration.getPeerList();

		initFileSystem();

		this.connectedPeers = new HashMap<>();
		this.connectedThreads = new HashMap<>();
		this.unchokedPeerSet = new HashSet<>();
		this.interestedPeerSet = new HashSet<>();
		this.downloadRate = new HashMap<>();

		this.availablePieces = getPieceAvailability();
		startPieceServer();
		createNeighbourConnections();

		// Initialize schedulers for this peer
		this.preferredNeighborScheduler = new PreferredNeighborScheduler(this);
		this.optimisticNeighborScheduler = new OptimisticNeighborScheduler(this);
		this.terminationScheduler = new TerminationScheduler(this);

		this.preferredNeighborScheduler.initializeScheduler();
		this.optimisticNeighborScheduler.initializeScheduler();
	}

	// Peer Initialization and File Initialization Utilities

	public void initFileSystem() {
		/*
			This method is responsible for creating a new directory for the peer,
			initializing the file to be sent, and initializes the file access class
			for accessing the file.

			Random File Access is required as a latter segment of data can be required
			to be written.
	 	*/
		try {
			String filepath = "peer_" + this.peerID;
			File file = new File(filepath);
			file.mkdir();
			String filename = filepath + "/" + getFileName();
			file = new File(filename);
			if (!hasFile()) {
				file.createNewFile();
			}
			this.randomFileAccessor = new RandomAccessFile(file, "rw");
			if (!hasFile()) {
				this.randomFileAccessor.setLength(this.commonConfiguration.fileSize);
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public HashMap<String, BitSet> getPieceAvailability() {
		/*
			This method is responsible for initializing the file pieces available
			in the peer's folder.

			Returns a hashmap with peerID as key and available pieces bitset
		 */
		HashMap<String, BitSet> availablePieces = new HashMap<>();

		// Check piece availability for all peer IDs
		for (String peerID : this.remotePeerInfoMap.keySet()) {
			BitSet availability = new BitSet(this.pieceCount);
			if (this.remotePeerInfoMap.get(peerID).containsFile == 1) {
				availability.set(0, this.pieceCount);
				availablePieces.put(peerID, availability);
			}
			else {
				availability.clear();
				availablePieces.put(peerID, availability);
			}
		}

		return availablePieces;
	}

	public void startPieceServer() {
		/*
			This method initializes a ServerSocket, which listens for requests from
			other neighbor peers. The server is run in a new thread.
		 */
		try {
			this.listener = new ServerSocket(this.currentPeerConfig.peerPort);
			this.server = new PeerServer(this.peerID, this.listener, this);
			this.serverThread = new Thread(this.server);
			this.serverThread.start();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createNeighbourConnections() {
		/*
			This method is responsible for creating a TCP connection
			with its neighbor peers.
		*/
		try {
			Thread.sleep(5000);
			for (String currentPeerID : this.currentPeerList) {
				// Skip if currentPeerID is equal to the peerID of this peer
				if (currentPeerID.equals(this.peerID)) {
					break;
				}
				// If not equal, create a new socket and run it in a thread
				else {
					RemotePeerInfo peer = this.remotePeerInfoMap.get(currentPeerID);
					Socket socket = new Socket(peer.peerAddress, peer.peerPort);
					PeerUtils peerUtils = new PeerUtils(socket, this);
					peerUtils.setNeighborPeerID(currentPeerID);
					this.putConnectedPeer(peerUtils, currentPeerID);
					Thread thread = new Thread(peerUtils);
					this.putConnectedThreads(currentPeerID, thread);
					thread.start();
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public PeerUtils getConnectedPeers(String peerID) {
		return this.connectedPeers.get(peerID);
	}

	public synchronized void putConnectedPeer(PeerUtils peerUtils, String neighborPeerID) {
		this.connectedPeers.put(neighborPeerID, peerUtils);
	}

	public synchronized HashMap<String, Thread> getConnectedThreads() {
		return this.connectedThreads;
	}

	public synchronized void putConnectedThreads(String peerHandler, Thread thread) {
		this.connectedThreads.put(peerHandler, thread);
	}
	
	// P2P Utilities

	public String getPeerID() {
		return this.peerID;
	}

	public int getNumberOfPreferredNeighbors() {
		return this.commonConfiguration.numberOfPreferredNeighbors;
	}

	public int getUnchokingInterval() {
		return this.commonConfiguration.unchokingInterval;
	}

	public int getOptimisticUnchokingInterval() {
		return this.commonConfiguration.optimisticUnchokingInterval;
	}

	public synchronized boolean checkIfInterested(String neighborPeerID) {
		/*
			This method is responsible for checking if a neighbor peer has an
			interested piece.
		*/
		BitSet receiverBitField = this.getAvailablePieces(neighborPeerID);
		BitSet senderBitField = this.getAvailablePieces(this.peerID);
		for (int pieceIndex = 0; pieceIndex < receiverBitField.size() && pieceIndex < this.pieceCount; pieceIndex++) {
			if (receiverBitField.get(pieceIndex) == true && senderBitField.get(pieceIndex) == false) {
				return true;
			}
		}
		return false;
	}

	public BitSet getAvailablePieces(String peerID) {
		return this.availablePieces.get(peerID);
	}

	public synchronized void announcePieceAvailability(int pieceIndex) {
		/*
			When a piece is downloaded by a peer, it has to announce the availability
			of the piece to all its connected peers.

			This message is responsible for sending a have message.
		*/
		for (String key : this.connectedPeers.keySet()) {
			this.connectedPeers.get(key).sendHaveMessage(pieceIndex);
		}
	}

	public synchronized int checkForRequestedPeerPieces(String neighborPeerID) {
		/*
			This method is responsible for setting the pieces requested
			by the neighbor peer
		 */
		BitSet receiverBitField = this.getAvailablePieces(neighborPeerID);
		BitSet senderBitField = this.getAvailablePieces(this.peerID);
		for (int pieceIndex = 0; pieceIndex < receiverBitField.size() && pieceIndex < this.pieceCount; pieceIndex++) {
			if (receiverBitField.get(pieceIndex) == true && senderBitField.get(pieceIndex) == false && this.requestedPeerPieces[pieceIndex] == null) {
				setRequestedPeerPieces(pieceIndex, neighborPeerID);
				return pieceIndex;
			}
		}
		return -1;
	}

	public synchronized void setRequestedPeerPieces(int id, String peerID) {
		this.requestedPeerPieces[id] = peerID;
	}

	public synchronized void resetRequestedPeerPieces(String neighborPeerID) {
		/*
			This method is responsible for resetting the requested pieces
			by the peers.
		*/
		for (int pieceIndex = 0; pieceIndex < this.requestedPeerPieces.length; pieceIndex++) {
			if (this.requestedPeerPieces[pieceIndex] != null && this.requestedPeerPieces[pieceIndex].compareTo(neighborPeerID) == 0) {
				setRequestedPeerPieces(pieceIndex, null);
			}
		}
	}

	public synchronized void setOptimisticUnchokedPeer(String optimisticUnchokedPeerID) {
		this.optimisticUnchokedPeerID = optimisticUnchokedPeerID;
	}

	public synchronized String getOptimisticUnchokedPeer() {
		return this.optimisticUnchokedPeerID;
	}

	// Interested Peer Set Utilities

	public synchronized void insertIntoInterestedPeerSet(String neighborPeerID) {
		this.interestedPeerSet.add(neighborPeerID);
	}

	public synchronized void removeFromInterestedPeerSet(String neighborPeerID) {
		if (this.interestedPeerSet != null) {
			this.interestedPeerSet.remove(neighborPeerID);
		}
	}

	public synchronized void resetInterestedPeerSet() {
		this.interestedPeerSet.clear();
	}

	public synchronized HashSet<String> getInterestedPeerSet() {
		return this.interestedPeerSet;
	}

	// Unchoked Peer Set Utilities

	public synchronized boolean insertIntoUnchokedPeerSet(String peerid) {
		return this.unchokedPeerSet.add(peerid);
	}

	public synchronized HashSet<String> getUnchokedPeerSet() {
		return this.unchokedPeerSet;
	}

	public synchronized void emptyUnchokedPeerSet() {
		this.unchokedPeerSet.clear();
	}

	public synchronized void updateUnchokedPeerSet(HashSet<String> newUnchokedPeerSet) {
		this.unchokedPeerSet = newUnchokedPeerSet;
	}

	// File Utilities

	public synchronized void writeToFile(byte[] data, int pieceIndex) {
		/*
			This method is responsible for writing data to the given pieceIndex.
			This process required random file access as a latter pieceIndex can
			also be received.
		 */
		try {
			// Piece Size * PieceIndex gives where to start writing the file from.
			int pieceIdxPosition = this.getPieceSize() * pieceIndex;

			// Seek the position and write the data
			this.randomFileAccessor.seek(pieceIdxPosition);
			this.randomFileAccessor.write(data);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized byte[] readFromFile(int pieceIndex) {
		/*
			This method is responsible for reading data from the given pieceIndex.
			This process required random file access as a latter pieceIndex can
			also be received.
		 */
		try {
			int pieceIdxPosition = this.getPieceSize() * pieceIndex;
			int pieceSize = this.getPieceSize();

			/*
				If the piece required to be read is the last piece.
				There is a possibility that the last piece can have the full piece size
				or the remaining.
			*/
			if (pieceIndex == getPieceCount() - 1) {
				pieceSize = getFileSize() % getPieceSize();
			}
			byte[] data = new byte[pieceSize];

			// Seek the position and read the data.
			this.randomFileAccessor.seek(pieceIdxPosition);
			this.randomFileAccessor.read(data);

			return data;
		} 
		catch (Exception e) {
			e.printStackTrace();

		}

		return new byte[0];
	}

	public boolean hasFile() {
		return this.currentPeerConfig.containsFile == 1;
	}

	public String getFileName() {
		return this.commonConfiguration.fileName;
	}

	public int getFileSize() {
		return this.commonConfiguration.fileSize;
	}

	public int getPieceSize() {
		return this.commonConfiguration.pieceSize;
	}

	public int calculateNumberOfPieces() {
		int numberOfPieces = (getFileSize() / getPieceSize());
		if (getFileSize() % getPieceSize() != 0) {
			numberOfPieces += 1;
		}
		return numberOfPieces;
	}

	public int getPieceCount() {
		return this.pieceCount;
	}

	public synchronized RandomAccessFile getRandomFileAccessor() {
		return this.randomFileAccessor;
	}

	public synchronized void updatePieceAvailability(String peerID, int pieceIndex) {
		this.availablePieces.get(peerID).set(pieceIndex);
	}

	public synchronized void updateBitset(String peerID, BitSet bitSet) {
		this.availablePieces.remove(peerID);
		this.availablePieces.put(peerID, bitSet);
	}

	public int getCompletedPieceCount() {
		return this.availablePieces.get(this.peerID).cardinality();
	}

	// Logger Utilities

	public PeerLogger getLogger() {
		return this.logger;
	}

	// Download Rate Utilities

	public HashMap<String, Integer> getDownloadRatesOfPeers() {
		/*
			This method is responsible for fetching the download rates
			of each peer and returning it.

			Returns a hashmap of rates.
		 */
		HashMap<String, Integer> downloadRates = new HashMap<>();
		for (String peerID : this.connectedPeers.keySet()) {
			downloadRates.put(peerID, this.connectedPeers.get(peerID).getDownloadRate());
		}
		return downloadRates;
	}

	public synchronized void updateDownloadRate(String neighborPeerID) {
		this.downloadRate.put(neighborPeerID, this.downloadRate.get(neighborPeerID) + 1);
	}

	// Destroy Peer Utilities

	public synchronized void destroyPeer() {
		/*
			This method is responsible for destroying all the components of a PeerAdmin
			that are involved in the P2P File Sharing Process for successful termination
			and memory leak prevention from files.
		 */
		try {
			this.getOptimisticNeighborScheduler().destroyScheduler();
			this.getPreferredNeighborScheduler().destroyScheduler();
			this.emptyUnchokedPeerSet();
			this.setOptimisticUnchokedPeer(null);
			this.resetInterestedPeerSet();
			this.getRandomFileAccessor().close();
			this.getLogger().deconstructLogger();
			this.getListener().close();
			this.getServerThread().stop();
			this.destroyPeer = true;
			this.terminationScheduler.initializeScheduler(2);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized OptimisticNeighborScheduler getOptimisticNeighborScheduler() {
		return this.optimisticNeighborScheduler;
	}

	public synchronized PreferredNeighborScheduler getPreferredNeighborScheduler() {
		return this.preferredNeighborScheduler;
	}

	public synchronized void destroyConnectedThreads() {
		/*
			This method is responsible for destroying all TCP Connections
		 */
		for (String peerID : this.connectedThreads.keySet()) {
			this.connectedThreads.get(peerID).stop();
		}
	}

	public synchronized ServerSocket getListener() {
		return this.listener;
	}

	public synchronized Thread getServerThread() {
		return this.serverThread;
	}

	public synchronized Boolean isDestroyPeer() {
		return this.destroyPeer;
	}

	public synchronized boolean isDownloadCompleted() {
		/*
			This method returns if all the peers have downloaded all the available
			pieces.
		 */
		for (String peerID : this.availablePieces.keySet()) {
			if (this.availablePieces.get(peerID).cardinality() != this.pieceCount) {
				return false;
			}
		}
		return true;
	}
}
