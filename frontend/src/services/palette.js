import { ref } from 'vue'
import { revealFrom } from './viewTransition'

// The colour palette. The choice lives in localStorage and is applied as [data-palette] on <html>; a tiny inline
// script in index.html applies it before first paint so a returning visitor never sees a flash of the default.
// The palettes themselves are token overrides in styles/palettes.css ("plum" is the default and has no attribute).
const STORAGE_KEY = 'quiz_palette'

// swatch = [page, primary, accent], drawn on the switcher so each dot previews its own palette
export const PALETTES = [
  { id: 'plum', name: 'Plum', swatch: ['#f6f2f9', '#5b2a86', '#e8590c'], themeColor: '#f6f2f9' },
  { id: 'moss', name: 'Moss', swatch: ['#0e1612', '#a3e635', '#fb7185'], themeColor: '#0e1612' },
  { id: 'terra', name: 'Terracotta', swatch: ['#faf3ea', '#b83d0b', '#0b7a95'], themeColor: '#faf3ea' },
]

function readStored() {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    return PALETTES.some(p => p.id === v) ? v : 'plum'
  } catch (e) { return 'plum' }
}

export const palette = ref(readStored())

function apply(id) {
  const root = document.documentElement
  if (id === 'plum') delete root.dataset.palette
  else root.dataset.palette = id
  // keeps the browser's own chrome (mobile address bar) in step with the page
  const meta = document.querySelector('meta[name="theme-color"]')
  const found = PALETTES.find(p => p.id === id)
  if (meta && found) meta.setAttribute('content', found.themeColor)
}

// origin = {x, y} of the control that was pressed: the new palette is revealed as a circle growing out of it
export function setPalette(id, origin) {
  palette.value = PALETTES.some(p => p.id === id) ? id : 'plum'
  try { localStorage.setItem(STORAGE_KEY, palette.value) } catch (e) { /* private mode - works for this visit only */ }
  if (origin) revealFrom(origin.x, origin.y, () => apply(palette.value))
  else apply(palette.value)
}

export function initPalette() {
  apply(palette.value)
}
