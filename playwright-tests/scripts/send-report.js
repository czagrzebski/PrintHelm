'use strict'

const fs = require('fs')
const nodemailer = require('nodemailer')

// --- read secrets from Docker secret files or env vars ---
function secret(name) {
  const path = `/run/secrets/${name}`
  if (fs.existsSync(path)) return fs.readFileSync(path, 'utf-8').trim()
  return process.env[name] ?? ''
}

const smtpHost = process.env.SMTP_HOST
const smtpPort = parseInt(process.env.SMTP_PORT || '587', 10)
const smtpUser = secret('SMTP_USERNAME')
const smtpPass = secret('SMTP_PASSWORD')
const from = process.env.SMTP_FROM
const to = process.env.SMTP_TO

if (!smtpHost || !smtpUser || !smtpPass || !from || !to) {
  console.error('[send-report] Missing required SMTP configuration — skipping email.')
  process.exit(0)
}

console.log(`[send-report] SMTP: ${smtpHost}:${smtpPort} from=${from} to=${to}`)

// --- parse results ---
const resultsPath = '/app/results.json'
console.log(`[send-report] Reading results from ${resultsPath}`)

let report
try {
  report = JSON.parse(fs.readFileSync(resultsPath, 'utf-8'))
  console.log('[send-report] Results file parsed successfully')
} catch {
  console.error('[send-report] Could not read results file — sending failure email')
  sendEmail(
    buildSubject(false),
    buildBody(null),
  )
  return
}

const { stats } = report
const passed = stats.expected ?? 0
const failed = stats.unexpected ?? 0
const flaky = stats.flaky ?? 0
const skipped = stats.skipped ?? 0
const durationSec = ((stats.duration ?? 0) / 1000).toFixed(1)
const overall = failed === 0

console.log(`[send-report] Results — passed: ${passed}, failed: ${failed}, flaky: ${flaky}, skipped: ${skipped}, duration: ${durationSec}s`)
console.log(`[send-report] Overall: ${overall ? 'PASSED' : 'FAILED'}`)

/** Flatten nested suites → collect all specs */
function collectSpecs(suites) {
  const specs = []
  for (const suite of suites) {
    for (const spec of suite.specs ?? []) specs.push({ suite: suite.title, spec })
    specs.push(...collectSpecs(suite.suites ?? []))
  }
  return specs
}

const allSpecs = collectSpecs(report.suites ?? [])

// --- read uploaded trace names written by upload-traces.js ---
let traceNames = []
try {
  traceNames = JSON.parse(fs.readFileSync('/app/trace-names.json', 'utf-8'))
  console.log(`[send-report] Loaded ${traceNames.length} trace name(s) from trace-names.json`)
} catch {
  console.log('[send-report] No trace-names.json found — traces section will be omitted')
}

function buildSubject(ok) {
  const status = ok ? '✅ PASSED' : '❌ FAILED'
  return `[PrintHelm] Deployment Checkout ${status} — ${new Date().toUTCString()}`
}

function specRow({ suite, spec }) {
  const test = spec.tests?.[0]
  const status = test?.status ?? 'unknown'
  const lastResult = test?.results?.at(-1)
  const duration = lastResult ? `${(lastResult.duration / 1000).toFixed(2)}s` : '—'
  const error = lastResult?.error?.message?.replace(/</g, '&lt;').replace(/>/g, '&gt;') ?? ''

  const dot = status === 'expected' ? '🟢' : status === 'flaky' ? '🟡' : status === 'skipped' ? '⚪' : '🔴'
  const label = status === 'expected' ? 'passed' : status === 'unexpected' ? 'failed' : status

  let row = `
    <tr>
      <td style="padding:8px 12px;border-bottom:1px solid #2a2a3a;">${dot} ${spec.title}</td>
      <td style="padding:8px 12px;border-bottom:1px solid #2a2a3a;color:#888;">${suite}</td>
      <td style="padding:8px 12px;border-bottom:1px solid #2a2a3a;text-align:right;color:#888;">${duration}</td>
      <td style="padding:8px 12px;border-bottom:1px solid #2a2a3a;font-weight:600;color:${label === 'passed' ? '#4ade80' : label === 'failed' ? '#f87171' : '#facc15'};">${label}</td>
    </tr>`

  if (error) {
    row += `
    <tr>
      <td colspan="4" style="padding:6px 12px 10px 32px;border-bottom:1px solid #2a2a3a;font-family:monospace;font-size:12px;color:#f87171;white-space:pre-wrap;">${error}</td>
    </tr>`
  }

  return row
}

