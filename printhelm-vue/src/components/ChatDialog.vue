<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import CameraPlayer from '@/components/CameraPlayer.vue'
import { chatApi } from '@/api/ChatApi'
import { printerCommandApi } from '@/api/PrinterCommandApi'
import printQueueApi from '@/api/PrintQueueApi'
import { ApiChatModel, ApiProposedActionActionTypeEnum } from '@/client/printhelm-web-openapi'
import type { ApiChatSessionSummary, ApiProposedAction } from '@/client/printhelm-web-openapi'

type ActionStatus = 'pending' | 'executing' | 'approved' | 'declined' | 'error'

interface ActionCard extends ApiProposedAction {
  status: ActionStatus
  errorMessage?: string
  showCamera?: boolean
}

interface Message {
  role: 'user' | 'assistant'
  content: string
  actions?: ActionCard[]
}

const isOpen = ref(false)
const showSessions = ref(false)
const loading = ref(false)
const sessionsLoading = ref(false)
const inputText = ref('')
const messages = ref<Message[]>([])
const sessions = ref<ApiChatSessionSummary[]>([])
const currentSessionId = ref<string | null>(null)
const selectedModel = ref<ApiChatModel>(ApiChatModel.Haiku)
const messagesEl = ref<HTMLElement | null>(null)

function toggleOpen() {
  isOpen.value = !isOpen.value
  if (!isOpen.value) showSessions.value = false
}

