import { ref } from 'vue'
import api from './api'

// Plain array of category name strings, e.g. ['Cycling', 'Football'] - the
// name IS the value stored on athletes/grids/pools/clubs now, there's no
// separate code/label split the way the old Sport enum had.
const categories = ref([])

// Full category objects (id, name, groupLabel) - kept separately so
// groupLabelFor() can look up the right word ("Team"/"Continent"/"Label")
// for whichever category is currently selected, without every consumer
// needing its own copy of this data.
const fullCategories = ref([])

let loaded = false
let loadingPromise = null

async function ensureLoaded() {
  if (loaded) return
  if (!loadingPromise) {
    loadingPromise = api.fetchGridCategories().then(list => {
      fullCategories.value = list
      categories.value = list.map(c => c.name).sort()
      loaded = true
    })
  }
  await loadingPromise
}

// What to call the grouping field for a given category - "Team" for sports,
// "Continent" for countries, whatever the admin set. Falls back to "Team"
// if the category isn't found (e.g. categories haven't loaded yet).
function groupLabelFor(categoryName) {
  return fullCategories.value.find(c => c.name === categoryName)?.groupLabel || 'Team'
}

// Call after an admin adds/renames/deletes a category, so every other
// already-open view picks up the change without needing a full page reload.
function invalidate() {
  loaded = false
  loadingPromise = null
}

export default { categories, fullCategories, ensureLoaded, invalidate, groupLabelFor }
