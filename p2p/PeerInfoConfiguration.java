package p2p;
import java.util.*;

import java.io.*;
public class PeerInfoConfiguration {
	private HashMap<String, RemotePeerInfo> remotePeerInfoMap;
	private ArrayList<String> peerList;

	public PeerInfoConfiguration(){
		this.remotePeerInfoMap = new HashMap<>();
		this.peerList = new ArrayList<>();
	}

	public RemotePeerInfo getremotePeerInfo(String peerID){
		return this.remotePeerInfoMap.get(peerID);
	}

	public HashMap<String, RemotePeerInfo> getRemotePeerInfoMap(){
		return this.remotePeerInfoMap;
	}

	public ArrayList<String> getPeerList(){
		return this.peerList;
	}

	public void setRemotePeerInfoMap(HashMap<String, RemotePeerInfo> remotePeerInfoMap) {
		this.remotePeerInfoMap = remotePeerInfoMap;
	}

	public void setPeerList(ArrayList<String> peerList) {
		this.peerList = peerList;
	}

	public void unpackConfigurationFile()
	{
		String line;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while((line = br.readLine()) != null) {
				String[] properties = line.split(" ");
				this.remotePeerInfoMap.put(properties[0], new RemotePeerInfo(properties[0], properties[1], properties[2], properties[3]));
				this.peerList.add(properties[0]);
			}
			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
