package back.fcz.infra.sms;

import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
public class CoolSmsClientTest {

    @Test
    @DisplayName("유효한 전화번호와 메세지를 입력하면 메세지 전송이 호출됨")
    public void sendSmsTest() throws Exception {
        // given
        DefaultMessageService mockMessageService = mock(DefaultMessageService.class);

        CoolSmsClient client = new CoolSmsClientForTest(
                mockMessageService,
                "01099998888"
        );
        String to = "010-1234-5678";
        String content = "Test";

        // when
        client.sendSms(to, content);

        //then
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mockMessageService, times(1)).send(captor.capture());

        Message sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("01099998888");
        assertThat(sent.getTo()).isEqualTo("01012345678");
        assertThat(sent.getText()).isEqualTo("Test");

    }
    static class CoolSmsClientForTest extends CoolSmsClient {
        protected  CoolSmsClientForTest(DefaultMessageService mockMessageService, String fromNumber) {
            super(mockMessageService, fromNumber);
        }
    }
}
