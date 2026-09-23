// Minimal CSV helpers - quoting only when a field actually needs it (contains
// a comma, quote, or newline). Not RFC 4180-complete, but plenty for the
// simple "name,value" import/export shape shared by the Bullseye and 501
// admin editors.

// Deliberately minimal - just "name,value" rows, no quoted/embedded commas,
// which is all this import needs. Skips a header row if the first row's
// second column isn't a number, and skips blank lines.
export function parseNameValueCsv(text) {
  const lines = text.split(/\r?\n/).map(l => l.trim()).filter(Boolean)
  let start = 0
  if (lines.length) {
    const firstCols = lines[0].split(',')
    if (firstCols.length >= 2 && Number.isNaN(Number(firstCols[1].trim()))) {
      start = 1 // first row looks like a header - skip it
    }
  }
  const rows = []
  for (let i = start; i < lines.length; i++) {
    const cols = lines[i].split(',')
    if (cols.length < 2) continue
    const name = cols[0].trim().replace(/^"|"$/g, '')
    const value = Number(cols[1].trim())
    if (!name || Number.isNaN(value)) continue
    rows.push({ name, value })
  }
  return rows
}

function escapeCsvField(value) {
  const str = String(value ?? '')
  if (/[",\n]/.test(str)) {
    return `"${str.replace(/"/g, '""')}"`
  }
  return str
}

export function toCsv(rows) {
  return rows.map(row => row.map(escapeCsvField).join(',')).join('\r\n')
}

// Triggers a browser download of `rows` (an array of arrays) as a CSV file -
// no server round trip, since the data driving these exports (a category's
// entries) is already loaded client-side in the admin editor.
export function downloadCsv(filename, rows) {
  const csv = toCsv(rows)
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
