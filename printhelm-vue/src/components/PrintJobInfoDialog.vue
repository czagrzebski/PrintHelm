<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import JSZip from 'jszip'
import * as THREE from 'three'
import { GCodeLoader } from 'three/examples/jsm/loaders/GCodeLoader.js'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { api } from '@/api/Configuration'

const props = defineProps<{
  printerId?: number
  filename: string
  fileSize?: number
  fetchFile?: () => Promise<ArrayBuffer>
}>()
const emit = defineEmits<{ close: [] }>()

const loading = ref(true)
const loadingLabel = ref('Downloading…')
const downloadProgress = ref(0)
const loadingStage = computed(() => {
  if (loadingLabel.value === 'Downloading…') return 'download'
  if (loadingLabel.value === 'Extracting…') return 'extract'
  return 'parse'
})
const loadingStageNum = computed(() =>
  loadingLabel.value === 'Downloading…' ? 1 : loadingLabel.value === 'Extracting…' ? 2 : 3
)
const error = ref('')
const cfg = ref<Record<string, unknown> | null>(null)
const plateImageUrl = ref<string | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const viewMode = ref<'gcode' | 'image'>('gcode')

// ── Viewer config ─────────────────────────────────────────────────────────
const extrudeColor = ref('#22d3ee')
const showTravel = ref(false)
const layerMin = ref(0)
const layerMax = ref(100)

// ── Animation ─────────────────────────────────────────────────────────────
const animPlaying = ref(false)
const animLayer = ref(0)
const animTotalLayers = ref(0)
const animSpeed = ref(5)

// ── Three.js state ────────────────────────────────────────────────────────
let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let controls: OrbitControls | null = null
let animFrameId: number | null = null
let resizeObserver: ResizeObserver | null = null
let pendingGcode: string | null = null
let vcMat: THREE.LineBasicMaterial | null = null
let pathMat: THREE.LineBasicMaterial | null = null
let worldTotalH = 0
let modelBaseY = 0   // world Y of the first extruded layer after centering
let extrudeGeos: THREE.BufferGeometry[] = []
let gcodeZMin = 0
let gcodeZRange = 1

// ── Layer animation internals ─────────────────────────────────────────────
interface LayerInfo { z: number; pct: number; endX: number; endY: number; segVerts: Float32Array }
let layerInfos: LayerInfo[] = []
let nozzleMesh: THREE.Object3D | null = null
let curLayerGeo: THREE.BufferGeometry | null = null
let curLayerLines: THREE.LineSegments | null = null
let animInternalSeg = 0
let lastLayerStarted = -1
const animLayerProgress = ref(0)
const gcodeCenter = new THREE.Vector3()
let lastRenderedLayer = -1

onMounted(async () => {
  try {
    let buffer: ArrayBuffer
    if (props.fetchFile) {
      buffer = await props.fetchFile()
    } else {
      const res = await api.get<ArrayBuffer>(
        `/printer/${props.printerId}/files/${encodeURIComponent(props.filename)}`,
        {
          responseType: 'arraybuffer',
          onDownloadProgress: (e) => {
            if (e.total) downloadProgress.value = Math.round((e.loaded / e.total) * 100)
          },
        },
      )
      buffer = res.data
    }

    loadingLabel.value = 'Extracting…'
    const zip = await JSZip.loadAsync(buffer)

    const cfgEntry = zip.file('Metadata/project_settings.config')
    if (cfgEntry) cfg.value = JSON.parse(await cfgEntry.async('text'))

    const imgEntry = zip.file('Metadata/plate_1.png')
    if (imgEntry) plateImageUrl.value = URL.createObjectURL(await imgEntry.async('blob'))

    const gcodeEntry = zip.file('Metadata/plate_1.gcode')
    if (!gcodeEntry) throw new Error('plate_1.gcode not found in archive')

    loadingLabel.value = 'Parsing GCode…'
    pendingGcode = await gcodeEntry.async('text')

    loading.value = false
    await nextTick()

    if (canvasRef.value) initViewer(canvasRef.value, pendingGcode)
  } catch (e: unknown) {
    error.value = (e as Error)?.message ?? 'Failed to load job info'
    loading.value = false
  }
})

onUnmounted(() => {
  teardownViewer()
  if (plateImageUrl.value) URL.revokeObjectURL(plateImageUrl.value)
})

watch(viewMode, async (mode) => {
  if (mode === 'gcode') {
    await nextTick()
    if (canvasRef.value && pendingGcode) {
      teardownViewer()
      initViewer(canvasRef.value, pendingGcode)
    }
  } else {
    teardownViewer()
  }
})

watch(extrudeColor, (c) => { applyVertexColors(new THREE.Color(c)) })
watch(showTravel, (v) => { if (pathMat) pathMat.visible = v })
watch([layerMin, layerMax], () => applyClipping())
// animSpeed is consumed per-frame in tickAnim via segsPerFrame — no restart needed

function teardownViewer() {
  pauseAnim()
  layerInfos = []
  nozzleMesh = null
  curLayerLines = null
  curLayerGeo = null
  animInternalSeg = 0
  lastLayerStarted = -1
  animLayerProgress.value = 0
  animTotalLayers.value = 0
  animLayer.value = 0
  lastRenderedLayer = -1
  worldTotalH = 0
  modelBaseY = 0
  if (animFrameId != null) { cancelAnimationFrame(animFrameId); animFrameId = null }
  resizeObserver?.disconnect(); resizeObserver = null
  controls?.dispose(); controls = null
  renderer?.dispose(); renderer = null
  scene?.clear(); scene = null
  camera = null
  vcMat = null
  pathMat = null
  extrudeGeos = []
}

