package aggregation;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.atomic.AtomicLong;

public class UdpAggregateServer {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9001;
        DatagramChannel ch = DatagramChannel.open();
        ch.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024 * 1024);
        ch.bind(new InetSocketAddress("0.0.0.0", port));

        ByteBuffer buf = ByteBuffer.allocateDirect(4096);
        AtomicLong datagrams = new AtomicLong();
        AtomicLong logical = new AtomicLong();
        long start = System.nanoTime();

        System.out.println("Aggregate server on port " + port);
        while (true) {
            buf.clear();
            SocketAddress remote = ch.receive(buf);
            if (remote == null) continue;
            buf.flip();
            datagrams.incrementAndGet();

            if (buf.remaining() < 4) continue;
            int k = buf.getInt();
            for (int i = 0; i < k; i++) {
                if (buf.remaining() < 2) break;
                int len = Short.toUnsignedInt(buf.getShort());
                if (buf.remaining() < len) break;
                buf.position(buf.position() + len);
                logical.incrementAndGet();
            }

            long d = datagrams.get();
            if (d % 200 == 0) {
                double secs = (System.nanoTime() - start) / 1e9;
                System.out.printf("Datagrams=%d LogicalMsgs=%d (%.1f msgs/dgram) Rate=%.0f dps%n",
                        d, logical.get(), (double)logical.get() / d, d / secs);
            }
        }
    }
}
