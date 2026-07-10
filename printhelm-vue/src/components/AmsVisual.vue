<script setup lang="ts">
import { computed } from 'vue'
import type { ApiMaterial } from '@/client/printhelm-web-openapi'

const props = defineProps<{
  materials: ApiMaterial[]
  humidity?: string
  amsTemp?: string
  nozzleTemp?: number
  nozzleTargetTemp?: number
  printing?: boolean
}>()

// ── Geometry ──────────────────────────────────────────────────────────────
const SLOT_W = 96
const GAP = 10
const PAD = 14
const BOX_Y = 2
const BOX_H = 184
const BAY_Y = 36
const BAY_H = 142
const SPOOL_CY = 82
const FLANGE_R = 30
const HUB_R = 9
const TUBE_START_Y = BOX_Y + BOX_H // 186
const FIN_Y = 244
const FIN_COUNT = 5
const FIN_STEP = 5.4
const FIN_H = 3.2
const BLOCK_Y = 271
const BLOCK_H = 13
const TIP_END_Y = 298
const HEIGHT = 322

const width = computed(
  () => PAD * 2 + props.materials.length * SLOT_W + (props.materials.length - 1) * GAP,
)
const centerX = computed(() => width.value / 2)

function materialColor(color?: string): string {
  if (!color) return '#3a3a3a'
  return color.startsWith('#') ? color : `#${color}`
}

function truncate(s: string, max = 14): string {
  return s.length > max ? s.slice(0, max - 1) + '…' : s
}

interface SlotVm {
  bayX: number
  cx: number
  color: string
  isEmpty: boolean
  loaded: boolean
  label: string
  sub: string
  tooltip: string
  bandR: number
  bandW: number
  outerR: number
  tubePath: string
}

const slots = computed<SlotVm[]>(() =>
  props.materials.map((mat, i) => {
    const bayX = PAD + i * (SLOT_W + GAP)
    const cx = bayX + SLOT_W / 2
    const isEmpty = !mat.name && !mat.type
    const remainKnown = mat.remain != null && mat.remain >= 0
    const frac = remainKnown ? Math.min(Math.max(mat.remain! / 100, 0), 1) : 0.85
    // Filament band: annulus around the hub whose thickness tracks remaining %
    const bandW = 4 + frac * 14
    const subParts: string[] = []
    if (mat.type) subParts.push(mat.type)
    if (remainKnown) subParts.push(`${Math.round(mat.remain!)}%`)
    const tipLines = [mat.name ?? 'Empty bay']
    if (mat.type) tipLines.push(mat.type)
    if (remainKnown) tipLines.push(`Remaining: ${Math.round(mat.remain!)}%`)
    return {
      bayX,
      cx,
      color: materialColor(mat.color),
      isEmpty,
      loaded: !!mat.loaded,
      label: truncate(mat.name ?? 'Empty'),
      sub: subParts.join(' · '),
      tooltip: tipLines.join('\n'),
      bandR: HUB_R + bandW / 2,
      bandW,
      outerR: HUB_R + bandW,
      tubePath: `M ${cx} ${TUBE_START_Y} C ${cx} 212, ${centerX.value} 206, ${centerX.value} 232 L ${centerX.value} 242`,
    }
  }),
)

const loadedSlot = computed(() => slots.value.find(s => s.loaded && !s.isEmpty) ?? null)

// Loaded filament continues past the merge point, through the hotend, to the tip
const loadedPath = computed(() => {
  const s = loadedSlot.value
  if (!s) return ''
  return `M ${s.cx} ${TUBE_START_Y} C ${s.cx} 212, ${centerX.value} 206, ${centerX.value} 232 L ${centerX.value} ${TIP_END_Y - 1}`
})

const fins = computed(() =>
  Array.from({ length: FIN_COUNT }, (_, i) => FIN_Y + i * FIN_STEP),
)

const nozzleTip = computed(() => {
  const x = centerX.value
  return [
    [x - 7, BLOCK_Y + BLOCK_H],
    [x + 7, BLOCK_Y + BLOCK_H],
    [x + 2.2, 294],
    [x + 2.2, TIP_END_Y],
    [x - 2.2, TIP_END_Y],
    [x - 2.2, 294],
  ]
    .map(p => p.join(','))
    .join(' ')
})

const isHot = computed(() => (props.nozzleTemp ?? 0) > 60)

const envLine = computed(() => {
  const parts: string[] = []
  if (props.amsTemp) parts.push(`${props.amsTemp}°C`)
  if (props.humidity) parts.push(`${props.humidity}% RH`)
  return parts.join('  ·  ')
})

const tempLine = computed(() => {
  if (props.nozzleTemp == null && props.nozzleTargetTemp == null) return ''
  const cur = props.nozzleTemp != null ? `${Math.round(props.nozzleTemp)}°` : '—'
  const tgt = props.nozzleTargetTemp != null ? `${Math.round(props.nozzleTargetTemp)}°` : '—'
  return `${cur} / ${tgt}`
})
</script>

