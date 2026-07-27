<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js'

export interface ViewerFile {
  filename: string
  fetchFile: () => Promise<ArrayBuffer>
}

const props = defineProps<{ files: ViewerFile[] }>()
const emit = defineEmits<{ close: [] }>()

// glTF/GLB is nominally meters; measurements are converted for display only
type Unit = 'mm' | 'cm' | 'm' | 'in'
const UNITS: Unit[] = ['mm', 'cm', 'm', 'in']
const UNIT_MM: Record<Unit, number> = { mm: 1, cm: 10, m: 1000, in: 25.4 }
const MODEL_UNIT_MM = 1000 // file unit (meters) expressed in mm
const displayUnit = ref<Unit>('mm')
const unitScale = computed(() => MODEL_UNIT_MM / UNIT_MM[displayUnit.value])

const loading = ref(true)
const loadProgress = ref('')
const error = ref('')
const canvasRef = ref<HTMLCanvasElement | null>(null)
const cubeCanvasRef = ref<HTMLCanvasElement | null>(null)

type Tool = 'orbit' | 'distance' | 'surface'
const tool = ref<Tool>('orbit')
const wireframe = ref(false)
const showGrid = ref(true)
const showDims = ref(false)
const showInfo = ref(false)
const autoRotate = ref(false)
const orthographic = ref(false)
const panelOpen = ref(true)
const clipEnabled = ref(false)
const clipAxis = ref<'x' | 'y' | 'z'>('y')
const clipPos = ref(100)
const explodeEnabled = ref(false)
const explodeAmount = ref(0)

interface PartEntry {
  id: number
  name: string
  visible: boolean
}
const parts = ref<PartEntry[]>([])
const isolatedPartId = ref<number | null>(null)

interface Measurement {
  dist: number
  dx: number
  dy: number
  dz: number
}
const measurement = ref<Measurement | null>(null)
const surfaceArea = ref<number | null>(null)

interface ScreenLabel {
  x: number
  y: number
  text: string
  kind: 'total' | 'dx' | 'dy' | 'dz' | 'area'
}
const screenLabels = ref<ScreenLabel[]>([])

const dims = ref<{ x: number; y: number; z: number } | null>(null)
const stats = ref<{ tris: number; verts: number; volume: number; area: number } | null>(null)

const title = computed(() =>
  props.files.length === 1 ? props.files[0].filename : `Assembly · ${props.files.length} files`,
)

const hint = computed(() => {
  if (tool.value === 'distance') return 'Click two points — snaps to vertices and edges'
  if (tool.value === 'surface') return 'Click a face to measure its surface area'
  return 'Drag to orbit · Scroll to zoom · Right-drag to pan'
})

// Deltas below this fraction of the model size count as "same plane" (no triangle leg)
const AXIS_EPS_FACTOR = 1e-4
const axisEps = () => modelMaxDim * AXIS_EPS_FACTOR

const measureRows = computed(() => {
  const m = measurement.value
  if (!m) return []
  const rows: { key: string; label: string; value: string; cls: string }[] = [
    { key: 'total', label: 'Distance', value: fmtLen(m.dist), cls: 'total' },
  ]
  const eps = axisEps()
  const axes = [
    { key: 'dx', label: 'ΔX', v: m.dx, cls: 'dx' },
    { key: 'dy', label: 'ΔY', v: m.dy, cls: 'dy' },
    { key: 'dz', label: 'ΔZ', v: m.dz, cls: 'dz' },
  ].filter((a) => a.v > eps)
  // Component deltas only matter when the segment isn't axis-aligned
  if (axes.length > 1) {
    for (const a of axes) rows.push({ key: a.key, label: a.label, value: fmtLen(a.v), cls: a.cls })
  }
  return rows
})

// ── Three.js internals (non-reactive) ─────────────────────────────────────
let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let perspCamera: THREE.PerspectiveCamera | null = null
let orthoCamera: THREE.OrthographicCamera | null = null
let activeCamera: THREE.Camera | null = null
let controls: OrbitControls | null = null
let animFrameId: number | null = null
let resizeObserver: ResizeObserver | null = null
let pmrem: THREE.PMREMGenerator | null = null
let modelRoot: THREE.Group | null = null
let partNodes: THREE.Object3D[] = []
let grid: THREE.GridHelper | null = null
let canvasEl: HTMLCanvasElement | null = null
let initialCameraPos = new THREE.Vector3()
let initialTarget = new THREE.Vector3()
let modelMaxDim = 1
const worldBox = new THREE.Box3()
let boxHelper: THREE.Box3Helper | null = null
let measureGroup: THREE.Group | null = null
let measurePoints: THREE.Vector3[] = []
let clipPlane: THREE.Plane | null = null
const raycaster = new THREE.Raycaster()
let labelAnchors: { pos: THREE.Vector3; kind: ScreenLabel['kind'] }[] = []
let pointerDownPos = { x: 0, y: 0 }

// View cube
let cubeRenderer: THREE.WebGLRenderer | null = null
let cubeScene: THREE.Scene | null = null
let cubeCamera: THREE.OrthographicCamera | null = null
let cubeMesh: THREE.Mesh | null = null
let cubeHover = -1
const cubeRaycaster = new THREE.Raycaster()

// Camera fly-to animation
let camAnim: {
  t0: number
  duration: number
  fromDir: THREE.Vector3
  toDir: THREE.Vector3
  dist: number
} | null = null

onMounted(async () => {
  try {
    const buffers = await Promise.all(
      props.files.map(async (f, i) => {
        loadProgress.value = props.files.length > 1 ? `Loading ${i + 1} / ${props.files.length}…` : ''
        return f.fetchFile()
      }),
    )
    const objects = await Promise.all(
      buffers.map((buf, i) => parseModel(buf, props.files[i].filename)),
    )
    loading.value = false
    await nextTick()
    if (canvasRef.value) initViewer(canvasRef.value, objects)
    window.addEventListener('keydown', onKeydown)
  } catch (e: unknown) {
    error.value = (e as Error)?.message ?? 'Failed to load model'
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  teardown()
})

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    if (tool.value !== 'orbit') {
      setTool('orbit')
    } else {
      emit('close')
    }
  }
}

/** Returns true when the filename has an extension the 3D viewer can render */
function fileExt(name: string): string {
  return name.toLowerCase().split('.').pop() ?? ''
}

function parseModel(buffer: ArrayBuffer, filename: string): Promise<THREE.Object3D> {
  const ext = fileExt(filename)
  if (ext !== 'glb' && ext !== 'gltf') {
    return Promise.reject(new Error(`Unsupported model format: .${ext} (only .glb/.gltf)`))
  }
  return new Promise((resolve, reject) => {
    const loader = new GLTFLoader()
    const data = ext === 'gltf' ? new TextDecoder().decode(buffer) : buffer
    loader.parse(
      data,
      '',
      (gltf) => {
        gltf.scene.name = filename
        resolve(gltf.scene)
      },
      (err) =>
        reject(
          err instanceof Error
            ? new Error(`${filename}: ${err.message}`)
            : new Error(`Failed to parse ${filename}`),
        ),
    )
  })
}

