// Minimal CSV helpers - quoting only when a field actually needs it (contains
// a comma, quote, or newline). Not RFC 4180-complete, but matches the equally
// minimal "name,value" parser already used for CSV import (see
// AdminBullseyeView's parseCsv) - this is that same round trip, outbound.
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
