import { t as _t } from '../i18n'

// What the player sees about each kind of door, and the badges. The server only sends codes (NORMAL, STREAK_7 ...).

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
    rules: _t('Her gjelder det å IKKE tenke som spillmesteren! Treffer du ingen av svarene hans, får du 5 poeng. Treffer du minst ett, får du 0.')
  },
  ONE_OF_FOUR: {
    emoji: '🥇',
    name: _t('Én av fire-luke'),
    rules: _t('Spillmesteren har bare valgt ETT svar. Treffer du det med ett av dine 4 svar, får du 5 poeng.')
  }
}

export const BADGES = {
  FIRST_DOOR: { emoji: '🎁', name: _t('Første luke'), text: _t('Åpnet din første luke') },
  STREAK_7: { emoji: '🔥', name: _t('7 på rad'), text: _t('Svarte 7 dager på rad') },
  PERFECT_DAY: { emoji: '🧠', name: _t('Tenker likt'), text: _t('Traff alle 4 av spillmesterens svar på én luke') },
  WILDCARD_HIT: { emoji: '🌟', name: _t('Wildcard-jeger'), text: _t('Traff wildcard-svaret') },
  RISK_PAYOFF: { emoji: '🎲', name: _t('Modig'), text: _t('Satset og traff på en Risk or Reward-luke') },
  CONTRARIAN: { emoji: '🙃', name: _t('Motstrømmer'), text: _t('Fikk 5 poeng på en Opp-ned-luke') },
  GOLD_HIT: { emoji: '🥇', name: _t('Gullsvaret'), text: _t('Traff det ene svaret på en Én av fire-luke') },
  JOKER_PAYOFF: { emoji: '🃏', name: _t('Joker-trekk'), text: _t('Jokeren ga deg 6 poeng eller mer på én luke') },
  FINALE: { emoji: '🎄', name: _t('Finalist'), text: _t('Var med på finalen 24. desember') },
  ALL_24: { emoji: '🏆', name: _t('Alle 24'), text: _t('Åpnet alle 24 luker') }
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
