package com.quizapp.service;

import com.quizapp.dto.ClientEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes browser-reported problems into the server log, where they sit next to the
 * server's own lines for the same moment - the only way to see "a player's suggestion
 * list never loaded" at all, since nothing reaches the server when a fetch fails or a
 * list comes back empty. Log-only: nothing is stored.
 *
 * Open to any signed-in player (guests included), so it's capped per person and every
 * field is flattened to one short printable line - a caller can't flood the log or forge
 * extra log lines with embedded newlines.
 */
@Service
public class ClientEventLogService {

    private static final Logger log = LoggerFactory.getLogger(ClientEventLogService.class);

    static final int MAX_EVENTS_PER_WINDOW = 30;
    static final long WINDOW_MS = 10 * 60 * 1000;
    private static final int MAX_FIELD_LENGTH = 200;

    private final Map<String, Deque<Long>> recentByCaller = new ConcurrentHashMap<>();

    /** @return false if this caller is over the cap and the event was dropped */
    public boolean record(String callerId, String callerRole, ClientEventRequest event, String userAgent) {
        if (!allow(callerId)) {
            return false;
        }
        log.warn("CLIENT-EVENT area={} kind={} key='{}' status={} detail='{}' caller={} role={} ua='{}'",
                clean(event.getArea()), clean(event.getKind()), clean(event.getKey()),
                event.getHttpStatus(), clean(event.getDetail()), clean(callerId), clean(callerRole), clean(userAgent));
        return true;
    }

    private boolean allow(String callerId) {
        long now = System.currentTimeMillis();
        Deque<Long> times = recentByCaller.computeIfAbsent(callerId == null ? "?" : callerId, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > WINDOW_MS) {
                times.pollFirst();
            }
            if (times.size() >= MAX_EVENTS_PER_WINDOW) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

    static String clean(String value) {
        if (value == null) return "";
        String flat = value.replaceAll("\\p{Cntrl}", " ").trim();
        return flat.length() > MAX_FIELD_LENGTH ? flat.substring(0, MAX_FIELD_LENGTH) + "…" : flat;
    }
}