function initViewer(canvas: HTMLCanvasElement, objects: THREE.Object3D[]) {
  const w = canvas.clientWidth
  const h = canvas.clientHeight
  canvasEl = canvas

  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.setSize(w, h, false)
  renderer.setClearColor(0x000000, 0)
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.localClippingEnabled = true

  scene = new THREE.Scene()

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

  modelRoot = new THREE.Group()
  for (const obj of objects) modelRoot.add(obj)
  scene.add(modelRoot)

  // Center the assembly on the origin, resting on the grid plane
  const box = new THREE.Box3().setFromObject(modelRoot)
  const center = box.getCenter(new THREE.Vector3())
  const size = box.getSize(new THREE.Vector3())
  modelRoot.position.set(-center.x, -box.min.y, -center.z)
  modelRoot.updateWorldMatrix(true, true)
  worldBox.setFromObject(modelRoot)
  dims.value = { x: size.x, y: size.y, z: size.z }
  stats.value = computeMeshStats(modelRoot)
  modelMaxDim = Math.max(size.x, size.y, size.z) || 1

  setupParts(objects)

  grid = buildGrid(Math.max(size.x, size.z) || 1)
  scene.add(grid)

  measureGroup = new THREE.Group()
  scene.add(measureGroup)

  perspCamera = new THREE.PerspectiveCamera(45, w / h, modelMaxDim / 100, modelMaxDim * 100)
  perspCamera.position.set(modelMaxDim * 1.1, modelMaxDim * 0.9, modelMaxDim * 1.5)
  orthoCamera = new THREE.OrthographicCamera(-1, 1, 1, -1, -modelMaxDim * 100, modelMaxDim * 100)
  activeCamera = perspCamera
  initialCameraPos = perspCamera.position.clone()
  initialTarget = new THREE.Vector3(0, size.y / 2, 0)

  controls = new OrbitControls(perspCamera, canvas)
  controls.target.copy(initialTarget)
  controls.enableDamping = true
  controls.dampingFactor = 0.06
  controls.autoRotateSpeed = 2.5

  canvas.addEventListener('pointerdown', onPointerDown)
  canvas.addEventListener('pointerup', onPointerUp)

  resizeObserver = new ResizeObserver(() => {
    if (!canvas || !renderer) return
    const nw = canvas.clientWidth
    const nh = canvas.clientHeight
    if (nw === 0 || nh === 0) return
    renderer.setSize(nw, nh, false)
    if (perspCamera) {
      perspCamera.aspect = nw / nh
      perspCamera.updateProjectionMatrix()
    }
    syncOrthoFrustum()
  })
  resizeObserver.observe(canvas)

  initViewCube()

  const animate = () => {
    animFrameId = requestAnimationFrame(animate)
    stepCameraAnimation()
    controls!.update()
    updateScreenLabels()
    renderer!.render(scene!, activeCamera!)
    renderViewCube()
  }
  animate()
}

/** Grid with a "nice" (1/2/5 × 10ⁿ) cell size scaled to the model footprint */
function buildGrid(footprint: number): THREE.GridHelper {
  const rawStep = (footprint * 1.4) / 10
  const mag = Math.pow(10, Math.floor(Math.log10(rawStep)))
  const norm = rawStep / mag
  const step = (norm < 1.5 ? 1 : norm < 3.5 ? 2 : norm < 7.5 ? 5 : 10) * mag
  const divisions = 14
  const g = new THREE.GridHelper(step * divisions, divisions, 0x1e4a5a, 0x1e4a5a)
  const mat = g.material as THREE.LineBasicMaterial
  mat.opacity = 0.5
  mat.transparent = true
  return g
}

/**
 * Assembly parts shown in the side panel: one entry per uploaded file, or —
 * for a single file — its named direct children when it contains several.
 */
function setupParts(objects: THREE.Object3D[]) {
  let nodes: THREE.Object3D[] = objects
  if (objects.length === 1) {
    const children = objects[0].children.filter((c) => containsMesh(c))
    if (children.length > 1) nodes = children
  }
  partNodes = nodes
  const overallCenter = worldBox.getCenter(new THREE.Vector3())
  const nodeBox = new THREE.Box3()
  parts.value = nodes.map((node, i) => {
    // Remember rest position and explode direction for the explode slider
    nodeBox.setFromObject(node)
    const dir = nodeBox.getCenter(new THREE.Vector3()).sub(overallCenter)
    node.userData.explodeBase = node.position.clone()
    node.userData.explodeDir = dir.lengthSq() > 0 ? dir.normalize() : new THREE.Vector3()
    return {
      id: i,
      name: node.name || props.files[Math.min(i, props.files.length - 1)]?.filename || `Part ${i + 1}`,
      visible: true,
    }
  })
}

function containsMesh(node: THREE.Object3D): boolean {
  if (node instanceof THREE.Mesh) return true
  return node.children.some(containsMesh)
}

/** Triangle/vertex counts plus signed-tetrahedron volume and surface area in file units */
function computeMeshStats(root: THREE.Object3D) {
  let tris = 0
  let verts = 0
  let volume = 0
  let area = 0
  const vA = new THREE.Vector3()
  const vB = new THREE.Vector3()
  const vC = new THREE.Vector3()
  const ab = new THREE.Vector3()
  const ac = new THREE.Vector3()
  const cross = new THREE.Vector3()
  root.traverse((child) => {
    if (!(child instanceof THREE.Mesh)) return
    const geom = child.geometry as THREE.BufferGeometry
    const pos = geom.getAttribute('position')
    if (!pos) return
    verts += pos.count
    const index = geom.getIndex()
    const count = index ? index.count : pos.count
    tris += count / 3
    for (let i = 0; i < count; i += 3) {
      const a = index ? index.getX(i) : i
      const b = index ? index.getX(i + 1) : i + 1
      const c = index ? index.getX(i + 2) : i + 2
      vA.fromBufferAttribute(pos, a).applyMatrix4(child.matrixWorld)
      vB.fromBufferAttribute(pos, b).applyMatrix4(child.matrixWorld)
      vC.fromBufferAttribute(pos, c).applyMatrix4(child.matrixWorld)
      volume += vA.dot(cross.crossVectors(vB, vC)) / 6
      area += ab.subVectors(vB, vA).cross(ac.subVectors(vC, vA)).length() / 2
    }
  })
  return { tris: Math.round(tris), verts, volume: Math.abs(volume), area }
}

// ── Unit-aware formatting (raw values are in file units; converted for display) ──

function fmtAdaptive(v: number): string {
  if (v === 0) return '0'
  if (v >= 1000) return v.toLocaleString(undefined, { maximumFractionDigits: 0 })
  if (v >= 1) return v.toFixed(2)
  return v.toPrecision(3)
}

function fmtLen(v: number): string {
  return `${fmtAdaptive(v * unitScale.value)} ${displayUnit.value}`
}

function fmtArea(v: number): string {
  const conv = v * unitScale.value ** 2
  if (displayUnit.value === 'mm' && conv >= 1000) return `${fmtAdaptive(conv / 100)} cm²`
  return `${fmtAdaptive(conv)} ${displayUnit.value}²`
}

function fmtVol(v: number): string {
  const conv = v * unitScale.value ** 3
  if (displayUnit.value === 'mm' && conv >= 1000) return `${fmtAdaptive(conv / 1000)} cm³`
  return `${fmtAdaptive(conv)} ${displayUnit.value}³`
}

function fmtNum(n: number): string {
  return n.toLocaleString(undefined, { maximumFractionDigits: 0 })
}

// ── Tools ──────────────────────────────────────────────────────────────────

function setTool(next: Tool) {
  tool.value = tool.value === next ? 'orbit' : next
  clearMeasurement()
}

function onPointerDown(e: PointerEvent) {
  pointerDownPos = { x: e.clientX, y: e.clientY }
}

function onPointerUp(e: PointerEvent) {
  if (tool.value === 'orbit' || !activeCamera || !modelRoot || !canvasEl) return
  // Ignore orbit drags; only treat near-stationary clicks as point picks
  if (Math.hypot(e.clientX - pointerDownPos.x, e.clientY - pointerDownPos.y) > 5) return
  const rect = canvasEl.getBoundingClientRect()
  const ndc = new THREE.Vector2(
    ((e.clientX - rect.left) / rect.width) * 2 - 1,
    -((e.clientY - rect.top) / rect.height) * 2 + 1,
  )
  raycaster.setFromCamera(ndc, activeCamera as THREE.Camera)
  const hit = raycaster.intersectObject(modelRoot, true).find((h) => isHitVisible(h.object))
  if (!hit) return
  if (tool.value === 'surface') {
    pickSurface(hit)
    return
  }
  if (measurePoints.length >= 2) clearMeasurement()
  const snap = snapToFeature(hit, e.clientX - rect.left, e.clientY - rect.top, rect)
  addMeasurePoint(snap.point, snap.snapped)
}

