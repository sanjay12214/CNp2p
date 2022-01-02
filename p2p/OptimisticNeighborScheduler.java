package p2p;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.lang.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OptimisticNeighborScheduler implements Runnable {
    private int interval;
    private PeerAdmin peerAdmin;
    private Random rand = new Random();
    private ScheduledFuture<?> scheduledJob = null;
    private ScheduledExecutorService scheduler;

    OptimisticNeighborScheduler(PeerAdmin peerAdmin) {
        this.peerAdmin = peerAdmin;
        this.interval = peerAdmin.getOptimisticUnchokingInterval();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void initializeScheduler() {
        this.scheduledJob = this.scheduler.scheduleAtFixedRate(this, 2, this.interval, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            PeerLogger peerLogger = this.peerAdmin.getLogger();
            String currentOptUnchokedNeighbor = this.peerAdmin.getOptimisticUnchokedPeer();

            // Remove current optimistically unchoked neighbor from interested list to not choose it again
            List<String> currentInterested = new ArrayList<String>(this.peerAdmin.getInterestedPeerSet());
            currentInterested.remove(currentOptUnchokedNeighbor);

            // Choose a random neighbor as optimistically unchoked neighbor if a neighbor is present
            int countOfNeighbors = currentInterested.size();
            if (countOfNeighbors > 0) {
                String nextInterestedPeer = currentInterested.get(rand.nextInt(countOfNeighbors));
                while (this.peerAdmin.getUnchokedPeerSet().contains(nextInterestedPeer)) {
                    currentInterested.remove(nextInterestedPeer);
                    countOfNeighbors--;
                    if(countOfNeighbors > 0) {
                        nextInterestedPeer = currentInterested.get(rand.nextInt(countOfNeighbors));
                    }
                    else {
                        nextInterestedPeer = null;
                        break;
                    }
                }
                this.peerAdmin.setOptimisticUnchokedPeer(nextInterestedPeer);

                // If an optimistically unchoked neighbor is present, sendUtil an unchoke message to it.
                if(nextInterestedPeer != null) {
                    PeerUtils peerUtils = this.peerAdmin.getConnectedPeers(nextInterestedPeer);
                    peerUtils.sendUnChokeMessage();
                    peerLogger.optimisticallyUnchokedNeighborChanged(this.peerAdmin.getOptimisticUnchokedPeer());
                }

                // If a new optimistically unchoked neighbor is found, sendUtil an unchoke message to the current one.
                if (currentOptUnchokedNeighbor != null && !this.peerAdmin.getUnchokedPeerSet().contains(currentOptUnchokedNeighbor)) {
                    this.peerAdmin.getConnectedPeers(currentOptUnchokedNeighbor).sendChokeMessage();
                }  
            }
            // If a neighbor is not found
            else {
                String currentOptChoked = this.peerAdmin.getOptimisticUnchokedPeer();
                this.peerAdmin.setOptimisticUnchokedPeer(null);
                if (currentOptChoked != null && !this.peerAdmin.getUnchokedPeerSet().contains(currentOptChoked)) {
                    PeerUtils peerUtils = this.peerAdmin.getConnectedPeers(currentOptChoked);
                    peerUtils.sendChokeMessage();
                }
                if(this.peerAdmin.isDownloadCompleted()) {
                    this.peerAdmin.destroyPeer();
                }
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroyScheduler() {
        this.scheduler.shutdownNow();
    }
}
