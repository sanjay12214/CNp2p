package p2p;

import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.BitSet;
import java.nio.*;
import java.lang.*;

public class PeerUtils implements Runnable {
	private volatile int downloadRate = 0;
	private String neighborPeerID;
	private boolean isConnected = false;
	private boolean isInitialized = false;
	private Socket listener;
	private PeerAdmin peerAdmin;
	private HandshakeMessage handshakeMessage;
	private PeerLogger logger;
	private volatile ObjectOutputStream oos;
	private volatile ObjectInputStream ois;

	public PeerUtils(Socket listener, PeerAdmin peerAdmin) {
		/*
			This is the constructor for PeerUtils class
		 */
		this.listener = listener;
		this.peerAdmin = peerAdmin;
		try {
			this.logger = peerAdmin.getLogger();
			this.oos = new ObjectOutputStream(this.listener.getOutputStream());
			this.ois = new ObjectInputStream(this.listener.getInputStream());
			this.oos.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		this.handshakeMessage = new HandshakeMessage(this.peerAdmin.getPeerID());

	}
	
	public void setNeighborPeerID(String pid) {
		this.neighborPeerID = pid;
		this.isInitialized = true;
	}

	public void run() {
		try {
			byte[] handshakeMessage = this.handshakeMessage.constructHandshakeMessage();
			this.oos.write(handshakeMessage);
			this.oos.flush();

			while (true) {

				if (!this.isConnected) {
					byte[] response = new byte[32];
					this.ois.readFully(response);
					this.processHandshakeMessage(response);
					if (this.peerAdmin.hasFile() || this.peerAdmin.getAvailablePieces(this.peerAdmin.getPeerID()).cardinality() > 0) {
						this.sendBitFieldMessage();
					}
				} 
				else {
					while (this.ois.available() < 4) {
						// Do nothing
					}
					int responseLength = this.ois.readInt();
					byte[] response = new byte[responseLength];
					this.ois.readFully(response);
					char messageType = (char) response[0];

					Message message = new Message();
					message.setMessage(responseLength, response);

					switch(messageType) {
						case '0':
							/*
								Choke Message:
								Remove neighbor from requested peer pieces info
							*/
							this.peerAdmin.resetRequestedPeerPieces(this.neighborPeerID);
							logger.chokedNeighbor(this.neighborPeerID);
							break;

						case '1':
							/*
								Unchoke Message:
								Check if any interested pieces are available with the neighbor
								If neighbor sends interested message and the piece is present, unchoke neighbor
								If piece not present, sendUtil not interested message.
							*/
							int requestedPieceIndex = this.peerAdmin.checkForRequestedPeerPieces(this.neighborPeerID);
							if (requestedPieceIndex != -1) {
								this.sendRequestMessage(requestedPieceIndex);
								logger.unchokedNeighbor(this.neighborPeerID);
							} else {
								this.sendNotInterestedMessage();
								logger.sendNotInterestedMessage(this.neighborPeerID);
							}
							break;

						case '2':
							/*
								Interested Message:
								Add neighbor peer ID to interested peer set
							*/
							this.peerAdmin.insertIntoInterestedPeerSet(this.neighborPeerID);
							logger.receivedInterestedMessage(this.neighborPeerID);
							break;

						case '3':
							/*
								Not Interested message:
								Remove neighbor peer ID from interested peer set
							*/
							this.peerAdmin.removeFromInterestedPeerSet(this.neighborPeerID);
							logger.receivedNotInterestedMessage(this.neighborPeerID);
							break;

						case '4':
							/*
								Have Message:
								If neighbor has all pieces, destroy the connection
								If peer is interested in any of the pieces of neighbor, sendUtil interested message
								If not any, sendUtil not interested message.
							*/
							int pieceIndex = message.getPieceIndexFromPayload();
							this.peerAdmin.updatePieceAvailability(this.neighborPeerID, pieceIndex);
							if (this.peerAdmin.isDownloadCompleted()) {
								this.peerAdmin.destroyPeer();
							}
							if (this.peerAdmin.checkIfInterested(this.neighborPeerID)) {
								logger.receivedHaveMessage(this.neighborPeerID, pieceIndex);
								this.sendInterestedMessage();
								logger.sendInterestedMessage(this.neighborPeerID);
							}
							else {
								this.sendNotInterestedMessage();
								logger.sendNotInterestedMessage(this.neighborPeerID);
							}
							break;

						case '5':
							/*
								Bitfield Message:
								Update the available pieces
								If interested in any pieces, peer sends interested message
								Else sends not interested message
							*/
							BitSet bitSet = message.getBitFieldMessage();
							this.processBitFieldMessage(bitSet);
							logger.receivedBitField(this.neighborPeerID);
							if (!this.peerAdmin.hasFile()) {
								if (this.peerAdmin.checkIfInterested(this.neighborPeerID)) {
									this.sendInterestedMessage();
									logger.sendInterestedMessage(this.neighborPeerID);
								}
								else {
									this.sendNotInterestedMessage();
									logger.sendNotInterestedMessage(this.neighborPeerID);
								}
							}
							break;

						case '6':
							/*
								If neighbor peer is in unchoked peers set or if neighbor peer is an
								optimistically unchoked peer, then accept the request message. Else discard the
								message.
							*/
							if (this.peerAdmin.getUnchokedPeerSet().contains(this.neighborPeerID)
									|| (this.peerAdmin.getOptimisticUnchokedPeer() != null && this.peerAdmin.getOptimisticUnchokedPeer().compareTo(this.neighborPeerID) == 0)) {
								pieceIndex = message.getPieceIndexFromPayload();
								logger.receivedRequestMessage(this.neighborPeerID, String.valueOf(pieceIndex));
								this.sendPieceMessage(pieceIndex, this.peerAdmin.readFromFile(pieceIndex));
								logger.sentPieceMessage(this.neighborPeerID, String.valueOf(pieceIndex));
							}
							break;

						case '7':
							/*
								Piece Message:
								Write the piece to the file and update the piece availability.
								Announce availability of the piece
								Check for other piece, if required sendUtil interested message or sendUtil not interested message
								If all pieces are downloaded, destroy the connection.
							 */
							pieceIndex = message.getPieceIndexFromPayload();
							byte[] piece = message.getPieceFromPayload();
							
							this.peerAdmin.writeToFile(piece, pieceIndex);
							this.peerAdmin.updatePieceAvailability(this.peerAdmin.getPeerID(), pieceIndex);
							this.downloadRate++;
							
							Boolean isCompleted = this.peerAdmin.isDownloadCompleted();
							logger.downloadedPiece(this.neighborPeerID, pieceIndex, this.peerAdmin.getCompletedPieceCount());
							this.peerAdmin.setRequestedPeerPieces(pieceIndex, null);
							this.peerAdmin.announcePieceAvailability(pieceIndex);

							if (this.peerAdmin.getAvailablePieces(this.peerAdmin.getPeerID()).cardinality() != this.peerAdmin.getPieceCount()) {
								requestedPieceIndex = this.peerAdmin.checkForRequestedPeerPieces(this.neighborPeerID);
								if (requestedPieceIndex != -1) {
									this.sendRequestMessage(requestedPieceIndex);
									logger.sendInterestedMessage(neighborPeerID);
								}
								else {
									this.sendNotInterestedMessage();
									logger.sendNotInterestedMessage(neighborPeerID);
								}
							}
							else {
								logger.downloadCompleted();
								if (isCompleted) {
									this.peerAdmin.destroyPeer();
								}
								this.sendNotInterestedMessage();
								logger.sendNotInterestedMessage(neighborPeerID);
							}
							break;

						default:
							break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processHandshakeMessage(byte[] message) {
		/*
			This method is responsible for processing the handshake messages
			between the peers. After successfully verifying the handshake messages,
			a TCP connection is established.
		*/
		try {
			this.handshakeMessage.readHandshakeMessage(message);
			this.neighborPeerID = this.handshakeMessage.getPeerID();
			logger.receivedHandshakeMessage(this.neighborPeerID);
			this.peerAdmin.putConnectedPeer(this, this.neighborPeerID);
			this.peerAdmin.putConnectedThreads(this.neighborPeerID, Thread.currentThread());
			this.isConnected = true;
			if (this.isInitialized) {
				logger.initiatedconnectionEstablishment(this.neighborPeerID);
			}
			else {
				logger.connectionEstablished(this.neighborPeerID);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendBitFieldMessage() {
		/*
			This method is responsible for sending the bit field of a peer.
		*/
		try {
			BitSet myAvailability = this.peerAdmin.getAvailablePieces(this.peerAdmin.getPeerID());
			Message message = new Message('5', myAvailability.toByteArray());
			this.sendUtil(message.constructMessage());
			logger.sentBitField(this.neighborPeerID);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendRequestMessage(int pieceIndex) {
		/*
			This method is responsible for sending a request message
		*/
		try {
			byte[] bytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			Message message = new Message('6', bytes);
			this.sendUtil(message.constructMessage());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendNotInterestedMessage() {
		/*
			This method is responsible for sending a not interested message
		*/
		try {
			Message message = new Message('3');
			this.sendUtil(message.constructMessage());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendInterestedMessage() {
		/*
			This method is responsible for sending an interested message
		*/
		try {
			Message message = new Message('2');
			this.sendUtil(message.constructMessage());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processBitFieldMessage(BitSet bitSet) {
		/*
			This method is responsible for updating the bitset assigned for each peer ID
		*/
		this.peerAdmin.updateBitset(this.neighborPeerID, bitSet);
	}

	public void sendPieceMessage(int pieceIndex, byte[] payload) {
		/*
			This method is responsible for sending a piece message
		*/
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] bytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			baos.write(bytes);
			baos.write(payload);
			Message message = new Message('7', baos.toByteArray());
			this.sendUtil(message.constructMessage());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendChokeMessage() {
		/*
			This method is responsible for sending a choke message
		*/
		try {
			Message message = new Message('0');
			this.sendUtil(message.constructMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendUnChokeMessage() {
		/*
			This method is responsible for sending an unchoke message
		*/
		try {
			Message message = new Message('1');
			this.sendUtil(message.constructMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendHaveMessage(int pieceIndex) {
		/*
			This method is responsible for sending a have message
		*/
		try {
			byte[] bytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
			Message message = new Message('4', bytes);
			this.sendUtil(message.constructMessage());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void sendUtil(byte[] obj) {
		/*
			This method is responsible for writing the message to the stream.
		*/
		try {
			this.oos.write(obj);
			this.oos.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getDownloadRate() {
		return this.downloadRate;
	}

	public void resetDownloadRate() {
		this.downloadRate = 0;
	}

}
