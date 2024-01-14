package com.example.signaling_server.service;

import com.example.signaling_server.dto.chat.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final Set<ChatRoom> rooms = new TreeSet<>(Comparator.comparing(ChatRoom::getRoomId));

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

    public void addClient(ChatRoom room, String name, WebSocketSession session){
        room.getClients().put(name, session);
    }

    public void removeClientByName(ChatRoom room, String name) {
        room.getClients().remove(name);
    }



    public Set<ChatRoom> getRooms() {
        final TreeSet<ChatRoom> defensiveCopy = new TreeSet<>(Comparator.comparing(ChatRoom::getRoomId));
        defensiveCopy.addAll(rooms);

        return defensiveCopy;
    }

    public Boolean addRoom(ChatRoom room) {
        return rooms.add(room);
    }
    public Long getRoomId(ChatRoom room) {
        return room.getRoomId();
    }


    public Optional<ChatRoom> findRoomByStringId(final String sid) {

        Long id = Long.valueOf(sid);
        return rooms.stream().filter(r -> r.getRoomId().equals(id)).findAny();
    }


}