function buildBody(report) {
  if (!report) {
    return `
    <div style="font-family:sans-serif;background:#0f1117;color:#e2e8f0;padding:32px;border-radius:12px;max-width:640px;margin:0 auto;">
      <h2 style="margin:0 0 8px;color:#f87171;">❌ Deployment Checkout — Results Unavailable</h2>
      <p style="color:#888;margin:0;">The test runner exited before producing a results file. Check container logs for details.</p>
    </div>`
  }

  const statusColor = overall ? '#4ade80' : '#f87171'
  const statusLabel = overall ? '✅ ALL TESTS PASSED' : '❌ TESTS FAILED'

  return `
  <div style="font-family:sans-serif;background:#0f1117;color:#e2e8f0;padding:32px;border-radius:12px;max-width:680px;margin:0 auto;">
    <h2 style="margin:0 0 4px;color:${statusColor};">${statusLabel}</h2>
    <p style="margin:0 0 24px;color:#888;font-size:14px;">
      PrintHelm &mdash; Deployment Checkout &mdash; ${new Date().toUTCString()}
    </p>

    <table style="width:100%;border-collapse:collapse;margin-bottom:24px;background:#161622;border-radius:8px;overflow:hidden;">
      <tr>
        <td style="padding:12px 16px;color:#888;font-size:13px;">Passed</td>
        <td style="padding:12px 16px;font-weight:700;color:#4ade80;">${passed}</td>
        <td style="padding:12px 16px;color:#888;font-size:13px;">Failed</td>
        <td style="padding:12px 16px;font-weight:700;color:#f87171;">${failed}</td>
        <td style="padding:12px 16px;color:#888;font-size:13px;">Flaky</td>
        <td style="padding:12px 16px;font-weight:700;color:#facc15;">${flaky}</td>
        <td style="padding:12px 16px;color:#888;font-size:13px;">Skipped</td>
        <td style="padding:12px 16px;font-weight:700;color:#94a3b8;">${skipped}</td>
        <td style="padding:12px 16px;color:#888;font-size:13px;">Duration</td>
        <td style="padding:12px 16px;font-weight:700;">${durationSec}s</td>
      </tr>
    </table>

    <table style="width:100%;border-collapse:collapse;background:#161622;border-radius:8px;overflow:hidden;">
      <thead>
        <tr style="background:#1e1e2e;">
          <th style="padding:10px 12px;text-align:left;font-size:12px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:.05em;">Test</th>
          <th style="padding:10px 12px;text-align:left;font-size:12px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:.05em;">Suite</th>
          <th style="padding:10px 12px;text-align:right;font-size:12px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:.05em;">Duration</th>
          <th style="padding:10px 12px;text-align:left;font-size:12px;color:#64748b;font-weight:600;text-transform:uppercase;letter-spacing:.05em;">Status</th>
        </tr>
      </thead>
      <tbody>
        ${allSpecs.map(specRow).join('')}
      </tbody>
    </table>

    ${traceNames.length > 0 ? `
    <div style="margin-top:24px;">
      <p style="margin:0 0 10px;font-size:12px;font-weight:600;text-transform:uppercase;letter-spacing:.05em;color:#64748b;">Traces (MongoDB GridFS)</p>
      <div style="background:#161622;border-radius:8px;padding:12px 16px;">
        ${traceNames.map(name => `
        <div style="font-family:monospace;font-size:12px;color:#94a3b8;padding:3px 0;">${name}</div>`).join('')}
      </div>
    </div>` : ''}
  </div>`
}

async function sendEmail(subject, html) {
  const transporter = nodemailer.createTransport({
    host: smtpHost,
    port: smtpPort,
    secure: smtpPort === 465,
    auth: { user: smtpUser, pass: smtpPass },
  })

  console.log(`[send-report] Sending email: "${subject}"`)
  await transporter.sendMail({ from, to, subject, html })
  console.log(`[send-report] Email sent to ${to}`)
}

sendEmail(buildSubject(overall), buildBody(report)).catch((err) => {
  console.error('[send-report] Failed to send email:', err.message)
  process.exit(1)
})