async function scrollToBottom() {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

function startNewChat() {
  messages.value = []
  currentSessionId.value = null
  showSessions.value = false
}

async function toggleSessions() {
  showSessions.value = !showSessions.value
  if (showSessions.value) {
    sessionsLoading.value = true
    try {
      const res = await chatApi.getChatSessions()
      sessions.value = res.data
    } catch {
      sessions.value = []
    } finally {
      sessionsLoading.value = false
    }
  }
}

async function loadSession(session: ApiChatSessionSummary) {
  showSessions.value = false
  try {
    const res = await chatApi.getChatSession(session.sessionId!)
    messages.value = res.data.messages!.map((m) => ({
      role: m.role as 'user' | 'assistant',
      content: m.content,
    }))
    currentSessionId.value = session.sessionId!
    await scrollToBottom()
  } catch {
    // ignore
  }
}

async function deleteSession(e: Event, session: ApiChatSessionSummary) {
  e.stopPropagation()
  try {
    await chatApi.deleteChatSession(session.sessionId!)
    sessions.value = sessions.value.filter((s) => s.sessionId !== session.sessionId)
    if (currentSessionId.value === session.sessionId) startNewChat()
  } catch {
    // ignore
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  showSessions.value = false
  await scrollToBottom()

  try {
    const res = await chatApi.sendChatMessage({
      content: text,
      sessionId: currentSessionId.value ?? undefined,
      model: selectedModel.value,
    })

    currentSessionId.value = res.data.sessionId ?? currentSessionId.value

    const actions: ActionCard[] = (res.data.proposedActions ?? []).map((a) => ({
      ...a,
      status: 'pending' as ActionStatus,
    }))

    messages.value.push({
      role: 'assistant',
      content: res.data.message ?? '',
      actions: actions.length > 0 ? actions : undefined,
    })
  } catch {
    messages.value.push({ role: 'assistant', content: 'Something went wrong. Please try again.' })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function declineAction(action: ActionCard) {
  action.status = 'declined'
}

async function approveAction(action: ActionCard) {
  action.status = 'executing'
  const id = action.printerId!
  let succeeded = false
  try {
    const T = ApiProposedActionActionTypeEnum
    const params = (action.parameters ?? {}) as Record<string, number>
    switch (action.actionType) {
      case T.Pause:        await printerCommandApi.pausePrint(id); break
      case T.Resume:       await printerCommandApi.resumePrint(id); break
      case T.Stop:         await printerCommandApi.stopPrint(id); break
      case T.SetNozzleTemp: await printerCommandApi.setNozzleTemp(id, { temp: params['temp'] }); break
      case T.SetBedTemp:   await printerCommandApi.setBedTemp(id, { temp: params['temp'] }); break
      case T.SetSpeed:     await printerCommandApi.setSpeed(id, { speed: params['speed'] }); break
      case T.Home:
        await printerCommandApi.homeAxes(id)
        action.showCamera = true
        break
      case T.StartPrint: {
        const p = (action.parameters ?? {}) as Record<string, any>
        await printQueueApi.startQueuedJob(id, Number(p['jobOrderId']), {
          amsMapping: (p['amsMapping'] as number[]) ?? [],
          flowCali: Boolean(p['flowCali']),
          vibrationCali: Boolean(p['vibrationCali']),
          layerInspect: Boolean(p['layerInspect']),
        })
        break
      }
    }
    action.status = 'approved'
    succeeded = true
  } catch {
    action.status = 'error'
    action.errorMessage = 'Command failed. Check the printer connection.'
  }

  if (succeeded) {
    await fetchActionSummary(action)
  }
}

async function fetchActionSummary(action: ActionCard) {
  if (!currentSessionId.value) return
  loading.value = true
  await scrollToBottom()
  try {
    const notice = `[System notice — do not repeat this text] The user approved and the following action was executed successfully: "${action.description}" on printer "${action.printerName}". Provide a short, friendly confirmation summary to the user.`
    const res = await chatApi.sendChatMessage({
      content: notice,
      sessionId: currentSessionId.value,
      model: selectedModel.value,
    })
    messages.value.push({
      role: 'assistant',
      content: res.data.message ?? '',
    })
  } catch {
    // summary is best-effort; silently ignore failures
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function renderMarkdown(content: string): string {
  return DOMPurify.sanitize(marked.parse(content) as string)
}

function actionIcon(type: string | undefined) {
  const T = ApiProposedActionActionTypeEnum
  switch (type) {
    case T.Pause:         return 'mdi mdi-pause-circle-outline'
    case T.Resume:        return 'mdi mdi-play-circle-outline'
    case T.Stop:          return 'mdi mdi-stop-circle-outline'
    case T.SetNozzleTemp: return 'mdi mdi-thermometer'
    case T.SetBedTemp:    return 'mdi mdi-table-furniture'
    case T.SetSpeed:      return 'mdi mdi-speedometer'
    case T.Home:          return 'mdi mdi-home-outline'
    case T.StartPrint:    return 'mdi mdi-printer-3d-nozzle'
    default:              return 'mdi mdi-printer-3d'
  }
}

function formatSessionDate(dateStr: string | undefined): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diffDays = Math.floor((now.getTime() - d.getTime()) / 86400000)
  if (diffDays === 0) return 'Today'
  if (diffDays === 1) return 'Yesterday'
  if (diffDays < 7) return `${diffDays}d ago`
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}
</script>

<template>
  <!-- FAB toggle (hidden while drawer is open) -->
  <button v-if="!isOpen" class="chat-fab" @click="toggleOpen" title="AI Assistant">
    <i class="mdi mdi-robot-outline" />
  </button>

  <!-- Backdrop -->
  <Transition name="fade">
    <div v-if="isOpen" class="chat-backdrop" @click="toggleOpen" />
  </Transition>

  <!-- Drawer -->
  <Transition name="drawer">
    <div v-if="isOpen" class="chat-drawer">
      <!-- Header -->
      <div class="drawer-header">
        <div class="drawer-title">
          <i class="mdi mdi-robot-outline" />
          <span>PrintHelm AI</span>
        </div>
        <div class="drawer-header-actions">
          <div class="model-toggle">
            <button
              class="model-btn"
              :class="{ active: selectedModel === ApiChatModel.Haiku }"
              @click="selectedModel = ApiChatModel.Haiku"
              title="Claude Haiku — faster"
            >Haiku</button>
            <button
              class="model-btn"
              :class="{ active: selectedModel === ApiChatModel.Sonnet }"
              @click="selectedModel = ApiChatModel.Sonnet"
              title="Claude Sonnet — smarter"
            >Sonnet</button>
          </div>
          <button class="icon-btn" @click="startNewChat" title="New chat">
            <i class="mdi mdi-plus" />
          </button>
          <button class="icon-btn" :class="{ active: showSessions }" @click.stop="toggleSessions" title="Chat history">
            <i class="mdi mdi-history" />
          </button>
          <button class="icon-btn" @click="toggleOpen" title="Close">
            <i class="mdi mdi-chevron-right" />
          </button>
        </div>
      </div>

      <!-- Sessions panel overlay -->
      <Transition name="sessions-slide">
        <div v-if="showSessions" class="sessions-panel">
          <div class="sessions-header">
            <span>Chat History</span>
            <button class="icon-btn" @click="showSessions = false" title="Close">
              <i class="mdi mdi-close" />
            </button>
          </div>
          <div v-if="sessionsLoading" class="sessions-loading">
            <span class="dot" /><span class="dot" /><span class="dot" />
          </div>
          <div v-else-if="sessions.length === 0" class="sessions-empty">
            No saved chats yet.
          </div>
          <div v-else class="sessions-list">
            <button
              v-for="s in sessions"
              :key="s.sessionId"
              class="session-item"
              :class="{ active: s.sessionId === currentSessionId }"
              @click="loadSession(s)"
            >
              <div class="session-info">
                <span class="session-title">{{ s.title }}</span>
                <span class="session-date">{{ formatSessionDate(s.updatedAt as unknown as string) }}</span>
              </div>
              <button class="session-delete" @click="deleteSession($event, s)" title="Delete">
                <i class="mdi mdi-delete-outline" />
              </button>
            </button>
          </div>
        </div>
      </Transition>

      <!-- Messages -->
      <div class="chat-messages" ref="messagesEl">
        <div v-if="messages.length === 0" class="chat-empty">
          <i class="mdi mdi-printer-3d-nozzle-outline" />
          <span>Ask me about your printers — temperatures, errors, print progress, and more.</span>
        </div>

        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="chat-msg"
          :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'"
        >
          <div v-if="msg.role === 'user'" class="msg-bubble">{{ msg.content }}</div>
          <div v-else class="msg-bubble markdown" v-html="renderMarkdown(msg.content)" />

          <div v-if="msg.actions && msg.actions.length > 0" class="action-cards">
            <div
              v-for="action in msg.actions"
              :key="action.actionId"
              class="action-card"
              :class="[`action-${action.status}`, { 'action-card-column': action.showCamera || action.actionType === 'START_PRINT' }]"
            >
              <!-- Action row: icon + text + buttons -->
              <div class="action-row">
                <div class="action-info">
                  <i :class="actionIcon(action.actionType)" class="action-icon" />
                  <div class="action-text">
                    <span class="action-desc">{{ action.description }}</span>
                    <span class="action-printer">{{ action.printerName }}</span>
                  </div>
                </div>

                <div v-if="action.status === 'pending'" class="action-btns">
                  <button class="action-approve" @click="approveAction(action)">
                    <i class="mdi mdi-check" /> Approve
                  </button>
                  <button class="action-decline" @click="declineAction(action)" title="Decline">
                    <i class="mdi mdi-close" />
                  </button>
                </div>
                <div v-else-if="action.status === 'executing'" class="action-status-badge executing">
                  <span class="dot" /><span class="dot" /><span class="dot" />
                </div>
                <div v-else-if="action.status === 'approved'" class="action-status-badge approved">
                  <i class="mdi mdi-check-circle" /> Sent
                </div>
                <div v-else-if="action.status === 'declined'" class="action-status-badge declined">
                  <i class="mdi mdi-cancel" /> Declined
                </div>
                <div v-else-if="action.status === 'error'" class="action-status-badge error" :title="action.errorMessage">
                  <i class="mdi mdi-alert-circle" /> Failed
                </div>
              </div>

              <!-- StartPrint parameters table -->
              <div v-if="action.actionType === 'START_PRINT'" class="action-params">
                <table class="action-params-table">
                  <tbody>
                    <tr>
                      <td class="ap-key">Job Order ID</td>
                      <td class="ap-val">{{ (action.parameters as Record<string, any>)?.['jobOrderId'] ?? '—' }}</td>
                    </tr>
                    <tr>
                      <td class="ap-key">AMS Mapping</td>
                      <td class="ap-val">{{ Array.isArray((action.parameters as Record<string, any>)?.['amsMapping']) ? ((action.parameters as Record<string, any>)['amsMapping'] as number[]).join(', ') || '(none)' : '—' }}</td>
                    </tr>
                    <tr>
                      <td class="ap-key">Flow Calibration</td>
                      <td class="ap-val">{{ (action.parameters as Record<string, any>)?.['flowCali'] ? 'Yes' : 'No' }}</td>
                    </tr>
                    <tr>
                      <td class="ap-key">Vibration Calibration</td>
                      <td class="ap-val">{{ (action.parameters as Record<string, any>)?.['vibrationCali'] ? 'Yes' : 'No' }}</td>
                    </tr>
                    <tr>
                      <td class="ap-key">Layer Inspect</td>
                      <td class="ap-val">{{ (action.parameters as Record<string, any>)?.['layerInspect'] ? 'Yes' : 'No' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Camera preview (HOME only) -->
              <div v-if="action.showCamera" class="action-camera">
                <div class="action-camera-label">
                  <i class="mdi mdi-cctv" />
                  <span>Live Camera — {{ action.printerName }}</span>
                </div>
                <div class="action-camera-viewport">
                  <CameraPlayer :src="`/hls/printer/${action.printerId}/stream/index.m3u8`" class="action-camera-feed" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="loading" class="chat-msg msg-ai">
          <div class="msg-bubble msg-loading">
            <span class="dot" /><span class="dot" /><span class="dot" />
          </div>
        </div>
      </div>

      <!-- Input -->
      <div class="chat-input-row">
        <input
          class="chat-input"
          v-model="inputText"
          placeholder="Ask about your printers…"
          :disabled="loading"
          @keydown="onKeydown"
        />
        <button class="chat-send" @click="sendMessage" :disabled="loading || !inputText.trim()">
          <i class="mdi mdi-send" />
        </button>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
/* FAB */
.chat-fab {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 1001;
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  border: none;
  background: var(--ph-accent);
  color: #0f2027;
  font-size: 1.35rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(6, 182, 212, 0.4);
  transition: transform 0.15s, box-shadow 0.15s, background 0.15s;
}
.chat-fab:hover { transform: scale(1.07); box-shadow: 0 6px 20px rgba(6, 182, 212, 0.55); }

/* Backdrop */
.chat-backdrop {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(1px);
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* Drawer */
.chat-drawer {
  position: fixed;
  right: 0;
  top: 0;
  height: 100vh;
  width: 500px;
  z-index: 1000;
  background: var(--ph-glass-heavy);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border-left: 1px solid var(--ph-border-strong);
  box-shadow: -12px 0 48px rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.drawer-enter-active, .drawer-leave-active { transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1); }
.drawer-enter-from, .drawer-leave-to { transform: translateX(100%); }

/* Drawer header */
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ph-border);
  flex-shrink: 0;
  gap: 0.75rem;
}
.drawer-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--ph-accent);
  flex-shrink: 0;
}
.drawer-title i { font-size: 1.1rem; }
.drawer-header-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.model-toggle {
  display: flex;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--ph-border);
  border-radius: 6px;
  overflow: hidden;
}
.model-btn {
  padding: 0.25rem 0.75rem;
  font-size: 0.72rem;
  font-weight: 600;
  border: none;
  background: transparent;
  color: var(--ph-text-muted);
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
  letter-spacing: 0.02em;
}
.model-btn.active { background: var(--ph-accent); color: #0f2027; }
.icon-btn {
  width: 1.9rem;
  height: 1.9rem;
  border: 1px solid var(--ph-border);
  border-radius: 6px;
  background: transparent;
  color: var(--ph-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  transition: background 0.12s, color 0.12s, border-color 0.12s;
  flex-shrink: 0;
}
.icon-btn:hover { background: rgba(255,255,255,0.07); color: var(--ph-text); }
.icon-btn.active { color: var(--ph-accent); border-color: var(--ph-accent); }

/* Sessions panel */
.sessions-panel {
  position: absolute;
  inset: 0;
  background: var(--ph-bg-darkest);
  z-index: 10;
  display: flex;
  flex-direction: column;
}
.sessions-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ph-border);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ph-text);
  flex-shrink: 0;
}
.sessions-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.sessions-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.82rem;
  color: var(--ph-text-muted);
}
.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.625rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.session-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 0.875rem;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.12s, border-color 0.12s;
  width: 100%;
}
.session-item:hover { background: rgba(255,255,255,0.05); border-color: var(--ph-border); }
.session-item.active { border-color: var(--ph-accent); background: rgba(6,182,212,0.07); }
.session-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 0.15rem; }
.session-title { font-size: 0.82rem; font-weight: 500; color: var(--ph-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.session-date { font-size: 0.7rem; color: var(--ph-text-muted); }
.session-delete {
  flex-shrink: 0;
  width: 1.5rem;
  height: 1.5rem;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: var(--ph-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  opacity: 0;
  transition: opacity 0.1s, background 0.1s, color 0.1s;
}
.session-item:hover .session-delete { opacity: 1; }
.session-delete:hover { background: rgba(248,113,113,0.15); color: #f87171; }

.sessions-slide-enter-active, .sessions-slide-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.sessions-slide-enter-from, .sessions-slide-leave-to { opacity: 0; transform: translateX(16px); }

/* Messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}
.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  color: var(--ph-text-muted);
  font-size: 0.85rem;
  text-align: center;
  padding: 2rem;
  line-height: 1.6;
}
.chat-empty i { font-size: 3rem; opacity: 0.2; }
.chat-msg { display: flex; flex-direction: column; }
.msg-user { align-items: flex-end; }
.msg-ai { align-items: flex-start; }
.msg-bubble {
  max-width: 78%;
  padding: 0.625rem 0.875rem;
  border-radius: 14px;
  font-size: 0.875rem;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-user .msg-bubble { background: var(--ph-accent); color: #0f2027; border-bottom-right-radius: 4px; }
.msg-ai .msg-bubble { background: rgba(255, 255, 255, 0.07); color: var(--ph-text); border-bottom-left-radius: 4px; }

/* Typing dots */
.msg-loading { display: flex; align-items: center; gap: 5px; padding: 0.7rem 1rem; }
.dot { width: 6px; height: 6px; background: var(--ph-text-muted); border-radius: 50%; animation: bounce 1.2s infinite; }
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.5; }
  40% { transform: translateY(-5px); opacity: 1; }
}

/* Action cards */
.action-cards { display: flex; flex-direction: column; gap: 0.4rem; margin-top: 0.5rem; max-width: 90%; }
.action-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.625rem 0.875rem;
  border-radius: 10px;
  border: 1px solid var(--ph-border);
  background: rgba(255, 255, 255, 0.04);
  transition: border-color 0.15s;
}
.action-card-column {
  flex-direction: column;
  align-items: stretch;
  padding: 0.625rem;
}
.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  width: 100%;
}
.action-camera {
  margin-top: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.action-camera-label {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ph-accent);
}
.action-camera-viewport {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
  border: 1px solid var(--ph-border);
}
.action-camera-feed {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.action-pending { border-color: rgba(251, 191, 36, 0.4); }
.action-approved { border-color: rgba(52, 211, 153, 0.4); opacity: 0.7; }
.action-declined { opacity: 0.4; }
.action-error { border-color: rgba(248, 113, 113, 0.4); }

.action-info { display: flex; align-items: center; gap: 0.5rem; flex: 1; min-width: 0; }
.action-icon { font-size: 1.05rem; color: var(--ph-accent); flex-shrink: 0; }
.action-text { display: flex; flex-direction: column; min-width: 0; }
.action-desc { font-size: 0.8rem; font-weight: 600; color: var(--ph-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.action-printer { font-size: 0.72rem; color: var(--ph-text-muted); }

.action-btns { display: flex; align-items: center; gap: 0.25rem; flex-shrink: 0; }
.action-approve {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.3rem 0.7rem;
  font-size: 0.75rem;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  background: rgba(52, 211, 153, 0.15);
  color: #34d399;
  cursor: pointer;
  transition: background 0.12s;
}
.action-approve:hover { background: rgba(52, 211, 153, 0.28); }
.action-decline {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border: none;
  border-radius: 6px;
  background: rgba(248, 113, 113, 0.1);
  color: #f87171;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.12s;
}
.action-decline:hover { background: rgba(248, 113, 113, 0.22); }
/* StartPrint params table */
.action-params {
  margin-top: 0.5rem;
  border-top: 1px solid var(--ph-border);
  padding-top: 0.5rem;
}
.action-params-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.72rem;
}
.action-params-table tr + tr td { border-top: 1px solid rgba(255,255,255,0.05); }
.ap-key {
  color: var(--ph-text-muted);
  padding: 0.2rem 0.5rem 0.2rem 0;
  white-space: nowrap;
  width: 1%;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.ap-val {
  color: var(--ph-text);
  padding: 0.2rem 0;
  font-variant-numeric: tabular-nums;
}

.action-status-badge { display: flex; align-items: center; gap: 0.25rem; font-size: 0.75rem; font-weight: 600; flex-shrink: 0; }
.action-status-badge.executing { color: var(--ph-text-muted); }
.action-status-badge.approved { color: #34d399; }
.action-status-badge.declined { color: var(--ph-text-muted); }
.action-status-badge.error { color: #f87171; }

/* Input */
.chat-input-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--ph-border);
  flex-shrink: 0;
}
.chat-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--ph-border);
  border-radius: 10px;
  padding: 0.625rem 0.875rem;
  font-size: 0.875rem;
  color: var(--ph-text);
  outline: none;
  transition: border-color 0.15s;
}
.chat-input::placeholder { color: var(--ph-text-muted); opacity: 0.6; }
.chat-input:focus { border-color: var(--ph-accent); }
.chat-input:disabled { opacity: 0.5; cursor: not-allowed; }
.chat-send {
  width: 2.5rem;
  height: 2.5rem;
  border: none;
  border-radius: 10px;
  background: var(--ph-accent);
  color: #0f2027;
  font-size: 1.05rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: opacity 0.15s;
}
.chat-send:disabled { opacity: 0.35; cursor: not-allowed; }

/* Markdown */
.msg-bubble.markdown { white-space: normal; }
.msg-bubble.markdown :deep(p) { margin: 0 0 0.5em; }
.msg-bubble.markdown :deep(p:last-child) { margin-bottom: 0; }
.msg-bubble.markdown :deep(h1),
.msg-bubble.markdown :deep(h2),
.msg-bubble.markdown :deep(h3) { font-weight: 700; margin: 0.6em 0 0.3em; color: var(--ph-accent); line-height: 1.3; }
.msg-bubble.markdown :deep(h1) { font-size: 1em; }
.msg-bubble.markdown :deep(h2) { font-size: 0.92em; }
.msg-bubble.markdown :deep(h3) { font-size: 0.85em; }
.msg-bubble.markdown :deep(ul),
.msg-bubble.markdown :deep(ol) { margin: 0.3em 0 0.5em 1.1em; padding: 0; }
.msg-bubble.markdown :deep(li) { margin-bottom: 0.15em; }
.msg-bubble.markdown :deep(code) {
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  font-size: 0.8em;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  padding: 0.1em 0.35em;
}
.msg-bubble.markdown :deep(pre) {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--ph-border);
  border-radius: 8px;
  padding: 0.75em 1em;
  margin: 0.4em 0;
  overflow-x: auto;
}
.msg-bubble.markdown :deep(pre code) { background: none; padding: 0; font-size: 0.8em; border-radius: 0; }
.msg-bubble.markdown :deep(blockquote) { border-left: 3px solid var(--ph-accent); margin: 0.4em 0; padding: 0.2em 0.75em; color: var(--ph-text-muted); }
.msg-bubble.markdown :deep(strong) { font-weight: 700; color: var(--ph-text); }
.msg-bubble.markdown :deep(em) { font-style: italic; }
.msg-bubble.markdown :deep(a) { color: var(--ph-accent); text-decoration: underline; text-underline-offset: 2px; }
.msg-bubble.markdown :deep(hr) { border: none; border-top: 1px solid var(--ph-border); margin: 0.5em 0; }
.msg-bubble.markdown :deep(table) { border-collapse: collapse; font-size: 0.8em; margin: 0.4em 0; width: 100%; }
.msg-bubble.markdown :deep(th),
.msg-bubble.markdown :deep(td) { border: 1px solid var(--ph-border); padding: 0.3em 0.6em; text-align: left; }
.msg-bubble.markdown :deep(th) { background: rgba(255, 255, 255, 0.06); font-weight: 600; color: var(--ph-accent); }
</style>
