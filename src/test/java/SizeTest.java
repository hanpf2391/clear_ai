import com.hanpf.clearai.clustering.FileScanner;

public class SizeTest {
    public static void main(String[] args) {
        System.out.println("Testing File Size Accumulation");
        System.out.println("===========================");

        try {
            // Test with a small directory first
            FileScanner fileScanner = new FileScanner();
            long startTime = System.currentTimeMillis();

            FileScanner.ScanResult result = fileScanner.scanAndCluster("C:\\Users\\12699\\Desktop", true, 3);

            long duration = System.currentTimeMillis() - startTime;

            System.out.println("\n📊 Test Results:");
            System.out.println("Files scanned: " + result.getTotalFiles());
            System.out.println("Total size: " + result.getTotalSize() + " bytes");
            System.out.println("Formatted size: " + formatFileSize(result.getTotalSize()));
            System.out.println("Duration: " + duration + "ms");

            // Calculate expected vs actual
            long expectedSize = 0;
            for (var cluster : result.getClusters()) {
                expectedSize += cluster.getTotalSize();
            }

            System.out.println("Expected total size: " + expectedSize + " bytes");
            System.out.println("Size mismatch: " + (result.getTotalSize() - expectedSize) + " bytes");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
}