// The fixed set of formation shapes an admin can assign a Starting XI board
// to. Mirrors backend Formations.java exactly - keep the two in sync if this
// list ever changes. Each shape is the row sizes from goalkeeper up to
// attack (always summing to 11); slot index 0 is the keeper, then rows fill
// left-to-right in ascending order.
export const FORMATIONS = {
  '4-4-2': [1, 4, 4, 2],
  '4-3-3': [1, 4, 3, 3],
  '4-2-3-1': [1, 4, 2, 3, 1],
  '4-1-4-1': [1, 4, 1, 4, 1],
  '3-5-2': [1, 3, 5, 2],
  '3-4-3': [1, 3, 4, 3],
  '5-3-2': [1, 5, 3, 2],
  '4-5-1': [1, 4, 5, 1]
}

export const FORMATION_NAMES = Object.keys(FORMATIONS)

export function slotCount(formation) {
  const rows = FORMATIONS[formation]
  return rows ? rows.reduce((a, b) => a + b, 0) : 0
}

// Splits a flat list of 11 slots (each needing a `slotIndex` field, 0-based in
// the same goalkeeper-first order as FORMATIONS) into rows for rendering,
// each row already in slotIndex order. Slots missing a row's worth of admin
// input still render, just left visually incomplete.
export function rowsFor(formation, slots) {
  const shape = FORMATIONS[formation]
  if (!shape) return [slots]
  const byIndex = new Map(slots.map(s => [s.slotIndex, s]))
  const rows = []
  let cursor = 0
  for (const size of shape) {
    const row = []
    for (let i = 0; i < size; i++) {
      row.push(byIndex.get(cursor) || { slotIndex: cursor })
      cursor++
    }
    rows.push(row)
  }
  return rows
}

// Regroups rowsFor()'s output for rendering, so the pitch reads like a real
// "guess the lineup" graphic instead of evenly-spaced strips:
//  - a row of 4 (a genuine flat back/mid four) spans the full pitch width
//    edge to edge - kind "full"
//  - a row of 1-3 (goalkeeper, a back/central-mid three, a front two or
//    three) stays narrow and centered rather than stretching across the
//    whole pitch - kind "central"
//  - a row of 5 (a back five as in 5-3-2, or a 5-wide midfield as in 3-5-2
//    / 4-5-1) is split into the two wide players (full-backs / wing-backs)
//    pinned out toward the touchlines - kind "wide" - and the three
//    central players who keep the tight "central" treatment.
//
// Where the wide pair sits relative to the central three depends on which
// row is being split, because a "wing-back" always physically belongs just
// ahead of the defensive line regardless of which formation row the data
// files them under:
//  - if the row-of-5 IS the defense row (index 1, e.g. 5-3-2) the wide
//    pair renders one tier further forward (attack-ward) than the three
//    center-backs, who stay deepest next to the keeper.
//  - if the row-of-5 comes later (e.g. 3-5-2's or 4-5-1's midfield row,
//    which already has its own defense row of 3/4 in front of it) the wide
//    pair renders one tier further back (defense-ward) than the three
//    central midfielders, landing right next to that defense row instead.
// Either way the wing-backs end up in the same physical spot on the pitch;
// this is purely a display-time regrouping - the underlying slot data and
// slotIndex-based admin assignment are untouched.
export function displayRowsFor(formation, slots) {
  const rows = rowsFor(formation, slots)
  const display = []
  rows.forEach((row, rowIdx) => {
    if (row.length === 5) {
      const central = { kind: 'central', items: [row[1], row[2], row[3]] }
      const wide = { kind: 'wide', items: [row[0], row[4]] }
      if (rowIdx === 1) {
        display.push(central, wide)
      } else {
        display.push(wide, central)
      }
    } else if (row.length === 4) {
      display.push({ kind: 'full', items: row })
    } else {
      display.push({ kind: 'central', items: row })
    }
  })
  return display
}

// One friendly label per slot index (e.g. "GK", "DF 1", "DF 2", ..., "FW"),
// in the same goalkeeper-first order as slot indexes - used by the admin
// slot-assignment dropdown.
export function slotLabels(formation) {
  const shape = FORMATIONS[formation]
  if (!shape) return []
  const rowNames = shape.map((_, i) => {
    if (i === 0) return 'GK'
    if (i === shape.length - 1) return 'FW'
    return i === 1 ? 'DF' : 'MF'
  })
  const labels = []
  shape.forEach((size, rowIdx) => {
    for (let i = 0; i < size; i++) {
      labels.push(size > 1 ? `${rowNames[rowIdx]} ${i + 1}` : rowNames[rowIdx])
    }
  })
  return labels
}
