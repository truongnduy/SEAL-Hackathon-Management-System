package hackthon.mangaement.hackathon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.net.URI;
@EnableCaching
@SpringBootApplication
public class HackathonApplication {

	public static void main(String[] args) {
		// Tự động chuyển đổi chuỗi kết nối từ dạng mysql:// của Railway sang jdbc:mysql:// cho Spring Boot
		String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
		if (dbUrl != null && dbUrl.startsWith("mysql://")) {
			try {
				URI uri = new URI(dbUrl);
				String userInfo = uri.getUserInfo();
				if (userInfo != null) {
					String[] credentials = userInfo.split(":");
					System.setProperty("spring.datasource.username", credentials[0]);
					if (credentials.length > 1) {
						System.setProperty("spring.datasource.password", credentials[1]);
					}
				}
				String jdbcUrl = "jdbc:mysql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
				System.setProperty("spring.datasource.url", jdbcUrl);
				System.out.println("Auto-configured JDBC URL from mysql:// environment variable.");
			} catch (Exception e) {
				System.err.println("Failed to parse SPRING_DATASOURCE_URL: " + e.getMessage());
			}
		}
		SpringApplication.run(HackathonApplication.class, args);
	}

}
