package nio;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class Buffers {

    public static void main(String[] args) throws IOException {

        FileChannel channel = new RandomAccessFile("client/1.txt", "rw")
                .getChannel();

        ByteBuffer buffer = ByteBuffer.allocate(6);
        channel.read(buffer);
        buffer.flip();
        byte[] buf = new byte[6];
        int pos = 0;
        while (buffer.hasRemaining()) {
            buf[pos++] = buffer.get();
        }
        System.out.println(new String(buf, StandardCharsets.UTF_8));
        buffer.rewind();
        channel.write(buffer, channel.size());

        ByteBuffer b1 = ByteBuffer.allocate(5);
        b1.put((byte) 1);
        b1.put((byte) 2);
        b1.put((byte) 3);
        b1.flip();
        while (b1.hasRemaining()) {
            System.out.println(b1.get());
        }
        b1.flip();
        b1.put((byte) 1);
        b1.put((byte) 2);
        b1.put((byte) 3);
        b1.put((byte) 4);
        b1.flip();
    }
}
