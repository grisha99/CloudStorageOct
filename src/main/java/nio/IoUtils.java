package nio;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;

public class IoUtils {

    public static void main(String[] args) throws IOException, InterruptedException {
        // Path - not File
        Path path = Path.of("client");
        System.out.println(path);
        System.out.println(path.toAbsolutePath());

        // iterator -> dir1, dir2, dir3, file.txt

        WatchService watchService = FileSystems
                .getDefault()
                .newWatchService();

        path.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        Files.newBufferedReader(Path.of("client", "12.txt"))
                .lines()
                .forEach(System.out::println);

        if (!Files.exists(Path.of("client", "dir1"))) {
            Files.createDirectory(Path.of("client", "dir1"));
        }

        Path p = Path.of("client", "dir1", "1.txt");
        if (!Files.exists(p)) {
            Files.createFile(p);
        }

        Files.copy(Path.of("client", "12.txt"), p, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Path.of("client", "12.txt"),
                Path.of("client/dir1/2.txt"),
                StandardCopyOption.REPLACE_EXISTING
        );

        Files.write(
                Path.of("client/dir1/3.txt"),
                "Hello world".getBytes(),
                StandardOpenOption.APPEND
        );

        Files.walkFileTree(Path.of("client"), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println(file.toAbsolutePath());
                return super.visitFile(file, attrs);
            }
        });

        Files.find(Path.of("client"), Integer.MAX_VALUE, new BiPredicate<Path, BasicFileAttributes>() {
            @Override
            public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
                return path.getFileName().toString().equals("3.txt");
            }
        }).forEach(System.out::println);



//        while (true) {
//            var key = watchService.take();
//            if (key.isValid()) {
//                var events = key.pollEvents();
//                for (WatchEvent<?> event : events) {
//                    System.out.println(event.kind() + " " + event.context());
//                }
//                key.reset();
//            }
//        }
    }
}
