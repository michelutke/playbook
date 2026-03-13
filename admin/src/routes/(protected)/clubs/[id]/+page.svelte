<script lang="ts">
  import { goto } from '$app/navigation';
  import { enhance } from '$app/forms';
  import { onMount } from 'svelte';
  import { statusColor, formatDate, formatDateOnly } from '$lib/utils.js';
  import {
    saveImpersonation,
    loadImpersonation,
    clearImpersonation,
    formatCountdown,
    isExpired,
    secondsRemaining,
    type ImpersonationState
  } from '$lib/stores.js';
  import type { PageData, ActionData } from './$types.js';

  let { data, form }: { data: PageData; form: ActionData } = $props();

  const club = $derived(data.club);
  const managers = $derived(data.managers ?? []);

  // UI state
  let editMode = $state(false);
  let showInviteSheet = $state(false);
  let showDeactivateDialog = $state(false);
  let showDeleteModal = $state(false);
  let dangerOpen = $state(false);

  let deleteConfirmName = $state('');
  let deleteLoading = $state(false);

  // Toast
  let toastMsg = $state('');
  let toastVisible = $state(false);
  let toastType = $state<'ok' | 'err'>('ok');

  function toast(msg: string, type: 'ok' | 'err' = 'ok') {
    toastMsg = msg;
    toastType = type;
    toastVisible = true;
    setTimeout(() => (toastVisible = false), 4000);
  }

  // Impersonation (local page state mirrors sessionStorage)
  let impersonation = $state<ImpersonationState | null>(null);
  let countdown = $state('00:00');
  let warned = $state(false);
  let intervalId: ReturnType<typeof setInterval> | null = null;

  function startTimer(state: ImpersonationState) {
    if (intervalId) clearInterval(intervalId);
    countdown = formatCountdown(state.expiresAt);
    intervalId = setInterval(() => {
      countdown = formatCountdown(state.expiresAt);
      const secs = secondsRemaining(state.expiresAt);
      if (secs <= 0) {
        clearInterval(intervalId!);
        handleImpersonationExpired();
        return;
      }
      if (secs <= 300 && !warned) {
        warned = true;
        toast('Impersonation session expires in 5 minutes', 'ok');
      }
    }, 1000);
  }

  function handleImpersonationExpired() {
    clearImpersonation();
    impersonation = null;
    toast('Impersonation session expired', 'err');
  }

  onMount(() => {
    const saved = loadImpersonation();
    if (saved && !isExpired(saved.expiresAt) && saved.clubId === club.id) {
      impersonation = saved;
      startTimer(saved);
    } else if (saved?.clubId === club.id) {
      clearImpersonation();
    }
    return () => { if (intervalId) clearInterval(intervalId); };
  });

  // Handle form results reactively
  $effect(() => {
    if (!form) return;

    if (form.action === 'impersonate' && form.success && form.impersonation) {
      const state: ImpersonationState = form.impersonation as ImpersonationState;
      saveImpersonation(state);
      impersonation = state;
      warned = false;
      startTimer(state);
      toast('Impersonation started');
    }

    if (form.action === 'invite' && form.success) {
      showInviteSheet = false;
      toast('Manager invited');
    }

    if (form.action === 'removeManager' && form.success) {
      toast('Manager removed');
    }

    if (form.action === 'update' && form.success) {
      editMode = false;
      toast('Club updated');
    }

    if (form.action === 'deactivate' && form.success) {
      showDeactivateDialog = false;
      toast('Club deactivated');
    }

    if (form.action === 'reactivate' && form.success) {
      toast('Club reactivated');
    }

    if (form.action === 'delete' && form.success) {
      goto('/clubs');
    }

    if (form.error) {
      toast(form.error, 'err');
    }
  });

  async function exitImpersonation() {
    if (!impersonation) return;
    try {
      await fetch(`/api/impersonation/${impersonation.sessionId}/end`, { method: 'POST' });
    } catch {
      // best-effort
    }
    clearImpersonation();
    if (intervalId) clearInterval(intervalId);
    impersonation = null;
    toast('Impersonation ended');
  }

  const isActive = $derived(club.status === 'ACTIVE');
  const deleteReady = $derived(deleteConfirmName === club.name);
</script>

<svelte:head>
  <title>{club.name} — Playbook Admin</title>
</svelte:head>

