package com.example.signaling_server.handler;

import com.example.signaling_server.dto.chat.ChatRoom;
import com.example.signaling_server.dto.chat.ChatRoomMap;
import com.example.signaling_server.dto.chat.SignalData;
import com.example.signaling_server.dto.chat.SignalType;
import com.example.signaling_server.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

@Component
@RequiredArgsConstructor
public class SignalHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final Map<Long, ChatRoom> chatRooms = ChatRoomMap.getInstance().getChatRooms();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, ChatRoom> sessionIdToRoomMap = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (chatRooms.isEmpty()) {
            ChatRoom room = ChatRoom.builder()
                    .roomId(1L)
                    .roomName("1번방")
                    .roomPwd("password") // 채팅방 패스워드
                    .userCount(0) // 채팅방 참여 인원수
                    .maxUserCnt(2) // 최대 인원수 제한
                    .clients(new HashMap<String, WebSocketSession>())
                    .build();

            chatRooms.put(room.getRoomId(), room);

        }

        System.out.println("들어왔따~~");

        SignalData sd = SignalData.builder()
                .sender("Server")
                .signalType("Join")
                .data(Boolean.toString(!sessionIdToRoomMap.isEmpty()))
                .iceCandidate(null)
                .sdp(null)
                .build();
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(sd)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        ChatRoom chatRoom = sessionIdToRoomMap.get(session.getId());
        // remove the client which leaves from the Room clients list
        Optional<String> client = chatService.getClients(chatRoom).entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().getId(), session.getId()))
                .map(Map.Entry::getKey)
                .findAny();
        client.ifPresent(c -> chatService.removeClientByName(chatRoom, c));
        sessionIdToRoomMap.remove(session.getId());

        System.out.println("나갔다~~");
        System.out.println("현재 존재하는 방 ID들:");
        for (Long roomId : chatRooms.keySet()) {
            System.out.println("방 ID: " + roomId);
            Map<String, WebSocketSession> clients = chatService.getClients(chatRooms.get(roomId));
            for (String d : clients.keySet()) {
                System.out.println("멤버: " + d);
            }
        }
        logger.debug("오잉?");
        super.afterConnectionClosed(session, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        SignalData signalData = objectMapper.readValue(message.getPayload(), SignalData.class);

        String sender = signalData.getSender(); // origin of the message
        String data = signalData.getData(); // payload
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@");
        System.out.println(sender+" 로 부터 "+signalData.getSignalType());
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@");

        ChatRoom chatRoom;

        if (signalData.getSignalType().equalsIgnoreCase(SignalType.Join.toString())) {
            System.out.println("[ws] "+sender+" has joined Room: #"+ data);


            logger.debug("[ws] {} has joined Room: #{}", sender, data);

            chatRoom = chatService.findRoomById(Long.parseLong(data));
            // add client to the Room clients list

            chatService.addClient(chatRoom, sender, session);
            sessionIdToRoomMap.put(session.getId(), chatRoom);

            System.out.println("현재 존재하는 방 ID들:");
            for (Long roomId : chatRooms.keySet()) {
                System.out.println("방 ID: " + roomId);
                Map<String, WebSocketSession> clients = chatService.getClients(chatRooms.get(roomId));
                for (String d : clients.keySet()) {
                    System.out.println("멤버: " + d);
                }
            }

            return;

        } else if (signalData.getSignalType().equalsIgnoreCase(SignalType.Leave.toString())) {
            System.out.println("[ws] {} is going to leave Room: #{}" + sender + data);
            logger.debug("[ws] {} is going to leave Room: #{}", sender, data);

            chatRoom = sessionIdToRoomMap.get(session.getId());
            // remove the client which leaves from the Room clients list
            Optional<String> client = chatService.getClients(chatRoom).entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getValue().getId(), session.getId()))
                    .map(Map.Entry::getKey)
                    .findAny();
            client.ifPresent(c -> chatService.removeClientByName(chatRoom, c));

            return;

        } else if (signalData.getSignalType().equalsIgnoreCase(SignalType.Offer.toString())) {
            Object iceCandidate = signalData.getIceCandidate();
            Object sdp = signalData.getSdp();
            System.out.println("[ws] offer offer");
            logger.debug("[ws] Signal: {}",
                    iceCandidate != null
                            ? iceCandidate.toString().substring(0, 64)
                            : sdp.toString().substring(0, 64));

            ChatRoom room = sessionIdToRoomMap.get(session.getId());

            if (room != null) {
                Map<String, WebSocketSession> clients = chatService.getClients(room);

                for (Map.Entry<String, WebSocketSession> client : clients.entrySet()) {
                    // send messages to all clients except current user
                    if (!client.getKey().equals(sender)) {
                        // select the same type to resend signal
                        SignalData sd = SignalData.builder()
                                .data(data)
                                .iceCandidate(iceCandidate)
                                .sdp(sdp)
                                .sender(sender)
                                .signalType(signalData.getSignalType())
                                .build();
                        client.getValue().sendMessage(new TextMessage(objectMapper.writeValueAsString(sd)));
                    }
                }

            }

        } else if (signalData.getSignalType().equalsIgnoreCase(SignalType.Answer.toString())) {
            Object iceCandidate = signalData.getIceCandidate();
            Object sdp = signalData.getSdp();
            System.out.println("[ws] answer answer");
            logger.debug("[ws] Signal: {}",
                    iceCandidate != null
                            ? iceCandidate.toString().substring(0, 64)
                            : sdp.toString().substring(0, 64));

            /* 여기도 마찬가지 */
            ChatRoom room = sessionIdToRoomMap.get(session.getId());

            if (room != null) {
                Map<String, WebSocketSession> clients = chatService.getClients(room);

                /*
                 * Map.Entry 는 Map 인터페이스 내부에서 Key, Value 를 쌍으로 다루기 위해 정의된 내부 인터페이스
                 * 보통 key 값들을 가져오는 entrySet() 과 함께 사용한다.
                 * entrySet 을 통해서 key 값들을 불러온 후 Map.Entry 를 사용하면서 Key 에 해당하는 Value 를 쌍으로 가져온다
                 *
                 * 여기를 고치면 1:1 대신 1:N 으로 바꿀 수 있지 않을까..?
                 */
                for (Map.Entry<String, WebSocketSession> client : clients.entrySet()) {

                    // send messages to all clients except current user
                    if (!client.getKey().equals(sender)) {
                        // select the same type to resend signal
                        SignalData sd = SignalData.builder()
                                .data(data)
                                .iceCandidate(iceCandidate)
                                .sdp(sdp)
                                .sender(sender)
                                .signalType(signalData.getSignalType())
                                .build();
                        client.getValue().sendMessage(new TextMessage(objectMapper.writeValueAsString(sd)));
                    }
                }
            }

        } else if (signalData.getSignalType().equalsIgnoreCase(SignalType.Ice.toString())) {
            Object iceCandidate = signalData.getIceCandidate();
            Object sdp = signalData.getSdp();

            System.out.println("[ws] Ice Ice");

            logger.debug("[ws] Signal: {}",
                    iceCandidate != null
                            ? iceCandidate.toString().substring(0, 64)
                            : sdp.toString().substring(0, 64));

            /* 여기도 마찬가지 */
            ChatRoom room = sessionIdToRoomMap.get(session.getId());

            if (room != null) {
                Map<String, WebSocketSession> clients = chatService.getClients(room);

                for (Map.Entry<String, WebSocketSession> client : clients.entrySet()) {

                    System.out.println(client.getKey()+"한테 갈까?");
                    // send messages to all clients except current user
                    if (!client.getKey().equals(sender)) {
                        System.out.println("보냈다!");
                        // select the same type to resend signal
                        SignalData sd = SignalData.builder()
                                .data(data)
                                .iceCandidate(iceCandidate)
                                .sdp(sdp)
                                .sender(sender)
                                .signalType(signalData.getSignalType())
                                .build();
                        client.getValue().sendMessage(new TextMessage(objectMapper.writeValueAsString(sd)));
                    }
                }
            }
        }
    }
}
