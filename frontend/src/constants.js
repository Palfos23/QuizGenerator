export const LANGUAGES = [
  { code: 'EN', label: 'English', flag: '🇬🇧' },
  { code: 'DE', label: 'German', flag: '🇩🇪' },
  { code: 'FR', label: 'French', flag: '🇫🇷' },
  { code: 'ES', label: 'Spanish', flag: '🇪🇸' },
  { code: 'NO', label: 'Norwegian', flag: '🇳🇴' }
]

export const SPORTS = [
  { code: 'FOOTBALL', label: 'Football' },
  { code: 'CYCLING', label: 'Cycling' }
]

export function languageLabel(code) {
  return LANGUAGES.find(l => l.code === code)?.label || code
}

export function sportLabel(code) {
  return SPORTS.find(s => s.code === code)?.label || code
}

// Maps a 1-10 difficulty to a hue on a green(easy) -> red(hard) scale, used for badges/sliders.
export function difficultyColor(level) {
  const hue = Math.round(130 - ((level - 1) / 9) * 130) // 130 = green, 0 = red
  return `hsl(${hue}, 70%, 45%)`
}

// Club hint-badge colors are admin-chosen and can be light or dark, so the badge
// text color has to adapt rather than always being the same dark shade - this picks
// black or white based on the background's relative luminance (WCAG-style).
export function readableTextColor(hex) {
  if (!hex) return '#241c00' // matches the default gold badge's existing dark text
  const clean = hex.replace('#', '')
  const full = clean.length === 3 ? clean.split('').map(c => c + c).join('') : clean
  const r = parseInt(full.substring(0, 2), 16) / 255
  const g = parseInt(full.substring(2, 4), 16) / 255
  const b = parseInt(full.substring(4, 6), 16) / 255
  const lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
  return lum > 0.55 ? '#1b1d24' : '#ffffff'
}
