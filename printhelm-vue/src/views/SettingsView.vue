<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Password from 'primevue/password'
import MultiSelect from 'primevue/multiselect'
import Tag from 'primevue/tag'
import ToggleSwitch from 'primevue/toggleswitch'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/Configuration'
import { getBusinessSettings, updateBusinessSettings } from '@/api/BusinessSettingsApi'

const toast = useToast()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin)

// ─── Shared ───────────────────────────────────────────────────

const printerTypeOptions = [
  { label: 'Bambu Lab', value: 'BAMBULAB' },
  { label: 'Prusa', value: 'PRUSA' },
  { label: 'Anycubic', value: 'ANYCUBIC' },
  { label: 'EPAX', value: 'EPAX' },
  { label: 'Elegoo', value: 'ELEGOO' },
  { label: 'Other', value: 'OTHER' },
]

// ─── Printers ─────────────────────────────────────────────────

interface ConnectionConfig {
  connectionType: string
  brokerUrl: string
  topic: string
  username: string
  password: string
}

interface PrinterRow {
  printerId: number
  printerName: string
  printerModel: string
  printerType: string
  location?: string
  serialNumber?: string
  connectionConfig?: ConnectionConfig
}

interface PrinterForm {
  printerName: string
  printerModel: string
  printerType: string
  location: string
  serialNumber: string
  connectionConfig: ConnectionConfig
}

const printers = ref<PrinterRow[]>([])
const printersLoading = ref(true)
const showPrinterDialog = ref(false)
const printerEditMode = ref(false)
const editingPrinterId = ref<number | null>(null)
const printerSaving = ref(false)
const showDeletePrinterDialog = ref(false)
const printerToDelete = ref<PrinterRow | null>(null)
const deletingPrinter = ref(false)

const printerForm = ref<PrinterForm>({
  printerName: '',
  printerModel: '',
  printerType: 'BAMBULAB',
  location: '',
  serialNumber: '',
  connectionConfig: { connectionType: 'MQTT', brokerUrl: '', topic: '', username: '', password: '' },
})

function resetPrinterForm() {
  printerForm.value = {
    printerName: '',
    printerModel: '',
    printerType: 'BAMBULAB',
    location: '',
    serialNumber: '',
    connectionConfig: { connectionType: 'MQTT', brokerUrl: '', topic: '', username: '', password: '' },
  }
}

function openAddPrinter() {
  printerEditMode.value = false
  editingPrinterId.value = null
  resetPrinterForm()
  showPrinterDialog.value = true
}

function openEditPrinter(p: PrinterRow) {
  printerEditMode.value = true
  editingPrinterId.value = p.printerId
  printerForm.value = {
    printerName: p.printerName,
    printerModel: p.printerModel,
    printerType: p.printerType,
    location: p.location ?? '',
    serialNumber: p.serialNumber ?? '',
    connectionConfig: {
      connectionType: p.connectionConfig?.connectionType ?? 'MQTT',
      brokerUrl: p.connectionConfig?.brokerUrl ?? '',
      topic: p.connectionConfig?.topic ?? '',
      username: p.connectionConfig?.username ?? '',
      password: p.connectionConfig?.password ?? '',
    },
  }
  showPrinterDialog.value = true
}

async function fetchPrinters() {
  printersLoading.value = true
  try {
    const res = await api.get<PrinterRow[]>('/printer')
    printers.value = res.data
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to load printers.', life: 4000 })
  } finally {
    printersLoading.value = false
  }
}

function validatePrinterForm(): string | null {
  if (!printerForm.value.printerName.trim()) return 'Printer name is required.'
  if (!printerForm.value.printerModel.trim()) return 'Printer model is required.'
  if (!printerForm.value.connectionConfig.brokerUrl.trim()) return 'Broker URL is required.'
  if (!printerForm.value.connectionConfig.topic.trim()) return 'Topic is required.'
  return null
}

