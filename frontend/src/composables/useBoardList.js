import { computed, ref, watch } from 'vue'

/**
 * Search + sort + paginate for the admin game-board list screens (Weekly grids,
 * Starting XI, Imposter, Bullseye, 501). Every one of those had the same
 * hand-rolled pagination; this folds search and sort in on top, consistently.
 *
 * @param source        ref/computed holding the full array of boards
 * @param options.searchFields  array of (item) => string|number - fields matched against the search box
 * @param options.sorts  array of { key, label, accessor, dir? } - the first entry is the default sort;
 *                        `dir` ('asc' | 'desc') is that sort's starting direction (default 'asc')
 * @param options.pageSize  rows per page (default 10)
 */
export function useBoardList(source, { searchFields = [], sorts = [], pageSize = 10 }) {
  const searchTerm = ref('')
  const sortKey = ref(sorts[0]?.key || '')
  const sortDir = ref(sorts[0]?.dir || 'asc')
  const page = ref(1)

  const activeSort = computed(() => sorts.find(s => s.key === sortKey.value) || sorts[0] || null)

  const filtered = computed(() => {
    let list = source.value || []

    const term = searchTerm.value.trim().toLowerCase()
    if (term) {
      list = list.filter(item =>
        searchFields.some(f => String(f(item) ?? '').toLowerCase().includes(term))
      )
    }

    const sort = activeSort.value
    if (sort) {
      const dir = sortDir.value === 'asc' ? 1 : -1
      list = [...list].sort((a, b) => {
        let av = sort.accessor(a)
        let bv = sort.accessor(b)
        // Blanks always sink to the bottom, regardless of direction.
        if (av == null || av === '') return 1
        if (bv == null || bv === '') return -1
        if (typeof av === 'string') av = av.toLowerCase()
        if (typeof bv === 'string') bv = bv.toLowerCase()
        if (av < bv) return -1 * dir
        if (av > bv) return 1 * dir
        return 0
      })
    }

    return list
  })

  const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))
  const paged = computed(() => {
    const start = (page.value - 1) * pageSize
    return filtered.value.slice(start, start + pageSize)
  })

  // Any change to the result set jumps back to a valid page.
  watch([searchTerm, sortKey, sortDir], () => { page.value = 1 })
  watch(totalPages, (n) => { if (page.value > n) page.value = n })

  return { searchTerm, sortKey, sortDir, page, pageSize, filtered, paged, totalPages, sorts }
}
