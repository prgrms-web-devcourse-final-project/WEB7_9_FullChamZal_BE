package back.fcz.infra.storage;

public record StoredObjectMetadata(
        String s3Key,
        String contentType,
        long size
) {
}
