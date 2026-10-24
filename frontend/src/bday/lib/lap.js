// Mario Kart lap times: stored as milliseconds on the server, written by people as m:ss.mmm.

/** 83456 -> "1:23.456" */
export function formatLap(ms) {
  if (ms === null || ms === undefined) return ''
  const minutes = Math.floor(ms / 60000)
  const seconds = Math.floor((ms % 60000) / 1000)
  const millis = ms % 1000
  return `${minutes}:${String(seconds).padStart(2, '0')}.${String(millis).padStart(3, '0')}`
}

/** Splits "1:23.456" into the three boxes of LapTimeInput. */
export function splitLap(text) {
  const m = /^(?:(\d{1,2}):)?(\d{1,3})(?:[.,](\d{1,3}))?$/.exec((text || '').trim())
  return m ? { minutes: m[1] || '', seconds: m[2] || '', millis: m[3] || '' } : { minutes: '', seconds: '', millis: '' }
}
