// Places for a list already sorted best-first, where equal scores share a place and the next
// different score skips ahead: 13, 12, 12, 11 -> 1, 2, 2, 4. (Who submitted first is not a tiebreak.)
export function sharedRanks(list, scoreOf) {
  const ranks = []
  list.forEach((item, i) => {
    ranks.push(i > 0 && scoreOf(item) === scoreOf(list[i - 1]) ? ranks[i - 1] : i + 1)
  })
  return ranks
}
