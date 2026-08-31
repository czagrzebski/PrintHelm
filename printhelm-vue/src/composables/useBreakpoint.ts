import { onBeforeUnmount, ref } from 'vue'

/** Reactive boolean tracking whether the viewport is at or below `maxWidthPx`. */
export function useBreakpoint(maxWidthPx: number) {
  const query = window.matchMedia(`(max-width: ${maxWidthPx}px)`)
  const matches = ref(query.matches)

  const handler = (e: MediaQueryListEvent) => {
    matches.value = e.matches
  }
  query.addEventListener('change', handler)

  onBeforeUnmount(() => {
    query.removeEventListener('change', handler)
  })

  return matches
}
