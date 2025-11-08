package fec;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class UdpFecServer {
    static class Block {
        final int baseSeq, N, len;
        final Map<Integer, byte[]> data = new HashMap<>();
        byte[] parity;
        Block(int baseSeq, int N, int len) { this.baseSeq = baseSeq; this.N = N; this.len = len; }
        boolean complete() { return data.size() == N || (data.size() == N - 1 && parity != null); }
        boolean tryRecover() {
            if (parity == null || data.size() != N - 1) return false;
            int missing = -1;
            for (int i = 0; i < N; i++) if (!data.containsKey(baseSeq + i)) { missing = baseSeq + i; break; }
            if (missing < 0) return false;
            byte[] rec = Arrays.copyOf(parity, len);
            for (byte[] v : data.values()) {
                for (int j = 0; j < len; j++) rec[j] ^= v[j];
            }
            data.put(missing, rec);
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9002;
        DatagramChannel ch = DatagramChannel.open();
        ch.setOption(StandardSocketOptions.SO_RCVBUF, 8 * 1024 * 1024);
        ch.bind(new InetSocketAddress("0.0.0.0", port));

        ByteBuffer in = ByteBuffer.allocateDirect(4096);
        Map<Integer, Block> blocks = new HashMap<>();
        long okBlocks = 0, recovered = 0, lostBlocks = 0;
        long start = System.nanoTime();

        System.out.println("FEC server on port " + port);
        while (true) {
            in.clear();
            SocketAddress r = ch.receive(in);
            if (r == null) continue;
            in.flip();
            if (in.remaining() < 9) continue;

            byte type = in.get();
            int seq = in.getInt();
            int N = in.getInt();
            int len = in.remaining();
            int base = (type == 1) ? seq : (seq - (seq % N));

            Block b = blocks.computeIfAbsent(base, k -> new Block(base, N, len));
            byte[] payload = new byte[len];
            in.get(payload);
            if (type == 0) b.data.put(seq, payload); else b.parity = payload;

            if (b.complete()) {
                if (b.data.size() == N) okBlocks++;
                else if (b.tryRecover()) { recovered++; okBlocks++; }
                else lostBlocks++;

                blocks.remove(base);
                long total = okBlocks + lostBlocks;
                if (total % 100 == 0) {
                    double secs = (System.nanoTime() - start) / 1e9;
                    System.out.printf("OK=%d Recovered=%d Lost=%d Blocks/s=%.1f%n", okBlocks, recovered, lostBlocks, total / secs);
                }
            }
        }
    }
}
