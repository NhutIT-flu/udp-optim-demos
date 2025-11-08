package aggregation;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Arrays;

public class UdpClientAggregate {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9001;
        int msgsPerPacket = args.length > 2 ? Integer.parseInt(args[2]) : 8;    // số message mỗi packet
        int smallMsgLen = args.length > 3 ? Integer.parseInt(args[3]) : 100;    // độ dài mỗi message (bytes)
        int packets = args.length > 4 ? Integer.parseInt(args[4]) : 1000;

        DatagramChannel ch = DatagramChannel.open();
        ch.connect(new InetSocketAddress(host, port));

        int maxDatagram = 1500;
        ByteBuffer buf = ByteBuffer.allocateDirect(maxDatagram);
        byte[] small = new byte[smallMsgLen];
        Arrays.fill(small, (byte) 'A');

        for (int p = 0; p < packets; p++) {
            buf.clear();
            buf.putInt(msgsPerPacket);
            for (int i = 0; i < msgsPerPacket; i++) {
                buf.putShort((short) small.length);
                buf.put(small);
            }
            buf.flip();
            ch.write(buf);
            Thread.sleep(1);
        }
        System.out.println("Aggregation client done.");
    }
}
