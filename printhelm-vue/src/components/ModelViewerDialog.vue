<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
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

type Unit = 'mm' | 'cm' | 'm' | 'in'
const UNITS: Unit[] = ['mm', 'cm', 'm', 'in']

const loading = ref(true)
const error = ref('')
const canvasRef = ref<HTMLCanvasElement | null>(null)
const wireframe = ref(false)
const autoRotate = ref(false)
const measureMode = ref(false)
const measureTool = ref<'distance' | 'surface'>('distance')
const measureDist = ref<number | null>(null)
const surfaceArea = ref<number | null>(null)
const labelScreen = ref({ visible: false, x: 0, y: 0 })
const showDims = ref(false)
const showInfo = ref(false)
const clipEnabled = ref(false)
const clipAxis = ref<'x' | 'y' | 'z'>('y')
const clipPos = ref(100)
// Mesh files carry no unit metadata: glTF is nominally meters, STL/OBJ conventionally mm.
// modelUnit = what one file unit represents; displayUnit = what measurements are shown in.
const modelUnit = ref<Unit>(['glb', 'gltf'].includes(fileExt()) ? 'm' : 'mm')
const displayUnit = ref<Unit>('mm')
const UNIT_MM: Record<Unit, number> = { mm: 1, cm: 10, m: 1000, in: 25.4 }
const unitScale = computed(() => UNIT_MM[modelUnit.value] / UNIT_MM[displayUnit.value])
const dims = ref<{ x: number; y: number; z: number } | null>(null)
const stats = ref<{ tris: number; verts: number; volume: number; area: number } | null>(null)

const labelDisplay = computed(() => {
  if (measureDist.value != null) return fmtLen(measureDist.value)
  if (surfaceArea.value != null) return fmtArea(surfaceArea.value)
  return ''
})

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let controls: OrbitControls | null = null
let animFrameId: number | null = null
let resizeObserver: ResizeObserver | null = null
let pmrem: THREE.PMREMGenerator | null = null
let modelRoot: THREE.Object3D | null = null
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
const labelAnchor = new THREE.Vector3()
const labelProj = new THREE.Vector3()
let pointerDownPos = { x: 0, y: 0 }

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
  canvasEl = canvas

  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.setSize(w, h, false)
  renderer.setClearColor(0x000000, 0)
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.localClippingEnabled = true

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

  object.updateWorldMatrix(true, true)
  worldBox.setFromObject(object)
  dims.value = { x: size.x, y: size.y, z: size.z }
  stats.value = computeMeshStats(object)

  const maxDim = Math.max(size.x, size.y, size.z) || 1
  modelMaxDim = maxDim
  const gridSpan = Math.ceil((Math.max(size.x, size.z) || 1) * 1.6 / 10) * 10 || 10
  const grid = new THREE.GridHelper(gridSpan, Math.min(Math.ceil(gridSpan / (gridSpan / 20)), 30), 0x1e4a5a, 0x1e4a5a)
  ;(grid.material as THREE.LineBasicMaterial).opacity = 0.5
  ;(grid.material as THREE.LineBasicMaterial).transparent = true
  scene.add(grid)

  measureGroup = new THREE.Group()
  scene.add(measureGroup)

  camera.position.set(maxDim * 1.1, maxDim * 0.9, maxDim * 1.5)
  initialCameraPos = camera.position.clone()
  initialTarget = new THREE.Vector3(0, size.y / 2, 0)

  controls = new OrbitControls(camera, canvas)
  controls.target.copy(initialTarget)
  controls.enableDamping = true
  controls.dampingFactor = 0.06

  canvas.addEventListener('pointerdown', onPointerDown)
  canvas.addEventListener('pointerup', onPointerUp)

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
    updateFloatLabel()
    controls!.update()
    renderer!.render(scene!, camera!)
  }
  animate()
}

/** Triangle/vertex counts plus signed-tetrahedron volume and surface area in world units */
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

// --- Unit-aware formatting (raw values are in file units; converted for display) ---

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

// --- Measurement ---

function toggleMeasure() {
  measureMode.value = !measureMode.value
  if (measureMode.value) autoRotate.value = false
}

