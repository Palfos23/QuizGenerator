<template>
  <AdventAdminGate>
    <AdminAdventPanel />
  </AdventAdminGate>
</template>

<script setup>
import AdventAdminGate from '../components/AdventAdminGate.vue'
import AdminAdventPanel from '../components/AdminAdventPanel.vue'

// The game master's page of the advent calendar: behind the admin login (the route) and the calendar's own password
// (the gate). The panel only loads once the gate is open, so it never asks the server before the password is known.
</script>
