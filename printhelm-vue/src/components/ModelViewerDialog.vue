<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { STLLoader } from 'three/examples/jsm/loaders/STLLoader.js'
import { OBJLoader } from 'three/examples/jsm/loaders/OBJLoader.js'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js'

const props = defineProps<{
  filename: string
  fetchFile: () => Promise<ArrayBuffer>
}>()
const emit = defineEmits<{ close: [] }>()

const loading = ref(true)
const error = ref('')
const canvasRef = ref<HTMLCanvasElement | null>(null)
const wireframe = ref(false)
const autoRotate = ref(false)

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let controls: OrbitControls | null = null
let animFrameId: number | null = null
let resizeObserver: ResizeObserver | null = null
let pmrem: THREE.PMREMGenerator | null = null
let modelRoot: THREE.Object3D | null = null
let initialCameraPos = new THREE.Vector3()
let initialTarget = new THREE.Vector3()

onMounted(async () => {
  try {
    const buffer = await props.fetchFile()
    const object = await parseModel(buffer)
    loading.value = false
    await nextTick()
    if (canvasRef.value) initViewer(canvasRef.value, object)
  } catch (e: unknown) {
    error.value = (e as Error)?.message ?? 'Failed to load model'
    loading.value = false
  }
})

onUnmounted(() => teardown())

function fileExt(): string {
  return props.filename.toLowerCase().split('.').pop() ?? ''
}

function parseModel(buffer: ArrayBuffer): Promise<THREE.Object3D> {
  const ext = fileExt()
  if (ext === 'glb' || ext === 'gltf') {
    return new Promise((resolve, reject) => {
      const loader = new GLTFLoader()
      const data = ext === 'gltf' ? new TextDecoder().decode(buffer) : buffer
      loader.parse(
        data,
        '',
        (gltf) => resolve(gltf.scene),
        (err) => reject(err instanceof Error ? err : new Error('Failed to parse glTF model')),
      )
    })
  }
  if (ext === 'stl') {
    const geometry = new STLLoader().parse(buffer)
    geometry.computeVertexNormals()
    const material = new THREE.MeshStandardMaterial({ color: 0x8fb8c9, metalness: 0.15, roughness: 0.55 })
    return Promise.resolve(new THREE.Mesh(geometry, material))
  }
  if (ext === 'obj') {
    const group = new OBJLoader().parse(new TextDecoder().decode(buffer))
    const material = new THREE.MeshStandardMaterial({ color: 0x8fb8c9, metalness: 0.15, roughness: 0.55 })
    group.traverse((child) => {
      if (child instanceof THREE.Mesh) child.material = material
    })
    return Promise.resolve(group)
  }
  return Promise.reject(new Error(`Unsupported model format: .${ext}`))
}

function initViewer(canvas: HTMLCanvasElement, object: THREE.Object3D) {
  const w = canvas.clientWidth
  const h = canvas.clientHeight

  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.setSize(w, h, false)
  renderer.setClearColor(0x000000, 0)
  renderer.outputColorSpace = THREE.SRGBColorSpace

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(45, w / h, 0.01, 100000)

  // Neutral studio environment so glTF PBR materials render correctly
  pmrem = new THREE.PMREMGenerator(renderer)
  scene.environment = pmrem.fromScene(new RoomEnvironment(), 0.04).texture

  scene.add(new THREE.AmbientLight(0xffffff, 0.35))
  const keyLight = new THREE.DirectionalLight(0xffffff, 1.4)
  keyLight.position.set(2, 4, 3)
  scene.add(keyLight)
  const fillLight = new THREE.DirectionalLight(0xffffff, 0.5)
  fillLight.position.set(-3, 2, -2)
  scene.add(fillLight)

  // STL files are commonly Z-up; rotate so the model stands upright in Y-up space
  if (fileExt() === 'stl') object.rotation.x = -Math.PI / 2

  // Center the model on the origin, resting on the grid plane
  const box = new THREE.Box3().setFromObject(object)
  const center = box.getCenter(new THREE.Vector3())
  const size = box.getSize(new THREE.Vector3())
  object.position.x -= center.x
  object.position.z -= center.z
  object.position.y -= box.min.y
  modelRoot = object
  scene.add(object)

  const maxDim = Math.max(size.x, size.y, size.z) || 1
  const gridSpan = Math.ceil((Math.max(size.x, size.z) || 1) * 1.6 / 10) * 10 || 10
  const grid = new THREE.GridHelper(gridSpan, Math.min(Math.ceil(gridSpan / (gridSpan / 20)), 30), 0x1e4a5a, 0x1e4a5a)
  ;(grid.material as THREE.LineBasicMaterial).opacity = 0.5
  ;(grid.material as THREE.LineBasicMaterial).transparent = true
  scene.add(grid)

  camera.position.set(maxDim * 1.1, maxDim * 0.9, maxDim * 1.5)
  initialCameraPos = camera.position.clone()
  initialTarget = new THREE.Vector3(0, size.y / 2, 0)

  controls = new OrbitControls(camera, canvas)
  controls.target.copy(initialTarget)
  controls.enableDamping = true
  controls.dampingFactor = 0.06

  resizeObserver = new ResizeObserver(() => {
    if (!canvas || !renderer || !camera) return
    const nw = canvas.clientWidth
    const nh = canvas.clientHeight
    renderer.setSize(nw, nh, false)
    camera.aspect = nw / nh
    camera.updateProjectionMatrix()
  })
  resizeObserver.observe(canvas)

  const animate = () => {
    animFrameId = requestAnimationFrame(animate)
    if (autoRotate.value && modelRoot) modelRoot.rotation.y += 0.005
    controls!.update()
    renderer!.render(scene!, camera!)
  }
  animate()
}

