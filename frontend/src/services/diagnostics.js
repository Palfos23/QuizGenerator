import api from './api'

// Tells the server about a problem only this browser can see - e.g. an answer box whose
// suggestion list failed to load or came back empty. Nothing reaches the server in those
// cases on its own, which made them impossible to diagnose from the logs. Lands in the
// backend log as a CLIENT-EVENT line (see ClientDiagnosticsController).
//
// Fire-and-forget and never throws: reporting a problem must not cause one. Each distinct
// area+kind+key is sent once per page load, so a retry loop or a player mashing a key can't
// spam it.
const alreadySent = new Set()

export function reportClientEvent({ area, kind, key, httpStatus, detail }) {
  const id = `${area}|${kind}|${key || ''}`
  if (alreadySent.has(id)) return
  alreadySent.add(id)
  try {
    api.reportClientEvent({
      area,
      kind,
      key: key || null,
      httpStatus: httpStatus ?? null,
      detail: detail ? String(detail).slice(0, 280) : null
    }).catch(() => {})
  } catch (e) {
    // see above - never let reporting itself throw
  }
}
