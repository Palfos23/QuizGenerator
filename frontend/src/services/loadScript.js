// Loads an external <script> on demand rather than statically in index.html -
// used so a third-party script (e.g. Google Identity Services) only ever
// reaches the browser once the visitor has actually opted into it. Safe to
// call more than once for the same src - resolves immediately if it's
// already present instead of injecting a duplicate tag.
export function loadScript(src) {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`Failed to load script: ${src}`))
    document.head.appendChild(script)
  })
}