/** The raycaster reports hits on invisible meshes; walk up to confirm visibility */
function isHitVisible(obj: THREE.Object3D): boolean {
  let o: THREE.Object3D | null = obj
  while (o) {
    if (!o.visible) return false
    o = o.parent
  }
  return true
}

/**
 * Snap a raycast hit to the nearest vertex or triangle edge of the hit face
 * when within a screen-space threshold, for CAD-style precise picking.
 */
function snapToFeature(
  hit: THREE.Intersection,
  px: number,
  py: number,
  rect: DOMRect,
): { point: THREE.Vector3; snapped: boolean } {
  const face = hit.face
  const mesh = hit.object as THREE.Mesh
  if (!face || !activeCamera) return { point: hit.point.clone(), snapped: false }
  const pos = (mesh.geometry as THREE.BufferGeometry).getAttribute('position')
  const verts = [face.a, face.b, face.c].map((i) =>
    new THREE.Vector3().fromBufferAttribute(pos, i).applyMatrix4(mesh.matrixWorld),
  )
  const proj = new THREE.Vector3()
  const screenDist = (p: THREE.Vector3) => {
    proj.copy(p).project(activeCamera as THREE.Camera)
    return Math.hypot(((proj.x + 1) / 2) * rect.width - px, ((1 - proj.y) / 2) * rect.height - py)
  }
  const SNAP_PX = 14
  let best: THREE.Vector3 | null = null
  let bestD = SNAP_PX
  for (const v of verts) {
    const d = screenDist(v)
    if (d < bestD) {
      bestD = d
      best = v
    }
  }
  if (best) return { point: best.clone(), snapped: true }
  const seg = new THREE.Line3()
  const cp = new THREE.Vector3()
  bestD = SNAP_PX
  for (const [i, j] of [[0, 1], [1, 2], [2, 0]] as const) {
    seg.set(verts[i], verts[j])
    seg.closestPointToPoint(hit.point, true, cp)
    const d = screenDist(cp)
    if (d < bestD) {
      bestD = d
      best = cp.clone()
    }
  }
  return best ? { point: best, snapped: true } : { point: hit.point.clone(), snapped: false }
}

function addMeasurePoint(point: THREE.Vector3, snapped: boolean) {
  if (!measureGroup) return
  measurePoints.push(point)
  const marker = new THREE.Mesh(
    new THREE.SphereGeometry(modelMaxDim * 0.008, 16, 12),
    new THREE.MeshBasicMaterial({ color: snapped ? 0xfbbf24 : 0x22d3ee, depthTest: false }),
  )
  marker.position.copy(point)
  marker.renderOrder = 999
  measureGroup.add(marker)

  if (measurePoints.length === 2) buildDistanceMeasurement(measurePoints[0], measurePoints[1])
}

/**
 * SolidWorks-style measurement: the direct distance plus dashed axis-aligned
 * "triangle" legs with ΔX/ΔY/ΔZ callouts — legs are only drawn when the two
 * points are not on the same axis plane (delta above threshold on ≥2 axes).
 */
function buildDistanceMeasurement(p1: THREE.Vector3, p2: THREE.Vector3) {
  if (!measureGroup) return
  measurement.value = {
    dist: p1.distanceTo(p2),
    dx: Math.abs(p2.x - p1.x),
    dy: Math.abs(p2.y - p1.y),
    dz: Math.abs(p2.z - p1.z),
  }

  const line = new THREE.Line(
    new THREE.BufferGeometry().setFromPoints([p1, p2]),
    new THREE.LineBasicMaterial({ color: 0x22d3ee, depthTest: false }),
  )
  line.renderOrder = 999
  measureGroup.add(line)
  labelAnchors.push({ pos: p1.clone().add(p2).multiplyScalar(0.5), kind: 'total' })

  const eps = axisEps()
  const activeAxes = [
    measurement.value.dx > eps,
    measurement.value.dy > eps,
    measurement.value.dz > eps,
  ].filter(Boolean).length
  if (activeAxes < 2) return

  // Axis-aligned path p1 → (x2,y1,z1) → (x2,y2,z1) → p2
  const c1 = new THREE.Vector3(p2.x, p1.y, p1.z)
  const c2 = new THREE.Vector3(p2.x, p2.y, p1.z)
  const legs: { from: THREE.Vector3; to: THREE.Vector3; color: number; kind: ScreenLabel['kind']; delta: number }[] = [
    { from: p1, to: c1, color: 0xf87171, kind: 'dx', delta: measurement.value.dx },
    { from: c1, to: c2, color: 0x4ade80, kind: 'dy', delta: measurement.value.dy },
    { from: c2, to: p2, color: 0x60a5fa, kind: 'dz', delta: measurement.value.dz },
  ]
  for (const leg of legs) {
    if (leg.delta <= eps) continue
    const geom = new THREE.BufferGeometry().setFromPoints([leg.from, leg.to])
    const dashed = new THREE.Line(
      geom,
      new THREE.LineDashedMaterial({
        color: leg.color,
        depthTest: false,
        dashSize: modelMaxDim * 0.02,
        gapSize: modelMaxDim * 0.012,
        transparent: true,
        opacity: 0.9,
      }),
    )
    dashed.computeLineDistances()
    dashed.renderOrder = 999
    measureGroup.add(dashed)
    labelAnchors.push({ pos: leg.from.clone().add(leg.to).multiplyScalar(0.5), kind: leg.kind })
  }
}

function clearMeasurement() {
  measurePoints = []
  measurement.value = null
  surfaceArea.value = null
  labelAnchors = []
  screenLabels.value = []
  if (!measureGroup) return
  for (const child of measureGroup.children) {
    if (child instanceof THREE.Mesh || child instanceof THREE.Line) {
      child.geometry.dispose()
      ;(child.material as THREE.Material).dispose()
    }
  }
  measureGroup.clear()
}

function updateScreenLabels() {
  if (labelAnchors.length === 0 || !activeCamera || !canvasEl) {
    if (screenLabels.value.length) screenLabels.value = []
    return
  }
  const m = measurement.value
  const proj = new THREE.Vector3()
  const next: ScreenLabel[] = []
  for (const anchor of labelAnchors) {
    proj.copy(anchor.pos).project(activeCamera as THREE.Camera)
    if (proj.z >= 1) continue
    let text = ''
    if (anchor.kind === 'total') text = m ? fmtLen(m.dist) : ''
    else if (anchor.kind === 'dx') text = m ? `ΔX ${fmtLen(m.dx)}` : ''
    else if (anchor.kind === 'dy') text = m ? `ΔY ${fmtLen(m.dy)}` : ''
    else if (anchor.kind === 'dz') text = m ? `ΔZ ${fmtLen(m.dz)}` : ''
    else if (anchor.kind === 'area') text = surfaceArea.value != null ? fmtArea(surfaceArea.value) : ''
    if (!text) continue
    next.push({
      x: ((proj.x + 1) / 2) * canvasEl.clientWidth,
      y: ((1 - proj.y) / 2) * canvasEl.clientHeight,
      text,
      kind: anchor.kind,
    })
  }
  screenLabels.value = next
}

// ── Surface (face) picking ─────────────────────────────────────────────────

interface MeshTopo {
  neighbors: Int32Array // 3 entries per triangle; -1 = open edge
  normals: Float32Array // per-triangle local-space normal
  triCount: number
}
const topoCache = new WeakMap<THREE.BufferGeometry, MeshTopo>()

