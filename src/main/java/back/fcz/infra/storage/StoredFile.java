package back.fcz.infra.storage;

public record StoredFile(
        String key,
        String filename,
        String contentType,
        long size
) {
}