function applyVertexColors(hotColor: THREE.Color) {
  const cold = new THREE.Color(0x050d10)
  for (const geo of extrudeGeos) {
    const pos = geo.attributes.position as THREE.BufferAttribute
    const n = pos.count
    let attr = geo.attributes.color as THREE.BufferAttribute | undefined
    const buf = attr ? (attr.array as Float32Array) : new Float32Array(n * 3)
    for (let i = 0; i < n; i++) {
      const t = Math.sqrt(Math.max(0, (pos.getZ(i) - gcodeZMin) / gcodeZRange))
      const c = cold.clone().lerp(hotColor, t)
      buf[i * 3] = c.r; buf[i * 3 + 1] = c.g; buf[i * 3 + 2] = c.b
    }
    if (!attr) {
      geo.setAttribute('color', new THREE.BufferAttribute(buf, 3))
    } else {
      attr.needsUpdate = true
    }
  }
}

function applyClipping() {
  if (!vcMat || worldTotalH === 0) return
  // GCodeLoader rotates group by -PI/2 on X: GCode Z (print height) → Three.js Y.
  // modelBaseY = world Y of first layer; worldTotalH = gcodeZRange (extruded only).
  const minY = modelBaseY + (layerMin.value / 100) * worldTotalH
  const maxY = modelBaseY + (layerMax.value / 100) * worldTotalH
  const planes = [
    new THREE.Plane(new THREE.Vector3(0, 1, 0), -minY),
    new THREE.Plane(new THREE.Vector3(0, -1, 0), maxY),
  ]
  vcMat.clippingPlanes = planes
  if (pathMat) pathMat.clippingPlanes = planes
}

// Parse Bambu GCode into per-layer info using "; CHANGE_LAYER" and "; Z_HEIGHT:" comments.
// Each layer captures all extrusion segments (M83 relative E: positive E = extruding)
// as Float32Array triplets [x1,y1,z, x2,y2,z] for per-layer trace animation.
function parseLayerInfos(text: string): LayerInfo[] {
  const result: LayerInfo[] = []
  let x = 0, y = 0
  let absolute = true
  let currentLayerZ = 0
  let pendingZ = 0
  let layerChangeSeen = false
  let currentSegs: number[] = []

  for (const rawLine of text.split('\n')) {
    if (rawLine.startsWith('; CHANGE_LAYER')) {
      if (result.length > 0) {
        result[result.length - 1].endX = x
        result[result.length - 1].endY = y
        result[result.length - 1].segVerts = new Float32Array(currentSegs)
        currentSegs = []
      }
      layerChangeSeen = true
      continue
    }

    if (rawLine.startsWith('; Z_HEIGHT:')) {
      pendingZ = parseFloat(rawLine.slice(11).trim())
      if (layerChangeSeen && !isNaN(pendingZ)) {
        currentLayerZ = pendingZ
        result.push({ z: pendingZ, pct: 0, endX: x, endY: y, segVerts: new Float32Array(0) })
        layerChangeSeen = false
      }
      continue
    }

    if (rawLine.charCodeAt(0) === 59) continue

    const ci = rawLine.indexOf(';')
    const line = (ci >= 0 ? rawLine.slice(0, ci) : rawLine).trim()
    if (!line) continue

    const sp = line.indexOf(' ')
    const cmd = (sp >= 0 ? line.slice(0, sp) : line).toUpperCase()

    if (cmd === 'G90') { absolute = true; continue }
    if (cmd === 'G91') { absolute = false; continue }
    if (cmd !== 'G0' && cmd !== 'G1') continue

    const rest = sp >= 0 ? line.slice(sp + 1) : ''
    let nx = x, ny = y, ne = 0
    for (const p of rest.split(' ')) {
      if (!p) continue
      const v = parseFloat(p.slice(1))
      if (isNaN(v)) continue
      const ch = p[0].toUpperCase()
      if (ch === 'X') nx = absolute ? v : x + v
      else if (ch === 'Y') ny = absolute ? v : y + v
      else if (ch === 'E') ne = v
    }

    // Record extrusion segment (M83 relative E; positive E with XY movement = toolpath)
    if (cmd === 'G1' && ne > 0 && result.length > 0 && (Math.abs(nx - x) > 0.001 || Math.abs(ny - y) > 0.001)) {
      currentSegs.push(x, y, currentLayerZ, nx, ny, currentLayerZ)
    }

    x = nx; y = ny
  }

  if (result.length > 0) {
    result[result.length - 1].endX = x
    result[result.length - 1].endY = y
    result[result.length - 1].segVerts = new Float32Array(currentSegs)
  }

  if (result.length > 1) {
    const zMin = result[0].z
    const zRange = result[result.length - 1].z - zMin || 1
    for (const l of result) l.pct = ((l.z - zMin) / zRange) * 100
  } else if (result.length === 1) {
    result[0].pct = 100
  }

  return result
}

function clearLayerTrace() {
  if (curLayerLines) {
    scene?.remove(curLayerLines)
    curLayerGeo?.dispose()
    ;(curLayerLines.material as THREE.Material).dispose()
  }
  curLayerLines = null
  curLayerGeo = null
  animInternalSeg = 0
  animLayerProgress.value = 0
  lastLayerStarted = -1
}

