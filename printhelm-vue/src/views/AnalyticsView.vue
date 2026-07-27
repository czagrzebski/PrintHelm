<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import Chart from 'primevue/chart'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Select from 'primevue/select'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import analyticsApi from '@/api/AnalyticsApi'
import type {
  ApiAnalyticsSummary,
  ApiAnalyticsTrendPoint,
  ApiPrinterAnalytics,
} from '@/client/printhelm-web-openapi'

// Series colors validated for CVD + contrast on the app's dark surface
const COLOR_COMPLETED = '#0ea5c6'
const COLOR_FAILED = '#e66767'
const COLOR_MINUTES = '#7c7ff2'
const COLOR_INK_MUTED = '#7fa8b5'
const COLOR_GRID = 'rgba(148, 210, 230, 0.08)'

const toast = useToast()

const days = ref(30)
const daysOptions = [
  { label: 'Last 7 days', value: 7 },
  { label: 'Last 30 days', value: 30 },
  { label: 'Last 90 days', value: 90 },
]

const loading = ref(true)
const summary = ref<ApiAnalyticsSummary | null>(null)
const trends = ref<ApiAnalyticsTrendPoint[]>([])
const printerStats = ref<ApiPrinterAnalytics[]>([])

async function fetchAll() {
  loading.value = true
  try {
    const [summaryRes, trendsRes, printersRes] = await Promise.all([
      analyticsApi.getAnalyticsSummary(days.value),
      analyticsApi.getAnalyticsTrends(days.value),
      analyticsApi.getPrinterAnalytics(days.value),
    ])
    summary.value = summaryRes.data
    trends.value = trendsRes.data
    printerStats.value = printersRes.data
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to load analytics', life: 4000 })
  } finally {
    loading.value = false
  }
}

watch(days, fetchAll)
onMounted(fetchAll)

function shortDate(iso?: string): string {
  if (!iso) return ''
  const date = new Date(`${iso}T00:00:00`)
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

const trendLabels = computed(() => trends.value.map((t) => shortDate(t.date)))

const printsChartData = computed(() => ({
  labels: trendLabels.value,
  datasets: [
    {
      label: 'Completed',
      data: trends.value.map((t) => t.printsCompleted ?? 0),
      backgroundColor: COLOR_COMPLETED,
      borderRadius: 4,
      maxBarThickness: 18,
    },
    {
      label: 'Failed / canceled',
      data: trends.value.map((t) => t.printsFailed ?? 0),
      backgroundColor: COLOR_FAILED,
      borderRadius: 4,
      maxBarThickness: 18,
    },
  ],
}))

const printsChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index' as const, intersect: false },
  plugins: {
    legend: {
      position: 'top' as const,
      align: 'end' as const,
      labels: { color: COLOR_INK_MUTED, boxWidth: 10, boxHeight: 10, usePointStyle: true, pointStyle: 'circle' },
    },
  },
  scales: {
    x: {
      stacked: true,
      grid: { display: false },
      ticks: { color: COLOR_INK_MUTED, maxTicksLimit: 12 },
      border: { color: COLOR_GRID },
    },
    y: {
      stacked: true,
      beginAtZero: true,
      grid: { color: COLOR_GRID },
      ticks: { color: COLOR_INK_MUTED, precision: 0 },
      border: { display: false },
    },
  },
}

const minutesChartData = computed(() => ({
  labels: trendLabels.value,
  datasets: [
    {
      label: 'Print hours',
      data: trends.value.map((t) => Math.round(((t.printMinutes ?? 0) / 60) * 10) / 10),
      borderColor: COLOR_MINUTES,
      backgroundColor: 'rgba(124, 127, 242, 0.12)',
      borderWidth: 2,
      pointRadius: 0,
      pointHoverRadius: 5,
      pointHoverBackgroundColor: COLOR_MINUTES,
      fill: true,
      tension: 0.3,
    },
  ],
}))

const minutesChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index' as const, intersect: false },
  plugins: { legend: { display: false } },
  scales: {
    x: {
      grid: { display: false },
      ticks: { color: COLOR_INK_MUTED, maxTicksLimit: 12 },
      border: { color: COLOR_GRID },
    },
    y: {
      beginAtZero: true,
      grid: { color: COLOR_GRID },
      ticks: { color: COLOR_INK_MUTED },
      border: { display: false },
    },
  },
}

const successRateLabel = computed(() => {
  const rate = summary.value?.successRate
  return rate != null ? `${Math.round(rate * 100)}%` : '—'
})

const printTimeLabel = computed(() => {
  const minutes = summary.value?.totalPrintMinutes ?? 0
  if (minutes < 60) return `${minutes}m`
  return `${(minutes / 60).toFixed(1)}h`
})

const filamentLabel = computed(() => {
  const grams = summary.value?.filamentGramsUsed ?? 0
  return grams >= 1000 ? `${(grams / 1000).toFixed(2)} kg` : `${Math.round(grams)} g`
})

const revenueLabel = computed(() => {
  const revenue = summary.value?.revenue ?? 0
  return revenue.toLocaleString(undefined, { style: 'currency', currency: 'USD' })
})