async function savePrinter() {
  const err = validatePrinterForm()
  if (err) { toast.add({ severity: 'warn', summary: 'Validation', detail: err, life: 4000 }); return }

  printerSaving.value = true
  try {
    const payload = {
      printerName: printerForm.value.printerName.trim(),
      printerModel: printerForm.value.printerModel.trim(),
      printerType: printerForm.value.printerType,
      location: printerForm.value.location.trim() || null,
      serialNumber: printerForm.value.serialNumber.trim() || null,
      connectionConfig: {
        connectionType: 'MQTT',
        brokerUrl: printerForm.value.connectionConfig.brokerUrl.trim(),
        topic: printerForm.value.connectionConfig.topic.trim(),
        username: printerForm.value.connectionConfig.username.trim() || null,
        password: printerForm.value.connectionConfig.password || null,
      },
    }
    if (printerEditMode.value && editingPrinterId.value != null) {
      await api.put(`/printer/${editingPrinterId.value}`, payload)
      toast.add({ severity: 'success', summary: 'Updated', detail: 'Printer updated.', life: 3000 })
    } else {
      await api.post('/printer/createPrinter', payload)
      toast.add({ severity: 'success', summary: 'Created', detail: 'Printer added.', life: 3000 })
    }
    showPrinterDialog.value = false
    await fetchPrinters()
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to save printer.', life: 4000 })
  } finally {
    printerSaving.value = false
  }
}

async function confirmDeletePrinter() {
  if (!printerToDelete.value) return
  deletingPrinter.value = true
  try {
    await api.delete(`/printer/${printerToDelete.value.printerId}`)
    toast.add({ severity: 'success', summary: 'Deleted', detail: 'Printer removed.', life: 3000 })
    showDeletePrinterDialog.value = false
    printerToDelete.value = null
    await fetchPrinters()
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete printer.', life: 4000 })
  } finally {
    deletingPrinter.value = false
  }
}

// ─── Users ────────────────────────────────────────────────────

interface RoleOption { roleId: number; name: string; description?: string }

interface UserRow {
  userId: number
  username: string
  firstName?: string
  lastName?: string
  isActive: boolean
  mustChangePassword: boolean
  roles: RoleOption[]
}

interface UserForm {
  username: string
  firstName: string
  lastName: string
  password: string
  isActive: boolean
  roles: RoleOption[]
}

const users = ref<UserRow[]>([])
const usersLoading = ref(true)
const availableRoles = ref<RoleOption[]>([])

const showUserDialog = ref(false)
const userEditMode = ref(false)
const editingUserId = ref<number | null>(null)
const userSaving = ref(false)
const showDeleteUserDialog = ref(false)
const userToDelete = ref<UserRow | null>(null)
const deletingUser = ref(false)
const showResetPasswordDialog = ref(false)
const resetPasswordUserId = ref<number | null>(null)
const resetPasswordUsername = ref('')
const resetPassword = ref('')
const resetMustChange = ref(true)
const resettingPassword = ref(false)

const userForm = ref<UserForm>({
  username: '',
  firstName: '',
  lastName: '',
  password: '',
  isActive: true,
  roles: [],
})

function resetUserForm() {
  userForm.value = { username: '', firstName: '', lastName: '', password: '', isActive: true, roles: [] }
}

function openAddUser() {
  userEditMode.value = false
  editingUserId.value = null
  resetUserForm()
  showUserDialog.value = true
}

function openEditUser(u: UserRow) {
  userEditMode.value = true
  editingUserId.value = u.userId
  userForm.value = {
    username: u.username,
    firstName: u.firstName ?? '',
    lastName: u.lastName ?? '',
    password: '',
    isActive: u.isActive,
    roles: u.roles,
  }
  showUserDialog.value = true
}

function openResetPassword(u: UserRow) {
  resetPasswordUserId.value = u.userId
  resetPasswordUsername.value = u.username
  resetPassword.value = ''
  resetMustChange.value = true
  showResetPasswordDialog.value = true
}

async function fetchUsers() {
  usersLoading.value = true
  try {
    const [usersRes, rolesRes] = await Promise.all([
      api.get<UserRow[]>('/user'),
      api.get<RoleOption[]>('/role'),
    ])
    users.value = usersRes.data
    availableRoles.value = rolesRes.data
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to load users.', life: 4000 })
  } finally {
    usersLoading.value = false
  }
}

