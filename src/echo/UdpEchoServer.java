package echo;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Iterator;

public class UdpEchoServer {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        DatagramChannel ch = DatagramChannel.open();
        ch.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        ch.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024 * 1024); // buffer nhận 4MB
        ch.bind(new InetSocketAddress("0.0.0.0", port));
        ch.configureBlocking(false);

        Selector sel = Selector.open();
        ch.register(sel, SelectionKey.OP_READ);
        ByteBuffer buf = ByteBuffer.allocateDirect(2048);
        long received = 0, echoed = 0, start = System.nanoTime();

        System.out.println("UDP Echo Server listening on port " + port);
        while (true) {
            sel.select(100);
            Iterator<SelectionKey> it = sel.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next(); it.remove();
                if (key.isReadable()) {
                    buf.clear();
                    SocketAddress remote = ch.receive(buf);
                    if (remote != null) {
                        received++;
                        buf.flip();
                        ch.send(buf, remote);
                        echoed++;
                    }
                }
            }
            if ((received % 5000) == 0 && received > 0) {
                double secs = (System.nanoTime() - start) / 1e9;
                System.out.printf("Received=%d Echoed=%d Rate=%.0f pkt/s%n", received, echoed, received / secs);
            }
        }
    }
}
