import { ref } from 'vue'
import { revealFrom } from './viewTransition'

// The colour palette. The choice lives in localStorage and is applied as [data-palette] on <html>; a tiny inline
// script in index.html applies it before first paint so a returning visitor never sees a flash of the default.
// The palettes themselves are token overrides in styles/palettes.css ("land" is the default and has no attribute).
const STORAGE_KEY = 'quiz_palette'

// swatch = [page, primary, accent], drawn on the switcher so each dot previews its own palette
export const PALETTES = [
  { id: 'land', name: 'Land', swatch: ['#f1efe8', '#2f6b63', '#e2693c'], themeColor: '#f1efe8' },
  { id: 'mono', name: 'Mono', swatch: ['#fafafa', '#151515', '#5b4bff'], themeColor: '#fafafa' },
  { id: 'ember', name: 'Ember', swatch: ['#111114', '#f0624f', '#f5b942'], themeColor: '#111114' },
]

function readStored() {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    return PALETTES.some(p => p.id === v) ? v : 'land'
  } catch (e) { return 'land' }
}

export const palette = ref(readStored())

function apply(id) {
  const root = document.documentElement
  if (id === 'land') delete root.dataset.palette
  else root.dataset.palette = id
  // keeps the browser's own chrome (mobile address bar) in step with the page
  const meta = document.querySelector('meta[name="theme-color"]')
  const found = PALETTES.find(p => p.id === id)
  if (meta && found) meta.setAttribute('content', found.themeColor)
}

// origin = {x, y} of the control that was pressed: the new palette is revealed as a circle growing out of it
export function setPalette(id, origin) {
  palette.value = PALETTES.some(p => p.id === id) ? id : 'land'
  try { localStorage.setItem(STORAGE_KEY, palette.value) } catch (e) { /* private mode - works for this visit only */ }
  if (origin) revealFrom(origin.x, origin.y, () => apply(palette.value))
  else apply(palette.value)
}

export function initPalette() {
  apply(palette.value)
}
