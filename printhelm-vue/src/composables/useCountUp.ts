import { ref, watch, type Ref } from 'vue'

/**
 * Animates a number toward a reactive target with an ease-out curve.
 * Returns a ref holding the current (rounded) display value.
 */
export function useCountUp(target: Ref<number>, durationMs = 800) {
  const display = ref(target.value)
  let rafId: number | null = null

  function animate(to: number) {
    if (rafId !== null) cancelAnimationFrame(rafId)
    const from = display.value
    if (from === to) return
    const start = performance.now()

    function step(now: number) {
      const t = Math.min((now - start) / durationMs, 1)
      const eased = 1 - Math.pow(1 - t, 3)
      display.value = Math.round(from + (to - from) * eased)
      if (t < 1) rafId = requestAnimationFrame(step)
      else rafId = null
    }

    rafId = requestAnimationFrame(step)
  }

  watch(target, animate)

  return display
}
