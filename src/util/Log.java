package util;

/**
 * Utilities for logging messages to standard output and error.
 * Uses VT100 escape sequences to control cursor movement, since a prompt is also displayed on standard output.
 */
public class Log {
    public static String PROMPT = "> ";

    /**
     * Print an informational message to standard output.
     *
     * @param msg The message to print
     */
    public static void i(String msg) {
        System.out.println("\033[2K\r" + msg);
        System.out.print(PROMPT);
    }

    /**
     * Print an error message to the standard error stream.
     *
     * @param err The error message to print
     */
    public static void e(String err) {
        Log.e(err, null);
    }

    /**
     * Print an error message and an exception's stack trace to the standard error stream.
     *
     * @param err The error message to print
     * @param e The exception that was caught
     */
    public static void e(String err, Exception e) {
        System.out.print("\033[2K\r");
        System.err.println(err);
        if (e != null)
            e.printStackTrace();
        System.out.print(PROMPT);
    }

    /**
     * Print a fatal error message and, optionally, an exception to the standard error stream, then exit with a return code.
     * This should only be called if the peer cannot recover or otherwise continue after this error.
     *
     * @param err The error message to print
     * @param e The exception that was caught
     * @param code The code to exit with.
     */
    public static void fatal(String err, Exception e, int code) {
        Log.e(err, e);
        System.exit(code);
    }
}
