package com.example.signaling_server.dto.chat;

import lombok.*;

@Data
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SignalData {
    private String sender;
    private String data;
    private String signalType;
    private Object IceCandidate;
    private Object sdp;
}