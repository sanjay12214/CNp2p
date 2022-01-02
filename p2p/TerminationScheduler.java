package p2p;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.lang.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TerminationScheduler implements Runnable {
    private int interval;
    private PeerAdmin peerAdmin;
    private Random rand = new Random();
    private ScheduledFuture<?> scheduledJob = null;
    private ScheduledExecutorService scheduler = null;

    TerminationScheduler(PeerAdmin padmin) {
        this.peerAdmin = padmin;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void initializeScheduler(int timeInterval) {
        this.interval = timeInterval * 3;
        this.scheduledJob = scheduler.scheduleAtFixedRate(this, 5, this.interval, TimeUnit.SECONDS);
    }

    public void destroyScheduler() {
        this.scheduler.shutdownNow();
    }

    public void run() {
        try {
            if(this.peerAdmin.isDestroyPeer()) {
                this.peerAdmin.destroyConnectedThreads();
                this.destroyScheduler();
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
