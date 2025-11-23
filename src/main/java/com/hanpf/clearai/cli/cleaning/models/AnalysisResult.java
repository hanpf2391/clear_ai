package com.hanpf.clearai.cli.cleaning.models;

import java.util.List;
import java.util.ArrayList;

/**
 * AI分析结果数据结构
 */
public class AnalysisResult {
    private Summary summary;
    private List<SafeGroup> safeGroups;
    private List<ReviewItem> reviewItems;

    public AnalysisResult() {
        this.safeGroups = new ArrayList<>();
        this.reviewItems = new ArrayList<>();
    }

    // Summary内部类
    public static class Summary {
        private int totalFiles;
        private String totalSize;
        private String scanPath;
        private String scanDate;

        public Summary(int totalFiles, String totalSize, String scanPath, String scanDate) {
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
            this.scanPath = scanPath;
            this.scanDate = scanDate;
        }

        // Getters
        public int getTotalFiles() { return totalFiles; }
        public String getTotalSize() { return totalSize; }
        public String getScanPath() { return scanPath; }
        public String getScanDate() { return scanDate; }
    }

    // SafeGroup内部类
    public static class SafeGroup {
        private String name;
        private String description;
        private String totalSize;
        private int fileCount;
        private String summary;
        private String advice;
        private List<String> files;

        public SafeGroup(String name, String description, String totalSize, int fileCount,
                        String summary, String advice) {
            this.name = name;
            this.description = description;
            this.totalSize = totalSize;
            this.fileCount = fileCount;
            this.summary = summary;
            this.advice = advice;
            this.files = new ArrayList<>();
        }

        // Getters and setters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getTotalSize() { return totalSize; }
        public int getFileCount() { return fileCount; }
        public String getSummary() { return summary; }
        public String getAdvice() { return advice; }
        public List<String> getFiles() { return files; }

        public void addFile(String filePath) {
            this.files.add(filePath);
        }
    }

    // ReviewItem内部类
    public static class ReviewItem {
        private String fileName;
        private String filePath;
        private String size;
        private String lastModified;
        private String fileType;
        private String summary;
        private String advice;

        public ReviewItem(String fileName, String filePath, String size, String lastModified,
                         String fileType, String summary, String advice) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.size = size;
            this.lastModified = lastModified;
            this.fileType = fileType;
            this.summary = summary;
            this.advice = advice;
        }

        // Getters
        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public String getSize() { return size; }
        public String getLastModified() { return lastModified; }
        public String getFileType() { return fileType; }
        public String getSummary() { return summary; }
        public String getAdvice() { return advice; }
    }

    // Getters and setters for AnalysisResult
    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }
    public List<SafeGroup> getSafeGroups() { return safeGroups; }
    public void setSafeGroups(List<SafeGroup> safeGroups) { this.safeGroups = safeGroups; }
    public List<ReviewItem> getReviewItems() { return reviewItems; }
    public void setReviewItems(List<ReviewItem> reviewItems) { this.reviewItems = reviewItems; }

    // Helper methods
    public void addSafeGroup(SafeGroup group) {
        this.safeGroups.add(group);
    }

    public void addReviewItem(ReviewItem item) {
        this.reviewItems.add(item);
    }

    public int getSafeFilesCount() {
        return safeGroups.stream().mapToInt(SafeGroup::getFileCount).sum();
    }

    public int getReviewFilesCount() {
        return reviewItems.size();
    }

    public boolean hasSafeFiles() {
        return !safeGroups.isEmpty();
    }

    public boolean hasReviewFiles() {
        return !reviewItems.isEmpty();
    }
}