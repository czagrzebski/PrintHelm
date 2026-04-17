<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'
import AuthService from '@/service/AuthService'

const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function login() {
  if (!username.value || !password.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    await AuthService.login(username.value, password.value)
    router.push('/dashboard')
  } catch {
    errorMessage.value = 'Invalid username or password.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <Card class="login-card">
      <template #header>
        <div class="login-header">
          <div class="login-icon-wrap">
            <i class="mdi mdi-printer-3d" />
          </div>
          <h1 class="login-title">PrintHelm</h1>
          <p class="login-subtitle">Sign in to your account</p>
        </div>
      </template>

      <template #content>
        <form class="login-form" @submit.prevent="login">
          <Message v-if="errorMessage" severity="error" :closable="false" class="login-error">
            {{ errorMessage }}
          </Message>

          <div class="field">
            <label for="username">Username</label>
            <InputText
              id="username"
              v-model="username"
              placeholder="Enter username"
              autocomplete="username"
              :disabled="loading"
              fluid
            />
          </div>

          <div class="field">
            <label for="password">Password</label>
            <Password
              id="password"
              v-model="password"
              placeholder="Enter password"
              :feedback="false"
              toggle-mask
              autocomplete="current-password"
              :disabled="loading"
              fluid
            />
          </div>

          <Button
            type="submit"
            label="Sign In"
            icon="pi pi-sign-in"
            :loading="loading"
            fluid
          />
        </form>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, var(--ph-bg-darkest) 0%, var(--ph-bg-mid) 55%, var(--ph-bg-light) 100%);
  padding: 1rem;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: rgba(15, 32, 39, 0.75) !important;
  backdrop-filter: blur(12px);
  border: 1px solid var(--ph-border) !important;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.5) !important;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2rem 2rem 0;
  text-align: center;
}

.login-icon-wrap {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 14px;
  background: var(--ph-accent-dim);
  border: 1px solid rgba(34, 211, 238, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}

.login-icon-wrap i {
  font-size: 1.5rem;
  color: var(--ph-accent);
}

.login-title {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  margin: 0 0 0.25rem;
  color: var(--ph-text);
}

.login-subtitle {
  font-size: 0.875rem;
  color: var(--ph-text-muted);
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.login-error {
  margin-bottom: 0;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.field label {
  font-size: 0.8rem;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--ph-text-muted);
}
</style>
