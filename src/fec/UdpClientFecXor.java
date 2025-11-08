package fec;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Arrays;

public class UdpClientFecXor {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9002;
        int blockN = args.length > 2 ? Integer.parseInt(args[2]) : 8;       // N packet data + 1 parity
        int payloadLen = args.length > 3 ? Integer.parseInt(args[3]) : 400; // độ dài mỗi packet data (bytes)
        int blocks = args.length > 4 ? Integer.parseInt(args[4]) : 500;

        DatagramChannel ch = DatagramChannel.open();
        ch.connect(new InetSocketAddress(host, port));

        ByteBuffer out = ByteBuffer.allocateDirect(2000);
        int seq = 0;
        for (int b = 0; b < blocks; b++) {
            byte[][] data = new byte[blockN][];
            for (int i = 0; i < blockN; i++) {
                data[i] = new byte[payloadLen];
                Arrays.fill(data[i], (byte) ((seq + i) & 0xFF));
            }
            // gửi N packet data
            for (int i = 0; i < blockN; i++) {
                out.clear();
                out.put((byte)0);     // loại: data
                out.putInt(seq + i);
                out.putInt(blockN);
                out.put(data[i]);
                out.flip();
                ch.write(out);
            }
            // tính parity
            byte[] parity = new byte[payloadLen];
            for (int j = 0; j < payloadLen; j++) {
                byte x = 0;
                for (int i = 0; i < blockN; i++) x ^= data[i][j];
                parity[j] = x;
            }
            out.clear();
            out.put((byte)1); // loại: parity
            out.putInt(seq);  // sequence cơ sở
            out.putInt(blockN);
            out.put(parity);
            out.flip();
            ch.write(out);

            seq += blockN;
            Thread.sleep(1);
        }
        System.out.println("FEC client done.");
    }
}
