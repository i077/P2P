package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Class used to provide values such as IP addresses.
 */
public class Values {

    private static String IPCHECK_URL = "http://checkip.amazonaws.com";

    // Time between (expected) heartbeats, in ms
    public static int HEARTBEAT_INTERVAL = 30000;
    public static int READER_INTERVAL = 100;

    public static int PONGWAIT_INTERVAL = 5000;

    // UTILITIES

    /**
     * Combine an IP address and port number into a single string.
     *
     * @param ip An IP address
     * @param port A port number
     * @return A string of the form "{ip}:{port}"
     */
    public static String ipPortStr(String ip, int port) {
        return ip + ":" + port;
    }

    /**
     * Get the external IP of the host running this program.
     *
     * Since <code>InetAddress.getAddress()</code> returns the address of the loopback interface,
     * this method uses an external service from Amazon to determine the IP address.
     * If this fails, get the host's private IP address.
     * Failing that, exit abnormally since an IP address could not be resolved.
     * @return A string containing the IP address, hopefully the external address, of this host
     */
    public static String ownIPAddr() {
        // First try contacting AWS for IP address
        try {
            URL checkIPURL = new URL(IPCHECK_URL);
            InputStream checkIPStream = checkIPURL.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(checkIPStream));
            return br.readLine();
        } catch (IOException e) {
            Log.e(Messages.ERR_CHECKIP, e);
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
            Log.fatal(Messages.ERR_INETHOSTIP, e, -1);
        }

        return thisHostIP;
    }
}
