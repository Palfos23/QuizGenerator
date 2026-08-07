import { ref } from 'vue'
import api from './api'

// Plain array of category name strings, e.g. ['Cycling', 'Football'] - the
// name IS the value stored on athletes/grids/pools/clubs now, there's no
// separate code/label split the way the old Sport enum had.
const categories = ref([])
let loaded = false
let loadingPromise = null

async function ensureLoaded() {
  if (loaded) return
  if (!loadingPromise) {
    loadingPromise = api.fetchGridCategories().then(list => {
      categories.value = list.map(c => c.name).sort()
      loaded = true
    })
  }
  await loadingPromise
}

// Call after an admin adds/renames/deletes a category, so every other
// already-open view picks up the change without needing a full page reload.
function invalidate() {
  loaded = false
  loadingPromise = null
}

export default { categories, ensureLoaded, invalidate }