/** Edge-adjacency and per-triangle normals, built once per geometry */
function getTopo(geom: THREE.BufferGeometry): MeshTopo {
  const cached = topoCache.get(geom)
  if (cached) return cached
  const pos = geom.getAttribute('position')
  const index = geom.getIndex()
  const triCount = Math.floor((index ? index.count : pos.count) / 3)
  if (!geom.boundingBox) geom.computeBoundingBox()
  const eps = geom.boundingBox!.getSize(new THREE.Vector3()).length() * 1e-5 || 1e-9
  // Coincident vertices are merged by rounded position so seams don't break adjacency
  const vertKey = (vi: number) =>
    `${Math.round(pos.getX(vi) / eps)},${Math.round(pos.getY(vi) / eps)},${Math.round(pos.getZ(vi) / eps)}`
  const neighbors = new Int32Array(triCount * 3).fill(-1)
  const normals = new Float32Array(triCount * 3)
  const edgeMap = new Map<string, number>() // edge key -> encoded (tri * 3 + slot) awaiting its pair
  const vA = new THREE.Vector3()
  const vB = new THREE.Vector3()
  const vC = new THREE.Vector3()
  const ab = new THREE.Vector3()
  const ac = new THREE.Vector3()
  for (let t = 0; t < triCount; t++) {
    const i0 = index ? index.getX(t * 3) : t * 3
    const i1 = index ? index.getX(t * 3 + 1) : t * 3 + 1
    const i2 = index ? index.getX(t * 3 + 2) : t * 3 + 2
    vA.fromBufferAttribute(pos, i0)
    vB.fromBufferAttribute(pos, i1)
    vC.fromBufferAttribute(pos, i2)
    ab.subVectors(vB, vA).cross(ac.subVectors(vC, vA)).normalize()
    normals[t * 3] = ab.x
    normals[t * 3 + 1] = ab.y
    normals[t * 3 + 2] = ab.z
    const keys = [vertKey(i0), vertKey(i1), vertKey(i2)]
    for (let s = 0; s < 3; s++) {
      const k1 = keys[s]
      const k2 = keys[(s + 1) % 3]
      const edgeKey = k1 < k2 ? `${k1}|${k2}` : `${k2}|${k1}`
      const other = edgeMap.get(edgeKey)
      if (other === undefined) {
        edgeMap.set(edgeKey, t * 3 + s)
      } else {
        neighbors[t * 3 + s] = Math.floor(other / 3)
        neighbors[other] = t
        edgeMap.delete(edgeKey)
      }
    }
  }
  const topo = { neighbors, normals, triCount }
  topoCache.set(geom, topo)
  return topo
}

/**
 * Region-grow from the clicked triangle across smoothly connected neighbors
 * (stops at sharp feature edges), highlight the region, and report its area.
 */
function pickSurface(hit: THREE.Intersection) {
  const mesh = hit.object as THREE.Mesh
  if (hit.faceIndex == null || !measureGroup) return
  clearMeasurement()
  const geom = mesh.geometry as THREE.BufferGeometry
  const topo = getTopo(geom)
  const cosThresh = Math.cos((25 * Math.PI) / 180)
  const visited = new Uint8Array(topo.triCount)
  const stack = [hit.faceIndex]
  visited[hit.faceIndex] = 1
  const region: number[] = []
  while (stack.length) {
    const t = stack.pop()!
    region.push(t)
    for (let s = 0; s < 3; s++) {
      const n = topo.neighbors[t * 3 + s]
      if (n < 0 || visited[n]) continue
      const dot =
        topo.normals[t * 3] * topo.normals[n * 3] +
        topo.normals[t * 3 + 1] * topo.normals[n * 3 + 1] +
        topo.normals[t * 3 + 2] * topo.normals[n * 3 + 2]
      if (dot >= cosThresh) {
        visited[n] = 1
        stack.push(n)
      }
    }
  }

  const pos = geom.getAttribute('position')
  const index = geom.getIndex()
  const positions = new Float32Array(region.length * 9)
  const vA = new THREE.Vector3()
  const vB = new THREE.Vector3()
  const vC = new THREE.Vector3()
  const ab = new THREE.Vector3()
  const ac = new THREE.Vector3()
  let area = 0
  region.forEach((t, r) => {
    const i0 = index ? index.getX(t * 3) : t * 3
    const i1 = index ? index.getX(t * 3 + 1) : t * 3 + 1
    const i2 = index ? index.getX(t * 3 + 2) : t * 3 + 2
    vA.fromBufferAttribute(pos, i0).applyMatrix4(mesh.matrixWorld)
    vB.fromBufferAttribute(pos, i1).applyMatrix4(mesh.matrixWorld)
    vC.fromBufferAttribute(pos, i2).applyMatrix4(mesh.matrixWorld)
    area += ab.subVectors(vB, vA).cross(ac.subVectors(vC, vA)).length() / 2
    vA.toArray(positions, r * 9)
    vB.toArray(positions, r * 9 + 3)
    vC.toArray(positions, r * 9 + 6)
  })

  const hlGeom = new THREE.BufferGeometry()
  hlGeom.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const highlight = new THREE.Mesh(
    hlGeom,
    new THREE.MeshBasicMaterial({
      color: 0x22d3ee,
      transparent: true,
      opacity: 0.35,
      side: THREE.DoubleSide,
      depthWrite: false,
      polygonOffset: true,
      polygonOffsetFactor: -2,
      polygonOffsetUnits: -2,
    }),
  )
  highlight.renderOrder = 998
  measureGroup.add(highlight)
  surfaceArea.value = area
  labelAnchors.push({ pos: hit.point.clone(), kind: 'area' })
}

// ── Bounding-box dimensions ────────────────────────────────────────────────

watch(showDims, (on) => {
  if (!scene) return
  if (on && !boxHelper) {
    boxHelper = new THREE.Box3Helper(worldBox, 0x22d3ee)
    ;(boxHelper.material as THREE.LineBasicMaterial).transparent = true
    ;(boxHelper.material as THREE.LineBasicMaterial).opacity = 0.7
    scene.add(boxHelper)
  }
  if (boxHelper) boxHelper.visible = on
})

watch(showGrid, (on) => {
  if (grid) grid.visible = on
})

watch(autoRotate, (on) => {
  if (controls) controls.autoRotate = on
})

// ── Parts / visibility ─────────────────────────────────────────────────────

function togglePartVisibility(part: PartEntry) {
  part.visible = !part.visible
  applyPartVisibility()
}

function isolatePart(part: PartEntry) {
  if (isolatedPartId.value === part.id) {
    isolatedPartId.value = null
    for (const p of parts.value) p.visible = true
  } else {
    isolatedPartId.value = part.id
    for (const p of parts.value) p.visible = p.id === part.id
  }
  applyPartVisibility()
}

function applyPartVisibility() {
  for (const p of parts.value) {
    const node = partNodes[p.id]
    if (node) node.visible = p.visible
  }
  clearMeasurement()
}

function fitPart(part: PartEntry) {
  const node = partNodes[part.id]
  if (node) flyToBox(new THREE.Box3().setFromObject(node))
}

// ── Explode ────────────────────────────────────────────────────────────────

watch(explodeEnabled, (on) => {
  if (!on) explodeAmount.value = 0
  applyExplode()
})
watch(explodeAmount, applyExplode)

function applyExplode() {
  const k = (explodeAmount.value / 100) * modelMaxDim * 0.75
  for (const node of partNodes) {
    const base = node.userData.explodeBase as THREE.Vector3 | undefined
    const dir = node.userData.explodeDir as THREE.Vector3 | undefined
    if (!base || !dir) continue
    node.position.copy(base).addScaledVector(dir, k)
  }
  // Moving parts invalidates world-space measurements and the cached bounds
  clearMeasurement()
  refreshWorldBox()
}

function refreshWorldBox() {
  if (!modelRoot) return
  modelRoot.updateWorldMatrix(true, true)
  worldBox.setFromObject(modelRoot)
  if (clipEnabled.value) applyClipping()
}

// ── Cross-section clipping ─────────────────────────────────────────────────

watch([clipEnabled, clipAxis, clipPos], applyClipping)

