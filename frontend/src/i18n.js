// Tiny i18n layer. Source strings in the app are written in Norwegian and used as the lookup key;
// the English catalog (i18n/en.js, generated) maps each key to its English text. A key that needs
// disambiguating carries a "§context" suffix, which is never shown. "{0}" placeholders take args.
// Switching language stores the choice and reloads the page, so nothing needs to be reactive.
const STORAGE_KEY = 'quiz_lang'

function readStored() {
  try { return localStorage.getItem(STORAGE_KEY) } catch (e) { return null }
}

export const lang = readStored() === 'en' ? 'en' : 'no'
export const locale = lang === 'en' ? 'en-GB' : 'nb-NO'

let dict = {}

// Called once before the app mounts: pulls in the English catalog only when it's needed.
export async function loadLang() {
  if (lang === 'en') dict = (await import('./i18n/en.js')).default
  document.documentElement.lang = lang === 'en' ? 'en' : 'nb'
  window.QUIZ_LOCALE = locale
}

export function t(key, args) {
  let s = (lang === 'en' && dict[key]) || key
  if (!(lang === 'en' && dict[key])) {
    const at = s.indexOf('§')
    if (at !== -1) s = s.slice(0, at)
  }
  // "singular¦plural" - picked by the first argument being exactly 1
  if (s.includes('¦')) {
    const [one, many] = s.split('¦')
    s = args && Number(args[0]) === 1 ? one : many
  }
  if (args) s = s.replace(/\{(\d+)\}/g, (_, i) => (args[Number(i)] ?? ''))
  return s
}

export function setLang(next) {
  try { localStorage.setItem(STORAGE_KEY, next) } catch (e) { /* private mode - the reload below still picks the default */ }
  window.location.reload()
}
