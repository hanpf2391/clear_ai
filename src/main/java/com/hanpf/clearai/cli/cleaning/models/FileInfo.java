package com.hanpf.clearai.cli.cleaning.models;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 文件信息数据结构
 */
public class FileInfo {
    private String name;
    private String path; // 改名为path以匹配代码中的使用
    private long size;
    private long lastModified;
    private String extension;
    private boolean isDirectory;
    private String parentPath;
    private String type; // 添加type字段

    public FileInfo(String name, String fullPath, long size, long lastModified, boolean isDirectory) {
        this.name = name;
        this.path = fullPath;
        this.size = size;
        this.lastModified = lastModified;
        this.isDirectory = isDirectory;
        this.extension = extractExtension(name);
        this.parentPath = extractParentPath(fullPath); // 这里仍然使用fullPath，它是一个局部变量
        this.type = "";
    }

    // 无参构造函数
    public FileInfo() {
        this.type = "";
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    private String extractParentPath(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return "";
        }
        int lastSeparator = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
        return lastSeparator > 0 ? fullPath.substring(0, lastSeparator) : "";
    }

    // Getters (删除重复的getter，保留后面完整的版本)

    // Helper methods
    public String getFormattedSize() {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1fGB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public String getFormattedLastModified() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(lastModified),
            ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public boolean isSafeToDelete() {
        String[] safeExtensions = {".tmp", ".log", ".cache", ".bak", ".temp", ".swp", ".dmp"};
        for (String safeExt : safeExtensions) {
            if (extension.equals(safeExt.toLowerCase())) {
                return true;
            }
        }
        return name.startsWith("~") || name.endsWith("~");
    }

    public boolean isDangerous() {
        String[] dangerousExtensions = {".exe", ".dll", ".sys", ".bat", ".cmd", ".ps1", ".sh"};
        for (String dangerousExt : dangerousExtensions) {
            if (extension.equals(dangerousExt.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %s)", name, getFormattedSize(), getFormattedLastModified());
    }

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // 为了兼容性，保留fullPath getter
    public String getFullPath() {
        return path;
    }

    public void setFullPath(String fullPath) {
        this.path = fullPath;
    }
}