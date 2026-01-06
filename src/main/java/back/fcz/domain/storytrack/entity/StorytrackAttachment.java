package back.fcz.domain.storytrack.entity;

import back.fcz.domain.capsule.entity.FileType;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "storytrack_attachment")
public class StorytrackAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storytrack_id")
    private Storytrack storytrack;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "storytrack_status", nullable = false)
    private StorytrackStatus status;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public static StorytrackAttachment createUploading(
            Long uploaderId,
            String s3Key,
            String fileName,
            long size,
            String mimeType
    ) {
        StorytrackAttachment attachment = new StorytrackAttachment();
        attachment.uploaderId = uploaderId;
        attachment.s3Key = s3Key;
        attachment.fileName = fileName;
        attachment.fileType = FileType.fromContentType(mimeType);
        attachment.fileSize = size;
        attachment.mimeType = mimeType;
        attachment.status = StorytrackStatus.UPLOADING;
        attachment.createdAt = LocalDateTime.now();
        attachment.expiredAt = LocalDateTime.now().plusMinutes(15);
        return attachment;
    }

    public void markDeleted(){
        this.status = StorytrackStatus.DELETED;
        this.deletedAt = java.time.LocalDateTime.now();
    }
    public void markTemp() { this.status = StorytrackStatus.TEMP; }
    public void markPending() { this.status = StorytrackStatus.PENDING; }
    public void attachToStorytrack(Storytrack storytrack){
        this.storytrack = storytrack;
        this.status = StorytrackStatus.THUMBNAIL;
    }
    public void validateContentType(String contentType) {
        if (this.fileType != null && contentType != null && !this.fileType.matches(contentType)) {
            throw new BusinessException(ErrorCode.CAPSULE_FILE_UPLOAD_TYPE_MISMATCH);
        }
    }
}
