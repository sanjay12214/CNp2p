package p2p;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.lang.*;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.*;
import java.util.concurrent.TimeUnit;

public class PreferredNeighborScheduler implements Runnable {
    private int interval;
    private int preferredNeighboursCount;
    private PeerAdmin peerAdmin;
    private Random rand = new Random();
    private ScheduledFuture<?> scheduledJob = null;
    private ScheduledExecutorService scheduler = null;

    PreferredNeighborScheduler(PeerAdmin padmin) {
        this.peerAdmin = padmin;
        this.interval = padmin.getUnchokingInterval();
        this.preferredNeighboursCount = padmin.getNumberOfPreferredNeighbors();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void initializeScheduler() {
        this.scheduledJob = this.scheduler.scheduleAtFixedRate(this, 2, this.interval, TimeUnit.SECONDS);
    }

    public void run() {
        try {
            HashSet<String> unchokedPeerSet = new HashSet<>(this.peerAdmin.getUnchokedPeerSet());
            HashSet<String> newUnchokedPeerSet = new HashSet<>();
            List<String> interestedPeerList = new ArrayList<String>(this.peerAdmin.getInterestedPeerSet());
            // If peers are present in the interestedPeerList
            if (interestedPeerList.size() > 0) {
                int minNeighborCount = Math.min(this.preferredNeighboursCount, interestedPeerList.size());
                if (this.peerAdmin.getCompletedPieceCount() == this.peerAdmin.getPieceCount()) {
                    for (int index = 0; index < minNeighborCount; index++) {
                        String nextInterestedPeer = interestedPeerList.get(this.rand.nextInt(interestedPeerList.size()));
                        PeerUtils nextPeer = this.peerAdmin.getConnectedPeers(nextInterestedPeer);
                        while (newUnchokedPeerSet.contains(nextInterestedPeer)) {
                            nextInterestedPeer = interestedPeerList.get(this.rand.nextInt(interestedPeerList.size()));
                            nextPeer = this.peerAdmin.getConnectedPeers(nextInterestedPeer);
                        }
                        if (!unchokedPeerSet.contains(nextInterestedPeer)) {
                            if (this.peerAdmin.getOptimisticUnchokedPeer() == null
                                    || this.peerAdmin.getOptimisticUnchokedPeer().compareTo(nextInterestedPeer) != 0) {
                                nextPeer.sendUnChokeMessage();
                            }
                        } 
                        else {
                            unchokedPeerSet.remove(nextInterestedPeer);
                        }
                        newUnchokedPeerSet.add(nextInterestedPeer);
                        nextPeer.resetDownloadRate();
                    }
                } else {
                    Map<String, Integer> downloadRates = new HashMap<>(this.peerAdmin.getDownloadRatesOfPeers());
                    Map<String, Integer> rates = downloadRates.entrySet().stream()
                                                              .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                                              .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                    Iterator<Map.Entry<String, Integer>> iterator = rates.entrySet().iterator();
                    int counter = 0;
                    while (counter < minNeighborCount && iterator.hasNext()) {

                        Map.Entry<String, Integer> nextDownloadRate = iterator.next();
                        if (interestedPeerList.contains(nextDownloadRate.getKey())) {
                            PeerUtils nextHandler = this.peerAdmin.getConnectedPeers(nextDownloadRate.getKey());
                            if (!unchokedPeerSet.contains(nextDownloadRate.getKey())) {
                                String optimisticUnchokedPeer = this.peerAdmin.getOptimisticUnchokedPeer();
                                if (optimisticUnchokedPeer == null || optimisticUnchokedPeer.compareTo(nextDownloadRate.getKey()) != 0) {
                                    nextHandler.sendUnChokeMessage();
                                }
                            } 
                            else {
                                unchokedPeerSet.remove(nextDownloadRate.getKey());
                            }
                            newUnchokedPeerSet.add(nextDownloadRate.getKey());
                            nextHandler.resetDownloadRate();
                            counter++;
                        }
                    }
                }
                this.peerAdmin.updateUnchokedPeerSet(newUnchokedPeerSet);
                if(newUnchokedPeerSet.size() > 0){
                    this.peerAdmin.getLogger().preferredNeighborsChanged(new ArrayList<>(newUnchokedPeerSet));
                }
                for (String peer : unchokedPeerSet) {
                    PeerUtils nextHandler = this.peerAdmin.getConnectedPeers(peer);
                    nextHandler.sendChokeMessage();
                }
            } 
            else {
                this.peerAdmin.emptyUnchokedPeerSet();
                for (String peer : unchokedPeerSet) {
                    PeerUtils nextHandler = this.peerAdmin.getConnectedPeers(peer);
                    nextHandler.sendChokeMessage();
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
