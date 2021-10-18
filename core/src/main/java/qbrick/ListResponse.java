package qbrick;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class ListResponse extends Command {

    private final List<FileInfo> fileInfos;

    public ListResponse(Path path) throws IOException {
        Stream<FileInfo> list = Files.list(path).map(FileInfo::new);
        fileInfos = list.collect(Collectors.toList());
        list.close();
    }

    @Override
    public CommandType getType() {
        return CommandType.LIST_RESPONSE;
    }

}