package pipeline;

import common.Util;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class UdpLoadClient {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9003;
        int size = args.length > 2 ? Integer.parseInt(args[2]) : 512;
        int rate = args.length > 3 ? Integer.parseInt(args[3]) : 20000; // tốc độ gửi (pkt/s)
        int seconds = args.length > 4 ? Integer.parseInt(args[4]) : 10;

        DatagramChannel ch = DatagramChannel.open();
        ch.setOption(StandardSocketOptions.SO_SNDBUF, 8 * 1024 * 1024);
        ch.connect(new InetSocketAddress(host, port));

        ByteBuffer buf = ByteBuffer.allocateDirect(size);
        long interval = (long)(1_000_000_000.0 / rate);
        long end = Util.nanos() + seconds * 1_000_000_000L;
        long next = Util.nanos();
        long sent = 0;
        long start = next;

        while (Util.nanos() < end) {
            buf.clear();
            for (int i = 0; i < size; i++) buf.put((byte) (i & 0xFF));
            buf.flip();
            ch.write(buf);
            sent++;
            next += interval;
            Util.busyWaitUntil(next);
        }
        double secs = (Util.nanos() - start) / 1e9;
        System.out.printf("Load sent=%d in %.2fs -> %.0f pkt/s%n", sent, secs, sent / secs);
    }
}
