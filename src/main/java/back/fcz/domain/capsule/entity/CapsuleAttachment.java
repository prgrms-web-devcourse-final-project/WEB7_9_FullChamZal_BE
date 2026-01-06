package back.fcz.domain.capsule.entity;

import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "capsule_attachment")
public class CapsuleAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "capsule_id")
    private Capsule capsule;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CapsuleAttachmentStatus status;

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

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    // 임시 업로드 팩토리 메소드
    public static CapsuleAttachment createUploading(
            Long uploaderId,
            String s3Key,
            String fileName,
            long size,
            String mimeType
    ) {
        CapsuleAttachment attachment = new CapsuleAttachment();
        attachment.uploaderId = uploaderId;
        attachment.s3Key = s3Key;
        attachment.fileName = fileName;
        attachment.fileType = FileType.fromContentType(mimeType);
        attachment.fileSize = size;
        attachment.mimeType = mimeType;
        attachment.status = CapsuleAttachmentStatus.UPLOADING;
        attachment.createdAt = LocalDateTime.now();
        attachment.expiredAt = LocalDateTime.now().plusMinutes(15);
        return attachment;
    }
    public void markTemp(){this.status = CapsuleAttachmentStatus.TEMP;}
    public void markDeleted() {
        this.status = CapsuleAttachmentStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
    public void markPending() {
        this.status = CapsuleAttachmentStatus.PENDING;
    }
    public void attachToCapsule(Capsule capsule) {
        this.capsule = capsule;
        this.status = CapsuleAttachmentStatus.USED;
    }

    public void validateContentType(String contentType) {
        if (this.fileType != null && contentType != null && !this.fileType.matches(contentType)) {
            throw new BusinessException(ErrorCode.CAPSULE_FILE_UPLOAD_TYPE_MISMATCH);
        }
    }


}
