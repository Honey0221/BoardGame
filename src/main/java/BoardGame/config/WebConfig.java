package BoardGame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

// 웹 설정 클래스
@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(@NonNull CorsRegistry registry) {
		registry.addMapping("/**")
				// 허용된 원본 패턴 설정
				.allowedOriginPatterns("http://localhost:[*]")
				// 허용된 HTTP 메서드 설정
				.allowedMethods("GET", "POST", "PUT", "DELETE")
				// 허용된 헤더 설정
				.allowedHeaders("*")
				// 자격 증명 허용 설정
				.allowCredentials(true)
				// 캐시 설정
				.maxAge(3600);
	}
}
