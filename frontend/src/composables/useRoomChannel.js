import { onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

// The backend serves the WS handshake at /ws, a sibling of /api, not under it -
// same origin as VITE_API_BASE_URL, just without the /api suffix.
const WS_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/api\/?$/, '') + '/ws'

// How long to wait for the WebSocket to actually connect before falling back
// to polling - generous enough for a slow network or a cold Render instance
// waking up, short enough that a genuinely broken connection doesn't leave
// the game looking frozen for long.
const CONNECT_TIMEOUT_MS = 4000
const FALLBACK_POLL_MS = 2000

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
  let stompClient = null
  let subscription = null
  let pollTimer = null
  let connectTimeoutTimer = null

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
