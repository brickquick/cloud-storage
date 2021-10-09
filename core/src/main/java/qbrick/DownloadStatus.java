package qbrick;

import lombok.Getter;

@Getter
public class DownloadStatus extends Command{

    private final long start;

    public DownloadStatus(long start) {
        this.start = start;
    }

    @Override
    public CommandType getType() {
        return CommandType.DOWNLOAD_STATUS;
    }
}
