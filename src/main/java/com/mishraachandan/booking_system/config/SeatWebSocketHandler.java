package com.mishraachandan.booking_system.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SeatWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("New WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket connection closed: {}", session.getId());
    }

    public void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.warn("Failed to send WebSocket message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    public void broadcastSeatStatus(Long showId, Long showSeatId, String status, Long lockedByUserId) {
        String payload = String.format(
                "{\"showId\":%d,\"showSeatId\":%d,\"status\":\"%s\",\"lockedByUserId\":%s}",
                showId,
                showSeatId,
                status,
                lockedByUserId != null ? lockedByUserId.toString() : "null"
        );
        log.info("Broadcasting WebSocket seat update: {}", payload);
        broadcast(payload);
    }
}
