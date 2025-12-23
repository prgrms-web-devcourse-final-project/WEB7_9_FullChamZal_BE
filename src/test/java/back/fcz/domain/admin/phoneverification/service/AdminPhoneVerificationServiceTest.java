package back.fcz.domain.admin.phoneverification.service;

import back.fcz.domain.admin.phoneverification.dto.AdminPhoneVerificationSearchRequest;
import back.fcz.domain.sms.repository.PhoneVerificationRepository;
import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPhoneVerificationServiceTest {

    @Mock
    PhoneVerificationRepository phoneVerificationRepository;

    AdminPhoneVerificationService service;

    @BeforeEach
    void setUp() {
        service = new AdminPhoneVerificationService(phoneVerificationRepository);
    }

    @Test
    void purpose가_이상하면_VALIDATION_FAILED() {
        AdminPhoneVerificationSearchRequest cond =
                AdminPhoneVerificationSearchRequest.of(0, 20, "NOT_EXISTS", null, null, null);

        BusinessException ex = catchThrowableOfType(() -> service.getPhoneVerifications(cond), BusinessException.class);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void status가_이상하면_VALIDATION_FAILED() {
        AdminPhoneVerificationSearchRequest cond =
                AdminPhoneVerificationSearchRequest.of(0, 20, null, "NOT_EXISTS", null, null);

        BusinessException ex = catchThrowableOfType(() -> service.getPhoneVerifications(cond), BusinessException.class);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void 단건조회_없으면_ADMIN_PHONE_VERIFICATION_NOT_FOUND() {
        when(phoneVerificationRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        BusinessException ex = catchThrowableOfType(() -> service.getPhoneVerificationDetail(1L), BusinessException.class);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ADMIN_PHONE_VERIFICATION_NOT_FOUND);
    }
}