async function saveUser() {
  if (!userForm.value.username.trim()) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Username is required.', life: 4000 })
    return
  }
  if (!userEditMode.value && !userForm.value.password) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Password is required.', life: 4000 })
    return
  }

  userSaving.value = true
  try {
    const rolesPayload = userForm.value.roles.map((r) => ({ roleId: r.roleId, name: r.name }))

    if (userEditMode.value && editingUserId.value != null) {
      await api.put(`/user/${editingUserId.value}`, {
        username: userForm.value.username.trim(),
        firstName: userForm.value.firstName.trim() || null,
        lastName: userForm.value.lastName.trim() || null,
        isActive: userForm.value.isActive,
        roles: rolesPayload,
      })
      toast.add({ severity: 'success', summary: 'Updated', detail: 'User updated.', life: 3000 })
    } else {
      await api.post('/user/createUser', {
        username: userForm.value.username.trim(),
        password: userForm.value.password,
        firstName: userForm.value.firstName.trim() || null,
        lastName: userForm.value.lastName.trim() || null,
        roles: rolesPayload,
      })
      toast.add({ severity: 'success', summary: 'Created', detail: 'User created.', life: 3000 })
    }
    showUserDialog.value = false
    await fetchUsers()
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to save user.', life: 4000 })
  } finally {
    userSaving.value = false
  }
}

async function confirmDeleteUser() {
  if (!userToDelete.value) return
  deletingUser.value = true
  try {
    await api.delete(`/user/${userToDelete.value.userId}`)
    toast.add({ severity: 'success', summary: 'Deleted', detail: 'User removed.', life: 3000 })
    showDeleteUserDialog.value = false
    userToDelete.value = null
    await fetchUsers()
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete user.', life: 4000 })
  } finally {
    deletingUser.value = false
  }
}

async function confirmResetPassword() {
  if (!resetPasswordUserId.value || !resetPassword.value) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'New password is required.', life: 4000 })
    return
  }
  resettingPassword.value = true
  try {
    await api.put(`/user/${resetPasswordUserId.value}/password`, {
      newPassword: resetPassword.value,
      mustChangePassword: resetMustChange.value,
    })
    toast.add({ severity: 'success', summary: 'Done', detail: 'Password has been reset.', life: 3000 })
    showResetPasswordDialog.value = false
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to reset password.', life: 4000 })
  } finally {
    resettingPassword.value = false
  }
}

// ─── Profile ──────────────────────────────────────────────────

const profileCurrentPassword = ref('')
const profileNewPassword = ref('')
const profileConfirmPassword = ref('')
const profileSaving = ref(false)

async function saveProfilePassword() {
  if (!profileCurrentPassword.value) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Current password is required.', life: 4000 })
    return
  }
  if (profileNewPassword.value.length < 6) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'New password must be at least 6 characters.', life: 4000 })
    return
  }
  if (profileNewPassword.value !== profileConfirmPassword.value) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Passwords do not match.', life: 4000 })
    return
  }
  profileSaving.value = true
  try {
    await api.put('/user/me/password', {
      currentPassword: profileCurrentPassword.value,
      newPassword: profileNewPassword.value,
    })
    toast.add({ severity: 'success', summary: 'Updated', detail: 'Password changed successfully.', life: 3000 })
    profileCurrentPassword.value = ''
    profileNewPassword.value = ''
    profileConfirmPassword.value = ''
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to change password. Check your current password.', life: 4000 })
  } finally {
    profileSaving.value = false
  }
}

// ─── Business Settings ────────────────────────────────────────

const businessName = ref('')
const businessAddress = ref('')
const businessEmail = ref('')
const businessPhone = ref('')
const businessSaving = ref(false)
const businessLoading = ref(false)

async function fetchBusinessSettings() {
  businessLoading.value = true
  try {
    const data = await getBusinessSettings()
    businessName.value = data.businessName ?? ''
    businessAddress.value = data.businessAddress ?? ''
    businessEmail.value = data.businessEmail ?? ''
    businessPhone.value = data.businessPhone ?? ''
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to load business settings.', life: 4000 })
  } finally {
    businessLoading.value = false
  }
}