function applyClipping() {
  if (!modelRoot) return
  if (!clipPlane) clipPlane = new THREE.Plane()
  const axis = clipAxis.value
  const normal = new THREE.Vector3(axis === 'x' ? -1 : 0, axis === 'y' ? -1 : 0, axis === 'z' ? -1 : 0)
  const min = worldBox.min[axis]
  const max = worldBox.max[axis]
  const cut = min + (max - min) * (clipPos.value / 100)
  clipPlane.set(normal, cut + (max - min) * 0.001)
  modelRoot.traverse((child) => {
    if (!(child instanceof THREE.Mesh)) return
    const mats = Array.isArray(child.material) ? child.material : [child.material]
    for (const mat of mats) {
      if (clipEnabled.value) {
        if (mat.userData.origSide === undefined) mat.userData.origSide = mat.side
        mat.side = THREE.DoubleSide
        mat.clippingPlanes = [clipPlane!]
      } else {
        if (mat.userData.origSide !== undefined) {
          mat.side = mat.userData.origSide
          delete mat.userData.origSide
        }
        mat.clippingPlanes = null
      }
      mat.needsUpdate = true
    }
  })
}

watch(wireframe, (on) => {
  modelRoot?.traverse((child) => {
    if (child instanceof THREE.Mesh) {
      const mats = Array.isArray(child.material) ? child.material : [child.material]
      for (const mat of mats) {
        if ('wireframe' in mat) (mat as THREE.MeshStandardMaterial).wireframe = on
      }
    }
  })
})

// ── Camera: projection, fit, reset, screenshot ─────────────────────────────

function syncOrthoFrustum() {
  if (!orthoCamera || !perspCamera || !controls || !canvasEl) return
  const dist = perspCamera.position.distanceTo(controls.target)
  const halfH = dist * Math.tan(THREE.MathUtils.degToRad(perspCamera.fov / 2))
  const aspect = canvasEl.clientWidth / Math.max(canvasEl.clientHeight, 1)
  orthoCamera.left = -halfH * aspect
  orthoCamera.right = halfH * aspect
  orthoCamera.top = halfH
  orthoCamera.bottom = -halfH
  orthoCamera.updateProjectionMatrix()
}

function toggleProjection() {
  if (!perspCamera || !orthoCamera || !controls) return
  orthographic.value = !orthographic.value
  if (orthographic.value) {
    syncOrthoFrustum()
    orthoCamera.position.copy(perspCamera.position)
    orthoCamera.quaternion.copy(perspCamera.quaternion)
    orthoCamera.zoom = 1
    orthoCamera.updateProjectionMatrix()
    activeCamera = orthoCamera
  } else {
    perspCamera.position.copy(orthoCamera.position)
    activeCamera = perspCamera
  }
  controls.object = activeCamera as THREE.PerspectiveCamera
  controls.update()
}

function flyToBox(box: THREE.Box3) {
  if (!activeCamera || !controls) return
  const center = box.getCenter(new THREE.Vector3())
  const size = box.getSize(new THREE.Vector3())
  const maxDim = Math.max(size.x, size.y, size.z) || modelMaxDim
  const cam = activeCamera as THREE.PerspectiveCamera | THREE.OrthographicCamera
  const dir = cam.position.clone().sub(controls.target).normalize()
  controls.target.copy(center)
  cam.position.copy(center).addScaledVector(dir, maxDim * 2)
  if (cam instanceof THREE.OrthographicCamera) {
    cam.zoom = 1
    syncOrthoFrustum()
  }
  controls.update()
}

function fitView() {
  if (!modelRoot) return
  const box = new THREE.Box3()
  let any = false
  for (const node of partNodes) {
    if (!node.visible) continue
    box.expandByObject(node)
    any = true
  }
  flyToBox(any ? box : worldBox)
}

function resetView() {
  if (!activeCamera || !controls) return
  const cam = activeCamera as THREE.PerspectiveCamera | THREE.OrthographicCamera
  cam.position.copy(initialCameraPos)
  cam.up.set(0, 1, 0)
  if (cam instanceof THREE.OrthographicCamera) {
    cam.zoom = 1
    syncOrthoFrustum()
  }
  controls.target.copy(initialTarget)
  autoRotate.value = false
  camAnim = null
  clearMeasurement()
}

function takeScreenshot() {
  if (!renderer || !scene || !activeCamera) return
  renderer.render(scene, activeCamera)
  const base = (props.files[0]?.filename ?? 'model').replace(/\.[^.]+$/, '')
  const url = renderer.domElement.toDataURL('image/png')
  const a = document.createElement('a')
  a.href = url
  a.download = `${base}.png`
  a.click()
}

// ── View cube ──────────────────────────────────────────────────────────────

const CUBE_FACES = ['RIGHT', 'LEFT', 'TOP', 'BOTTOM', 'FRONT', 'BACK']

