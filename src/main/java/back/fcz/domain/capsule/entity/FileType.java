package back.fcz.domain.capsule.entity;

public enum FileType {
    IMAGE("image/"),
    VIDEO("video/"),
    AUDIO("audio/");

    private final String mimePrefix;

    FileType(String mimePrefix) {
        this.mimePrefix = mimePrefix;
    }

    public boolean matches(String contentType) {
        return contentType != null && contentType.startsWith(mimePrefix);
    }

    public static FileType fromContentType(String contentType) {
        if (contentType == null) return null;
        if (contentType.startsWith("image/")) return IMAGE;
        if (contentType.startsWith("video/")) return VIDEO;
        if (contentType.startsWith("audio/")) return AUDIO;
        throw new IllegalArgumentException("Unsupported contentType: " + contentType);
    }
}
