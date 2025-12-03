package back.fcz.global.init;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"local","dev"})
@Configuration
@RequiredArgsConstructor
public class BaseInitData {

}
