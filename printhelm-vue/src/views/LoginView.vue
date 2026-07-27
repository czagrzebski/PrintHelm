<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
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

const particlesOptions = {
  fullScreen: { enable: false },
  background: { color: { value: 'transparent' } },
  fpsLimit: 60,
  particles: {
    number: { value: 60, density: { enable: true } },
    color: { value: ['#22d3ee', '#0891b2', '#67e8f9'] },
    shape: { type: 'circle' },
    opacity: {
      value: { min: 0.1, max: 0.4 },
      animation: { enable: true, speed: 0.5, sync: false },
    },
    size: {
      value: { min: 1, max: 3 },
      animation: { enable: true, speed: 2, sync: false },
    },
    links: {
      enable: true,
      distance: 140,
      color: '#22d3ee',
      opacity: 0.12,
      width: 1,
    },
    move: {
      enable: true,
      speed: 0.6,
      direction: 'none' as const,
      random: true,
      straight: false,
      outModes: { default: 'out' as const },
    },
  },
  interactivity: {
    events: {
      onHover: { enable: true, mode: 'grab' },
      onClick: { enable: false },
    },
    modes: {
      grab: { distance: 120, links: { opacity: 0.3 } },
    },
  },
  detectRetina: true,
}

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
    <vue-particles
      id="login-particles"
      class="particles-canvas"
      :options="particlesOptions"
    />

    <div class="login-card">
      <div class="login-header">
        <div class="login-icon-wrap">
          <i class="mdi mdi-printer-3d" />
        </div>
        <h1 class="login-title">PrintHelm</h1>
        <p class="login-subtitle">Sign in to your account</p>
      </div>

      <div class="login-body">
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
              :pt="{ pcInputText: { root: { autocomplete: 'current-password' } } }"
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
            class="login-btn"
          />
        </form>
      </div>
    </div>
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
  position: relative;
  overflow: hidden;
}

/* Aurora blobs drifting behind the particles */
.login-container::before,
.login-container::after {
  content: '';
  position: absolute;
  width: 55vmax;
  height: 55vmax;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
  z-index: 0;
}

.login-container::before {
  top: -22vmax;
  left: -14vmax;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.14), transparent 65%);
  animation: blob-drift 18s ease-in-out infinite alternate;
}

.login-container::after {
  bottom: -24vmax;
  right: -16vmax;
  background: radial-gradient(circle, rgba(129, 140, 248, 0.12), transparent 65%);
  animation: blob-drift 22s ease-in-out infinite alternate-reverse;
}

@keyframes blob-drift {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to   { transform: translate3d(6vmax, 4vmax, 0) scale(1.15); }
}

.particles-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  background:
    linear-gradient(rgba(10, 22, 30, 0.82), rgba(10, 22, 30, 0.82)) padding-box,
    linear-gradient(130deg,
      rgba(34, 211, 238, 0.55),
      rgba(34, 211, 238, 0.08) 30%,
      rgba(129, 140, 248, 0.1) 65%,
      rgba(129, 140, 248, 0.5)) border-box;
  background-size: 100% 100%, 300% 300%;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid transparent;
  border-radius: 20px;
  box-shadow:
    0 32px 80px rgba(0, 0, 0, 0.6),
    0 0 48px rgba(34, 211, 238, 0.07),
    0 1px 0 rgba(255, 255, 255, 0.08) inset;
  overflow: hidden;
  animation:
    card-enter 0.55s cubic-bezier(0.16, 1, 0.3, 1) both,
    border-pan 7s ease infinite;
}

@keyframes border-pan {
  0%   { background-position: 0 0, 0% 50%; }
  50%  { background-position: 0 0, 100% 50%; }
  100% { background-position: 0 0, 0% 50%; }
}

@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(28px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2.5rem 2rem 1.5rem;
  text-align: center;
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.06) 0%, transparent 100%);
  border-bottom: 1px solid rgba(34, 211, 238, 0.08);
}

.login-icon-wrap {
  width: 4rem;
  height: 4rem;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(34, 211, 238, 0.2) 0%, rgba(8, 145, 178, 0.15) 100%);
  border: 1px solid rgba(34, 211, 238, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.25rem;
  box-shadow: 0 0 24px rgba(34, 211, 238, 0.15);
  animation: icon-float 3.5s ease-in-out infinite;
}

@keyframes icon-float {
  0%, 100% { transform: translateY(0); }
  50%       { transform: translateY(-5px); }
}

.login-icon-wrap i {
  font-size: 1.75rem;
  color: var(--ph-accent);
}

.login-title {
  font-size: 1.625rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  margin: 0 0 0.375rem;
  color: var(--ph-text);
  text-shadow: 0 0 32px rgba(34, 211, 238, 0.25);
}

.login-subtitle {
  font-size: 0.875rem;
  color: var(--ph-text-muted);
  margin: 0;
  letter-spacing: 0.01em;
}

.login-body {
  padding: 1.75rem 2rem 2rem;
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
  animation: field-in 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.field:nth-of-type(1) { animation-delay: 0.15s; }
.field:nth-of-type(2) { animation-delay: 0.25s; }

@keyframes field-in {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}

.field label {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ph-text-muted);
}

.login-btn {
  margin-top: 0.25rem;
  position: relative;
  overflow: hidden;
  animation: field-in 0.5s cubic-bezier(0.16, 1, 0.3, 1) 0.35s both;
}

/* shine sweep across the sign-in button */
.login-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -80%;
  width: 50%;
  height: 100%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.25), transparent);
  transform: skewX(-20deg);
  animation: btn-shine 3.5s ease-in-out infinite;
  pointer-events: none;
}

@keyframes btn-shine {
  0%, 60% { left: -80%; }
  100%    { left: 130%; }
}
</style>
