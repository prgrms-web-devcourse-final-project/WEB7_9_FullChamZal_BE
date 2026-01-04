package back.fcz.domain.capsule.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // 이 어노테이션이 setSend, setReceive를 자동으로 생성함
@NoArgsConstructor  // 캐시에 저장할 때 역직렬화(JSON -> Object)용
@AllArgsConstructor
public class MonthlyCapsuleStat {
    private String name;    // "1월"
    private long receive;   // 수신 개수
    private long send;      // 송신 개수
}
