package qbrick;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ListResponse extends Command{

//    private final List<String> fileNames;
    private final List<FileInfo> fileInfos;

    public ListResponse(Path path) throws IOException {
//        fileNames = Files.list(path)
//                .map(p -> p.getFileName().toString())
//                .collect(Collectors.toList());

        fileInfos = Files.list(path)
                .map(FileInfo::new)
                .collect(Collectors.toList());
    }

    @Override
    public CommandType getType() {
        return CommandType.LIST_RESPONSE;
    }

}