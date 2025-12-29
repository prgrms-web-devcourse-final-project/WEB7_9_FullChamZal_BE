package back.fcz.infra.storage;

public interface FileStorage {
    StoredFile store(FileUploadCommand command, String key);
    void delete(String key);
}
