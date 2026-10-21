import { ref } from 'vue'

// Light ("paper") or dark. The choice lives in localStorage and is applied as [data-theme] on <html>; a tiny
// inline script in index.html applies it before first paint so a dark-mode visitor never sees a light flash.
// Light is the default - the toggle (components/ThemeToggle.vue) is the only thing that switches it.
const STORAGE_KEY = 'quiz_theme'

function readStored() {
  try { return localStorage.getItem(STORAGE_KEY) === 'dark' ? 'dark' : 'light' } catch (e) { return 'light' }
}

export const theme = ref(readStored())

function apply(value) {
  document.documentElement.dataset.theme = value
  // keeps the browser's own chrome (mobile address bar) in step with the page
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.setAttribute('content', value === 'dark' ? '#171510' : '#f3ede1')
}

export function setTheme(value) {
  theme.value = value === 'dark' ? 'dark' : 'light'
  try { localStorage.setItem(STORAGE_KEY, theme.value) } catch (e) { /* private mode - works for this visit only */ }
  apply(theme.value)
}

export function toggleTheme() {
  setTheme(theme.value === 'dark' ? 'light' : 'dark')
}

export function initTheme() {
  apply(theme.value)
}