function setMeasureTool(tool: 'distance' | 'surface') {
  measureTool.value = tool
  clearMeasurement()
}

function onPointerDown(e: PointerEvent) {
  pointerDownPos = { x: e.clientX, y: e.clientY }
}

function onPointerUp(e: PointerEvent) {
  if (!measureMode.value || !camera || !modelRoot || !canvasEl) return
  // Ignore orbit drags; only treat near-stationary clicks as point picks
  if (Math.hypot(e.clientX - pointerDownPos.x, e.clientY - pointerDownPos.y) > 5) return
  const rect = canvasEl.getBoundingClientRect()
  const ndc = new THREE.Vector2(
    ((e.clientX - rect.left) / rect.width) * 2 - 1,
    -((e.clientY - rect.top) / rect.height) * 2 + 1,
  )
  raycaster.setFromCamera(ndc, camera)
  const hit = raycaster.intersectObject(modelRoot, true)[0]
  if (!hit) return
  if (measureTool.value === 'surface') {
    pickSurface(hit)
    return
  }
  if (measurePoints.length >= 2) clearMeasurement()
  const snap = snapToFeature(hit, e.clientX - rect.left, e.clientY - rect.top, rect)
  addMeasurePoint(snap.point, snap.snapped)
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
  if (!face || !camera) return { point: hit.point.clone(), snapped: false }
  const pos = (mesh.geometry as THREE.BufferGeometry).getAttribute('position')
  const verts = [face.a, face.b, face.c].map((i) =>
    new THREE.Vector3().fromBufferAttribute(pos, i).applyMatrix4(mesh.matrixWorld),
  )
  const proj = new THREE.Vector3()
  const screenDist = (p: THREE.Vector3) => {
    proj.copy(p).project(camera!)
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

  if (measurePoints.length === 2) {
    const [p1, p2] = measurePoints
    const line = new THREE.Line(
      new THREE.BufferGeometry().setFromPoints([p1, p2]),
      new THREE.LineBasicMaterial({ color: 0x22d3ee, depthTest: false }),
    )
    line.renderOrder = 999
    measureGroup.add(line)
    measureDist.value = p1.distanceTo(p2)
    labelAnchor.copy(p1).add(p2).multiplyScalar(0.5)
  }
}

function clearMeasurement() {
  measurePoints = []
  measureDist.value = null
  surfaceArea.value = null
  labelScreen.value = { visible: false, x: 0, y: 0 }
  if (!measureGroup) return
  for (const child of measureGroup.children) {
    if (child instanceof THREE.Mesh || child instanceof THREE.Line) {
      child.geometry.dispose()
      ;(child.material as THREE.Material).dispose()
    }
  }
  measureGroup.clear()
}

function updateFloatLabel() {
  if ((measureDist.value == null && surfaceArea.value == null) || !camera || !canvasEl) {
    if (labelScreen.value.visible) labelScreen.value = { visible: false, x: 0, y: 0 }
    return
  }
  labelProj.copy(labelAnchor).project(camera)
  labelScreen.value = {
    visible: labelProj.z < 1,
    x: ((labelProj.x + 1) / 2) * canvasEl.clientWidth,
    y: ((1 - labelProj.y) / 2) * canvasEl.clientHeight,
  }
}

// --- Surface (face) picking ---

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
  labelAnchor.copy(hit.point)
}

// --- Bounding-box dimensions ---

function toggleDims() {
  showDims.value = !showDims.value
  if (!scene) return
  if (showDims.value && !boxHelper) {
    boxHelper = new THREE.Box3Helper(worldBox, 0x22d3ee)
    ;(boxHelper.material as THREE.LineBasicMaterial).transparent = true
    ;(boxHelper.material as THREE.LineBasicMaterial).opacity = 0.7
    scene.add(boxHelper)
  }
  if (boxHelper) boxHelper.visible = showDims.value
}

// --- Cross-section clipping ---

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

function toggleAutoRotate() {
  autoRotate.value = !autoRotate.value
  // Rotating the model detaches it from world-space measurement overlays
  if (autoRotate.value) {
    measureMode.value = false
    clearMeasurement()
  }
}

