package back.fcz.infra.sms;


import back.fcz.global.exception.BusinessException;
import back.fcz.global.exception.ErrorCode;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CoolSmsClient {

    private final DefaultMessageService messageService;
    private final String fromNumber;

    @Autowired
    public CoolSmsClient(
            @Value("${coolsms.api-key}")String apiKey,
            @Value("${coolsms.api-secret}")String apiSecret,
            @Value("${coolsms.from-number}")String fromNumber
    ){
        this.messageService = SolapiClient.INSTANCE.createInstance(apiKey, apiSecret);
        this.fromNumber = fromNumber;
    }

    // 테스트용 생성자
    protected CoolSmsClient(DefaultMessageService messageService, String fromNumber) {
        this.messageService = messageService;
        this.fromNumber = fromNumber;
    }

    // 인증번호 문자 발송
    public void sendSms(String to, String content){
        try{
            Message message = new Message();
            message.setFrom(fromNumber);
            message.setTo(formatToNumber(to));
            message.setText(content);

            messageService.send(message);
        } catch (Exception e){
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
    }
    private String formatToNumber(String to) {
        if (to == null) return null;
        return to.replaceAll("[^0-9]", "");
    }
}