// Build per-layer LineSegments with drawRange=0. segVerts holds GCode-space coords.
// GCodeLoader stores GCode (X,Y,Z) directly as Three.js (X,Y,Z) — no axis swap.
function startLayerAnim(idx: number) {
  if (curLayerLines) {
    scene?.remove(curLayerLines)
    curLayerGeo?.dispose()
    ;(curLayerLines.material as THREE.Material).dispose()
    curLayerLines = null
    curLayerGeo = null
  }
  animInternalSeg = 0
  animLayerProgress.value = 0
  lastLayerStarted = idx
  lastRenderedLayer = idx

  const layer = layerInfos[idx]
  if (!layer || !scene) return

  // Clip GCodeLoader model to layers below idx so the trace draws this layer cleanly
  layerMax.value = idx > 0 ? Math.min(100, layerInfos[idx - 1].pct + 0.5) : 0
  applyClipping()

  const src = layer.segVerts
  if (src.length === 0) return

  // Apply the same -PI/2 X rotation that GCodeLoader uses: GCode(X,Y,Z) → Three.js(X, Z, -Y),
  // then subtract the world-space bounding-box center used when centering the model.
  const world = new Float32Array(src.length)
  const cx = gcodeCenter.x, cy = gcodeCenter.y, cz = gcodeCenter.z
  for (let i = 0; i < src.length; i += 3) {
    world[i]     =  src[i]     - cx   // GCode X → Three.js X
    world[i + 1] =  src[i + 2] - cy   // GCode Z → Three.js Y (print height)
    world[i + 2] = -src[i + 1] - cz   // -GCode Y → Three.js Z
  }

  curLayerGeo = new THREE.BufferGeometry()
  curLayerGeo.setAttribute('position', new THREE.BufferAttribute(world, 3))
  curLayerGeo.setDrawRange(0, 0)
  const traceMat = new THREE.LineBasicMaterial({ color: 0xffffff, opacity: 0.95, transparent: true })
  curLayerLines = new THREE.LineSegments(curLayerGeo, traceMat)
  scene.add(curLayerLines)
}

function tickAnim() {
  const idx = animLayer.value
  const layer = layerInfos[idx]
  if (!layer) { pauseAnim(); return }

  const totalSegs = layer.segVerts.length / 6
  if (totalSegs === 0 || !curLayerLines || !curLayerGeo) { advanceToNextLayer(); return }

  const segsPerFrame = Math.max(1, Math.ceil(totalSegs * animSpeed.value / 300))
  animInternalSeg = Math.min(animInternalSeg + segsPerFrame, totalSegs)
  curLayerGeo.setDrawRange(0, animInternalSeg * 2)

  // Move nozzle to end of last drawn segment — apply same rotation as GCodeLoader
  if (animInternalSeg > 0 && nozzleMesh) {
    const si = (animInternalSeg - 1) * 6
    nozzleMesh.position.set(
       layer.segVerts[si + 3] - gcodeCenter.x,   // GCode X → Three.js X
       layer.segVerts[si + 5] - gcodeCenter.y,   // GCode Z → Three.js Y
      -layer.segVerts[si + 4] - gcodeCenter.z,   // -GCode Y → Three.js Z
    )
  }

  animLayerProgress.value = Math.round((animInternalSeg / totalSegs) * 100)
  if (animInternalSeg >= totalSegs) advanceToNextLayer()
}

function advanceToNextLayer() {
  if (animLayer.value >= animTotalLayers.value - 1) {
    const l = layerInfos[animLayer.value]
    if (l && nozzleMesh) {
      layerMax.value = 100
      applyClipping()
      nozzleMesh.position.set(l.endX - gcodeCenter.x, l.z - gcodeCenter.y, -l.endY - gcodeCenter.z)
    }
    pauseAnim()
    return
  }
  animLayer.value++
  startLayerAnim(animLayer.value)
}

function playAnim() {
  if (animPlaying.value) return
  if (animLayer.value >= animTotalLayers.value - 1) animLayer.value = 0
  animPlaying.value = true
}

function pauseAnim() {
  animPlaying.value = false
}

function toggleAnim() {
  animPlaying.value ? pauseAnim() : playAnim()
}

function stepAnim(delta: number) {
  if (!animPlaying.value) pauseAnim()
  animLayer.value = Math.max(0, Math.min(animTotalLayers.value - 1, animLayer.value + delta))
}

function goToFirstLayer() { pauseAnim(); animLayer.value = 0 }
function goToLastLayer()  { pauseAnim(); animLayer.value = animTotalLayers.value - 1 }