function drawCubeFace(label: string, hover: boolean): THREE.CanvasTexture {
  const c = document.createElement('canvas')
  c.width = c.height = 128
  const ctx = c.getContext('2d')!
  ctx.fillStyle = hover ? 'rgba(34, 211, 238, 0.28)' : 'rgba(10, 26, 33, 0.95)'
  ctx.fillRect(0, 0, 128, 128)
  ctx.strokeStyle = hover ? '#22d3ee' : 'rgba(34, 211, 238, 0.35)'
  ctx.lineWidth = 4
  ctx.strokeRect(2, 2, 124, 124)
  ctx.fillStyle = hover ? '#e2f7fb' : '#8fa8b1'
  ctx.font = '700 21px Inter, system-ui, sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(label, 64, 66)
  const tex = new THREE.CanvasTexture(c)
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

function initViewCube() {
  const canvas = cubeCanvasRef.value
  if (!canvas) return
  cubeRenderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  cubeRenderer.setPixelRatio(window.devicePixelRatio)
  cubeRenderer.setSize(canvas.clientWidth, canvas.clientHeight, false)
  cubeScene = new THREE.Scene()
  cubeCamera = new THREE.OrthographicCamera(-1.35, 1.35, 1.35, -1.35, 0.1, 10)
  cubeCamera.position.set(0, 0, 4)

  const materials = CUBE_FACES.map(
    (label) => new THREE.MeshBasicMaterial({ map: drawCubeFace(label, false), transparent: true }),
  )
  cubeMesh = new THREE.Mesh(new THREE.BoxGeometry(1.5, 1.5, 1.5), materials)
  cubeScene.add(cubeMesh)
  const edges = new THREE.LineSegments(
    new THREE.EdgesGeometry(cubeMesh.geometry as THREE.BoxGeometry),
    new THREE.LineBasicMaterial({ color: 0x22d3ee, transparent: true, opacity: 0.5 }),
  )
  cubeMesh.add(edges)

  canvas.addEventListener('pointermove', onCubePointerMove)
  canvas.addEventListener('pointerleave', onCubePointerLeave)
  canvas.addEventListener('click', onCubeClick)
}

function renderViewCube() {
  if (!cubeRenderer || !cubeScene || !cubeCamera || !cubeMesh || !activeCamera) return
  // Cube shows world orientation as seen by the main camera
  cubeMesh.quaternion.copy((activeCamera as THREE.Camera).quaternion).invert()
  cubeRenderer.render(cubeScene, cubeCamera)
}

function cubeHitInfo(e: MouseEvent): THREE.Intersection | null {
  const canvas = cubeCanvasRef.value
  if (!canvas || !cubeCamera || !cubeMesh) return null
  const rect = canvas.getBoundingClientRect()
  const ndc = new THREE.Vector2(
    ((e.clientX - rect.left) / rect.width) * 2 - 1,
    -((e.clientY - rect.top) / rect.height) * 2 + 1,
  )
  cubeRaycaster.setFromCamera(ndc, cubeCamera)
  return cubeRaycaster.intersectObject(cubeMesh, false)[0] ?? null
}

function onCubePointerMove(e: PointerEvent) {
  const hit = cubeHitInfo(e)
  const canvas = cubeCanvasRef.value
  const faceIndex = hit?.face ? hit.face.materialIndex : -1
  if (canvas) canvas.style.cursor = faceIndex >= 0 ? 'pointer' : 'default'
  if (faceIndex === cubeHover || !cubeMesh) return
  const mats = cubeMesh.material as THREE.MeshBasicMaterial[]
  if (cubeHover >= 0) {
    mats[cubeHover].map?.dispose()
    mats[cubeHover].map = drawCubeFace(CUBE_FACES[cubeHover], false)
  }
  if (faceIndex >= 0) {
    mats[faceIndex].map?.dispose()
    mats[faceIndex].map = drawCubeFace(CUBE_FACES[faceIndex], true)
  }
  cubeHover = faceIndex
}

function onCubePointerLeave() {
  if (cubeHover >= 0 && cubeMesh) {
    const mats = cubeMesh.material as THREE.MeshBasicMaterial[]
    mats[cubeHover].map?.dispose()
    mats[cubeHover].map = drawCubeFace(CUBE_FACES[cubeHover], false)
  }
  cubeHover = -1
  if (cubeCanvasRef.value) cubeCanvasRef.value.style.cursor = 'default'
}

function onCubeClick(e: MouseEvent) {
  const hit = cubeHitInfo(e)
  if (!hit || !cubeMesh) return
  // Snap the local hit point to a face/edge/corner direction (components -1/0/1)
  const local = cubeMesh.worldToLocal(hit.point.clone())
  const half = 0.75
  const dir = new THREE.Vector3(
    Math.abs(local.x / half) > 0.55 ? Math.sign(local.x) : 0,
    Math.abs(local.y / half) > 0.55 ? Math.sign(local.y) : 0,
    Math.abs(local.z / half) > 0.55 ? Math.sign(local.z) : 0,
  )
  if (dir.lengthSq() === 0 && hit.face) dir.copy(hit.face.normal)
  flyToDirection(dir.normalize())
}

function flyToDirection(dir: THREE.Vector3) {
  if (!activeCamera || !controls) return
  const cam = activeCamera as THREE.PerspectiveCamera | THREE.OrthographicCamera
  const offset = cam.position.clone().sub(controls.target)
  const dist = offset.length()
  const toDir = dir.clone()
  // Camera up stays Y-up (OrbitControls caches its up basis); tilt pure
  // top/bottom directions a hair so lookAt never degenerates at the pole
  if (Math.abs(toDir.y) > 0.999) {
    toDir.z = 0.02
    toDir.normalize()
  }
  const fromDir = offset.normalize()
  // Antipodal directions would lerp through zero; bend the start slightly
  if (fromDir.dot(toDir) < -0.999) {
    const ortho = Math.abs(fromDir.y) < 0.9 ? new THREE.Vector3(0, 1, 0) : new THREE.Vector3(1, 0, 0)
    fromDir.addScaledVector(ortho.cross(fromDir).normalize(), 0.05).normalize()
  }
  camAnim = {
    t0: performance.now(),
    duration: 450,
    fromDir,
    toDir,
    dist,
  }
}

function stepCameraAnimation() {
  if (!camAnim || !activeCamera || !controls) return
  const cam = activeCamera as THREE.PerspectiveCamera | THREE.OrthographicCamera
  const t = Math.min((performance.now() - camAnim.t0) / camAnim.duration, 1)
  const ease = 1 - Math.pow(1 - t, 3)
  // nlerp is plenty smooth for a short camera hop
  const dir = camAnim.fromDir
    .clone()
    .lerp(camAnim.toDir, ease)
    .normalize()
  cam.position.copy(controls.target).addScaledVector(dir, camAnim.dist)
  cam.lookAt(controls.target)
  if (t >= 1) camAnim = null
}

// ── Teardown ───────────────────────────────────────────────────────────────

function teardown() {
  if (animFrameId != null) {
    cancelAnimationFrame(animFrameId)
    animFrameId = null
  }
  resizeObserver?.disconnect()
  resizeObserver = null
  if (canvasEl) {
    canvasEl.removeEventListener('pointerdown', onPointerDown)
    canvasEl.removeEventListener('pointerup', onPointerUp)
    canvasEl = null
  }
  const cubeCanvas = cubeCanvasRef.value
  if (cubeCanvas) {
    cubeCanvas.removeEventListener('pointermove', onCubePointerMove)
    cubeCanvas.removeEventListener('pointerleave', onCubePointerLeave)
    cubeCanvas.removeEventListener('click', onCubeClick)
  }
  controls?.dispose()
  controls = null
  clearMeasurement()
  measureGroup = null
  if (boxHelper) {
    boxHelper.geometry.dispose()
    ;(boxHelper.material as THREE.Material).dispose()
    boxHelper = null
  }
  if (grid) {
    grid.geometry.dispose()
    ;(grid.material as THREE.Material).dispose()
    grid = null
  }
  modelRoot?.traverse((child) => {
    if (child instanceof THREE.Mesh) {
      child.geometry.dispose()
      const mats = Array.isArray(child.material) ? child.material : [child.material]
      mats.forEach((m) => m.dispose())
    }
  })
  modelRoot = null
  partNodes = []
  if (cubeMesh) {
    cubeMesh.geometry.dispose()
    for (const mat of cubeMesh.material as THREE.MeshBasicMaterial[]) {
      mat.map?.dispose()
      mat.dispose()
    }
    cubeMesh = null
  }
  cubeRenderer?.dispose()
  cubeRenderer = null
  cubeScene = null
  cubeCamera = null
  pmrem?.dispose()
  pmrem = null
  renderer?.dispose()
  renderer = null
  scene?.clear()
  scene = null
  perspCamera = null
  orthoCamera = null
  activeCamera = null
}
</script>

<script lang="ts">
/** Returns true when the filename has an extension the 3D viewer can render */
export function isViewableModel(filename?: string): boolean {
  if (!filename) return false
  const ext = filename.toLowerCase().split('.').pop() ?? ''
  return ['glb', 'gltf'].includes(ext)
}
</script>

<template>
  <div class="mv-overlay" @click.self="emit('close')">
    <div class="mv-box">
      <div class="mv-header">
        <span class="mv-title">
          <i class="mdi mdi-cube-scan" />
          {{ title }}
        </span>
        <span class="mv-header-meta">glTF · meters</span>
        <button class="mv-close" title="Close (Esc)" @click="emit('close')">
          <i class="mdi mdi-close" />
        </button>
      </div>

      <div v-if="loading" class="mv-state">
        <span class="mv-spinner" />
        <span>Loading model… {{ loadProgress }}</span>
      </div>
      <div v-else-if="error" class="mv-state mv-state--error">
        <i class="mdi mdi-alert-circle-outline" /> {{ error }}
      </div>

      <div v-else class="mv-body">
        <div class="mv-stage">
          <canvas ref="canvasRef" class="mv-canvas" :class="{ 'mv-canvas--measure': tool !== 'orbit' }" />

          <!-- View cube -->
          <canvas ref="cubeCanvasRef" class="mv-viewcube" width="104" height="104" />

          <!-- Floating toolbar -->
          <div class="mv-toolbar">
            <button class="mv-tool-btn" title="Fit view" @click="fitView">
              <i class="mdi mdi-image-filter-center-focus" />
            </button>
            <button
              class="mv-tool-btn"
              :class="{ 'mv-tool-btn--active': orthographic }"
              title="Toggle orthographic projection"
              @click="toggleProjection"
            >
              <i class="mdi mdi-grid-large" />
            </button>
            <button
              class="mv-tool-btn"
              :class="{ 'mv-tool-btn--active': autoRotate }"
              title="Auto-rotate"
              @click="autoRotate = !autoRotate"
            >
              <i class="mdi mdi-rotate-3d-variant" />
            </button>
            <div class="mv-tool-divider" />
            <button
              class="mv-tool-btn"
              :class="{ 'mv-tool-btn--active': tool === 'distance' }"
              title="Measure distance (two points)"
              @click="setTool('distance')"
            >
              <i class="mdi mdi-ruler" />
            </button>
            <button
              class="mv-tool-btn"
              :class="{ 'mv-tool-btn--active': tool === 'surface' }"
              title="Measure face area"
              @click="setTool('surface')"
            >
              <i class="mdi mdi-vector-square" />
            </button>
            <div class="mv-tool-divider" />
            <button
              class="mv-tool-btn"
              :class="{ 'mv-tool-btn--active': wireframe }"
              title="Wireframe"
              @click="wireframe = !wireframe"
            >
              <i class="mdi mdi-vector-triangle" />
            </button>
            <button
              class="mv-tool-btn"
              :class="{ 'mv-tool-btn--active': showGrid }"
              title="Grid"
              @click="showGrid = !showGrid"
            >
              <i class="mdi mdi-grid" />
            </button>
            <button
              class="mv-tool-btn"
              :class="{ 'mv-tool-btn--active': showDims }"
              title="Bounding-box dimensions"
              @click="showDims = !showDims"
            >
              <i class="mdi mdi-arrow-expand-all" />
            </button>
            <button
              class="mv-tool-btn"
              :class="{ 'mv-tool-btn--active': clipEnabled }"
              title="Cross-section"
              @click="clipEnabled = !clipEnabled"
            >
              <i class="mdi mdi-content-cut" />
            </button>
            <button
              v-if="parts.length > 1"
              class="mv-tool-btn"
              :class="{ 'mv-tool-btn--active': explodeEnabled }"
              title="Explode assembly"
              @click="explodeEnabled = !explodeEnabled"
            >
              <i class="mdi mdi-arrow-expand" />
            </button>
            <div class="mv-tool-divider" />
            <button
              class="mv-tool-btn"
              :class="{ 'mv-tool-btn--active': showInfo }"
              title="Model statistics"
              @click="showInfo = !showInfo"
            >
              <i class="mdi mdi-information-outline" />
            </button>
            <button class="mv-tool-btn" title="Save screenshot" @click="takeScreenshot">
              <i class="mdi mdi-camera-outline" />
            </button>
            <button class="mv-tool-btn" title="Reset view" @click="resetView">
              <i class="mdi mdi-restore" />
            </button>
          </div>

          <!-- Measurement labels -->
          <div
            v-for="(label, i) in screenLabels"
            :key="i"
            class="mv-measure-label"
            :class="`mv-measure-label--${label.kind}`"
            :style="{ left: `${label.x}px`, top: `${label.y}px` }"
          >
            {{ label.text }}
          </div>

          <!-- Bounding box dims -->
          <div v-if="showDims && dims" class="mv-dims-chip">
            <span><em class="mv-ax mv-ax--x">X</em>{{ fmtLen(dims.x) }}</span>
            <span><em class="mv-ax mv-ax--y">Y</em>{{ fmtLen(dims.y) }}</span>
            <span><em class="mv-ax mv-ax--z">Z</em>{{ fmtLen(dims.z) }}</span>
          </div>

          <!-- Status bar -->
          <div class="mv-statusbar">
            <span class="mv-status-hint">{{ hint }}</span>
            <span class="mv-status-proj">{{ orthographic ? 'Orthographic' : 'Perspective' }}</span>
          </div>

          <!-- Panel toggle -->
          <button
            class="mv-panel-toggle"
            :title="panelOpen ? 'Hide panel' : 'Show panel'"
            @click="panelOpen = !panelOpen"
          >
            <i :class="panelOpen ? 'mdi mdi-chevron-right' : 'mdi mdi-chevron-left'" />
          </button>
        </div>

        <!-- Side panel -->
        <aside v-if="panelOpen" class="mv-panel">
          <section v-if="parts.length > 1" class="mv-section">
            <h3 class="mv-section-title"><i class="mdi mdi-file-tree" /> Parts</h3>
            <div class="mv-part-list">
              <div v-for="part in parts" :key="part.id" class="mv-part-row">
                <button class="mv-part-name" title="Zoom to part" @click="fitPart(part)">
                  {{ part.name }}
                </button>
                <button
                  class="mv-part-btn"
                  :class="{ 'mv-part-btn--active': isolatedPartId === part.id }"
                  title="Isolate"
                  @click="isolatePart(part)"
                >
                  <i class="mdi mdi-target" />
                </button>
                <button
                  class="mv-part-btn"
                  :title="part.visible ? 'Hide' : 'Show'"
                  @click="togglePartVisibility(part)"
                >
                  <i :class="part.visible ? 'mdi mdi-eye-outline' : 'mdi mdi-eye-off-outline'" />
                </button>
              </div>
            </div>
          </section>

          <section v-if="tool !== 'orbit' || measurement || surfaceArea != null" class="mv-section">
            <h3 class="mv-section-title"><i class="mdi mdi-ruler" /> Measure</h3>
            <template v-if="measureRows.length">
              <div v-for="row in measureRows" :key="row.key" class="mv-kv" :class="`mv-kv--${row.cls}`">
                <span>{{ row.label }}</span>
                <span>{{ row.value }}</span>
              </div>
            </template>
            <div v-else-if="surfaceArea != null" class="mv-kv mv-kv--total">
              <span>Face area</span>
              <span>{{ fmtArea(surfaceArea) }}</span>
            </div>
            <p v-else class="mv-section-hint">
              {{ tool === 'distance' ? 'Pick two points on the model.' : 'Click a face on the model.' }}
            </p>
            <button
              v-if="measurement || surfaceArea != null"
              class="mv-mini-btn"
              @click="clearMeasurement"
            >
              <i class="mdi mdi-eraser" /> Clear
            </button>
          </section>

          <section v-if="clipEnabled" class="mv-section">
            <h3 class="mv-section-title"><i class="mdi mdi-content-cut" /> Section</h3>
            <div class="mv-axis-row">
              <button
                v-for="axis in (['x', 'y', 'z'] as const)"
                :key="axis"
                class="mv-chip-btn"
                :class="{ 'mv-chip-btn--active': clipAxis === axis }"
                @click="clipAxis = axis"
              >
                {{ axis.toUpperCase() }}
              </button>
            </div>
            <input v-model.number="clipPos" type="range" min="1" max="100" class="mv-slider" />
          </section>

          <section v-if="explodeEnabled" class="mv-section">
            <h3 class="mv-section-title"><i class="mdi mdi-arrow-expand" /> Explode</h3>
            <input v-model.number="explodeAmount" type="range" min="0" max="100" class="mv-slider" />
          </section>

          <section class="mv-section">
            <h3 class="mv-section-title"><i class="mdi mdi-tape-measure" /> Units</h3>
            <div class="mv-axis-row">
              <button
                v-for="u in UNITS"
                :key="u"
                class="mv-chip-btn"
                :class="{ 'mv-chip-btn--active': displayUnit === u }"
                @click="displayUnit = u"
              >
                {{ u }}
              </button>
            </div>
          </section>

          <section v-if="showInfo && stats && dims" class="mv-section">
            <h3 class="mv-section-title"><i class="mdi mdi-information-outline" /> Model</h3>
            <div class="mv-kv">
              <span>Size</span>
              <span>{{ fmtAdaptive(dims.x * unitScale) }} × {{ fmtAdaptive(dims.y * unitScale) }} × {{ fmtAdaptive(dims.z * unitScale) }} {{ displayUnit }}</span>
            </div>
            <div class="mv-kv"><span>Triangles</span><span>{{ fmtNum(stats.tris) }}</span></div>
            <div class="mv-kv"><span>Vertices</span><span>{{ fmtNum(stats.verts) }}</span></div>
            <div class="mv-kv"><span>Volume</span><span>{{ fmtVol(stats.volume) }}</span></div>
            <div class="mv-kv"><span>Surface</span><span>{{ fmtArea(stats.area) }}</span></div>
          </section>

          <section v-if="files.length > 1" class="mv-section">
            <h3 class="mv-section-title"><i class="mdi mdi-file-multiple-outline" /> Files</h3>
            <p v-for="f in files" :key="f.filename" class="mv-file-item">{{ f.filename }}</p>
          </section>
        </aside>
      </div>
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
  padding: 1.25rem;
}

