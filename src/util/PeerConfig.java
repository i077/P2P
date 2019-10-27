package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Data class used to store peer configuration.
 * This includes the ports it will use (and for what purpose),
 * as well as files it can share.
 *
 * This configuration is stored in two files:
 * - "config_peer.txt": Stores the ports that the peer will use.
 *  The first two lines are the UDP server and client ports, respectively.
 *  The first port is also implied to be the beginning of the range of 20 ports that this peer can use.
 *  The following lines are the ports that the peer can use for maintaining TCP connections with neighboring peers.
 *
 * - "config_sharing.txt": Stores the filenames of files that can be shared.
 *  Each line is a path, implied to start from "~/p2p/shared/", to a file (that must exist) in ~/p2p/shared/
 */
public class PeerConfig {
    public final int udpServerPort, udpClientPort;
    public final ArrayList<Integer> tcpNeighborPorts;
    public final ArrayList<File> sharedFileList;

    /**
     * Read the peer configuration files into the object.
     *
     * @throws IOException If one of the two config files could not be read.
     * @throws FileNotFoundException If a file to be shared could not be found.
     */
    public PeerConfig() throws IOException, FileNotFoundException {
        File    configPeer = new File("config_peer.txt"),
                configSharing = new File("config_sharing.txt");

        try (BufferedReader br = new BufferedReader(new FileReader(configPeer))) {
            // Read in UDP ports
            String udpServerPortText = br.readLine();
            String udpClientPortText = br.readLine();
            udpServerPort = Integer.parseInt(udpServerPortText);
            udpClientPort = Integer.parseInt(udpClientPortText);

            // Read in TCP ports
            tcpNeighborPorts = new ArrayList<>();
            String remainingLine;
            while ((remainingLine = br.readLine()) != null) {
                tcpNeighborPorts.add(Integer.parseInt(remainingLine));
            }
        }

        // Read in shared files.
        sharedFileList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(configSharing))) {
            String path;
            while ((path = br.readLine()) != null) {
                File file = new File("~/p2p/sharing/" + path);
                // Check that this file exists and is a file
                if (!file.exists() || !file.isFile()) {
                    throw new FileNotFoundException("Could not read ~/p2p/sharing" + path + ". Check that this file exists.");
                }
                sharedFileList.add(file);
            }
        }
    }
}
