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