.mv-box {
  width: 100%;
  max-width: 1320px;
  height: min(88vh, 900px);
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

/* ── Header ─────────────────────────────────────────────── */
.mv-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ph-border);
  flex-shrink: 0;
}

.mv-title {
  font-size: 0.78rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--ph-text);
  display: flex;
  align-items: center;
  gap: 0.4rem;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mv-title .mdi { color: var(--ph-accent, #22d3ee); }

.mv-header-meta {
  font-size: 0.68rem;
  color: var(--ph-text-muted);
  opacity: 0.7;
  padding: 0.1rem 0.5rem;
  border: 1px solid var(--ph-border);
  border-radius: 999px;
  white-space: nowrap;
}

.mv-close {
  margin-left: auto;
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

/* ── Loading / error ────────────────────────────────────── */
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

/* ── Layout ─────────────────────────────────────────────── */
.mv-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.mv-stage {
  flex: 1;
  position: relative;
  min-width: 0;
}

.mv-canvas {
  width: 100%;
  height: 100%;
  display: block;
  background: radial-gradient(ellipse at 50% 40%, #0a1920 0%, #060f14 70%);
}
.mv-canvas--measure { cursor: crosshair; }

/* ── View cube ──────────────────────────────────────────── */
.mv-viewcube {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  width: 104px;
  height: 104px;
  z-index: 3;
}

/* ── Toolbar ────────────────────────────────────────────── */
.mv-toolbar {
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding: 0.3rem;
  background: rgba(6, 20, 27, 0.85);
  border: 1px solid var(--ph-border);
  border-radius: 10px;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 3;
}

.mv-tool-btn {
  width: 2.1rem;
  height: 2.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: 1px solid transparent;
  border-radius: 7px;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 1.05rem;
  transition: color 0.15s, background 0.15s, border-color 0.15s;
}
.mv-tool-btn:hover { color: var(--ph-text); background: rgba(255,255,255,0.08); }
.mv-tool-btn--active {
  color: var(--ph-accent, #22d3ee);
  border-color: rgba(34, 211, 238, 0.45);
  background: rgba(34, 211, 238, 0.12);
}

.mv-tool-divider {
  height: 1px;
  margin: 0.15rem 0.3rem;
  background: var(--ph-border);
}

/* ── Measurement labels ─────────────────────────────────── */
.mv-measure-label {
  position: absolute;
  transform: translate(-50%, -140%);
  background: rgba(6, 20, 27, 0.92);
  border: 1px solid var(--ph-accent, #22d3ee);
  color: var(--ph-accent, #22d3ee);
  padding: 0.15rem 0.5rem;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 600;
  pointer-events: none;
  white-space: nowrap;
  z-index: 2;
}
.mv-measure-label--dx { border-color: #f87171; color: #f87171; }
.mv-measure-label--dy { border-color: #4ade80; color: #4ade80; }
.mv-measure-label--dz { border-color: #60a5fa; color: #60a5fa; }

/* ── Dims chip ──────────────────────────────────────────── */
.mv-dims-chip {
  position: absolute;
  left: 0.75rem;
  bottom: 2.6rem;
  display: flex;
  gap: 0.9rem;
  padding: 0.35rem 0.7rem;
  background: rgba(6, 20, 27, 0.85);
  border: 1px solid var(--ph-border);
  border-radius: 8px;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ph-text);
  z-index: 2;
  pointer-events: none;
}
.mv-ax {
  font-style: normal;
  font-weight: 700;
  margin-right: 0.3rem;
}
.mv-ax--x { color: #f87171; }
.mv-ax--y { color: #4ade80; }
.mv-ax--z { color: #60a5fa; }

/* ── Status bar ─────────────────────────────────────────── */
.mv-statusbar {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.35rem 0.75rem;
  background: rgba(6, 20, 27, 0.75);
  border-top: 1px solid var(--ph-border);
  font-size: 0.68rem;
  color: var(--ph-text-muted);
  z-index: 2;
  pointer-events: none;
}
.mv-status-proj {
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-weight: 600;
  opacity: 0.75;
  white-space: nowrap;
}

/* ── Panel ──────────────────────────────────────────────── */
.mv-panel-toggle {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 1.1rem;
  height: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(6, 20, 27, 0.9);
  border: 1px solid var(--ph-border);
  border-right: none;
  border-radius: 8px 0 0 8px;
  color: var(--ph-text-muted);
  cursor: pointer;
  z-index: 3;
  transition: color 0.15s;
}
.mv-panel-toggle:hover { color: var(--ph-accent, #22d3ee); }

.mv-panel {
  width: 236px;
  flex-shrink: 0;
  border-left: 1px solid var(--ph-border);
  background: rgba(0, 0, 0, 0.25);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.mv-section {
  padding: 0.75rem 0.85rem;
  border-bottom: 1px solid var(--ph-border);
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.mv-section-title {
  margin: 0;
  font-size: 0.66rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ph-text-muted);
  display: flex;
  align-items: center;
  gap: 0.35rem;
}
.mv-section-title .mdi { color: var(--ph-accent, #22d3ee); font-size: 0.85rem; }

.mv-section-hint {
  margin: 0;
  font-size: 0.72rem;
  color: var(--ph-text-muted);
  opacity: 0.75;
}

.mv-kv {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  font-size: 0.74rem;
  color: var(--ph-text-muted);
}
.mv-kv span:last-child {
  color: var(--ph-text);
  font-weight: 600;
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.mv-kv--total span:last-child { color: var(--ph-accent, #22d3ee); }
.mv-kv--dx span:last-child { color: #f87171; }
.mv-kv--dy span:last-child { color: #4ade80; }
.mv-kv--dz span:last-child { color: #60a5fa; }

.mv-mini-btn {
  align-self: flex-start;
  display: flex;
  align-items: center;
  gap: 0.3rem;
  background: rgba(255,255,255,0.05);
  border: 1px solid var(--ph-border);
  border-radius: 6px;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 0.7rem;
  font-weight: 600;
  padding: 0.25rem 0.55rem;
  transition: color 0.15s, background 0.15s;
}
.mv-mini-btn:hover { color: var(--ph-text); background: rgba(255,255,255,0.1); }

.mv-axis-row {
  display: flex;
  gap: 0.3rem;
  flex-wrap: wrap;
}

.mv-chip-btn {
  background: rgba(255,255,255,0.05);
  border: 1px solid var(--ph-border);
  border-radius: 5px;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.25rem 0.5rem;
  transition: color 0.15s, background 0.15s;
}
.mv-chip-btn:hover { color: var(--ph-text); }
.mv-chip-btn--active {
  color: var(--ph-accent, #22d3ee);
  border-color: var(--ph-accent, #22d3ee);
  background: rgba(34,211,238,0.1);
}

.mv-slider {
  width: 100%;
  accent-color: var(--ph-accent, #22d3ee);
}

/* ── Parts list ─────────────────────────────────────────── */
.mv-part-list {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.mv-part-row {
  display: flex;
  align-items: center;
  gap: 0.15rem;
  border-radius: 6px;
  padding: 0.1rem 0.15rem;
  transition: background 0.15s;
}
.mv-part-row:hover { background: rgba(255,255,255,0.04); }

.mv-part-name {
  flex: 1;
  min-width: 0;
  background: none;
  border: none;
  color: var(--ph-text);
  cursor: pointer;
  font-size: 0.74rem;
  text-align: left;
  padding: 0.2rem 0.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-radius: 4px;
}
.mv-part-name:hover { color: var(--ph-accent, #22d3ee); }

.mv-part-btn {
  flex-shrink: 0;
  width: 1.5rem;
  height: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: 4px;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 0.85rem;
  transition: color 0.15s, background 0.15s;
}
.mv-part-btn:hover { color: var(--ph-text); background: rgba(255,255,255,0.08); }
.mv-part-btn--active { color: var(--ph-accent, #22d3ee); }

.mv-file-item {
  margin: 0;
  font-family: monospace;
  font-size: 0.7rem;
  color: var(--ph-text-muted);
  word-break: break-all;
}
</style>