function toggleWireframe() {
  wireframe.value = !wireframe.value
  modelRoot?.traverse((child) => {
    if (child instanceof THREE.Mesh) {
      const mats = Array.isArray(child.material) ? child.material : [child.material]
      for (const mat of mats) {
        if ('wireframe' in mat) (mat as THREE.MeshStandardMaterial).wireframe = wireframe.value
      }
    }
  })
}

function resetView() {
  if (!camera || !controls) return
  camera.position.copy(initialCameraPos)
  controls.target.copy(initialTarget)
  if (modelRoot) modelRoot.rotation.y = 0
  autoRotate.value = false
}

function teardown() {
  if (animFrameId != null) { cancelAnimationFrame(animFrameId); animFrameId = null }
  resizeObserver?.disconnect(); resizeObserver = null
  controls?.dispose(); controls = null
  modelRoot?.traverse((child) => {
    if (child instanceof THREE.Mesh) {
      child.geometry.dispose()
      const mats = Array.isArray(child.material) ? child.material : [child.material]
      mats.forEach((m) => m.dispose())
    }
  })
  modelRoot = null
  pmrem?.dispose(); pmrem = null
  renderer?.dispose(); renderer = null
  scene?.clear(); scene = null
  camera = null
}
</script>

<script lang="ts">
/** Returns true when the filename has an extension the 3D viewer can render */
export function isViewableModel(filename?: string): boolean {
  if (!filename) return false
  const ext = filename.toLowerCase().split('.').pop() ?? ''
  return ['glb', 'gltf', 'stl', 'obj'].includes(ext)
}
</script>

<template>
  <div class="mv-overlay" @click.self="emit('close')">
    <div class="mv-box">
      <div class="mv-header">
        <span class="mv-title">
          <i class="mdi mdi-cube-scan" />
          {{ filename }}
        </span>
        <button class="mv-close" @click="emit('close')">
          <i class="mdi mdi-close" />
        </button>
      </div>

      <div v-if="loading" class="mv-state">
        <span class="mv-spinner" />
        <span>Loading model…</span>
      </div>
      <div v-else-if="error" class="mv-state mv-state--error">
        <i class="mdi mdi-alert-circle-outline" /> {{ error }}
      </div>

      <template v-else>
        <canvas ref="canvasRef" class="mv-canvas" />
        <div class="mv-controls">
          <button
            class="mv-ctrl-btn"
            :class="{ 'mv-ctrl-btn--active': wireframe }"
            title="Toggle wireframe"
            @click="toggleWireframe"
          >
            <i class="mdi mdi-vector-triangle" /> Wireframe
          </button>
          <button
            class="mv-ctrl-btn"
            :class="{ 'mv-ctrl-btn--active': autoRotate }"
            title="Toggle auto-rotate"
            @click="autoRotate = !autoRotate"
          >
            <i class="mdi mdi-rotate-3d-variant" /> Rotate
          </button>
          <button class="mv-ctrl-btn" title="Reset view" @click="resetView">
            <i class="mdi mdi-restore" /> Reset
          </button>
          <span class="mv-hint">Drag to orbit · Scroll to zoom · Right-drag to pan</span>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.mv-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.mv-box {
  width: 100%;
  max-width: 960px;
  height: min(80vh, 760px);
  background: var(--ph-glass-heavy);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border: 1px solid var(--ph-border-strong);
  border-radius: 16px;
  box-shadow: var(--ph-shadow-pop);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: ph-scale-in 0.25s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.mv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ph-border);
  flex-shrink: 0;
}

.mv-title {
  font-size: 0.78rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--ph-text-muted);
  display: flex;
  align-items: center;
  gap: 0.4rem;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mv-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ph-text-muted);
  font-size: 1.1rem;
  padding: 0.25rem;
  border-radius: 4px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  transition: color 0.15s, background 0.15s;
}
.mv-close:hover { color: var(--ph-text); background: rgba(255,255,255,0.07); }

.mv-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  font-size: 0.85rem;
  color: var(--ph-text-muted);
}
.mv-state--error { color: #f87171; }

@keyframes mv-spin { to { transform: rotate(360deg); } }
.mv-spinner {
  width: 1.25rem;
  height: 1.25rem;
  border: 2px solid var(--ph-border);
  border-top-color: var(--ph-accent, #22d3ee);
  border-radius: 50%;
  animation: mv-spin 0.7s linear infinite;
}

.mv-canvas {
  flex: 1;
  width: 100%;
  min-height: 0;
  display: block;
  background: #060f14;
}

.mv-controls {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 0.75rem;
  border-top: 1px solid var(--ph-border);
  background: rgba(0,0,0,0.3);
  flex-wrap: wrap;
}

.mv-ctrl-btn {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  background: rgba(255,255,255,0.05);
  border: 1px solid var(--ph-border);
  border-radius: 6px;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.3rem 0.6rem;
  transition: color 0.15s, background 0.15s;
}
.mv-ctrl-btn:hover { color: var(--ph-text); background: rgba(255,255,255,0.1); }
.mv-ctrl-btn--active {
  color: var(--ph-accent, #22d3ee);
  border-color: var(--ph-accent, #22d3ee);
  background: rgba(34,211,238,0.1);
}

.mv-hint {
  margin-left: auto;
  font-size: 0.68rem;
  color: var(--ph-text-muted);
  opacity: 0.7;
  white-space: nowrap;
}
</style>