function initViewer(canvas: HTMLCanvasElement, gcodeText: string) {
  const w = canvas.clientWidth
  const h = canvas.clientHeight

  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.setSize(w, h, false)
  renderer.setClearColor(0x000000, 0)
  renderer.localClippingEnabled = true

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 100000)

  const loader = new GCodeLoader()
  const obj = loader.parse(gcodeText)

  // Measure Z range in local geometry space
  let zMin = Infinity, zMax = -Infinity
  obj.traverse(child => {
    if (child instanceof THREE.LineSegments) {
      const mat = child.material as THREE.LineBasicMaterial
      if (mat.name === 'extruded') {
        const pos = child.geometry.attributes.position as THREE.BufferAttribute
        for (let i = 0; i < pos.count; i++) {
          const z = pos.getZ(i)
          if (z < zMin) zMin = z
          if (z > zMax) zMax = z
        }
      }
    }
  })
  gcodeZMin = zMin === Infinity ? 0 : zMin
  gcodeZRange = (zMax - gcodeZMin) || 1

  // Swap extruding material to vertex-colours, collect geo refs
  vcMat = new THREE.LineBasicMaterial({ vertexColors: true })
  vcMat.name = 'extruded'
  extrudeGeos = []
  obj.traverse(child => {
    if (child instanceof THREE.LineSegments) {
      const mat = child.material as THREE.LineBasicMaterial
      if (mat.name === 'extruded') { extrudeGeos.push(child.geometry); child.material = vcMat }
      if (mat.name === 'path') { pathMat = mat; mat.visible = showTravel.value }
    }
  })
  applyVertexColors(new THREE.Color(extrudeColor.value))

  // Centre model
  const box = new THREE.Box3().setFromObject(obj)
  const center = box.getCenter(new THREE.Vector3())
  const size = box.getSize(new THREE.Vector3())
  gcodeCenter.copy(center)
  obj.position.sub(center)
  scene.add(obj)

  // GCodeLoader rotates group by -PI/2 on X: GCode Z (height) → Three.js Y, GCode Y → Three.js -Z.
  // Use only the extruded range for height so travel-path vertices (incl. initial Z=0) don't
  // push the grid below the actual first layer.
  worldTotalH = gcodeZRange
  modelBaseY = gcodeZMin - gcodeCenter.y   // world Y of the first extruded layer
  const gridSpan = Math.ceil(Math.max(size.x, size.z) * 1.3 / 10) * 10
  const divisions = Math.min(Math.ceil(gridSpan / 10), 30)
  const grid = new THREE.GridHelper(gridSpan, divisions, 0x1e4a5a, 0x1e4a5a)
  ;(grid.material as THREE.LineBasicMaterial).opacity = 0.5
  ;(grid.material as THREE.LineBasicMaterial).transparent = true
  // GridHelper lies in the XZ plane by default — correct for Y-up Three.js after the loader rotation
  grid.position.y = modelBaseY  // Align grid with bottom of extruded model
  scene.add(grid)

  applyClipping()

  // Camera: isometric-ish so top surface is visible
  const maxDim = Math.max(size.x, size.y, size.z)
  camera.position.set(maxDim * 1.0, maxDim * 1.1, maxDim * 1.4)
  camera.lookAt(0, 0, 0)

  // Lights for nozzle shading (LineBasicMaterial used by GCode is unaffected by lights)
  scene.add(new THREE.AmbientLight(0xffffff, 0.55))
  const dirLight = new THREE.DirectionalLight(0xffffff, 1.1)
  dirLight.position.set(1, 3, 2)
  scene.add(dirLight)

  // Nozzle: tip at group origin (the printing point), body extends upward
  const totalH = maxDim * 0.075
  const taperH = totalH * 0.68
  const bodyH  = totalH * 0.32
  const bodyR  = maxDim * 0.014
  const tipR   = maxDim * 0.0045

  const brassMat = new THREE.MeshStandardMaterial({ color: 0xd4a84b, metalness: 0.75, roughness: 0.30 })
  const steelMat = new THREE.MeshStandardMaterial({ color: 0x8899aa, metalness: 0.85, roughness: 0.25 })
  const hotMat   = new THREE.MeshBasicMaterial({ color: 0xff6600 })

  const nozzleGroup = new THREE.Group()

  // Tapered cone — tip (tipR) at Y=0, widens to bodyR at top
  const taperMesh = new THREE.Mesh(new THREE.CylinderGeometry(bodyR, tipR, taperH, 10), brassMat)
  taperMesh.position.y = taperH / 2
  nozzleGroup.add(taperMesh)

  // Cylindrical body above the taper
  const bodyMesh = new THREE.Mesh(new THREE.CylinderGeometry(bodyR * 1.15, bodyR, bodyH, 10), steelMat)
  bodyMesh.position.y = taperH + bodyH / 2
  nozzleGroup.add(bodyMesh)

  // Hot-tip dot — small glowing sphere at the extrusion point
  const tipDot = new THREE.Mesh(new THREE.SphereGeometry(tipR * 2.2, 8, 6), hotMat)
  nozzleGroup.add(tipDot)

  nozzleMesh = nozzleGroup
  scene.add(nozzleGroup)

  // Parse layers
  layerInfos = parseLayerInfos(gcodeText)
  animTotalLayers.value = layerInfos.length
  animLayer.value = layerInfos.length - 1
  lastRenderedLayer = -1  // force idle sync on first frame

  controls = new OrbitControls(camera, canvas)
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
    if (animPlaying.value) {
      // Start a new layer trace if animLayer changed (manual scrub or layer advance)
      if (animLayer.value !== lastLayerStarted) startLayerAnim(animLayer.value)
      tickAnim()
    } else {
      // Idle: sync nozzle + clip to the scrubber position
      const idx = animLayer.value
      if (idx !== lastRenderedLayer) {
        lastRenderedLayer = idx
        clearLayerTrace()
        const l = layerInfos[idx]
        if (l && nozzleMesh) {
          layerMax.value = idx >= layerInfos.length - 1 ? 100 : Math.min(100, l.pct + 1)
          applyClipping()
          nozzleMesh.position.set(l.endX - gcodeCenter.x, l.z - gcodeCenter.y, -l.endY - gcodeCenter.z)
        }
      }
    }
    controls!.update()
    renderer!.render(scene!, camera!)
  }
  animate()
}

// ── Settings helpers ──────────────────────────────────────────────────────
function first(val: unknown): string {
  if (Array.isArray(val)) return val[0] != null ? String(val[0]) : '—'
  return val != null ? String(val) : '—'
}
function get(key: string): string { return first(cfg.value?.[key]) }
function bedTemp(): string {
  const t = String(cfg.value?.curr_bed_type ?? '')
  if (t.includes('Textured')) return get('textured_plate_temp')
  if (t.includes('Engineering')) return get('eng_plate_temp')
  return get('hot_plate_temp')
}
function fmtId(raw: string): string { return raw.replace(/ ?@.*$/, '') }
function fmtNozzleType(raw: string): string {
  return raw.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}
function fmtBrimType(raw: string): string {
  return raw.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}
</script>

