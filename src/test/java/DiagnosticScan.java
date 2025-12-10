import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;

public class DiagnosticScan {
    public static void main(String[] args) {
        System.out.println("Diagnostic Scan Tool");
        System.out.println("====================");

        try {
            String targetDir = "C:\\Users\\12699";
            System.out.println("Scanning: " + targetDir);
            System.out.println();

            // Use Files.walkFileTree for comprehensive scan
            DiagnosticVisitor visitor = new DiagnosticVisitor();
            long startTime = System.currentTimeMillis();

            Files.walkFileTree(Paths.get(targetDir), visitor);

            long duration = System.currentTimeMillis() - startTime;

            System.out.println("\nScan Results:");
            System.out.println("Total files found: " + visitor.totalFiles);
            System.out.println("Total directories: " + visitor.totalDirs);
            System.out.println("Skipped files (IO errors): " + visitor.skippedFiles);
            System.out.println("Total size: " + formatFileSize(visitor.totalSize));
            System.out.println("Scan duration: " + duration + "ms");
            System.out.println("Throughput: " + (visitor.totalFiles * 1000.0 / duration) + " files/sec");

        } catch (Exception e) {
            System.err.println("Diagnostic scan failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    private static class DiagnosticVisitor extends SimpleFileVisitor<Path> {
        long totalFiles = 0;
        long totalDirs = 0;
        long skippedFiles = 0;
        long totalSize = 0;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            totalFiles++;
            totalSize += attrs.size();

            // Report progress every 10000 files
            if (totalFiles % 10000 == 0) {
                System.out.println("Scanned " + totalFiles + " files...");
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            skippedFiles++;
            if (skippedFiles <= 10) { // Only show first 10 errors
                System.out.println("Skipped (access denied): " + file);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            totalDirs++;
            return FileVisitResult.CONTINUE;
        }
    }
}