function printerHours(stats: ApiPrinterAnalytics): string {
  const minutes = stats.totalPrintMinutes ?? 0
  return minutes < 60 ? `${minutes}m` : `${(minutes / 60).toFixed(1)}h`
}

function printerSuccess(stats: ApiPrinterAnalytics): string {
  return stats.successRate != null ? `${Math.round(stats.successRate * 100)}%` : '—'
}

function printerGrams(stats: ApiPrinterAnalytics): string {
  const grams = stats.filamentGramsUsed ?? 0
  return grams >= 1000 ? `${(grams / 1000).toFixed(2)} kg` : `${Math.round(grams)} g`
}
</script>

<template>
  <div class="analytics-view">
    <Toast />
    <div class="page-header">
      <div>
        <h2 class="page-title">Analytics</h2>
        <p class="page-subtitle">Print activity, printer utilization and revenue</p>
      </div>
      <Select v-model="days" :options="daysOptions" option-label="label" option-value="value" />
    </div>

    <div class="stat-row">
      <div class="stat-tile">
        <span class="stat-value">{{ summary?.printsCompleted ?? '—' }}</span>
        <span class="stat-label">Prints completed</span>
      </div>
      <div class="stat-tile">
        <span class="stat-value">{{ successRateLabel }}</span>
        <span class="stat-label">Success rate</span>
      </div>
      <div class="stat-tile">
        <span class="stat-value">{{ printTimeLabel }}</span>
        <span class="stat-label">Print time</span>
      </div>
      <div class="stat-tile">
        <span class="stat-value">{{ filamentLabel }}</span>
        <span class="stat-label">Filament used</span>
      </div>
      <div class="stat-tile">
        <span class="stat-value">{{ revenueLabel }}</span>
        <span class="stat-label">Revenue invoiced</span>
      </div>
      <div class="stat-tile">
        <span class="stat-value">{{ summary?.openOrders ?? '—' }}</span>
        <span class="stat-label">Open orders</span>
      </div>
    </div>

    <div class="chart-grid">
      <div class="chart-card">
        <h3 class="card-title">Prints per day</h3>
        <div class="chart-wrap">
          <Chart type="bar" :data="printsChartData" :options="printsChartOptions" class="chart" />
        </div>
      </div>
      <div class="chart-card">
        <h3 class="card-title">Print hours per day</h3>
        <div class="chart-wrap">
          <Chart type="line" :data="minutesChartData" :options="minutesChartOptions" class="chart" />
        </div>
      </div>
    </div>

    <div class="table-card">
      <h3 class="card-title table-title">Printer utilization</h3>
      <DataTable :value="printerStats" :loading="loading" data-key="printerId" size="small"
        empty-message="No printers registered yet.">
        <Column field="printerName" header="Printer" style="min-width:160px" />
        <Column header="Completed" style="min-width:100px">
          <template #body="{ data }">{{ data.printsCompleted }}</template>
        </Column>
        <Column header="Failed" style="min-width:90px">
          <template #body="{ data }">{{ data.printsFailed }}</template>
        </Column>
        <Column header="Canceled" style="min-width:90px">
          <template #body="{ data }">{{ data.printsCanceled }}</template>
        </Column>
        <Column header="Success rate" style="min-width:110px">
          <template #body="{ data }">{{ printerSuccess(data) }}</template>
        </Column>
        <Column header="Print time" style="min-width:100px">
          <template #body="{ data }">{{ printerHours(data) }}</template>
        </Column>
        <Column header="Filament" style="min-width:100px">
          <template #body="{ data }">{{ printerGrams(data) }}</template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<style scoped>
.analytics-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

@keyframes fade-up {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  animation: fade-up 0.3s ease-out both;
}
.page-title { font-size: 1.5rem; font-weight: 700; margin: 0 0 0.25rem; color: var(--ph-text); }
.page-subtitle { font-size: 0.875rem; color: var(--ph-text-muted); margin: 0; }

.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  animation: fade-up 0.32s ease-out 0.05s both;
}

.stat-tile {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 1rem 1.25rem;
  background: var(--ph-glass);
  border: 1px solid var(--ph-glass-border);
  border-radius: 14px;
  box-shadow: var(--ph-shadow-card);
}
.stat-value { font-size: 1.5rem; font-weight: 700; color: var(--ph-text); }
.stat-label { font-size: 0.8rem; color: var(--ph-text-muted); }

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 1rem;
  animation: fade-up 0.34s ease-out 0.08s both;
}

.chart-card,
.table-card {
  background: var(--ph-glass);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border: 1px solid var(--ph-glass-border);
  border-radius: 16px;
  box-shadow: var(--ph-shadow-card), 0 1px 0 rgba(255, 255, 255, 0.05) inset;
}

.chart-card { padding: 1.25rem; }
.card-title {
  margin: 0 0 0.75rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--ph-text);
}
.chart-wrap { height: 260px; }
.chart { height: 100%; }

.table-card {
  overflow: hidden;
  animation: fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both;
}
.table-title { padding: 1.25rem 1.25rem 0; }
</style>
