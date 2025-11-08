package common;

public class Util {
    public static long nanos() { return System.nanoTime(); }
    public static void busyWaitUntil(long targetNs) {
        while (System.nanoTime() < targetNs) {
            Thread.onSpinWait();
        }
    }
}
