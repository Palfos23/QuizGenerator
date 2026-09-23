// Warms the browser's image cache for a URL before it's shown, so the <img>
// element that later gets it as its src resolves instantly instead of
// visibly loading in front of the player mid-round. Never rejects - a
// missing/broken/slow URL should never block gameplay, it should just
// resolve one way or another (loaded, errored, or timed out) and let the
// caller move on.
export function preloadImage(url, timeoutMs = 4000) {
  return new Promise((resolve) => {
    if (!url) {
      resolve()
      return
    }
    const img = new Image()
    let settled = false
    const finish = () => {
      if (settled) return
      settled = true
      resolve()
    }
    img.onload = finish
    img.onerror = finish
    img.src = url
    setTimeout(finish, timeoutMs)
  })
}

export function preloadImages(urls, timeoutMs) {
  return Promise.all([...new Set((urls || []).filter(Boolean))].map(u => preloadImage(u, timeoutMs)))
}
