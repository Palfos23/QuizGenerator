import { ref } from 'vue'
import { revealFrom } from './viewTransition'

// The colour palette. The choice lives in localStorage and is applied as [data-palette] on <html>; a tiny inline
// script in index.html applies it before first paint so a returning visitor never sees a flash of the default.
// The palettes themselves are token overrides in styles/palettes.css ("umber" is the default; "cobalt" is the base palette in :root).
const STORAGE_KEY = 'quiz_palette'
const DEFAULT_ID = 'umber'
const BASE_ID = 'cobalt' // the light palette that lives in :root, so it is the one with no data-palette attribute

// swatch = [page, primary, accent, secondary], drawn on the switcher so each dot previews its own palette
export const PALETTES = [
  { id: 'umber', name: 'Brass & Umber', swatch: ['#14100d', '#d9a066', '#8fb39a', '#e0623e'], themeColor: '#14100d' },
  { id: BASE_ID, name: 'Cream & Cobalt', swatch: ['#eeefe9', '#1d4aff', '#f54e00', '#f9bd2b'], themeColor: '#eeefe9' },
  { id: 'blurple', name: 'Blurple', swatch: ['#14152b', '#5865f2', '#eb459e', '#fee75c'], themeColor: '#14152b' },
  { id: 'rausch', name: 'Rausch & Babu', swatch: ['#f6f4f1', '#e31c5f', '#fc642d', '#00a699'], themeColor: '#f6f4f1' },
  { id: 'mint', name: 'Mint Console', swatch: ['#121212', '#3ecf8e', '#ffc857', '#4cc2ff'], themeColor: '#121212' },
  { id: 'ledger', name: 'Neon Ledger', swatch: ['#f4f4f1', '#0a0a0a', '#00d64f', '#6c47ff'], themeColor: '#f4f4f1' },
  { id: 'monocle', name: 'Monocle', swatch: ['#f0e9d8', '#0f3d3a', '#c8a24a', '#b3202a'], themeColor: '#f0e9d8' },
  { id: 'dusk', name: 'Dusk', swatch: ['#0d1228', '#8fb8ff', '#ffb38a', '#c9a7ff'], themeColor: '#0d1228' },
  { id: 'spectrum', name: 'Spectrum', swatch: ['#f5f5f7', '#7b3fe4', '#f24e1e', '#0d99ff'], themeColor: '#f5f5f7' },
]

function readStored() {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    return PALETTES.some(p => p.id === v) ? v : DEFAULT_ID
  } catch (e) { return DEFAULT_ID }
}

export const palette = ref(readStored())

function apply(id) {
  const root = document.documentElement
  if (id === BASE_ID) delete root.dataset.palette
  else root.dataset.palette = id
  // keeps the browser's own chrome (mobile address bar) in step with the page
  const meta = document.querySelector('meta[name="theme-color"]')
  const found = PALETTES.find(p => p.id === id)
  if (meta && found) meta.setAttribute('content', found.themeColor)
}

// origin = {x, y} of the control that was pressed: the new palette is revealed as a circle growing out of it
export function setPalette(id, origin) {
  palette.value = PALETTES.some(p => p.id === id) ? id : DEFAULT_ID
  try { localStorage.setItem(STORAGE_KEY, palette.value) } catch (e) { /* private mode - works for this visit only */ }
  if (origin) revealFrom(origin.x, origin.y, () => apply(palette.value))
  else apply(palette.value)
}

export function initPalette() {
  apply(palette.value)
}
