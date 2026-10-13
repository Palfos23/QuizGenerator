// Short names for compact leaderboards: a first name, unless two people on the same board share one -
// then the first letters of their surnames are added until they can be told apart ("Erik H." and
// "Erik S."; "Erik Han." and "Erik Hag." if even the initial matches). Two people with exactly the same
// full name can't be separated, so those keep the same label.
//
// shortNames(allNames) -> a function: full name -> short label. Pass EVERY name shown together (the
// whole board, not just the visible rows), so a label doesn't change as people move up and down.

function parts(fullName) {
  const tokens = (fullName || '').trim().split(/\s+/).filter(Boolean)
  return { first: tokens[0] || '', surname: tokens.length > 1 ? tokens[tokens.length - 1] : '' }
}

export function shortNames(allNames) {
  const uniqueNames = [...new Set((allNames || []).filter(Boolean))]
  const byFirst = new Map()
  for (const full of uniqueNames) {
    const key = parts(full).first.toLowerCase()
    if (!byFirst.has(key)) byFirst.set(key, [])
    byFirst.get(key).push(full)
  }

  const labels = new Map()
  for (const group of byFirst.values()) {
    if (group.length === 1) {
      labels.set(group[0], parts(group[0]).first)
      continue
    }
    // Several people share this first name: each gets the shortest surname prefix that tells them apart
    // from the others ("Erik H.", "Erik S."; "Erik Han." and "Erik Hag." if the initial is shared too).
    // If one surname is a prefix of another, the whole surname is used.
    for (const full of group) {
      const { surname } = parts(full)
      const others = group.filter(n => n !== full).map(n => parts(n).surname.toLowerCase())
      let len = 1
      while (len < surname.length && others.some(o => o.slice(0, len) === surname.slice(0, len).toLowerCase())) len++
      const stillClashes = others.some(o => o.slice(0, len) === surname.slice(0, len).toLowerCase())
      labels.set(full, stillClashes ? labelWith(full, surname.length) : labelWith(full, len))
    }
  }
  return fullName => labels.get(fullName) ?? parts(fullName).first
}

function labelWith(fullName, len) {
  const { first, surname } = parts(fullName)
  if (!surname) return first
  const prefix = surname.slice(0, len)
  return `${first} ${prefix}${prefix.length < surname.length ? '.' : ''}`
}
