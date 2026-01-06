package back.fcz.domain.storytrack.entity;

public enum FileType {
    IMAGE("image/");


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
        throw new IllegalArgumentException("Unsupported contentType: " + contentType);
    }
}