<div class="p-6 max-w-5xl mx-auto">
  <!-- Breadcrumb -->
  <div class="flex items-center gap-2 text-sm text-zinc-500 mb-5">
    <a href="/clubs" class="hover:text-zinc-300 transition-colors">Clubs</a>
    <span>/</span>
    <span class="text-zinc-300">{club.name}</span>
  </div>

  <!-- Impersonation banner (local) -->
  {#if impersonation}
    <div class="bg-amber-500/10 border border-amber-500/30 rounded-xl px-5 py-4 mb-5 flex items-center justify-between">
      <div>
        <p class="text-amber-400 font-medium text-sm">Active Impersonation Session</p>
        <p class="text-amber-300/70 text-xs mt-0.5">
          As <strong>{impersonation.managerEmail}</strong> — expires in
          <strong class="font-mono">{countdown}</strong>
        </p>
      </div>
      <button
        onclick={exitImpersonation}
        class="bg-amber-500 hover:bg-amber-400 text-amber-950 text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors"
      >
        Exit
      </button>
    </div>
  {/if}

  <!-- Club Header -->
  <div class="bg-zinc-900 border border-zinc-800 rounded-xl p-6 mb-5">
    {#if editMode}
      <form
        method="POST"
        action="?/update"
        use:enhance={() => ({ async update() { await update(); } })}
        class="space-y-4"
      >
        <div class="grid sm:grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-medium text-zinc-500 mb-1.5">Club Name *</label>
            <input name="name" value={club.name} required
              class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2.5 text-sm text-zinc-100 focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <div>
            <label class="block text-xs font-medium text-zinc-500 mb-1.5">Sport Type</label>
            <input name="sportType" value={club.sportType}
              class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2.5 text-sm text-zinc-100 focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
          <div>
            <label class="block text-xs font-medium text-zinc-500 mb-1.5">Location</label>
            <input name="location" value={club.location ?? ''}
              class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2.5 text-sm text-zinc-100 focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>
        </div>
        <div class="flex gap-3">
          <button type="submit" class="bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors">
            Save
          </button>
          <button type="button" onclick={() => (editMode = false)}
            class="bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-sm px-4 py-2 rounded-lg transition-colors">
            Cancel
          </button>
        </div>
      </form>
    {:else}
      <div class="flex items-start justify-between gap-4">
        <div>
          <div class="flex items-center gap-3 mb-2">
            <h1 class="text-2xl font-bold text-white">{club.name}</h1>
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border {statusColor(club.status)}">
              {club.status}
            </span>
          </div>
          <div class="flex flex-wrap gap-4 text-sm text-zinc-500">
            <span>{club.sportType}</span>
            {#if club.location}<span>· {club.location}</span>{/if}
            <span>· Created {formatDateOnly(club.createdAt)}</span>
          </div>
        </div>
        <button
          onclick={() => (editMode = true)}
          class="bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-sm px-3 py-2 rounded-lg transition-colors flex items-center gap-2 shrink-0"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
          </svg>
          Edit
        </button>
      </div>

      <!-- Summary stats -->
      <div class="grid grid-cols-3 gap-4 mt-5 pt-5 border-t border-zinc-800">
        <div>
          <p class="text-2xl font-bold text-white">{club.teamCount}</p>
          <p class="text-xs text-zinc-500 mt-0.5">Teams</p>
        </div>
        <div>
          <p class="text-2xl font-bold text-white">{club.memberCount}</p>
          <p class="text-xs text-zinc-500 mt-0.5">Members</p>
        </div>
        <div>
          <p class="text-2xl font-bold text-white">{club.managerCount}</p>
          <p class="text-xs text-zinc-500 mt-0.5">Managers</p>
        </div>
      </div>
    {/if}
  </div>

  <!-- Managers -->
  <div class="bg-zinc-900 border border-zinc-800 rounded-xl mb-5">
    <div class="px-5 py-4 border-b border-zinc-800 flex items-center justify-between">
      <h2 class="font-semibold text-white text-sm">Managers</h2>
      <button
        onclick={() => (showInviteSheet = true)}
        class="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5"
      >
        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        Invite Manager
      </button>
    </div>

    {#if managers.length === 0}
      <div class="px-5 py-4 bg-amber-500/5 border-b border-amber-500/20">
        <p class="text-amber-400 text-sm flex items-center gap-2">
          <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
          </svg>
          This club has no managers. Invite one to get started.
        </p>
      </div>
    {/if}

    <div class="divide-y divide-zinc-800">
      {#each managers as manager}
        <div class="px-5 py-3 flex items-center justify-between gap-4">
          <div class="min-w-0">
            <p class="text-sm font-medium text-zinc-200 truncate">
              {manager.displayName ?? manager.invitedEmail}
            </p>
            {#if manager.displayName}
              <p class="text-xs text-zinc-500 truncate">{manager.invitedEmail}</p>
            {/if}
          </div>
          <div class="flex items-center gap-2 shrink-0">
            <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium border {statusColor(manager.status)}">
              {manager.status}
            </span>

            {#if manager.status === 'ACTIVE' && manager.userId}
              <form method="POST" action="?/impersonate" use:enhance>
                <input type="hidden" name="managerId" value={manager.id} />
                <input type="hidden" name="managerEmail" value={manager.invitedEmail} />
                <button
                  type="submit"
                  class="text-xs bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 px-2.5 py-1 rounded-lg transition-colors"
                >
                  Impersonate
                </button>
              </form>
            {/if}

            <form method="POST" action="?/removeManager" use:enhance>
              <input type="hidden" name="managerId" value={manager.id} />
              <button
                type="submit"
                class="text-xs bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 px-2.5 py-1 rounded-lg transition-colors"
                onclick={(e) => {
                  if (!confirm(`Remove ${manager.invitedEmail}?`)) e.preventDefault();
                }}
              >
                Remove
              </button>
            </form>
          </div>
        </div>
      {/each}
    </div>
  </div>

  <!-- Danger Zone -->
  <div class="bg-zinc-900 border border-red-900/30 rounded-xl">
    <button
      onclick={() => (dangerOpen = !dangerOpen)}
      class="w-full px-5 py-4 flex items-center justify-between text-left"
      aria-expanded={dangerOpen}
    >
      <div class="flex items-center gap-2">
        <svg class="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
        </svg>
        <span class="font-semibold text-sm text-red-400">Danger Zone</span>
      </div>
      <svg
        class="w-4 h-4 text-zinc-500 transition-transform {dangerOpen ? 'rotate-180' : ''}"
        fill="none" stroke="currentColor" viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
      </svg>
    </button>

    {#if dangerOpen}
      <div class="px-5 pb-5 space-y-3 border-t border-red-900/30">
        <div class="pt-4">
          {#if isActive}
            <div class="flex items-start justify-between gap-4 p-4 bg-zinc-800/50 rounded-lg">
              <div>
                <p class="font-medium text-zinc-200 text-sm">Deactivate Club</p>
                <p class="text-zinc-500 text-xs mt-0.5">Suspends all club activity. Members cannot access the club.</p>
              </div>
              <button
                onclick={() => (showDeactivateDialog = true)}
                class="bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 border border-amber-500/30 text-xs font-medium px-3 py-1.5 rounded-lg transition-colors shrink-0"
              >
                Deactivate
              </button>
            </div>
          {:else}
            <div class="flex items-start justify-between gap-4 p-4 bg-zinc-800/50 rounded-lg">
              <div>
                <p class="font-medium text-zinc-200 text-sm">Reactivate Club</p>
                <p class="text-zinc-500 text-xs mt-0.5">Restores access for all members and managers.</p>
              </div>
              <form method="POST" action="?/reactivate" use:enhance>
                <button
                  type="submit"
                  class="bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 text-xs font-medium px-3 py-1.5 rounded-lg transition-colors"
                >
                  Reactivate
                </button>
              </form>
            </div>
          {/if}

          <div class="flex items-start justify-between gap-4 p-4 bg-zinc-800/50 rounded-lg mt-3">
            <div>
              <p class="font-medium text-red-400 text-sm">Delete Club</p>
              <p class="text-zinc-500 text-xs mt-0.5">Permanently removes the club and all associated data. Cannot be undone.</p>
            </div>
            <button
              onclick={() => (showDeleteModal = true)}
              class="bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 text-xs font-medium px-3 py-1.5 rounded-lg transition-colors shrink-0"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    {/if}
  </div>
</div>

<!-- Invite Manager Sheet -->
{#if showInviteSheet}
  <div
    class="fixed inset-0 z-50 flex"
    role="dialog"
    aria-modal="true"
    aria-label="Invite manager"
  >
    <button class="flex-1 bg-black/50" onclick={() => (showInviteSheet = false)} aria-label="Close"></button>
    <div class="w-full max-w-sm bg-zinc-900 border-l border-zinc-800 overflow-y-auto">
      <div class="px-6 py-5 border-b border-zinc-800 flex items-center justify-between">
        <h2 class="font-semibold text-white">Invite Manager</h2>
        <button onclick={() => (showInviteSheet = false)} class="text-zinc-500 hover:text-zinc-300" aria-label="Close">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      <form method="POST" action="?/inviteManager" use:enhance class="p-6 space-y-4">
        {#if form?.action === 'invite' && form.error}
          <div class="bg-red-500/10 border border-red-500/30 rounded-lg px-3 py-2.5 text-sm text-red-400">
            {form.error}
          </div>
        {/if}
        <div>
          <label for="inv-email" class="block text-sm font-medium text-zinc-300 mb-1.5">Email address *</label>
          <input
            id="inv-email"
            name="email"
            type="email"
            required
            placeholder="manager@club.com"
            class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2.5 text-sm text-zinc-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div class="flex gap-3">
          <button type="button" onclick={() => (showInviteSheet = false)}
            class="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-sm font-medium py-2.5 rounded-lg transition-colors">
            Cancel
          </button>
          <button type="submit"
            class="flex-1 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold py-2.5 rounded-lg transition-colors">
            Send Invite
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}

<!-- Deactivate Confirm Dialog -->
{#if showDeactivateDialog}
  <div
    class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-label="Confirm deactivation"
  >
    <div class="bg-zinc-900 border border-zinc-800 rounded-xl w-full max-w-md shadow-2xl p-6">
      <div class="flex items-start gap-4 mb-5">
        <div class="w-10 h-10 rounded-full bg-amber-500/10 flex items-center justify-center shrink-0">
          <svg class="w-5 h-5 text-amber-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
          </svg>
        </div>
        <div>
          <h3 class="font-semibold text-white">Deactivate "{club.name}"?</h3>
          <div class="text-zinc-400 text-sm mt-2 space-y-1">
            <p>This will:</p>
            <ul class="list-disc pl-4 space-y-1 text-zinc-500">
              <li>Suspend all club activity immediately</li>
              <li>Prevent members from accessing the club</li>
              <li>Block new registrations</li>
              <li>Keep all data intact (reversible)</li>
            </ul>
          </div>
        </div>
      </div>
      <div class="flex gap-3">
        <button onclick={() => (showDeactivateDialog = false)}
          class="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-sm font-medium py-2.5 rounded-lg transition-colors">
          Cancel
        </button>
        <form method="POST" action="?/deactivate" use:enhance class="flex-1">
          <button type="submit"
            class="w-full bg-amber-500 hover:bg-amber-400 text-amber-950 text-sm font-semibold py-2.5 rounded-lg transition-colors">
            Deactivate
          </button>
        </form>
      </div>
    </div>
  </div>
{/if}

<!-- Delete Confirm Modal (type-to-confirm) -->
{#if showDeleteModal}
  <div
    class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-label="Confirm club deletion"
  >
    <div class="bg-zinc-900 border border-red-900/50 rounded-xl w-full max-w-md shadow-2xl p-6">
      <div class="flex items-start gap-4 mb-5">
        <div class="w-10 h-10 rounded-full bg-red-500/10 flex items-center justify-center shrink-0">
          <svg class="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
          </svg>
        </div>
        <div>
          <h3 class="font-semibold text-white">Delete club permanently?</h3>
          <p class="text-zinc-400 text-sm mt-1">
            This action cannot be undone. All teams, members, events, and data will be permanently deleted.
          </p>
        </div>
      </div>

      <div class="mb-5">
        <label class="block text-sm text-zinc-400 mb-2">
          Type <strong class="text-zinc-200 font-mono">{club.name}</strong> to confirm:
        </label>
        <input
          type="text"
          bind:value={deleteConfirmName}
          placeholder={club.name}
          class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2.5 text-sm text-zinc-100 focus:outline-none focus:ring-2 focus:ring-red-500"
        />
      </div>

      {#if form?.action === 'delete' && form.error}
        <div class="bg-red-500/10 border border-red-500/30 rounded-lg px-3 py-2.5 text-sm text-red-400 mb-4">
          {form.error}
        </div>
      {/if}

      <div class="flex gap-3">
        <button
          onclick={() => { showDeleteModal = false; deleteConfirmName = ''; }}
          class="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-sm font-medium py-2.5 rounded-lg transition-colors"
        >
          Cancel
        </button>
        <form method="POST" action="?/delete" use:enhance class="flex-1">
          <input type="hidden" name="confirmName" value={deleteConfirmName} />
          <button
            type="submit"
            disabled={!deleteReady || deleteLoading}
            class="w-full bg-red-600 hover:bg-red-500 disabled:opacity-40 disabled:cursor-not-allowed text-white text-sm font-semibold py-2.5 rounded-lg transition-colors"
          >
            Delete permanently
          </button>
        </form>
      </div>
    </div>
  </div>
{/if}

<!-- Toast -->
{#if toastVisible}
  <div
    class="fixed bottom-4 right-4 z-50 px-4 py-3 rounded-lg shadow-xl text-sm font-medium max-w-sm
      {toastType === 'err' ? 'bg-red-600 text-white' : 'bg-zinc-800 text-zinc-100 border border-zinc-700'}"
  >
    {toastMsg}
  </div>
{/if}
