<script setup lang="ts">
import { ref, computed } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/Configuration'

const authStore = useAuthStore()
const toast = useToast()

const mustChange = computed(() => !!authStore.accessToken && authStore.mustChangePassword)

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const changing = ref(false)

async function submitPasswordChange() {
  if (!newPassword.value || newPassword.value.length < 6) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'New password must be at least 6 characters.', life: 4000 })
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Passwords do not match.', life: 4000 })
    return
  }
  if (!currentPassword.value) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Current password is required.', life: 4000 })
    return
  }
  changing.value = true
  try {
    await api.put('/user/me/password', { currentPassword: currentPassword.value, newPassword: newPassword.value })
    authStore.clearMustChangePassword()
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    toast.add({ severity: 'success', summary: 'Password changed', detail: 'Your password has been updated.', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to change password. Check your current password.', life: 4000 })
  } finally {
    changing.value = false
  }
}
</script>

<template>
  <Toast />
  <RouterView />

  <Dialog
    :visible="mustChange"
    :closable="false"
    :modal="true"
    header="Password Change Required"
    :style="{ width: '420px' }"
  >
    <div class="force-change-body">
      <p class="force-change-msg">
        Your password must be changed before you can continue. Please set a new password below.
      </p>

      <div class="field">
        <label class="field-label">Current Password <span class="required">*</span></label>
        <Password v-model="currentPassword" :feedback="false" toggle-mask class="w-full" input-class="w-full" />
      </div>
      <div class="field">
        <label class="field-label">New Password <span class="required">*</span></label>
        <Password v-model="newPassword" :feedback="true" toggle-mask class="w-full" input-class="w-full" />
      </div>
      <div class="field">
        <label class="field-label">Confirm New Password <span class="required">*</span></label>
        <Password v-model="confirmPassword" :feedback="false" toggle-mask class="w-full" input-class="w-full" />
      </div>
    </div>

    <template #footer>
      <Button label="Change Password" :loading="changing" @click="submitPasswordChange" />
    </template>
  </Dialog>
</template>

<style scoped>
.force-change-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 0.25rem 0;
}

.force-change-msg {
  font-size: 0.875rem;
  color: var(--ph-text-muted);
  margin: 0 0 0.5rem;
  line-height: 1.5;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.field-label {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--ph-text-muted);
}

.required {
  color: #f87171;
}

.w-full {
  width: 100%;
}
</style>