<template>
  <div class="amsv">
    <svg
      :viewBox="`0 0 ${width} ${HEIGHT}`"
      class="amsv-svg"
      role="img"
      aria-label="AMS material system diagram"
    >
      <defs>
        <radialGradient id="amsv-sheen" cx="0.35" cy="0.3" r="0.75">
          <stop offset="0%" stop-color="#fff" stop-opacity="0.22" />
          <stop offset="55%" stop-color="#fff" stop-opacity="0.04" />
          <stop offset="100%" stop-color="#fff" stop-opacity="0" />
        </radialGradient>
        <linearGradient id="amsv-box" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="rgba(148,210,230,0.06)" />
          <stop offset="100%" stop-color="rgba(148,210,230,0.015)" />
        </linearGradient>
        <linearGradient id="amsv-metal" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#4b5563" />
          <stop offset="50%" stop-color="#374151" />
          <stop offset="100%" stop-color="#1f2937" />
        </linearGradient>
        <linearGradient id="amsv-brass" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#a16207" />
          <stop offset="100%" stop-color="#713f12" />
        </linearGradient>
        <radialGradient id="amsv-heat" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0%" stop-color="#f97316" stop-opacity="0.55" />
          <stop offset="60%" stop-color="#f97316" stop-opacity="0.18" />
          <stop offset="100%" stop-color="#f97316" stop-opacity="0" />
        </radialGradient>
      </defs>

      <!-- ── AMS unit body ── -->
      <rect
        x="2"
        :y="BOX_Y"
        :width="width - 4"
        :height="BOX_H"
        rx="14"
        fill="url(#amsv-box)"
        stroke="rgba(148,210,230,0.14)"
        stroke-width="1.5"
      />
      <text x="16" y="23" class="amsv-header">Material Station</text>
      <text v-if="envLine" :x="width - 16" y="23" class="amsv-env" text-anchor="end">
        {{ envLine }}
      </text>
      <line
        x1="10"
        :x2="width - 10"
        y1="30"
        y2="30"
        stroke="rgba(148,210,230,0.08)"
        stroke-width="1"
      />

      <!-- ── Slots ── -->
      <g v-for="(s, i) in slots" :key="i">
        <title>{{ s.tooltip }}</title>

        <!-- Bay -->
        <rect
          :x="s.bayX"
          :y="BAY_Y"
          :width="SLOT_W"
          :height="BAY_H"
          rx="10"
          fill="rgba(6,13,19,0.55)"
          :stroke="s.loaded ? s.color : 'rgba(148,210,230,0.1)'"
          :stroke-opacity="s.loaded ? 0.7 : 1"
          :stroke-width="s.loaded ? 1.6 : 1"
        />
        <rect
          v-if="s.loaded"
          :x="s.bayX - 1.5"
          :y="BAY_Y - 1.5"
          :width="SLOT_W + 3"
          :height="BAY_H + 3"
          rx="11.5"
          fill="none"
          :stroke="s.color"
          stroke-opacity="0.2"
          stroke-width="4"
        />

        <!-- Slot number + status LED -->
        <text :x="s.bayX + 9" :y="BAY_Y + 15" class="amsv-slot-num">{{ i + 1 }}</text>
        <circle
          v-if="s.loaded"
          :cx="s.bayX + SLOT_W - 11"
          :cy="BAY_Y + 11"
          r="4.5"
          fill="#4ade80"
          opacity="0.35"
        />
        <circle
          :cx="s.bayX + SLOT_W - 11"
          :cy="BAY_Y + 11"
          r="2.4"
          :fill="s.loaded ? '#4ade80' : 'rgba(255,255,255,0.12)'"
        />

        <!-- Spool -->
        <g v-if="!s.isEmpty">
          <circle
            :cx="s.cx"
            :cy="SPOOL_CY"
            :r="FLANGE_R"
            fill="#10161c"
            stroke="rgba(255,255,255,0.14)"
            stroke-width="2"
          />
          <!-- filament band (thickness = remaining) -->
          <circle
            :cx="s.cx"
            :cy="SPOOL_CY"
            :r="s.bandR"
            fill="none"
            :stroke="s.color"
            :stroke-width="s.bandW"
          />
          <circle
            :cx="s.cx"
            :cy="SPOOL_CY"
            :r="s.outerR"
            fill="none"
            stroke="rgba(255,255,255,0.12)"
            stroke-width="1"
          />
          <!-- winding texture -->
          <circle
            :cx="s.cx"
            :cy="SPOOL_CY"
            :r="HUB_R + s.bandW * 0.38"
            fill="none"
            stroke="rgba(0,0,0,0.28)"
            stroke-width="0.8"
          />
          <circle
            :cx="s.cx"
            :cy="SPOOL_CY"
            :r="HUB_R + s.bandW * 0.72"
            fill="none"
            stroke="rgba(0,0,0,0.28)"
            stroke-width="0.8"
          />
          <!-- hub -->
          <circle
            :cx="s.cx"
            :cy="SPOOL_CY"
            :r="HUB_R"
            fill="#0b1117"
            stroke="rgba(255,255,255,0.16)"
            stroke-width="1.2"
          />
          <circle :cx="s.cx" :cy="SPOOL_CY" r="3" fill="#060d13" />
          <!-- sheen -->
          <circle
            :cx="s.cx"
            :cy="SPOOL_CY"
            :r="FLANGE_R - 1"
            fill="url(#amsv-sheen)"
            pointer-events="none"
          />
        </g>
        <g v-else>
          <circle
            :cx="s.cx"
            :cy="SPOOL_CY"
            :r="FLANGE_R"
            fill="none"
            stroke="rgba(255,255,255,0.1)"
            stroke-width="1.5"
            stroke-dasharray="4 5"
          />
          <circle :cx="s.cx" :cy="SPOOL_CY" :r="HUB_R" fill="none" stroke="rgba(255,255,255,0.08)" stroke-width="1" />
        </g>

        <!-- Labels -->
        <text :x="s.cx" y="134" class="amsv-name" text-anchor="middle">{{ s.label }}</text>
        <text v-if="s.sub" :x="s.cx" y="148" class="amsv-sub" text-anchor="middle">{{ s.sub }}</text>
      </g>

      <!-- ── Feed tubes (idle) ── -->
      <path
        v-for="(s, i) in slots"
        :key="'tube-' + i"
        :d="s.tubePath"
        fill="none"
        stroke="rgba(148,210,230,0.09)"
        stroke-width="3"
        stroke-linecap="round"
      />

      <!-- ── Loaded filament path ── -->
      <g v-if="loadedSlot">
        <path
          :d="loadedPath"
          fill="none"
          :stroke="loadedSlot.color"
          stroke-width="7"
          stroke-opacity="0.22"
          stroke-linecap="round"
        />
        <path
          :d="loadedPath"
          fill="none"
          :stroke="loadedSlot.color"
          stroke-width="3"
          stroke-linecap="round"
        />
        <path
          v-if="printing"
          :d="loadedPath"
          fill="none"
          stroke="rgba(255,255,255,0.75)"
          stroke-width="1.2"
          stroke-linecap="round"
          class="amsv-flow"
        />
      </g>

      <!-- ── Hotend ── -->
      <ellipse
        v-if="isHot"
        :cx="centerX"
        :cy="BLOCK_Y + BLOCK_H / 2 + 6"
        rx="34"
        ry="26"
        fill="url(#amsv-heat)"
        class="amsv-heat-glow"
      />
      <g>
        <rect
          v-for="(y, i) in fins"
          :key="'fin-' + i"
          :x="centerX - 17"
          :y="y"
          width="34"
          :height="FIN_H"
          rx="1.6"
          fill="url(#amsv-metal)"
        />
        <rect
          :x="centerX - 14"
          :y="BLOCK_Y"
          width="28"
          :height="BLOCK_H"
          rx="2.5"
          :fill="isHot ? '#7f2d12' : 'url(#amsv-metal)'"
          stroke="rgba(255,255,255,0.12)"
          stroke-width="0.8"
        />
        <polygon
          :points="nozzleTip"
          fill="url(#amsv-brass)"
          stroke="rgba(255,255,255,0.1)"
          stroke-width="0.8"
        />
      </g>

      <!-- molten tip -->
      <circle
        v-if="loadedSlot && printing"
        :cx="centerX"
        :cy="TIP_END_Y + 3"
        r="2.4"
        :fill="loadedSlot.color"
        class="amsv-melt"
      />

      <!-- Nozzle temp -->
      <text v-if="tempLine" :x="centerX" :y="HEIGHT - 4" class="amsv-temp" text-anchor="middle">
        <tspan :class="{ 'amsv-temp--hot': isHot }">{{ tempLine }}</tspan>
      </text>
    </svg>
  </div>
