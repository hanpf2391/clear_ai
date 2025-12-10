import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class DepthAnalyzer {
    public static void main(String[] args) {
        System.out.println("Directory Depth Analyzer");
        System.out.println("========================");

        try {
            String targetDir = "C:\\Users\\12699";
            System.out.println("Analyzing: " + targetDir);
            System.out.println();

            DepthVisitor visitor = new DepthVisitor();
            Files.walkFileTree(Paths.get(targetDir), visitor);

            System.out.println("\n📊 Depth Analysis Results:");
            System.out.println("Max depth found: " + visitor.maxDepth);
            System.out.println("Files at max depth: " + visitor.filesAtMaxDepth);
            System.out.println("Example paths at max depth:");
            visitor.deepPaths.stream().limit(5).forEach(path -> {
                System.out.println("  " + path);
            });

        } catch (Exception e) {
            System.err.println("Depth analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class DepthVisitor extends SimpleFileVisitor<Path> {
        int maxDepth = 0;
        int filesAtMaxDepth = 0;
        java.util.List<String> deepPaths = new java.util.ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            int depth = file.getNameCount() - Paths.get("C:\\Users\\12699").getNameCount();

            if (depth > maxDepth) {
                maxDepth = depth;
                filesAtMaxDepth = 1;
                deepPaths.clear();
                deepPaths.add(file.toString());
            } else if (depth == maxDepth) {
                filesAtMaxDepth++;
                if (deepPaths.size() < 10) {
                    deepPaths.add(file.toString());
                }
            }

            // Safety: stop if depth gets too large
            if (depth > 50) {
                System.out.println("WARNING: Very deep path found (" + depth + " levels): " + file);
                if (depth > 100) {
                    System.out.println("ERROR: Path depth too large, stopping analysis");
                    return FileVisitResult.TERMINATE;
                }
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            // Same depth logic for directories
            int depth = dir.getNameCount() - Paths.get("C:\\Users\\12699").getNameCount();

            if (depth > 50) {
                System.out.println("WARNING: Very deep directory (" + depth + " levels): " + dir);
                if (depth > 100) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            }

            return FileVisitResult.CONTINUE;
        }
    }
}