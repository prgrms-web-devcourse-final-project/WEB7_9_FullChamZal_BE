package back.fcz.domain.sms.service;

import back.fcz.infra.sms.SmsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
@RequiredArgsConstructor
public class SmsNotificaationService {

    private final SmsSender smsSender;

    public void sendCapsuleCreatedNotification(
            String receiverPhoneNumber,
            String senderName,
            String capsuleTitle
    ) {
        String message = buildMessage(senderName, capsuleTitle);

        smsSender.send(
                receiverPhoneNumber,
                message
        );
    }

    private String buildMessage(String senderName, String capsuleTitle) {
        return String.format(
                "[Dear._]\n%s님이 캡슐을 보냈어요.\n캡슐 제목: %s\n지정된 날짜에 열어보세요!",
                senderName,
                capsuleTitle
        );
    }

}
