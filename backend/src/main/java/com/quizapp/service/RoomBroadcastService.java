package com.quizapp.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * The only thing that ever sends on the room-state WebSocket topics (see
 * WebSocketConfig) - every online-room controller calls this right after a
 * mutation, broadcasting the exact same DTO it's already returning to the
 * caller over REST, so every other device in the room finds out immediately
 * instead of waiting for its next poll.
 *
 * Deliberately NOT personalized: a state DTO normally carries fields specific
 * to whoever asked for it (yourParticipantId, isHost) - broadcasting one
 * caller's version of those to everyone would be wrong for everyone else.
 * Every frontend Online*Game.vue component already sources "which
 * participant am I" from its own yourParticipantId PROP (passed down once at
 * join/create time), not from the polled state DTO, so this doesn't need to
 * do anything special there. The lobby is the one exception - see
 * activeRoom.js/each View.vue's onMessage handler, which merges an incoming
 * broadcast's shared fields onto its own already-known host/yourParticipantId
 * rather than trusting the broadcast for those two.
 */
@Service
public class RoomBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public RoomBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** The lobby's RoomDto - participant list/status, sent on every join/leave/start. */
    public void broadcastLobby(String roomCode, Object roomDto) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/lobby", roomDto);
    }

    /** Whatever the game's own *OnlineStateDto shape is - sent after every action that changes it. */
    public void broadcastState(String roomCode, Object stateDto) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/state", stateDto);
    }
}
