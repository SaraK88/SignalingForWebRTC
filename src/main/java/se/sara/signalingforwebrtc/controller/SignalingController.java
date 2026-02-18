package se.sara.signalingforwebrtc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import se.sara.signalingforwebrtc.dto.TokenResponse;
import se.sara.signalingforwebrtc.service.TokenService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/signaling")
@CrossOrigin(origins = "*")
public class SignalingController {
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // Store room participants
    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    
    @GetMapping("/rtm-token")
    public ResponseEntity<TokenResponse> getRtmToken(@RequestParam String uid) {
        return tokenService.generateRtmToken(uid);
    }
    
    @GetMapping("/media-token")
    public ResponseEntity<TokenResponse> getMediaToken(@RequestParam String channelName, @RequestParam String uid) {
        return tokenService.generateMediaToken(channelName, uid);
    }
    
    // WebSocket message mapping for real-time signaling
    @MessageMapping("/signaling")
    @SendTo("/topic/signaling/{room}")
    public Map<String, Object> handleSignalingMessage(@DestinationVariable String room, Map<String, Object> message) {
        // Add room to message for routing
        message.put("room", room);
        return message;
    }

    // HTTP fallback for signaling
    @PostMapping("/signaling")
    public ResponseEntity<?> handleSignaling(@RequestBody Map<String, Object> message) {
        try {
            System.out.println("=== HTTP SIGNALING ENDPOINT CALLED ===");
            System.out.println("Received message: " + message);
            
            String room = (String) message.get("room");
            String from = (String) message.get("from");
            
            System.out.println("Room: " + room + ", From: " + from);
            
            // Add user to room
            rooms.computeIfAbsent(room, k -> new HashSet<>()).add(from);
            userSessions.put(from, room);
            
            System.out.println("Broadcasting to topic: /topic/signaling/" + room);
            
            // Broadcast to room via WebSocket
            messagingTemplate.convertAndSend("/topic/signaling/" + room, (Object) message);
            
            System.out.println("Message broadcasted successfully");
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.out.println("HTTP signaling error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to process signaling message",
                "message", e.getMessage()
            ));
        }
    }

    // Get room participants
    @GetMapping("/rooms/{room}/users")
    public ResponseEntity<?> getRoomUsers(@PathVariable String room) {
        Set<String> users = rooms.getOrDefault(room, new HashSet<>());
        return ResponseEntity.ok(Map.of(
            "room", room,
            "users", users,
            "count", users.size()
        ));
    }

    // Join room
    @PostMapping("/rooms/{room}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String room, @RequestBody Map<String, String> request) {
        String username = request.get("username");
        
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username required"));
        }
        
        // Add user to room
        rooms.computeIfAbsent(room, k -> new HashSet<>()).add(username);
        userSessions.put(username, room);
        
        // Notify other users
        Map<String, Object> joinMessage = Map.of(
            "type", "user-joined",
            "username", username,
            "room", room,
            "timestamp", System.currentTimeMillis()
        );
        
        messagingTemplate.convertAndSend("/topic/signaling/" + room, (Object) joinMessage);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "room", room,
            "username", username,
            "users", rooms.get(room)
        ));
    }

    // Leave room
    @PostMapping("/rooms/{room}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String room, @RequestBody Map<String, String> request) {
        String username = request.get("username");
        
        if (username != null) {
            // Remove from room
            Set<String> roomUsers = rooms.get(room);
            if (roomUsers != null) {
                roomUsers.remove(username);
                if (roomUsers.isEmpty()) {
                    rooms.remove(room);
                }
            }
            
            userSessions.remove(username);
            
            // Notify other users
            Map<String, Object> leaveMessage = Map.of(
                "type", "user-left",
                "username", username,
                "room", room,
                "timestamp", System.currentTimeMillis()
            );
            
            messagingTemplate.convertAndSend("/topic/signaling/" + room, (Object) leaveMessage);
        }
        
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Get all rooms
    @GetMapping("/rooms")
    public ResponseEntity<?> getAllRooms() {
        Map<String, Object> roomInfo = new HashMap<>();
        rooms.forEach((room, users) -> {
            roomInfo.put(room, Map.of(
                "users", users,
                "count", users.size()
            ));
        });
        
        return ResponseEntity.ok(Map.of(
            "rooms", roomInfo,
            "totalRooms", rooms.size()
        ));
    }

    // Chat message endpoint
    @PostMapping("/chat")
    public ResponseEntity<?> sendChatMessage(@RequestBody Map<String, Object> message) {
        try {
            String room = (String) message.get("room");
            String username = (String) message.get("username");
            String text = (String) message.get("message");
            
            if (room == null || username == null || text == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }
            
            // Create chat message
            Map<String, Object> chatMessage = Map.of(
                "type", "chat",
                "username", username,
                "message", text,
                "room", room,
                "timestamp", System.currentTimeMillis()
            );
            
            // Broadcast to room
            messagingTemplate.convertAndSend("/topic/signaling/" + room, (Object) chatMessage);
            
            return ResponseEntity.ok(Map.of("success", true));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to send message",
                "message", e.getMessage()
            ));
        }
    }

    // Get user's current room
    @GetMapping("/users/{username}/room")
    public ResponseEntity<?> getUserRoom(@PathVariable String username) {
        String room = userSessions.get(username);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "username", username,
            "room", room,
            "users", rooms.getOrDefault(room, new HashSet<>())
        ));
    }
}
