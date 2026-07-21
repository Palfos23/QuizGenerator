export const LANGUAGES = [
  { code: 'EN', label: 'English', flag: '🇬🇧' },
  { code: 'DE', label: 'German', flag: '🇩🇪' },
  { code: 'FR', label: 'French', flag: '🇫🇷' },
  { code: 'ES', label: 'Spanish', flag: '🇪🇸' },
  { code: 'NO', label: 'Norwegian', flag: '🇳🇴' }
]

export function languageLabel(code) {
  return LANGUAGES.find(l => l.code === code)?.label || code
}

// Maps a 1-10 difficulty to a hue on a green(easy) -> red(hard) scale, used for badges/sliders.
export function difficultyColor(level) {
  const hue = Math.round(130 - ((level - 1) / 9) * 130) // 130 = green, 0 = red
  return `hsl(${hue}, 70%, 45%)`
}
