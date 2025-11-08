package echo;

import common.Util;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.ThreadLocalRandom;

public class UdpClientPaced {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9000;
        int messages = args.length > 2 ? Integer.parseInt(args[2]) : 20000;
        int payloadBytes = args.length > 3 ? Integer.parseInt(args[3]) : 512;  // thử 256..1200
        int tps = args.length > 4 ? Integer.parseInt(args[4]) : 5000;          // tốc độ mục tiêu (pkt/s)
        int batch = args.length > 5 ? Integer.parseInt(args[5]) : 10;

        DatagramChannel ch = DatagramChannel.open();
        ch.setOption(StandardSocketOptions.SO_SNDBUF, 4 * 1024 * 1024);
        ch.connect(new InetSocketAddress(host, port));
        ch.configureBlocking(false);

        ByteBuffer send = ByteBuffer.allocateDirect(payloadBytes);
        ByteBuffer recv = ByteBuffer.allocateDirect(2048);

        long intervalNs = (long)(1_000_000_000.0 / Math.max(tps, 1));
        long next = Util.nanos();
        long sent = 0, recvCount = 0;
        long start = Util.nanos();

        while (sent < messages) {
            for (int i = 0; i < batch && sent < messages; i++) {
                send.clear();
                send.putInt((int)sent);
                while (send.hasRemaining()) {
                    send.put((byte) ThreadLocalRandom.current().nextInt(33, 126));
                }
                send.flip();
                ch.write(send);
                sent++;
            }
            next += intervalNs * batch;
            Util.busyWaitUntil(next);

            recv.clear();
            SocketAddress r = ch.receive(recv);
            if (r != null) recvCount++;
        }

        double secs = (Util.nanos() - start) / 1e9;
        System.out.printf("DONE. Sent=%d in %.2fs -> %.0f pkt/s. EchoReceived(sampled)=%d%n",
                sent, secs, sent / secs, recvCount);
        System.out.println("Tip: try different payload/tps/batch to see packet loss pattern in Wireshark.");
    }
}