<template>
  <div class="pji-overlay" @click.self="emit('close')">
    <div class="pji-box">
      <div class="pji-header">
        <span class="pji-title">
          <i class="mdi mdi-information-outline" />
          {{ filename }}
        </span>
        <button class="pji-close" @click="emit('close')">
          <i class="mdi mdi-close" />
        </button>
      </div>

      <div v-if="loading" class="pji-state pji-state--loading">
        <div class="pji-stage-wrapper">
          <!-- Download graphic -->
          <div v-if="loadingStage === 'download'" class="pji-stage-graphic pji-stage-graphic--download">
            <i class="mdi mdi-cloud-outline pji-cloud-icon" />
            <div class="pji-download-dots">
              <span class="pji-dot" />
              <span class="pji-dot" />
              <span class="pji-dot" />
            </div>
          </div>

          <!-- Extract graphic -->
          <div v-else-if="loadingStage === 'extract'" class="pji-stage-graphic pji-stage-graphic--extract">
            <div class="pji-extract-rays">
              <span v-for="n in 8" :key="n" class="pji-ray" :style="`--ri:${n}`" />
            </div>
            <i class="mdi mdi-package-variant pji-pkg-icon" />
          </div>

          <!-- Parse graphic -->
          <div v-else class="pji-stage-graphic pji-stage-graphic--parse">
            <i class="mdi mdi-printer-3d-nozzle-outline pji-nozzle-icon" />
            <div class="pji-parse-layers">
              <span class="pji-parse-layer" style="--li:1;--lw:52px" />
              <span class="pji-parse-layer" style="--li:2;--lw:40px" />
              <span class="pji-parse-layer" style="--li:3;--lw:28px" />
              <span class="pji-parse-layer" style="--li:4;--lw:16px" />
            </div>
          </div>

          <div class="pji-loading-info">
            <span class="pji-loading-label">{{ loadingLabel }}</span>
            <span class="pji-loading-step">Step {{ loadingStageNum }} of 3</span>
          </div>

          <div v-if="loadingStage === 'download'" class="pji-progress-track">
            <div class="pji-progress-fill" :style="{ width: downloadProgress + '%' }" />
          </div>
          <span v-if="loadingStage === 'download' && (props.fileSize ?? 0) > 10 * 1024 * 1024" class="pji-loading-hint">
            This may take a bit…
          </span>
        </div>
      </div>
      <div v-else-if="error" class="pji-state pji-state--error">
        <i class="mdi mdi-alert-circle-outline" /> {{ error }}
      </div>

      <div v-else class="pji-body">
        <!-- Left panel: viewer -->
        <div class="pji-viewer">
          <!-- View mode tabs -->
          <div class="pji-view-tabs">
            <button
              class="pji-view-tab"
              :class="{ 'pji-view-tab--active': viewMode === 'gcode' }"
              @click="viewMode = 'gcode'"
            >
              <i class="mdi mdi-rotate-3d-variant" /> GCode
            </button>
            <button
              class="pji-view-tab"
              :class="{ 'pji-view-tab--active': viewMode === 'image' }"
              :disabled="!plateImageUrl"
              @click="viewMode = 'image'"
            >
              <i class="mdi mdi-image-outline" /> Preview
            </button>
          </div>

          <!-- GCode canvas -->
          <template v-if="viewMode === 'gcode'">
            <canvas ref="canvasRef" class="pji-canvas" />

            <!-- Viewer controls -->
            <div class="pji-viewer-controls">
              <label class="pji-ctrl" title="Extrusion color">
                <span class="pji-ctrl-label">Color</span>
                <input type="color" v-model="extrudeColor" class="pji-color-input" />
              </label>
              <label class="pji-ctrl pji-ctrl--toggle" title="Show travel moves">
                <input type="checkbox" v-model="showTravel" class="pji-toggle-cb" />
                <span class="pji-toggle-track" />
                <span class="pji-ctrl-label">Travel</span>
              </label>
              <div class="pji-ctrl pji-ctrl--layers">
                <span class="pji-ctrl-label">Layers</span>
                <div class="pji-layer-sliders">
                  <div class="pji-layer-row">
                    <span class="pji-layer-lbl">Min</span>
                    <input type="range" v-model.number="layerMin" min="0" max="100"
                      :style="{ '--fill': layerMin + '%' }" class="pji-range" />
                    <span class="pji-layer-val">{{ layerMin }}%</span>
                  </div>
                  <div class="pji-layer-row">
                    <span class="pji-layer-lbl">Max</span>
                    <input type="range" v-model.number="layerMax" min="0" max="100"
                      :style="{ '--fill': layerMax + '%' }" class="pji-range" />
                    <span class="pji-layer-val">{{ layerMax }}%</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Animation controls -->
            <div class="pji-anim-controls" v-if="animTotalLayers > 0">
              <div class="pji-anim-toolbar">
                <button class="pji-anim-btn" title="First layer" @click="goToFirstLayer">
                  <i class="mdi mdi-skip-previous" />
                </button>
                <button class="pji-anim-btn" title="Previous layer" @click="stepAnim(-1)">
                  <i class="mdi mdi-chevron-left" />
                </button>
                <button class="pji-anim-btn pji-anim-btn--play" :title="animPlaying ? 'Pause' : 'Play'" @click="toggleAnim">
                  <i :class="animPlaying ? 'mdi mdi-pause' : 'mdi mdi-play'" />
                </button>
                <button class="pji-anim-btn" title="Next layer" @click="stepAnim(1)">
                  <i class="mdi mdi-chevron-right" />
                </button>
                <button class="pji-anim-btn" title="Last layer" @click="goToLastLayer">
                  <i class="mdi mdi-skip-next" />
                </button>

                <span class="pji-anim-layer-count">
                  Layer {{ animLayer + 1 }}&thinsp;/&thinsp;{{ animTotalLayers }}
                  <template v-if="animPlaying">&nbsp;·&nbsp;{{ animLayerProgress }}%</template>
                </span>
              </div>

              <div class="pji-anim-row2">
                <div class="pji-anim-speed-group">
                  <span class="pji-ctrl-label">Speed</span>
                  <button
                    v-for="s in [1, 5, 10, 30]" :key="s"
                    class="pji-speed-btn"
                    :class="{ 'pji-speed-btn--active': animSpeed === s }"
                    @click="animSpeed = s"
                  >{{ s }}x</button>
                </div>
                <input
                  type="range"
                  class="pji-range"
                  v-model.number="animLayer"
                  min="0"
                  :max="animTotalLayers - 1"
                  :style="{ '--fill': animTotalLayers > 1 ? (animLayer / (animTotalLayers - 1) * 100) + '%' : '100%' }"
                />
              </div>
            </div>

          </template>

          <!-- Plate image -->
          <div v-else class="pji-image-wrap">
            <img :src="plateImageUrl!" alt="Plate preview" class="pji-plate-img" />
          </div>
        </div>

        <!-- Settings panels -->
        <div class="pji-settings">
          <div class="pji-group">
            <div class="pji-group-label"><i class="mdi mdi-layers-outline" /> Print</div>
            <div class="pji-stats">
              <div class="pji-stat pji-stat--wide">
                <span class="pji-sk">Profile</span>
                <span class="pji-sv">{{ fmtId(get('print_settings_id')) }}</span>
              </div>
              <div class="pji-stat"><span class="pji-sk">Layer</span><span class="pji-sv">{{ get('layer_height') }} mm</span></div>
              <div class="pji-stat"><span class="pji-sk">1st Layer</span><span class="pji-sv">{{ get('initial_layer_print_height') }} mm</span></div>
              <div class="pji-stat"><span class="pji-sk">Walls</span><span class="pji-sv">{{ get('wall_loops') }}</span></div>
              <div class="pji-stat"><span class="pji-sk">Top / Bot</span><span class="pji-sv">{{ get('top_shell_layers') }} / {{ get('bottom_shell_layers') }}</span></div>
              <div class="pji-stat"><span class="pji-sk">Infill</span><span class="pji-sv">{{ get('sparse_infill_density') }} · {{ get('sparse_infill_pattern') }}</span></div>
            </div>
          </div>

          <div class="pji-group">
            <div class="pji-group-label"><i class="mdi mdi-palette-outline" /> Material</div>
            <div class="pji-stats">
              <div class="pji-stat">
                <span class="pji-sk">Type</span>
                <span class="pji-sv pji-sv--row">
                  <span class="pji-swatch" :style="{ background: get('filament_colour') }" />
                  {{ get('filament_type') }}
                </span>
              </div>
              <div class="pji-stat pji-stat--wide">
                <span class="pji-sk">Profile</span>
                <span class="pji-sv">{{ fmtId(get('filament_settings_id')) }}</span>
              </div>
              <div class="pji-stat"><span class="pji-sk">Nozzle Temp</span><span class="pji-sv">{{ get('nozzle_temperature') }}°C</span></div>
              <div class="pji-stat"><span class="pji-sk">Bed Temp</span><span class="pji-sv">{{ bedTemp() }}°C</span></div>
            </div>
          </div>

          <div class="pji-group">
            <div class="pji-group-label"><i class="mdi mdi-printer-3d" /> Hardware</div>
            <div class="pji-stats">
              <div class="pji-stat pji-stat--wide"><span class="pji-sk">Printer</span><span class="pji-sv">{{ get('printer_model') }}</span></div>
              <div class="pji-stat"><span class="pji-sk">Nozzle</span><span class="pji-sv">{{ get('nozzle_diameter') }} mm · {{ fmtNozzleType(get('nozzle_type')) }}</span></div>
              <div class="pji-stat pji-stat--wide"><span class="pji-sk">Bed</span><span class="pji-sv">{{ String(cfg?.curr_bed_type ?? '—') }}</span></div>
            </div>
          </div>

          <div class="pji-group">
            <div class="pji-group-label"><i class="mdi mdi-tune" /> Features</div>
            <div class="pji-stats">
              <div class="pji-stat">
                <span class="pji-sk">Support</span>
                <span class="pji-sv" style="text-transform:capitalize">
                  {{ cfg?.enable_support === '1' ? get('support_type').replace(/[()]/g, '') : 'None' }}
                </span>
              </div>
              <div class="pji-stat"><span class="pji-sk">Brim</span><span class="pji-sv">{{ fmtBrimType(get('brim_type')) }}</span></div>
              <div class="pji-stat"><span class="pji-sk">Prime Tower</span><span class="pji-sv">{{ cfg?.enable_prime_tower === '1' ? 'Yes' : 'No' }}</span></div>
              <div class="pji-stat"><span class="pji-sk">Seam</span><span class="pji-sv" style="text-transform:capitalize">{{ get('seam_position') }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pji-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.pji-box {
  width: 100%;
  max-width: 1280px;
  max-height: 90vh;
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

/* ── Header ─────────────────────────────────────────────────────────── */
.pji-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ph-border);
  flex-shrink: 0;
}

