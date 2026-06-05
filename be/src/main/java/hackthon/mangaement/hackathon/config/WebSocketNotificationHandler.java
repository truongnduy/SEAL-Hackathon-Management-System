package hackthon.mangaement.hackathon.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketNotificationHandler extends TextWebSocketHandler {

    private static final Map<Integer, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            try {
                String userIdStr = query.split("userId=")[1].split("&")[0];
                Integer userId = Integer.parseInt(userIdStr);
                sessions.put(userId, session);
            } catch (Exception e) {
                System.err.println("Failed to parse userId from WebSocket query: " + e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.values().remove(session);
    }

    public static void sendNotificationToUser(Integer userId, String jsonPayload) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(jsonPayload));
            } catch (IOException e) {
                System.err.println("Failed to send WebSocket message: " + e.getMessage());
            }
        }
    }
}
