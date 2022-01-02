package p2p;

import java.io.*;
import java.util.*;

public class CommonConfiguration {
    public int numberOfPreferredNeighbors;
    public int unchokingInterval;
    public int optimisticUnchokingInterval;
    public String fileName;
    public int fileSize;
    public int pieceSize;

    public void unpackCommonConfiguration() {
        /*
            This method is responsible for unpacking the common configuration file
            and loading the CommonConfiguration object.
        */
        try {
            FileReader fileReader = new FileReader("Common.cfg");
            Scanner scanner = new Scanner(fileReader);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] configuration = line.split(" ");
                switch(configuration[0]) {
                    case "NumberOfPreferredNeighbors":
                        this.numberOfPreferredNeighbors = Integer.parseInt(configuration[1]);
                        break;
                    case "UnchokingInterval":
                        this.unchokingInterval = Integer.parseInt(configuration[1]);
                        break;
                    case "OptimisticUnchokingInterval":
                        this.optimisticUnchokingInterval = Integer.parseInt(configuration[1]);
                        break;
                    case "FileName":
                        this.fileName = configuration[1];
                        break;
                    case "FileSize":
                        this.fileSize = Integer.parseInt(configuration[1]);
                        break;
                    case "PieceSize":
                        this.pieceSize = Integer.parseInt(configuration[1]);
                        break;
                    default:
                        break;
                }
            }
            scanner.close();
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