async function saveBusinessSettings() {
  businessSaving.value = true
  try {
    const data = await updateBusinessSettings({
      businessName: businessName.value.trim() || undefined,
      businessAddress: businessAddress.value.trim() || undefined,
      businessEmail: businessEmail.value.trim() || undefined,
      businessPhone: businessPhone.value.trim() || undefined,
    })
    businessName.value = data.businessName ?? ''
    businessAddress.value = data.businessAddress ?? ''
    businessEmail.value = data.businessEmail ?? ''
    businessPhone.value = data.businessPhone ?? ''
    toast.add({ severity: 'success', summary: 'Saved', detail: 'Business settings updated.', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to save business settings.', life: 4000 })
  } finally {
    businessSaving.value = false
  }
}

// ─── Init ─────────────────────────────────────────────────────

onMounted(async () => {
  await fetchPrinters()
  if (isAdmin.value) {
    await fetchUsers()
    await fetchBusinessSettings()
  }
})
</script>

<template>
  <div class="settings-view">
  <Toast />
  <div class="settings-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">Settings</h2>
        <p class="page-subtitle">Manage printers, users, and your profile</p>
      </div>
    </div>

    <div class="tabs-card">
      <Tabs value="printers">
        <TabList>
          <Tab value="printers">
            <i class="mdi mdi-printer-3d tab-icon" />
            Printers
          </Tab>
          <Tab v-if="isAdmin" value="users">
            <i class="mdi mdi-account-group-outline tab-icon" />
            Users
          </Tab>
          <Tab value="profile">
            <i class="mdi mdi-account-key-outline tab-icon" />
            Profile
          </Tab>
          <Tab v-if="isAdmin" value="business">
            <i class="mdi mdi-domain tab-icon" />
            Business
          </Tab>
        </TabList>

        <TabPanels>

          <!-- ── Printers Tab ── -->
          <TabPanel value="printers">
            <div class="panel-header">
              <span class="section-title">Printers</span>
              <Button label="Add Printer" icon="mdi mdi-plus" size="small" @click="openAddPrinter" />
            </div>
            <DataTable :value="printers" :loading="printersLoading" data-key="printerId"
              empty-message="No printers configured." size="small">
              <Column field="printerName" header="Name" style="min-width:140px" />
              <Column field="printerModel" header="Model" style="min-width:120px" />
              <Column header="Type" style="min-width:110px">
                <template #body="{ data }"><Tag :value="data.printerType" severity="secondary" /></template>
              </Column>
              <Column header="Location" style="min-width:110px">
                <template #body="{ data }">
                  <span :class="{ muted: !data.location }">{{ data.location || '—' }}</span>
                </template>
              </Column>
              <Column header="Connection" style="min-width:200px">
                <template #body="{ data }">
                  <span v-if="data.connectionConfig" class="connection-info">
                    <i class="mdi mdi-lan-connect" />{{ data.connectionConfig.brokerUrl }}
                  </span>
                  <span v-else class="muted">Not configured</span>
                </template>
              </Column>
              <Column header="Actions" style="min-width:100px">
                <template #body="{ data }">
                  <div class="row-actions">
                    <Button icon="mdi mdi-pencil-outline" severity="secondary" text size="small"
                      title="Edit" @click="openEditPrinter(data)" />
                    <Button icon="mdi mdi-trash-can-outline" severity="danger" text size="small"
                      title="Delete" @click="printerToDelete = data; showDeletePrinterDialog = true" />
                  </div>
                </template>
              </Column>
            </DataTable>
          </TabPanel>

          <!-- ── Users Tab ── -->
          <TabPanel v-if="isAdmin" value="users">
            <div class="panel-header">
              <span class="section-title">Users</span>
              <Button label="Add User" icon="mdi mdi-account-plus-outline" size="small" @click="openAddUser" />
            </div>
            <DataTable :value="users" :loading="usersLoading" data-key="userId"
              empty-message="No users found." size="small">
              <Column field="username" header="Username" style="min-width:130px" />
              <Column header="Name" style="min-width:150px">
                <template #body="{ data }">
                  <span :class="{ muted: !data.firstName && !data.lastName }">
                    {{ [data.firstName, data.lastName].filter(Boolean).join(' ') || '—' }}
                  </span>
                </template>
              </Column>
              <Column header="Roles" style="min-width:160px">
                <template #body="{ data }">
                  <div class="role-tags">
                    <Tag v-for="role in data.roles" :key="role.roleId"
                      :value="role.name?.replace('ROLE_', '')"
                      :severity="role.name === 'ROLE_ADMIN' ? 'warn' : 'secondary'"
                      style="font-size:0.7rem" />
                  </div>
                </template>
              </Column>
              <Column header="Status" style="min-width:90px">
                <template #body="{ data }">
                  <Tag :value="data.isActive ? 'Active' : 'Inactive'"
                    :severity="data.isActive ? 'success' : 'secondary'" />
                </template>
              </Column>
              <Column header="Flags" style="min-width:120px">
                <template #body="{ data }">
                  <Tag v-if="data.mustChangePassword" value="Must Reset" severity="warn"
                    style="font-size:0.7rem" />
                </template>
              </Column>
              <Column header="Actions" style="min-width:130px">
                <template #body="{ data }">
                  <div class="row-actions">
                    <Button icon="mdi mdi-pencil-outline" severity="secondary" text size="small"
                      title="Edit" @click="openEditUser(data)" />
                    <Button icon="mdi mdi-lock-reset" severity="secondary" text size="small"
                      title="Reset Password" @click="openResetPassword(data)" />
                    <Button icon="mdi mdi-trash-can-outline" severity="danger" text size="small"
                      title="Delete" @click="userToDelete = data; showDeleteUserDialog = true" />
                  </div>
                </template>
              </Column>
            </DataTable>
          </TabPanel>

          <!-- ── Profile Tab ── -->
          <TabPanel value="profile">
            <div class="profile-section">
              <div class="panel-header">
                <span class="section-title">Change Password</span>
              </div>
              <div class="profile-form">
                <div class="field">
                  <label class="field-label">Current Password <span class="required">*</span></label>
                  <Password v-model="profileCurrentPassword" :feedback="false" toggle-mask
                    placeholder="Enter current password" class="field-input" input-class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">New Password <span class="required">*</span></label>
                  <Password v-model="profileNewPassword" :feedback="true" toggle-mask
                    placeholder="At least 6 characters" class="field-input" input-class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">Confirm New Password <span class="required">*</span></label>
                  <Password v-model="profileConfirmPassword" :feedback="false" toggle-mask
                    placeholder="Repeat new password" class="field-input" input-class="field-input" />
                </div>
                <div class="profile-actions">
                  <Button label="Change Password" :loading="profileSaving" @click="saveProfilePassword" />
                </div>
              </div>
            </div>
          </TabPanel>

          <!-- ── Business Tab ── -->
          <TabPanel v-if="isAdmin" value="business">
            <div class="business-section">
              <div class="panel-header">
                <span class="section-title">Business Information</span>
              </div>
              <p class="business-hint">This information appears on quotes and invoices sent to customers.</p>
              <div class="business-form" v-if="!businessLoading">
                <div class="field">
                  <label class="field-label">Business Name</label>
                  <InputText v-model="businessName" placeholder="e.g. Acme 3D Printing" class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">Address</label>
                  <InputText v-model="businessAddress" placeholder="e.g. 123 Main St, Springfield, IL 62701" class="field-input" />
                </div>
                <div class="field-row">
                  <div class="field">
                    <label class="field-label">Email</label>
                    <InputText v-model="businessEmail" placeholder="e.g. contact@acme3d.com" class="field-input" />
                  </div>
                  <div class="field">
                    <label class="field-label">Phone</label>
                    <InputText v-model="businessPhone" placeholder="e.g. (555) 123-4567" class="field-input" />
                  </div>
                </div>
                <div class="business-actions">
                  <Button label="Save Settings" icon="mdi mdi-content-save-outline" :loading="businessSaving" @click="saveBusinessSettings" />
                </div>
              </div>
            </div>
          </TabPanel>

        </TabPanels>
      </Tabs>
    </div>

    <!-- ── Printer Add/Edit Dialog ── -->
    <Dialog v-model:visible="showPrinterDialog"
      :header="printerEditMode ? 'Edit Printer' : 'Add Printer'"
      modal :style="{ width: '560px' }" :closable="!printerSaving">
      <div class="form-body">
        <div class="form-section-label">Basic Info</div>
        <div class="field">
          <label class="field-label">Printer Name <span class="required">*</span></label>
          <InputText v-model="printerForm.printerName" placeholder="e.g. BambuLab X1C" class="field-input" />
        </div>
        <div class="field">
          <label class="field-label">Model <span class="required">*</span></label>
          <InputText v-model="printerForm.printerModel" placeholder="e.g. X1C" class="field-input" />
        </div>
        <div class="field">
          <label class="field-label">Printer Type</label>
          <Select v-model="printerForm.printerType" :options="printerTypeOptions"
            option-label="label" option-value="value" class="field-input" />
        </div>
        <div class="field-row">
          <div class="field">
            <label class="field-label">Location</label>
            <InputText v-model="printerForm.location" placeholder="e.g. Lab Room 3" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Serial Number</label>
            <InputText v-model="printerForm.serialNumber" placeholder="e.g. 01P00A..." class="field-input" />
          </div>
        </div>
        <div class="form-section-label" style="margin-top:1.25rem">MQTT Connection</div>
        <div class="field">
          <label class="field-label">Broker URL <span class="required">*</span></label>
          <InputText v-model="printerForm.connectionConfig.brokerUrl"
            placeholder="e.g. mqtts://192.168.1.100:8883" class="field-input" />
        </div>
        <div class="field">
          <label class="field-label">Topic <span class="required">*</span></label>
          <InputText v-model="printerForm.connectionConfig.topic"
            placeholder="e.g. device/01P00A4P00100001" class="field-input" />
        </div>
        <div class="field-row">
          <div class="field">
            <label class="field-label">Username</label>
            <InputText v-model="printerForm.connectionConfig.username" placeholder="bblp" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Password</label>
            <Password v-model="printerForm.connectionConfig.password" :feedback="false" toggle-mask
              placeholder="Access code" class="field-input" input-class="field-input" />
          </div>
        </div>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="printerSaving" @click="showPrinterDialog = false" />
        <Button :label="printerEditMode ? 'Save Changes' : 'Add Printer'" :loading="printerSaving" @click="savePrinter" />
      </template>
    </Dialog>

    <!-- ── Printer Delete Dialog ── -->
    <Dialog v-model:visible="showDeletePrinterDialog" header="Delete Printer"
      modal :style="{ width: '420px' }" :closable="!deletingPrinter">
      <div class="confirm-body">
        <i class="mdi mdi-alert-circle-outline confirm-icon" />
        <p>Delete <strong>{{ printerToDelete?.printerName }}</strong>? This will disconnect and remove all configuration.</p>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="deletingPrinter"
          @click="showDeletePrinterDialog = false" />
        <Button label="Delete" severity="danger" :loading="deletingPrinter" @click="confirmDeletePrinter" />
      </template>
    </Dialog>

    <!-- ── User Add/Edit Dialog ── -->
    <Dialog v-model:visible="showUserDialog"
      :header="userEditMode ? 'Edit User' : 'Add User'"
      modal :style="{ width: '500px' }" :closable="!userSaving">
      <div class="form-body">
        <div class="field">
          <label class="field-label">Username <span class="required">*</span></label>
          <InputText v-model="userForm.username" placeholder="e.g. jsmith" class="field-input" />
        </div>
        <div class="field-row">
          <div class="field">
            <label class="field-label">First Name</label>
            <InputText v-model="userForm.firstName" placeholder="John" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Last Name</label>
            <InputText v-model="userForm.lastName" placeholder="Smith" class="field-input" />
          </div>
        </div>
        <div v-if="!userEditMode" class="field">
          <label class="field-label">Password <span class="required">*</span></label>
          <Password v-model="userForm.password" :feedback="true" toggle-mask
            placeholder="Initial password" class="field-input" input-class="field-input" />
        </div>
        <div class="field">
          <label class="field-label">Roles</label>
          <MultiSelect v-model="userForm.roles" :options="availableRoles"
            option-label="name" data-key="roleId" placeholder="Select roles"
            class="field-input" display="chip" />
        </div>
        <div v-if="userEditMode" class="field field-inline">
          <label class="field-label">Active</label>
          <ToggleSwitch v-model="userForm.isActive" />
        </div>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="userSaving" @click="showUserDialog = false" />
        <Button :label="userEditMode ? 'Save Changes' : 'Create User'" :loading="userSaving" @click="saveUser" />
      </template>
    </Dialog>

    <!-- ── User Delete Dialog ── -->
    <Dialog v-model:visible="showDeleteUserDialog" header="Delete User"
      modal :style="{ width: '420px' }" :closable="!deletingUser">
      <div class="confirm-body">
        <i class="mdi mdi-alert-circle-outline confirm-icon" />
        <p>Delete user <strong>{{ userToDelete?.username }}</strong>? This action cannot be undone.</p>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="deletingUser"
          @click="showDeleteUserDialog = false" />
        <Button label="Delete" severity="danger" :loading="deletingUser" @click="confirmDeleteUser" />
      </template>
    </Dialog>

    <!-- ── Reset Password Dialog ── -->
    <Dialog v-model:visible="showResetPasswordDialog" header="Reset Password"
      modal :style="{ width: '420px' }" :closable="!resettingPassword">
      <div class="form-body">
        <p class="reset-info">
          Reset password for <strong>{{ resetPasswordUsername }}</strong>.
        </p>
        <div class="field">
          <label class="field-label">New Password <span class="required">*</span></label>
          <Password v-model="resetPassword" :feedback="true" toggle-mask
            placeholder="Temporary password" class="field-input" input-class="field-input" />
        </div>
        <div class="field field-inline">
          <label class="field-label">Require password change on next login</label>
          <ToggleSwitch v-model="resetMustChange" />
        </div>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="resettingPassword"
          @click="showResetPasswordDialog = false" />
        <Button label="Reset Password" :loading="resettingPassword" @click="confirmResetPassword" />
      </template>
    </Dialog>
  </div>
  </div>
</template>

<style scoped>
.settings-page {
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

.page-title {
  font-size: 1.5rem; font-weight: 700; margin: 0 0 0.25rem; color: var(--ph-text);
}

.page-subtitle {
  font-size: 0.875rem; color: var(--ph-text-muted); margin: 0;
}

/* ── Unified tab card surface ── */
.tabs-card {
  background: var(--ph-glass);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border: 1px solid var(--ph-glass-border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--ph-shadow-card), 0 1px 0 rgba(255, 255, 255, 0.05) inset;
  animation: fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both;
}

.tabs-card :deep(.p-tablist) {
  border-radius: 0 !important;
  padding: 0 1.25rem !important;
}

.tabs-card :deep(.p-tablist-tab-list) {
  background: transparent !important;
}

.tabs-card :deep(.p-tab) {
  padding: 0.75rem 1rem !important;
}

.tabs-card :deep(.p-tabpanels) {
  padding: 1.25rem !important;
}

/* Fade + slide animation on tab switch */
.tabs-card :deep(.p-tabpanel) {
  animation: tab-fade-in 0.2s ease-out both;
}

@keyframes tab-fade-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ── Panel header (title row + separator) ── */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.875rem;
  margin-bottom: 0.875rem;
  border-bottom: 1px solid var(--ph-border);
}

.tab-icon {
  margin-right: 0.35rem; font-size: 1rem;
}

.section-title {
  font-size: 0.8rem; font-weight: 600; text-transform: uppercase;
  letter-spacing: 0.06em; color: var(--ph-text-muted);
}

.muted { color: var(--ph-text-muted); }

.connection-info {
  display: flex; align-items: center; gap: 0.4rem;
  font-size: 0.8rem; color: var(--ph-text-muted); font-family: monospace;
}

.row-actions { display: flex; gap: 0.25rem; }
.role-tags { display: flex; flex-wrap: wrap; gap: 0.25rem; }

/* Profile */
.profile-section { max-width: 480px; }

.profile-form {
  display: flex; flex-direction: column; gap: 1rem;
}

.profile-actions { display: flex; justify-content: flex-end; margin-top: 0.5rem; }

/* Dialogs */
.form-body {
  display: flex; flex-direction: column; gap: 1rem; padding: 0.25rem 0;
}

.form-section-label {
  font-size: 0.75rem; font-weight: 600; text-transform: uppercase;
  letter-spacing: 0.06em; color: var(--ph-text-muted);
  padding-bottom: 0.25rem; border-bottom: 1px solid var(--ph-border);
}

.field { display: flex; flex-direction: column; gap: 0.375rem; flex: 1; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.field-label { font-size: 0.8rem; font-weight: 500; color: var(--ph-text-muted); }
.field-input { width: 100%; }

.field-inline {
  flex-direction: row; align-items: center;
  justify-content: space-between; gap: 1rem;
}

.required { color: #f87171; }

.confirm-body {
  display: flex; align-items: flex-start; gap: 1rem; padding: 0.5rem 0;
}

.confirm-icon { font-size: 2rem; color: #f87171; flex-shrink: 0; }

.confirm-body p, .reset-info {
  margin: 0; font-size: 0.9rem; line-height: 1.5; color: var(--ph-text);
}

.reset-info { margin-bottom: 0.5rem; }

/* Business */
.business-section { max-width: 560px; }

.business-hint {
  font-size: 0.85rem; color: var(--ph-text-muted); margin: 0 0 1.25rem;
}

.business-form {
  display: flex; flex-direction: column; gap: 1rem;
}

.business-actions { display: flex; justify-content: flex-end; margin-top: 0.5rem; }
</style>
