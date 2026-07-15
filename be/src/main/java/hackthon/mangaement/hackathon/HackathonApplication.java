package hackthon.mangaement.hackathon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.net.URI;

@EnableCaching
@SpringBootApplication
public class HackathonApplication {

	public static void main(String[] args) {
		// Tự động chuyển đổi chuỗi kết nối từ dạng mysql:// của Railway/Aiven sang jdbc:mysql:// cho Spring Boot
		String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
		if (dbUrl == null || dbUrl.trim().isEmpty()) {
			dbUrl = System.getenv("Service_URI"); // Fallback cho Aiven Service_URI
		}

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
				
				int port = uri.getPort();
				String hostAndPort = uri.getHost() + (port == -1 ? "" : ":" + port);
				String path = uri.getPath();
				String query = uri.getQuery();
				
				String jdbcUrl = "jdbc:mysql://" + hostAndPort + path;
				if (query != null && !query.isEmpty()) {
					// Chuyển đổi ssl-mode= sang sslMode= để tương thích với MySQL Connector/J (như Aiven MySQL)
					query = query.replace("ssl-mode=", "sslMode=");
					jdbcUrl += "?" + query;
				} else {
					jdbcUrl += "?useSSL=false&allowPublicKeyRetrieval=true";
				}
				
				System.setProperty("spring.datasource.url", jdbcUrl);
				System.out.println("Auto-configured JDBC URL from environment variable: " + jdbcUrl);
			} catch (Exception e) {
				System.err.println("Failed to parse database URL: " + e.getMessage());
			}
		}
		SpringApplication.run(HackathonApplication.class, args);
	}

}