</template>

<style scoped>
.amsv {
  max-width: 560px;
  margin: 0 auto;
}

.amsv-svg {
  width: 100%;
  height: auto;
  display: block;
}

.amsv-header {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.6px;
  text-transform: uppercase;
  fill: var(--ph-text-muted);
}

.amsv-env {
  font-size: 10px;
  font-weight: 600;
  fill: var(--ph-text-muted);
}

.amsv-slot-num {
  font-size: 9px;
  font-weight: 700;
  fill: rgba(127, 168, 181, 0.55);
}

.amsv-name {
  font-size: 11px;
  font-weight: 600;
  fill: var(--ph-text);
}

.amsv-sub {
  font-size: 9.5px;
  fill: var(--ph-text-muted);
}

.amsv-temp {
  font-size: 11px;
  font-weight: 600;
  fill: var(--ph-text-muted);
}

.amsv-temp--hot {
  fill: #fb923c;
}

.amsv-flow {
  stroke-dasharray: 8 16;
  animation: amsv-flow 1.1s linear infinite;
}

@keyframes amsv-flow {
  to {
    stroke-dashoffset: -24;
  }
}

.amsv-melt {
  animation: amsv-pulse 1.4s ease-in-out infinite;
}

.amsv-heat-glow {
  animation: amsv-pulse 2.4s ease-in-out infinite;
}

@keyframes amsv-pulse {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 1;
  }
}
</style>
