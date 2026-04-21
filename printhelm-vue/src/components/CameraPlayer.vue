<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import Hls from 'hls.js'

const props = defineProps<{ src: string }>()
const emit = defineEmits<{ ready: []; error: [] }>()

const videoRef = ref<HTMLVideoElement | null>(null)
let hls: Hls | null = null

function init() {
  const video = videoRef.value
  if (!video) return

  destroy()

  if (Hls.isSupported()) {
    hls = new Hls({ lowLatencyMode: true })
    hls.loadSource(props.src)
    hls.attachMedia(video)
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      video.play().catch(() => {})
      emit('ready')
    })
    hls.on(Hls.Events.ERROR, (_, data) => {
      if (data.fatal) emit('error')
    })
  } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
    video.src = props.src
    video.addEventListener('loadedmetadata', () => {
      video.play().catch(() => {})
      emit('ready')
    }, { once: true })
    video.addEventListener('error', () => emit('error'), { once: true })
  } else {
    emit('error')
  }
}

function destroy() {
  hls?.destroy()
  hls = null
}

function onPageHide(e: PageTransitionEvent) {
  if (e.persisted) destroy()
}

function onPageShow(e: PageTransitionEvent) {
  if (e.persisted) init()
}

onMounted(() => {
  init()
  window.addEventListener('pagehide', onPageHide)
  window.addEventListener('pageshow', onPageShow)
})

onUnmounted(() => {
  destroy()
  window.removeEventListener('pagehide', onPageHide)
  window.removeEventListener('pageshow', onPageShow)
})

watch(() => props.src, init)
</script>

<template>
  <video ref="videoRef" muted playsinline autoplay />
</template>
