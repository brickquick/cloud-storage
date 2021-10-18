package qbrick;

import lombok.Getter;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
public class FileInfo implements Serializable {
    public enum FileType {
        FILE("F"),
        DIRECTORY("D");

        private final String name;

        public String getName() {
            return name;
        }

        FileType(String name) {
            this.name = name;
        }
    }

    private String filename;
    private FileType type;
    private long size;
    private LocalDateTime lastModified;

    public FileInfo(Path path) {
        try {
            this.filename = path.getFileName().toString();
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE;
            try {
                if (this.type == FileType.DIRECTORY) {
                    this.size = -1L;
                } else {
                    this.size = Files.size(path);
                }
                this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
            } catch (AccessDeniedException e) {
                e.printStackTrace();
                this.size = -2L;
                lastModified = LocalDateTime.now();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}