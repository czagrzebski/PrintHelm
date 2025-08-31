<script setup lang="ts">
import { ref } from 'vue'
import AuthService from '@/service/AuthService'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const username = ref('')
const password = ref('')

function login() {
  AuthService.login(username.value, password.value)
    .then((response) => {
      console.log('Login successful:', response)
      // Handle successful login (e.g., redirect to dashboard)
    })
    .catch((error) => {
      console.error('Login failed:', error)
      // Handle login failure (e.g., show error message)
    })
}
</script>

<template>
  <div
    style="
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
      width: 100%;
      flex-direction: column;
    "
  >
    <h2>PrintHelm UI</h2>
    <p>Logged in: {{ authStore.accessToken != null ? 'Yes' : 'No' }}</p>
    <form @submit.prevent="login">
      <div style="margin-bottom: 1rem">
        <label for="username">Username: </label>
        <input id="username" v-model="username" type="text" required />
      </div>
      <div style="margin-bottom: 1rem">
        <label for="password">Password: </label>
        <input id="password" v-model="password" type="password" required />
      </div>
    </form>
    <button @click="login">Login</button>
  </div>
</template>
