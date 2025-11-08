package pipeline;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class UdpPipelineServer {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9003;
        int workers = args.length > 1 ? Integer.parseInt(args[1]) : Math.max(2, Runtime.getRuntime().availableProcessors() - 1);

        DatagramChannel ch = DatagramChannel.open();
        ch.setOption(StandardSocketOptions.SO_RCVBUF, 8 * 1024 * 1024);
        ch.bind(new InetSocketAddress("0.0.0.0", port));
        ch.configureBlocking(false);

        Selector sel = Selector.open();
        ch.register(sel, SelectionKey.OP_READ);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(65536);
        AtomicLong received = new AtomicLong();
        long start = System.nanoTime();

        // Worker threads mô phỏng xử lý
        for (int i = 0; i < workers; i++) {
            pool.submit(() -> {
                try {
                    while (true) {
                        byte[] msg = queue.take();
                        // Mô phỏng công việc CPU
                        int sum = 0;
                        for (byte b : msg) sum += (b & 0xFF);
                        // demo này không gửi phản hồi
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ByteBuffer buf = ByteBuffer.allocateDirect(2048);
        System.out.printf("Pipeline UDP server on %d with %d workers%n", port, workers);
        while (true) {
            sel.select(10);
            for (SelectionKey key : sel.selectedKeys()) {
                if (key.isReadable()) {
                    buf.clear();
                    SocketAddress remote = ch.receive(buf);
                    if (remote != null) {
                        buf.flip();
                        byte[] data = new byte[buf.remaining()];
                        buf.get(data);
                        queue.offer(data);
                        long c = received.incrementAndGet();
                        if (c % 10000 == 0) {
                            double secs = (System.nanoTime() - start) / 1e9;
                            System.out.printf("Ingested=%d (%.0f pkt/s) Queue=%d%n", c, c / secs, queue.size());
                        }
                    }
                }
            }
            sel.selectedKeys().clear();
        }
    }
}