.pji-title {
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

.pji-close {
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
.pji-close:hover { color: var(--ph-text); background: rgba(255,255,255,0.07); }

/* ── Loading / error states ──────────────────────────────────────────── */
.pji-state {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 3rem;
  justify-content: center;
  font-size: 0.85rem;
  color: var(--ph-text-muted);
}
.pji-state--error { color: #f87171; }

.pji-state--loading {
  flex: 1;
  align-items: center;
  justify-content: center;
}

/* ── Stage wrapper ───────────────────────────────────────────────────── */
.pji-stage-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.25rem;
  min-width: 200px;
}

/* ── Common stage graphic shell ──────────────────────────────────────── */
.pji-stage-graphic {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
}

/* ── Download stage ──────────────────────────────────────────────────── */
.pji-stage-graphic--download {
  gap: 6px;
}

.pji-cloud-icon {
  font-size: 3rem;
  line-height: 1;
  color: var(--ph-accent, #22d3ee);
  animation: cloud-pulse 2s ease-in-out infinite;
}

@keyframes cloud-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%       { opacity: 0.65; transform: scale(0.95); }
}

.pji-download-dots {
  display: flex;
  gap: 7px;
}

.pji-dot {
  display: block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--ph-accent, #22d3ee);
  animation: dot-fall 1.3s ease-in-out infinite;
}
.pji-dot:nth-child(1) { animation-delay: 0s; }
.pji-dot:nth-child(2) { animation-delay: 0.2s; }
.pji-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes dot-fall {
  0%   { transform: translateY(-6px); opacity: 0; }
  35%  { transform: translateY(0);    opacity: 1; }
  65%  { transform: translateY(0);    opacity: 1; }
  100% { transform: translateY(8px);  opacity: 0; }
}

/* ── Extract stage ───────────────────────────────────────────────────── */
.pji-stage-graphic--extract {
  align-items: center;
  justify-content: center;
}

.pji-pkg-icon {
  font-size: 3rem;
  line-height: 1;
  color: var(--ph-accent, #22d3ee);
  position: relative;
  z-index: 1;
  animation: pkg-pulse 1.4s ease-in-out infinite;
}

@keyframes pkg-pulse {
  0%, 100% { transform: scale(1);    filter: brightness(1); }
  50%       { transform: scale(1.12); filter: brightness(1.3); }
}

.pji-extract-rays {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.pji-ray {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 3px;
  height: 12px;
  border-radius: 2px;
  background: var(--ph-accent, #22d3ee);
  transform-origin: 50% 0%;
  transform: translateX(-50%) rotate(calc((var(--ri) - 1) * 45deg)) translateY(-42px);
  animation: ray-burst 1.4s ease-in-out infinite;
  animation-delay: calc((var(--ri) - 1) * 0.07s);
}

@keyframes ray-burst {
  0%   { opacity: 0; transform: translateX(-50%) rotate(calc((var(--ri) - 1) * 45deg)) translateY(-34px) scaleY(0.3); }
  40%  { opacity: 1; transform: translateX(-50%) rotate(calc((var(--ri) - 1) * 45deg)) translateY(-44px) scaleY(1); }
  70%  { opacity: 0.6; }
  100% { opacity: 0; transform: translateX(-50%) rotate(calc((var(--ri) - 1) * 45deg)) translateY(-52px) scaleY(0.5); }
}

/* ── Parse stage ─────────────────────────────────────────────────────── */
.pji-stage-graphic--parse {
  height: auto;
  gap: 6px;
}

.pji-nozzle-icon {
  font-size: 2.6rem;
  line-height: 1;
  color: var(--ph-accent, #22d3ee);
  animation: nozzle-sweep 1.6s ease-in-out infinite;
}

@keyframes nozzle-sweep {
  0%, 100% { transform: translateX(-14px); }
  50%       { transform: translateX(14px); }
}

.pji-parse-layers {
  display: flex;
  flex-direction: column-reverse;
  gap: 4px;
  align-items: center;
}

.pji-parse-layer {
  display: block;
  width: var(--lw);
  height: 5px;
  border-radius: 3px;
  background: var(--ph-accent, #22d3ee);
  animation: layer-appear 1.6s ease-out infinite;
  animation-delay: calc((var(--li) - 1) * 0.2s);
  opacity: 0;
}

@keyframes layer-appear {
  0%   { opacity: 0; transform: scaleX(0); }
  25%  { opacity: 1; transform: scaleX(1); }
  75%  { opacity: 1; transform: scaleX(1); }
  100% { opacity: 0.3; transform: scaleX(1); }
}

/* ── Loading info (label + step) ─────────────────────────────────────── */
.pji-loading-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.pji-loading-label {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--ph-text);
}

.pji-loading-step {
  font-size: 0.72rem;
  color: var(--ph-text-muted);
  opacity: 0.6;
}

/* ── Progress bar (download stage) ──────────────────────────────────── */
.pji-progress-track {
  width: 100%;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.pji-progress-fill {
  height: 100%;
  border-radius: 2px;
  background: var(--ph-accent, #22d3ee);
  transition: width 0.2s ease;
}

.pji-loading-hint {
  font-size: 0.72rem;
  color: var(--ph-text-muted);
  opacity: 0.6;
}

/* ── Body layout ─────────────────────────────────────────────────────── */
.pji-body {
  display: grid;
  grid-template-columns: 600px 1fr;
  overflow: hidden;
  flex: 1;
  min-height: 0;
}
@media (max-width: 720px) {
  .pji-body { grid-template-columns: 1fr; grid-template-rows: 320px 1fr; }
}

/* ── Left viewer panel ───────────────────────────────────────────────── */
.pji-viewer {
  position: relative;
  border-right: 1px solid var(--ph-border);
  background: #060f14;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ── View mode tabs ──────────────────────────────────────────────────── */
.pji-view-tabs {
  display: flex;
  flex-shrink: 0;
  border-bottom: 1px solid var(--ph-border);
}

.pji-view-tab {
  flex: 1;
  background: none;
  border: none;
  padding: 0.45rem 0;
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  transition: color 0.15s, background 0.15s;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.pji-view-tab:hover:not(:disabled) { color: var(--ph-text); background: rgba(255,255,255,0.04); }
.pji-view-tab--active { color: var(--ph-accent, #22d3ee); border-bottom-color: var(--ph-accent, #22d3ee); }
.pji-view-tab:disabled { opacity: 0.3; cursor: not-allowed; }

/* ── Canvas ──────────────────────────────────────────────────────────── */
.pji-canvas {
  flex: 1;
  width: 100%;
  min-height: 0;
  display: block;
}


/* ── Viewer controls bar ─────────────────────────────────────────────── */
.pji-viewer-controls {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.6rem 0.75rem;
  border-top: 1px solid var(--ph-border);
  background: rgba(0,0,0,0.3);
  flex-wrap: wrap;
}

.pji-ctrl {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  cursor: pointer;
}

.pji-ctrl-label {
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
  white-space: nowrap;
}

/* Color input */
.pji-color-input {
  width: 28px;
  height: 20px;
  padding: 0;
  border: 1px solid var(--ph-border);
  border-radius: 4px;
  background: none;
  cursor: pointer;
}
.pji-color-input::-webkit-color-swatch-wrapper { padding: 2px; }
.pji-color-input::-webkit-color-swatch { border: none; border-radius: 3px; }

/* Travel toggle */
.pji-ctrl--toggle { gap: 0.5rem; }

.pji-toggle-cb {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.pji-toggle-track {
  position: relative;
  width: 30px;
  height: 17px;
  border-radius: 999px;
  background: rgba(255,255,255,0.12);
  flex-shrink: 0;
  transition: background 0.2s;
}
.pji-toggle-track::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s;
}
.pji-toggle-cb:checked + .pji-toggle-track { background: var(--ph-accent, #22d3ee); }
.pji-toggle-cb:checked + .pji-toggle-track::after { transform: translateX(13px); }

/* Layer sliders */
.pji-ctrl--layers {
  flex: 1;
  min-width: 160px;
  align-items: flex-start;
  flex-direction: column;
  gap: 0.25rem;
}

.pji-layer-sliders {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  width: 100%;
}

.pji-layer-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.pji-layer-lbl {
  font-size: 0.62rem;
  color: var(--ph-text-muted);
  width: 22px;
  flex-shrink: 0;
}

.pji-layer-val {
  font-size: 0.62rem;
  color: var(--ph-text-muted);
  width: 28px;
  text-align: right;
  flex-shrink: 0;
}

.pji-range {
  flex: 1;
  -webkit-appearance: none;
  appearance: none;
  height: 4px;
  border-radius: 2px;
  background: linear-gradient(
    to right,
    var(--ph-accent, #22d3ee) 0%,
    var(--ph-accent, #22d3ee) var(--fill, 50%),
    rgba(255,255,255,0.12) var(--fill, 50%),
    rgba(255,255,255,0.12) 100%
  );
  outline: none;
  cursor: pointer;
}
.pji-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: var(--ph-accent, #22d3ee);
  border: 2px solid #0f2027;
  cursor: pointer;
}
.pji-range::-moz-range-thumb {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: var(--ph-accent, #22d3ee);
  border: 2px solid #0f2027;
  cursor: pointer;
}

/* ── Animation controls ──────────────────────────────────────────────── */
.pji-anim-controls {
  flex-shrink: 0;
  padding: 0.45rem 0.75rem 0.55rem;
  border-top: 1px solid var(--ph-border);
  background: rgba(0,0,0,0.25);
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.pji-anim-toolbar {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.pji-anim-btn {
  background: rgba(255,255,255,0.05);
  border: 1px solid var(--ph-border);
  border-radius: 5px;
  color: var(--ph-text-muted);
  cursor: pointer;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  transition: color 0.15s, background 0.15s;
  flex-shrink: 0;
  padding: 0;
}
.pji-anim-btn:hover { color: var(--ph-text); background: rgba(255,255,255,0.1); }

.pji-anim-btn--play {
  width: 30px;
  height: 30px;
  color: var(--ph-accent, #22d3ee);
  border-color: var(--ph-accent, #22d3ee);
  font-size: 1rem;
}
.pji-anim-btn--play:hover {
  background: rgba(34,211,238,0.12);
  color: var(--ph-accent, #22d3ee);
}

.pji-anim-layer-count {
  font-size: 0.67rem;
  color: var(--ph-text-muted);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  margin: 0 0.25rem;
}

.pji-anim-row2 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pji-anim-row2 .pji-range {
  flex: 1;
  min-width: 0;
}

.pji-anim-speed-group {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-shrink: 0;
}

.pji-speed-btn {
  background: rgba(255,255,255,0.05);
  border: 1px solid var(--ph-border);
  border-radius: 4px;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 0.6rem;
  font-weight: 600;
  padding: 0.15rem 0.35rem;
  transition: color 0.15s, background 0.15s;
}
.pji-speed-btn:hover { color: var(--ph-text); background: rgba(255,255,255,0.1); }
.pji-speed-btn--active {
  color: var(--ph-accent, #22d3ee);
  border-color: var(--ph-accent, #22d3ee);
  background: rgba(34,211,238,0.1);
}

/* ── Plate image ─────────────────────────────────────────────────────── */
.pji-image-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  min-height: 0;
}

.pji-plate-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 6px;
}

/* ── Settings scroll area ────────────────────────────────────────────── */
.pji-settings {
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  scrollbar-width: thin;
  scrollbar-color: rgba(255,255,255,0.1) transparent;
}

/* ── Group ───────────────────────────────────────────────────────────── */
.pji-group-label {
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ph-text-muted);
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.pji-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.375rem;
}

/* ── Stat cell ───────────────────────────────────────────────────────── */
.pji-stat {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  padding: 0.4rem 0.625rem;
  border-radius: 7px;
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--ph-border);
  min-width: 0;
}
.pji-stat--wide { grid-column: span 3; }

.pji-sk {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
}

.pji-sv {
  font-size: 0.82rem;
  font-weight: 500;
  color: var(--ph-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pji-sv--row { display: flex; align-items: center; gap: 0.4rem; }

/* ── Color swatch ────────────────────────────────────────────────────── */
.pji-swatch {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(255,255,255,0.15);
}

/* ── Dialog open/close transition ───────────────────────────────────── */
.pji-dialog-enter-active { transition: opacity 0.25s ease; }
.pji-dialog-leave-active { transition: opacity 0.2s ease; }
.pji-dialog-enter-from,
.pji-dialog-leave-to { opacity: 0; }

.pji-dialog-enter-active .pji-box {
  transition: transform 0.28s cubic-bezier(0.34, 1.3, 0.64, 1), opacity 0.25s ease;
}
.pji-dialog-leave-active .pji-box {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.pji-dialog-enter-from .pji-box,
.pji-dialog-leave-to .pji-box {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
}
</style>
