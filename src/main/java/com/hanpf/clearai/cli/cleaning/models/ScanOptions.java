package com.hanpf.clearai.cli.cleaning.models;

import java.util.Arrays;
import java.util.List;

/**
 * 文件扫描选项配置
 */
public class ScanOptions {
    private int maxFiles = 1000;
    private long minSize = 0;
    private long maxSize = Long.MAX_VALUE;
    private boolean includeHidden = false;
    private boolean includeDirectories = false;
    private int maxDepth = Integer.MAX_VALUE;
    private List<String> excludePatterns;
    private List<String> includeExtensions;
    private List<String> excludeExtensions;

    public ScanOptions() {
        this.excludePatterns = Arrays.asList(
            ".git", "node_modules", ".vscode", ".idea",
            "target", "build", "dist", "__pycache__"
        );
        this.includeExtensions = null; // null means include all
        this.excludeExtensions = null;
    }

    // Static builder
    public static ScanOptions create() {
        return new ScanOptions();
    }

    // Getters
    public int getMaxFiles() { return maxFiles; }
    public long getMinSize() { return minSize; }
    public long getMaxSize() { return maxSize; }
    public long getMinFileSize() { return minSize; }
    public long getMaxFileSize() { return maxSize; }
    public boolean isIncludeHidden() { return includeHidden; }
    public boolean isIncludeDirectories() { return includeDirectories; }
    public int getMaxDepth() { return maxDepth; }
    public List<String> getExcludePatterns() { return excludePatterns; }
    public List<String> getIncludeExtensions() { return includeExtensions; }
    public List<String> getExcludeExtensions() { return excludeExtensions; }

    // Setters
    public ScanOptions setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
        return this;
    }

    public ScanOptions setMinSize(long minSize) {
        this.minSize = minSize;
        return this;
    }

    public ScanOptions setMaxSize(long maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public ScanOptions setIncludeHidden(boolean includeHidden) {
        this.includeHidden = includeHidden;
        return this;
    }

    public ScanOptions setIncludeDirectories(boolean includeDirectories) {
        this.includeDirectories = includeDirectories;
        return this;
    }

    public ScanOptions setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public ScanOptions setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
        return this;
    }

    public ScanOptions setIncludeExtensions(List<String> includeExtensions) {
        this.includeExtensions = includeExtensions;
        return this;
    }

    public ScanOptions setMinFileSize(long minFileSize) {
        this.minSize = minFileSize;
        return this;
    }

    public ScanOptions setMaxFileSize(long maxFileSize) {
        this.maxSize = maxFileSize;
        return this;
    }

    public boolean shouldScanExtension(String extension) {
        if (includeExtensions == null || includeExtensions.isEmpty()) {
            return true;
        }
        return includeExtensions.contains(extension.toLowerCase());
    }

    public boolean shouldExcludePath(String path) {
        if (excludePatterns == null || excludePatterns.isEmpty()) {
            return false;
        }

        String lowerPath = path.toLowerCase();
        for (String pattern : excludePatterns) {
            if (lowerPath.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // 添加缺少的方法
    private List<String> includeExtensionList = null;
    private List<String> excludeExtensionList = null;

    public void addIncludeExtension(String extension) {
        if (includeExtensionList == null) {
            includeExtensionList = new java.util.ArrayList<>();
        }
        includeExtensionList.add(extension);
        this.includeExtensions = includeExtensionList;
    }

    public void addExcludeExtension(String extension) {
        if (excludeExtensionList == null) {
            excludeExtensionList = new java.util.ArrayList<>();
        }
        excludeExtensionList.add(extension);
        this.excludeExtensions = excludeExtensionList;
    }
}