package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Class used to provide values such as strings, port numbers, and IP addresses.
 */
public class Values {
    // MESSAGES
    public static String WELCOME_MSG = "Starting P2P node...";
    public static String WELCOME_IP() { return "Your current IP address is: " + ownIPAddr(); }

    // NETWORK VALUES

    private static String IPCHECK_URL = "http://checkip.amazonaws.com";
    public static int PORT_RANGE_START = 50320;

    // UTILITIES

    /**
     * Get the external IP of the host running this program.
     *
     * Since <code>InetAddress.getAddress()</code> returns the address of the loopback interface,
     * this method uses an external service from Amazon to determine the IP address.
     * If this fails, get the host's private IP address.
     * Failing that, exit abnormally since an IP address could not be resolved.
     * @return A string containing the IP address, hopefully the external address, of this host
     */
    private static String ownIPAddr() {
        // First try contacting AWS for IP address
        try {
            URL checkIPURL = new URL(IPCHECK_URL);
            InputStream checkIPStream = checkIPURL.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(checkIPStream));
            return br.readLine();
        } catch (IOException e) {
            System.err.println("Using external checkip service failed, trying InetAddress.");
            e.printStackTrace();
        }

        // At this point, using the URL didn't work,
        // so we use the JDK's InetAddress to try to get an IP address.
        // This may by the address of the loopback interface in some conditions.
        InetAddress thisHost;
        String thisHostIP = null;

        try {
            thisHost = InetAddress.getLocalHost();
            thisHostIP = InetAddress.getByName(thisHost.getHostName()).getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Something went wrong when trying to get this host's IP address.");
            e.printStackTrace();
            System.exit(-1);
        }

        return thisHostIP;
    }
}
