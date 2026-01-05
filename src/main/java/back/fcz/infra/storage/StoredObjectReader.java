package back.fcz.infra.storage;

public interface StoredObjectReader {
    StoredObjectMetadata head(String s3Key);
}
