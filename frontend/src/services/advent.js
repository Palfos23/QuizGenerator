import { t as _t } from '../i18n'

// What the player sees about each kind of door. The server only sends codes (NORMAL, WILDCARD ...).

export const TYPES = {
  NORMAL: {
    emoji: '🎁',
    name: _t('Vanlig luke'),
    rules: _t('Velg 4 svar. For hvert av svarene dine som matcher et av spillmesterens, får du 1 poeng (0-4 poeng).')
  },
  DOUBLE: {
    emoji: '✨',
    name: _t('X2-luke'),
    rules: _t('Alle poeng dobles. Treffer du 3 av spillmesterens svar, får du 6 poeng.')
  },
  RISK_REWARD: {
    emoji: '🎲',
    name: _t('Risk or Reward-luke'),
    rules: _t('Velg om du vil spille trygt eller satse før du sender inn. Trygt gir vanlige poeng. Satser du, får du 2 poeng per treff - men -1 poeng hvis du ikke treffer noen.')
  },
  WILDCARD: {
    emoji: '🌟',
    name: _t('Wildcard-luke'),
    rules: _t('Spillmesteren har valgt ett av sine fire svar som wildcard. Treffer du det, får du 3 ekstra poeng.')
  },
  OPPOSITE: {
    emoji: '🙃',
    name: _t('Opp-ned-luke'),
    rules: _t('Her gjelder det å IKKE tenke som spillmesteren! Alle 4 svarene dine må passe kategorien, og ingen av dem må være blant spillmesterens - da får du 5 poeng. Treffer du minst ett, eller skriver noe som ikke passer, får du 0.')
  },
  ONE_OF_FOUR: {
    emoji: '🥇',
    name: _t('Én av fire-luke'),
    rules: _t('Spillmesteren har bare valgt ETT svar. Treffer du det med ett av dine 4 svar, får du 5 poeng.')
  }
}

export function typeInfo(type) {
  return TYPES[type] || TYPES.NORMAL
}

/** "3 poeng" - a real minus sign for negatives, singular for exactly one. */
export function pointsText(n) {
  const value = n < 0 ? '−' + Math.abs(n) : String(n)
  return `${value} ${_t('poeng¦poeng', [Math.abs(n)])}`
}

/**
 * The text a player can paste to friends after a door - a row of squares, one per answer, so it gives nothing away
 * about what the answers were: 🟩 hit, ⬛ miss (a gold medal for the one answer on an "én av fire" door).
 */
export function shareText(detail, year) {
  const r = detail.result
  const gold = detail.type === 'ONE_OF_FOUR'
  const squares = detail.entry
    ? detail.entry.answers.map((_, i) => ((r?.answerMatches?.[i] ?? -1) >= 0 ? (gold ? '🥇' : '🟩') : '⬛')).join('')
    : ''
  const lines = [
    `🎄 ${_t('Julekalender')} ${year} - ${_t('luke')} ${detail.day} ${typeInfo(detail.type).emoji}`,
    squares,
    `${r ? pointsText(r.points) : ''}${detail.entry?.jokerUsed ? ' 🃏' : ''}${detail.entry?.risky ? ' 🎲' : ''}`.trim(),
    'quizzes.no/julekalender'
  ]
  return lines.filter(Boolean).join('\n')
}

/** Opens the phone's share sheet, or copies the text. Returns 'shared' | 'copied' | 'failed'. */
export async function shareOrCopy(text) {
  try {
    if (navigator.share) {
      await navigator.share({ text })
      return 'shared'
    }
  } catch (e) {
    if (e && e.name === 'AbortError') return 'failed' // the player closed the share sheet
  }
  try {
    await navigator.clipboard.writeText(text)
    return 'copied'
  } catch (e) {
    return 'failed'
  }
}
