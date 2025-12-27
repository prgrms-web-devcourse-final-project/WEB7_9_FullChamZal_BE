package back.fcz.domain.capsule.entity;

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

    @ManyToOne
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

    @Column(name = "file_type", nullable = false)
    private String fileType;

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
    public static CapsuleAttachment createTemp(
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
        attachment.fileType = "IMAGE";
        attachment.fileSize = size;
        attachment.mimeType = mimeType;
        attachment.status = CapsuleAttachmentStatus.TEMP;
        attachment.createdAt = LocalDateTime.now();
        attachment.expiredAt = LocalDateTime.now().plusMinutes(15);
        return attachment;
    }

    public void markDeleted() {
        this.status = CapsuleAttachmentStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void attachToCapsule(Capsule capsule) {
        this.capsule = capsule;
        this.status = CapsuleAttachmentStatus.USED;
    }
}
