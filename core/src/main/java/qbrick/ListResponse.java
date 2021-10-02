package qbrick;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ListResponse extends Command {

    private final List<FileInfo> fileInfos;

    public ListResponse(Path path) throws IOException {
        fileInfos = Files.list(path)
                .map(FileInfo::new)
                .collect(Collectors.toList());
    }

    @Override
    public CommandType getType() {
        return CommandType.LIST_RESPONSE;
    }

}