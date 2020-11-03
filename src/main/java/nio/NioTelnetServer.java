package nio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class NioTelnetServer {

    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final String rootPath = "server";
//    private File file;
    private StringBuilder currentDir;

    public NioTelnetServer() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started!");
        currentDir = new StringBuilder();
//        file = new File(rootPath + currentDir.toString());
        while (server.isOpen()) {
            selector.select();
            var selectionKeys = selector.selectedKeys();
            var iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                var key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                }
                if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    // TODO: 30.10.2020
    //  ls - список файлов (сделано на уроке),
    //  cd (name) - перейти в папку
    //  touch (name) создать текстовый файл с именем
    //  mkdir (name) создать директорию
    //  rm (name) удалить файл по имени
    //  copy (src, target) скопировать файл из одного пути в другой
    //  cat (name) - вывести в консоль содержимое файла

    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int read = channel.read(buffer);
        if (read == -1) {
            channel.close();
            return;
        }
        if (read == 0) {
            return;
        }
        buffer.flip();
        byte[] buf = new byte[read];
        int pos = 0;
        while (buffer.hasRemaining()) {
            buf[pos++] = buffer.get();
        }
        buffer.clear();
        String command = new String(buf, StandardCharsets.UTF_8)
                .replace("\n", "")
                .replace("\r", "");
        System.out.println(command);
        if (command.equals("--help")) {
            channel.write(ByteBuffer.wrap("input ls for show file list".getBytes()));
        }
        if (command.equals("ls")) {
            channel.write(ByteBuffer.wrap(getFilesList().getBytes()));
        }
        if (command.startsWith("cd ")) {
            String openDir = command.split(" ")[1];
            if (changeCurrentDir(openDir)) {
                channel.write(ByteBuffer.wrap(("work directory change to \"" + rootPath + currentDir + "\"").getBytes()));
            } else {
                channel.write(ByteBuffer.wrap(("Directory \"" + rootPath + currentDir + "\" not found!").getBytes()));
            }
        }
        if (command.startsWith("touch ")) {
            String newFile = command.split(" ")[1];
            if (createNewFileOrDir(newFile)) {
                channel.write(ByteBuffer.wrap(("file \"" + rootPath + currentDir + "/" + newFile + "\" create.").getBytes()));
            } else {
                channel.write(ByteBuffer.wrap(("Error create \"" + rootPath + currentDir + "/" + newFile + "\" file.").getBytes()));
            }
        }

        if (command.startsWith("mkdir ")) {
            String newFile = command.split(" ")[1];
            if (createNewFileOrDir(newFile)) {
                channel.write(ByteBuffer.wrap(("Directory \"" + rootPath + currentDir + "/" + newFile + "\" create.").getBytes()));
            } else {
                channel.write(ByteBuffer.wrap(("Error create \"" + rootPath + currentDir + "/" + newFile + "\" directory.").getBytes()));
            }
        }

        if (command.startsWith("rm ")) {
            String targetFile = command.split(" ")[1];
            if (deleteFileOrDir(targetFile)) {
                channel.write(ByteBuffer.wrap(("Directory or file \"" + rootPath + currentDir + "/" + targetFile + "\" deleted.").getBytes()));
            } else {
                channel.write(ByteBuffer.wrap(("Error delete \"" + rootPath + currentDir + "/" + targetFile + "\" directory or file.").getBytes()));
            }
        }

        if (command.startsWith("copy ")) {
            String srcFileName = command.split(" ")[1];
            String dstFolder = command.split(" ")[2];
            if (copyFileOrDir(srcFileName, dstFolder)) {
                channel.write(ByteBuffer.wrap(("File \"" + srcFileName + "\" copyed to \"" + rootPath + currentDir + "/" + dstFolder + "\"").getBytes()));
            } else {
                channel.write(ByteBuffer.wrap(("Error copy file \"" + srcFileName + "\"").getBytes()));
            }

        }

        if (command.startsWith("cat ")) {
            String catFileName = command.split(" ")[1];
            byte [] fileContent = catFile(catFileName);
            if (fileContent != null) {
                channel.write(ByteBuffer.wrap((new String(fileContent)).getBytes()));
            } else {
                channel.write(ByteBuffer.wrap(("File \"" + catFileName + "\" not exists.").getBytes()));
            }
        }
    }



    private void sendMessage(String message, Selector selector) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                ((SocketChannel)key.channel())
                        .write(ByteBuffer.wrap(message.getBytes()));
            }
        }
    }

    private String getFilesList() {
        return String.join(" ", new File(rootPath + currentDir.toString()).list());
    }

    private boolean changeCurrentDir(String newDir) {
        if (Files.exists(Paths.get(rootPath + currentDir.toString() + "/" + newDir) )) {
            currentDir.append("/").append(newDir);
            return true;
        }
        return false;
    }

    private boolean createNewFileOrDir(String newElementName) {
        Path pathNewElement = Paths.get(rootPath + currentDir.toString() + "/" + newElementName);

        if (Files.exists(pathNewElement)) {
            return false;
        } else {
            try {
                if (Files.isDirectory(pathNewElement)) {
                    Files.createDirectory(pathNewElement);
                } else {
                    Files.createFile(pathNewElement);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private boolean deleteFileOrDir(String targetElement) {
        Path pathTargetElement = Paths.get(rootPath + currentDir.toString() + "/" + targetElement);
        try {
            return Files.deleteIfExists(pathTargetElement);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean copyFileOrDir(String srcFileName, String dstFilePath) {
        Path srcFilePath = Paths.get(rootPath + currentDir.toString() + "/" + srcFileName);
        Path dstPath = Paths.get(rootPath + "/" + dstFilePath);
        try {
            Files.copy(srcFilePath, dstPath, StandardCopyOption.COPY_ATTRIBUTES);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private byte[] catFile(String catFileName) {
        Path fileName = Paths.get(rootPath + currentDir.toString() + "/" + catFileName);
        if (Files.exists(fileName)) {
            try {
                return Files.readAllBytes(fileName);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "LOL");
        channel.write(ByteBuffer.wrap("Enter --help".getBytes()));
    }

    public static void main(String[] args) throws IOException {
        new NioTelnetServer();
    }
}
