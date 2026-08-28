<template>
  <div class="board-toolbar">
    <div class="field board-toolbar-search">
      <label>Search</label>
      <input
        type="text"
        :value="search"
        :placeholder="placeholder"
        @input="$emit('update:search', $event.target.value)"
      />
    </div>

    <div class="field board-toolbar-sort">
      <label>Sort by</label>
      <div class="board-toolbar-sort-row">
        <select :value="sortKey" @change="$emit('update:sortKey', $event.target.value)">
          <option v-for="s in sorts" :key="s.key" :value="s.key">{{ s.label }}</option>
        </select>
        <button
          type="button"
          class="board-toolbar-dir"
          :aria-label="sortDir === 'asc' ? 'Sorted ascending, click for descending' : 'Sorted descending, click for ascending'"
          :title="sortDir === 'asc' ? 'Ascending' : 'Descending'"
          @click="$emit('update:sortDir', sortDir === 'asc' ? 'desc' : 'asc')"
        >
          <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
            <path
              v-if="sortDir === 'asc'"
              d="M8 3.5v9M8 3.5 4.5 7M8 3.5 11.5 7"
              fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"
            />
            <path
              v-else
              d="M8 12.5v-9M8 12.5 4.5 9M8 12.5 11.5 9"
              fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"
            />
          </svg>
        </button>
      </div>
    </div>

    <div v-if="search.trim()" class="board-toolbar-count">
      {{ filteredCount }} of {{ totalCount }}
    </div>
  </div>
</template>

<script setup>
defineProps({
  search: { type: String, default: '' },
  sortKey: { type: String, default: '' },
  sortDir: { type: String, default: 'asc' },
  sorts: { type: Array, default: () => [] },
  totalCount: { type: Number, default: 0 },
  filteredCount: { type: Number, default: 0 },
  placeholder: { type: String, default: 'Search…' }
})
defineEmits(['update:search', 'update:sortKey', 'update:sortDir'])
</script>

<style scoped>
.board-toolbar {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  align-items: flex-end;
  margin-bottom: 20px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.02);
}
.board-toolbar .field { margin-bottom: 0; }
.board-toolbar-search { flex: 2; min-width: 200px; }
.board-toolbar-sort { flex: 1; min-width: 190px; }

.board-toolbar-sort-row {
  display: flex;
  gap: 8px;
  align-items: stretch;
}
.board-toolbar-sort-row select { flex: 1; min-width: 0; }

.board-toolbar-dir {
  flex: 0 0 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-dim);
}
.board-toolbar-dir:hover {
  border-color: var(--gold);
  color: var(--text);
}
.board-toolbar-dir:active { transform: none; }

.board-toolbar-count {
  font-size: 0.82rem;
  color: var(--text-dim);
  white-space: nowrap;
  padding-bottom: 10px;
}
</style>
