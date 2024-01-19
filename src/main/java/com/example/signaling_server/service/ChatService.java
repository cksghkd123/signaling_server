package com.example.signaling_server.service;

import com.example.signaling_server.dto.chat.ChatRoom;
import com.example.signaling_server.dto.chat.ChatRoomMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final Map<Long, ChatRoom> chatRooms = ChatRoomMap.getInstance().getChatRooms();

    public ChatRoom createChatRoom(String roomName, String roomPwd, Integer maxUserCnt, Long roomId) {

        return ChatRoom.builder()
                .roomId(roomId)
                .roomName(roomName)
                .roomPwd(roomPwd)
                .userCount(0)
                .maxUserCnt(maxUserCnt)
                .build();

    }

    public Map<String, WebSocketSession> getClients(final ChatRoom room) {
        return Optional.ofNullable(room)
                .map(r -> Collections.unmodifiableMap(r.getClients()))
                .orElse(Collections.emptyMap());
    }

    public void addClient(ChatRoom room, String name, WebSocketSession session) {
        room.getClients().put(name, session);
    }

    public void removeClientByName(ChatRoom room, String name) {
        room.getClients().remove(name);
    }


    public void addRoom(ChatRoom room) {
        chatRooms.put(room.getRoomId(), room);
    }

    public Long getRoomId(ChatRoom room) {
        return room.getRoomId();
    }

    public ChatRoom findRoomById(Long roomId) {
        return ChatRoomMap.getInstance().getChatRooms().get(roomId);
    }
}