function resetView() {
  if (!camera || !controls) return
  camera.position.copy(initialCameraPos)
  controls.target.copy(initialTarget)
  if (modelRoot) modelRoot.rotation.y = 0
  autoRotate.value = false
  clearMeasurement()
}

function teardown() {
  if (animFrameId != null) { cancelAnimationFrame(animFrameId); animFrameId = null }
  resizeObserver?.disconnect(); resizeObserver = null
  if (canvasEl) {
    canvasEl.removeEventListener('pointerdown', onPointerDown)
    canvasEl.removeEventListener('pointerup', onPointerUp)
    canvasEl = null
  }
  controls?.dispose(); controls = null
  clearMeasurement()
  measureGroup = null
  if (boxHelper) {
    boxHelper.geometry.dispose()
    ;(boxHelper.material as THREE.Material).dispose()
    boxHelper = null
  }
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
        <div class="mv-stage">
          <canvas ref="canvasRef" class="mv-canvas" :class="{ 'mv-canvas--measure': measureMode }" />

          <div
            v-if="labelScreen.visible && labelDisplay"
            class="mv-measure-label"
            :style="{ left: `${labelScreen.x}px`, top: `${labelScreen.y}px` }"
          >
            {{ labelDisplay }}
          </div>

          <div v-if="showDims && dims" class="mv-panel mv-panel--dims">
            <div class="mv-panel-row"><span class="mv-dim-axis">X</span> {{ fmtLen(dims.x) }}</div>
            <div class="mv-panel-row"><span class="mv-dim-axis">Y</span> {{ fmtLen(dims.y) }}</div>
            <div class="mv-panel-row"><span class="mv-dim-axis">Z</span> {{ fmtLen(dims.z) }}</div>
          </div>

          <div v-if="showInfo && stats && dims" class="mv-panel mv-panel--info">
            <div class="mv-panel-row"><span>Size</span><span>{{ fmtAdaptive(dims.x * unitScale) }} × {{ fmtAdaptive(dims.y * unitScale) }} × {{ fmtAdaptive(dims.z * unitScale) }} {{ displayUnit }}</span></div>
            <div class="mv-panel-row"><span>Triangles</span><span>{{ fmtNum(stats.tris) }}</span></div>
            <div class="mv-panel-row"><span>Vertices</span><span>{{ fmtNum(stats.verts) }}</span></div>
            <div class="mv-panel-row"><span>Volume</span><span>{{ fmtVol(stats.volume) }}</span></div>
            <div class="mv-panel-row"><span>Surface</span><span>{{ fmtArea(stats.area) }}</span></div>
          </div>
        </div>

        <div class="mv-controls">
          <button
            class="mv-ctrl-btn"
            :class="{ 'mv-ctrl-btn--active': measureMode }"
            title="Measure the model"
            @click="toggleMeasure"
          >
            <i class="mdi mdi-ruler" /> Measure
          </button>
          <div v-if="measureMode" class="mv-btn-group">
            <button
              class="mv-axis-btn"
              :class="{ 'mv-axis-btn--active': measureTool === 'distance' }"
              title="Point-to-point distance (snaps to vertices and edges)"
              @click="setMeasureTool('distance')"
            >
              Distance
            </button>
            <button
              class="mv-axis-btn"
              :class="{ 'mv-axis-btn--active': measureTool === 'surface' }"
              title="Click a face to measure its surface area"
              @click="setMeasureTool('surface')"
            >
              Face
            </button>
          </div>
          <button
            class="mv-ctrl-btn"
            :class="{ 'mv-ctrl-btn--active': showDims }"
            title="Show bounding-box dimensions"
            @click="toggleDims"
          >
            <i class="mdi mdi-arrow-expand-all" /> Dims
          </button>
          <button
            class="mv-ctrl-btn"
            :class="{ 'mv-ctrl-btn--active': clipEnabled }"
            title="Cross-section view"
            @click="clipEnabled = !clipEnabled"
          >
            <i class="mdi mdi-content-cut" /> Section
          </button>
          <div v-if="clipEnabled" class="mv-btn-group">
            <button
              v-for="axis in (['x', 'y', 'z'] as const)"
              :key="axis"
              class="mv-axis-btn"
              :class="{ 'mv-axis-btn--active': clipAxis === axis }"
              @click="clipAxis = axis"
            >
              {{ axis.toUpperCase() }}
            </button>
            <input v-model.number="clipPos" type="range" min="1" max="100" class="mv-slider" />
          </div>
          <button
            class="mv-ctrl-btn"
            :class="{ 'mv-ctrl-btn--active': showInfo }"
            title="Model statistics"
            @click="showInfo = !showInfo"
          >
            <i class="mdi mdi-information-outline" /> Info
          </button>
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
            @click="toggleAutoRotate"
          >
            <i class="mdi mdi-rotate-3d-variant" /> Rotate
          </button>
          <button class="mv-ctrl-btn" title="Reset view" @click="resetView">
            <i class="mdi mdi-restore" /> Reset
          </button>
          <div class="mv-btn-group" title="Units the file was exported in (glTF is nominally meters)">
            <span class="mv-unit-label">File</span>
            <button
              v-for="u in UNITS"
              :key="`file-${u}`"
              class="mv-axis-btn"
              :class="{ 'mv-axis-btn--active': modelUnit === u }"
              @click="modelUnit = u"
            >
              {{ u }}
            </button>
          </div>
          <div class="mv-btn-group" title="Units measurements are displayed in">
            <span class="mv-unit-label">Show</span>
            <button
              v-for="u in UNITS"
              :key="`show-${u}`"
              class="mv-axis-btn"
              :class="{ 'mv-axis-btn--active': displayUnit === u }"
              @click="displayUnit = u"
            >
              {{ u }}
            </button>
          </div>
          <span class="mv-hint">
            {{
              measureMode
                ? measureTool === 'distance'
                  ? 'Click two points — snaps to vertices and edges'
                  : 'Click a face to measure its area'
                : 'Drag to orbit · Scroll to zoom · Right-drag to pan'
            }}
          </span>
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

.mv-stage {
  flex: 1;
  position: relative;
  min-height: 0;
}

.mv-canvas {
  width: 100%;
  height: 100%;
  display: block;
  background: #060f14;
}
.mv-canvas--measure { cursor: crosshair; }

.mv-measure-label {
  position: absolute;
  transform: translate(-50%, -140%);
  background: rgba(6, 20, 27, 0.9);
  border: 1px solid var(--ph-accent, #22d3ee);
  color: var(--ph-accent, #22d3ee);
  padding: 0.15rem 0.5rem;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 600;
  pointer-events: none;
  white-space: nowrap;
}

.mv-panel {
  position: absolute;
  top: 0.75rem;
  background: rgba(6, 20, 27, 0.85);
  border: 1px solid var(--ph-border);
  border-radius: 8px;
  padding: 0.5rem 0.7rem;
  font-size: 0.72rem;
  color: var(--ph-text-muted);
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  pointer-events: none;
}
.mv-panel--dims { left: 0.75rem; }
.mv-panel--info { right: 0.75rem; min-width: 180px; }

.mv-panel-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  white-space: nowrap;
}
.mv-panel-row span:last-child { color: var(--ph-text); font-weight: 600; }

.mv-dim-axis {
  color: var(--ph-accent, #22d3ee);
  font-weight: 700;
  margin-right: 0.4rem;
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

.mv-btn-group {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.mv-unit-label {
  font-size: 0.62rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--ph-text-muted);
  opacity: 0.7;
  margin-right: 0.1rem;
}

.mv-axis-btn {
  background: rgba(255,255,255,0.05);
  border: 1px solid var(--ph-border);
  border-radius: 4px;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.25rem 0.45rem;
  transition: color 0.15s, background 0.15s;
}
.mv-axis-btn:hover { color: var(--ph-text); }
.mv-axis-btn--active {
  color: var(--ph-accent, #22d3ee);
  border-color: var(--ph-accent, #22d3ee);
  background: rgba(34,211,238,0.1);
}

.mv-slider {
  width: 110px;
  accent-color: var(--ph-accent, #22d3ee);
}

.mv-hint {
  margin-left: auto;
  font-size: 0.68rem;
  color: var(--ph-text-muted);
  opacity: 0.7;
  white-space: nowrap;
}
</style>
