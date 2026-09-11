import { onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import api from '../services/api'

// The backend serves the WS handshake at /ws, a sibling of /api, not under it -
// same origin as VITE_API_BASE_URL, just without the /api suffix.
const WS_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/api\/?$/, '') + '/ws'

// How long to wait for the WebSocket to actually connect before falling back
// to polling - generous enough for a slow network or a cold Render instance
// waking up, short enough that a genuinely broken connection doesn't leave
// the game looking frozen for long.
const CONNECT_TIMEOUT_MS = 4000
const FALLBACK_POLL_MS = 2000

// Comfortably under RoomService.DISCONNECT_THRESHOLD (20s server-side) - see
// the heartbeat comment in start() below for why this runs at all.
const HEARTBEAT_MS = 10000

// A participant going quiet isn't a broadcast-worthy event the way every
// other bit of room state is (nobody calls RoomBroadcastService just because
// time passed) - it's purely a side effect of RoomService#isConnected doing
// its lastSeenAt-vs-now math whenever someone else's toDto() happens to run.
// So a healthy WebSocket, which stops polling once connected, would never
// actually learn that another participant went stale - the .connected flag
// each player card/lobby row reads would just silently freeze at whatever it
// last was. This is the one thing still worth a slow poll for even with a
// perfectly healthy socket - much slower than FALLBACK_POLL_MS since it's
// not covering for a broken connection, just refreshing a value that decays
// on its own.
const PRESENCE_POLL_MS = 15000

/**
 * Guards against exactly the race that caused turn order to visibly flicker
 * for real players on real networks: with a healthy WebSocket AND a
 * presence poll AND a fallback poll all capable of firing independently (see
 * PRESENCE_POLL_MS above), nothing stopped an in-flight poll - kicked off
 * against an older snapshot - from resolving *after* a newer WebSocket push
 * (or after the player's own action response) had already landed, and
 * silently overwriting the fresher state with stale data. On localhost, with
 * near-zero latency, that race window is too narrow to hit; on a real
 * multi-device game over real networks it's wide enough to hit often.
 *
 * One of these per online game screen. `markApplied()` is called from
 * inside each screen's own `applyState`/`onMessage` function - the single
 * place every source of truth (WS push, poll response, or the direct
 * response to the player's own action) already funnels through - so every
 * apply, regardless of source, invalidates any older poll still in flight,
 * with no change needed at any of those individual call sites. `begin()` is
 * called right before a poll's fetch starts; the function it returns tells
 * that poll, once its response arrives, whether anything newer has already
 * been applied in the meantime - if so, the poll's own (now-stale) result
 * must be dropped instead of applied.
 */
export function createStaleGuard() {
  let seq = 0
  return {
    begin() {
      const startedAt = seq
      return () => seq === startedAt
    },
    markApplied() {
      seq += 1
    }
  }
}

/**
 * The actual push-with-polling-fallback machinery, as a plain start()/stop()
 * pair - no Vue lifecycle attached. Used directly by each View.vue's lobby
 * (started/stopped on room-create/join/start/leave, not on component
 * mount/unmount - see startLobbyChannel there), and wrapped by
 * useRoomChannel() below for the actual game screens, which really do just
 * want it tied to mount/unmount.
 *
 * `poll` is each caller's own existing poll()/lobby-refresh function -
 * unchanged, still does the REST GET and applies the result itself. It's
 * still used for: the very first fetch on start(), the fallback loop below,
 * and a one-off catch-up fetch right after every (re)connect, in case
 * something happened during a gap the socket wasn't open for.
 *
 * `onMessage` is called with the already-parsed broadcast payload - in
 * practice, each caller just passes its own apply function directly, since a
 * push and a poll response are the exact same DTO shape.
 *
 * Falls back to actually polling whenever the WebSocket isn't connected
 * (never connected yet, mid-reconnect, or erroring) - a flaky connection
 * degrades to the old behavior instead of leaving a player's screen stuck.
 */
export function createRoomChannel(topicPath, { poll, onMessage }) {
  // Every topic this app has is /topic/rooms/{code}/(state|lobby) - pulling
  // the code back out here, rather than threading a separate parameter
  // through all 14 call sites, since it's already right there.
  const roomCode = topicPath.match(/\/rooms\/([^/]+)\//)?.[1]

  let stompClient = null
  let subscription = null
  let pollTimer = null
  let connectTimeoutTimer = null
  let heartbeatTimer = null
  let presencePollTimer = null

  function startFallbackPolling() {
    if (pollTimer) return
    pollTimer = setInterval(poll, FALLBACK_POLL_MS)
  }

  function stopFallbackPolling() {
    clearInterval(pollTimer)
    pollTimer = null
  }

  function start() {
    poll()
    // GameRoomParticipant.lastSeenAt (RoomService#isConnected) used to get
    // refreshed as a side effect of every poll - now that a healthy
    // connection barely polls at all, nothing else keeps it fresh, so this
    // runs unconditionally alongside the socket/fallback-poll rather than
    // only while one specific transport is active (simpler, and harmless
    // when the fallback poll's own GET already happens to do the same thing).
    if (roomCode) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = setInterval(() => api.sendRoomHeartbeat(roomCode), HEARTBEAT_MS)
    }
    clearInterval(presencePollTimer)
    presencePollTimer = setInterval(poll, PRESENCE_POLL_MS)
    stompClient = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        clearTimeout(connectTimeoutTimer)
        stopFallbackPolling()
        subscription = stompClient.subscribe(topicPath, message => {
          onMessage(JSON.parse(message.body))
        })
        poll() // catch up on anything missed while not connected
      },
      // stompjs keeps retrying on its own (reconnectDelay) after any of these -
      // the fallback poll just covers the gap until the next successful onConnect.
      onWebSocketError: startFallbackPolling,
      onDisconnect: startFallbackPolling,
      onStompError: startFallbackPolling
    })
    connectTimeoutTimer = setTimeout(startFallbackPolling, CONNECT_TIMEOUT_MS)
    stompClient.activate()
  }

  function stop() {
    clearTimeout(connectTimeoutTimer)
    clearInterval(heartbeatTimer)
    clearInterval(presencePollTimer)
    stopFallbackPolling()
    if (subscription) subscription.unsubscribe()
    if (stompClient) stompClient.deactivate()
    stompClient = null
    subscription = null
  }

  return { start, stop }
}

/** Drop-in replacement for usePolling(poll, intervalMs) on any online game screen. */
export function useRoomChannel(topicPath, { poll, onMessage }) {
  const channel = createRoomChannel(topicPath, { poll, onMessage })
  onMounted(channel.start)
  onUnmounted(channel.stop)
  return { stop: channel.stop }
}
