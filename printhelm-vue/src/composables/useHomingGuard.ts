import { ref } from 'vue'
import type { usePrinterCommands } from './usePrinterCommands'

const STALE_MS = 5 * 60 * 1000

function storageKey(printerId: number) {
  return `printhelm_homing_${printerId}`
}

function getLastHomedAt(printerId: number): number | null {
  try {
    const raw = localStorage.getItem(storageKey(printerId))
    return raw ? (JSON.parse(raw).lastHomedAt ?? null) : null
  } catch {
    return null
  }
}

function recordHome(printerId: number) {
  localStorage.setItem(storageKey(printerId), JSON.stringify({ lastHomedAt: Date.now() }))
}

export function useHomingGuard(
  printerId: number,
  commands: ReturnType<typeof usePrinterCommands>,
) {
  const showNeverHomedDialog = ref(false)
  const showStaleDialog = ref(false)
  const minutesSinceHome = ref(0)
  const pendingJog = ref<{ axis: string; distance: number } | null>(null)

  function homingStatus(): 'recent' | 'stale' | 'never' {
    const last = getLastHomedAt(printerId)
    if (last === null) return 'never'
    const elapsed = Date.now() - last
    if (elapsed < STALE_MS) return 'recent'
    minutesSinceHome.value = Math.floor(elapsed / 60000)
    return 'stale'
  }

  async function requestJog(axis: string, distance: number) {
    const status = homingStatus()
    if (status === 'recent') {
      await commands.jog(axis, distance)
      return
    }
    pendingJog.value = { axis, distance }
    if (status === 'never') {
      showNeverHomedDialog.value = true
    } else {
      showStaleDialog.value = true
    }
  }

  async function home() {
    await commands.home()
    recordHome(printerId)
  }

  async function homeAndContinue() {
    showNeverHomedDialog.value = false
    showStaleDialog.value = false
    await home()
    if (pendingJog.value) {
      const { axis, distance } = pendingJog.value
      pendingJog.value = null
      await commands.jog(axis, distance)
    }
  }

  async function continueWithoutHoming() {
    showNeverHomedDialog.value = false
    showStaleDialog.value = false
    if (pendingJog.value) {
      const { axis, distance } = pendingJog.value
      pendingJog.value = null
      await commands.jog(axis, distance)
    }
  }

  function cancelJog() {
    showNeverHomedDialog.value = false
    showStaleDialog.value = false
    pendingJog.value = null
  }

  return {
    showNeverHomedDialog,
    showStaleDialog,
    minutesSinceHome,
    requestJog,
    home,
    homeAndContinue,
    continueWithoutHoming,
    cancelJog,
  }
}
