'use strict'

const fs = require('fs')
const os = require('os')
const path = require('path')
const { MongoClient, GridFSBucket } = require('mongodb')

function secret(name) {
  const filePath = `/run/secrets/${name}`
  if (fs.existsSync(filePath)) {
    console.log(`[upload-traces] Reading ${name} from Docker secret`)
    return fs.readFileSync(filePath, 'utf-8').trim()
  }
  console.log(`[upload-traces] Reading ${name} from environment variable`)
  return process.env[name] ?? ''
}

const mongoHost = process.env.MONGODB_HOST
const mongoPort = process.env.MONGODB_PORT || '27017'
const mongoUsername = secret('MONGODB_USERNAME')
const mongoPassword = secret('MONGODB_PASSWORD')
const mongoAuthDb = process.env.MONGODB_AUTH_DATABASE || 'admin'
const reportDb = 'playwright-reports'

function findTraceFiles(dir) {
  console.log(`[upload-traces] Scanning for trace files in: ${dir}`)
  const traces = []
  if (!fs.existsSync(dir)) {
    console.log(`[upload-traces] Directory does not exist: ${dir}`)
    return traces
  }
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  console.log(`[upload-traces] Found ${entries.length} entries in test-results/`)
  for (const entry of entries) {
    if (!entry.isDirectory()) continue
    const tracePath = path.join(dir, entry.name, 'trace.zip')
    if (fs.existsSync(tracePath)) {
      const { size } = fs.statSync(tracePath)
      console.log(`[upload-traces]   + ${entry.name}/trace.zip (${(size / 1024).toFixed(1)} KB)`)
      traces.push({ testName: entry.name, tracePath })
    } else {
      console.log(`[upload-traces]   - ${entry.name}/ (no trace.zip)`)
    }
  }
  return traces
}

async function uploadTraces() {
  console.log('[upload-traces] Starting trace upload')
  console.log(`[upload-traces] Container ID: ${os.hostname()}`)

  const traceDir = path.join(__dirname, '..', 'test-results')
  const traces = findTraceFiles(traceDir)

  if (traces.length === 0) {
    console.log('[upload-traces] No trace files found — skipping.')
    return
  }

  console.log(`[upload-traces] ${traces.length} trace(s) to upload`)

  const missing = [
    !mongoHost && 'MONGODB_HOST',
    !mongoUsername && 'MONGODB_USERNAME',
    !mongoPassword && 'MONGODB_PASSWORD',
  ].filter(Boolean)

  if (missing.length > 0) {
    console.error(`[upload-traces] Missing MongoDB configuration (${missing.join(', ')}) — skipping.`)
    return
  }

  console.log(`[upload-traces] Connecting to MongoDB at ${mongoHost}:${mongoPort} (db: ${reportDb}, authSource: ${mongoAuthDb})`)

  const uri = `mongodb://${encodeURIComponent(mongoUsername)}:${encodeURIComponent(mongoPassword)}@${mongoHost}:${mongoPort}/?authSource=${mongoAuthDb}`
  const client = new MongoClient(uri)

  try {
    await client.connect()
    console.log('[upload-traces] Connected to MongoDB')

    const bucket = new GridFSBucket(client.db(reportDb))
    const runTimestamp = new Date().toISOString()
    const containerId = os.hostname()
    const uploadedNames = []

    for (const { testName, tracePath } of traces) {
      const filename = `${runTimestamp}_${testName}.zip`
      const { size } = fs.statSync(tracePath)
      console.log(`[upload-traces] Uploading ${filename} (${(size / 1024).toFixed(1)} KB)...`)
      await new Promise((resolve, reject) => {
        const upload = bucket.openUploadStream(filename, {
          metadata: {
            testName,
            runTimestamp,
            source: 'playwright',
            containerId,
          },
        })
        fs.createReadStream(tracePath).pipe(upload)
        upload.on('finish', resolve)
        upload.on('error', reject)
      })
      uploadedNames.push(filename)
      console.log(`[upload-traces] ✓ Uploaded ${filename}`)
    }

    fs.writeFileSync('/app/trace-names.json', JSON.stringify(uploadedNames))
    console.log(`[upload-traces] Wrote trace-names.json with ${uploadedNames.length} entries`)
    console.log(`[upload-traces] Done — ${traces.length} trace(s) uploaded to ${reportDb}.fs`)
  } catch (err) {
    console.error('[upload-traces] Upload failed:', err.message)
    process.exit(1)
  } finally {
    await client.close()
    console.log('[upload-traces] MongoDB connection closed')
  }
}

uploadTraces()
