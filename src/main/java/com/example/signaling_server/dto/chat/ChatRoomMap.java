package com.example.signaling_server.dto.chat;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class ChatRoomMap {
    private static final ChatRoomMap chatRoomMap = new ChatRoomMap();
    private Map<String, ChatRoom> chatRooms = new LinkedHashMap<>();

    private ChatRoomMap(){}

    public static ChatRoomMap getInstance(){
        return chatRoomMap;
    }


}