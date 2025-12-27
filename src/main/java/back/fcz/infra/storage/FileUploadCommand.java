package back.fcz.infra.storage;

import java.io.InputStream;

public record FileUploadCommand(
        String directory,
        String filename,
        String contentType,
        InputStream inputStream,
        long size
) {
}
