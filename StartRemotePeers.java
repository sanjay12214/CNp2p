import java.io.*;
import java.util.*;

import p2p.RemotePeerInfo;
import p2p.PeerInfoConfiguration;

public class StartRemotePeers {
    public static void main(String[] args) {
        try {
            PeerInfoConfiguration peerConf = new PeerInfoConfiguration();
            peerConf.unpackConfigurationFile();

            HashMap<String, RemotePeerInfo> remotePeerInfoMap = peerConf.getRemotePeerInfoMap();

            for(String peer: peerConf.getPeerList()) {
                RemotePeerInfo info = remotePeerInfoMap.get(peer);
                System.out.println("Starting Remote Peer [" + peer + "] at hostName=" + info.peerAddress + " portNumber=" + info.peerPort);
                Runtime.getRuntime().exec("java PeerProcess " + peer);
                Thread.sleep(10);
            }

            System.out.println("All remote peers has been started.");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
