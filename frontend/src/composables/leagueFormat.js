import { t as _t } from '../i18n'

// Shared wording for the league pages: player names, formats, dates, matchup sides.

/** A player's name - the Expert and deleted accounts have none of their own. */
export function playerName(p) {
  if (!p) return ''
  if (p.expert) return _t('Eksperten')
  return p.name || _t('Slettet spiller')
}

export function formatName(format) {
  return format === 'HEAD_TO_HEAD' ? _t('Head-to-head') : _t('Poengliga')
}

export function stateName(state) {
  if (state === 'UPCOMING') return _t('Starter snart')
  if (state === 'FINISHED') return _t('Ferdig§League')
  return _t('Pågår§League')
}

function asDate(iso) {
  return new Date(`${iso}T12:00:00`)
}

export function shortDate(iso) {
  return iso ? asDate(iso).toLocaleDateString(window.QUIZ_LOCALE, { day: 'numeric', month: 'short' }) : ''
}

export function dayName(iso) {
  return iso ? asDate(iso).toLocaleDateString(window.QUIZ_LOCALE, { weekday: 'long', day: 'numeric', month: 'long' }) : ''
}

/** "3. des - 30. des", or "fra 3. des" for a season without an end. */
export function seasonSpan(season) {
  if (!season) return ''
  if (!season.endDate) return _t('fra {0}, uten slutt', [shortDate(season.startDate)])
  return `${shortDate(season.startDate)} – ${shortDate(season.endDate)}`
}

/** Whose side of a playoff matchup this will be, before it is known. */
export function placeholderName(ph) {
  if (!ph) return ''
  const seed = /^SEED_(\d)$/.exec(ph)
  if (seed) return _t('{0}. plass', [seed[1]])
  const map = {
    WINNER_SEMI_1: _t('Vinner semifinale 1'),
    WINNER_SEMI_2: _t('Vinner semifinale 2'),
    LOSER_SEMI_1: _t('Taper semifinale 1'),
    LOSER_SEMI_2: _t('Taper semifinale 2')
  }
  return map[ph] || ''
}

export function sideName(match, side) {
  const p = side === 'a' ? match.a : match.b
  return p ? playerName(p) : placeholderName(side === 'a' ? match.placeholderA : match.placeholderB)
}

/** What to show for one side's score: the score, "…" in review, "–" not handed in, nothing yet. */
export function sideScore(match, side) {
  const state = side === 'a' ? match.stateA : match.stateB
  const score = side === 'a' ? match.scoreA : match.scoreB
  if (state === 'SCORED') return String(score)
  if (state === 'PENDING') return '…'
  if (state === 'MISSED') return '–'
  return ''
}

/** A decimal as the page's language writes it (9,9 in Norwegian). */
export function num(v) {
  return v == null ? '–' : Number(v).toLocaleString(window.QUIZ_LOCALE)
}

export function stageName(stage) {
  if (stage === 'SEMI') return _t('Semifinaler')
  if (stage === 'FINAL') return _t('Finale og bronsefinale')
  return ''
}

export function matchStatusName(match) {
  if (match.status === 'LIVE') return _t('Pågår i dag')
  if (match.status === 'PENDING') return _t('Venter på gjennomgang')
  if (match.status === 'UPCOMING') return ''
  if (match.outcome === 'DRAW') return _t('Uavgjort')
  if (match.outcome === 'BOTH_LOST') return _t('Ingen leverte')
  return ''
}

/** Did this side win (or go through, in the playoffs)? */
export function sideWon(match, side) {
  const key = side === 'a' ? 'A' : 'B'
  return match.advancing ? match.advancing === key : match.outcome === key
}
