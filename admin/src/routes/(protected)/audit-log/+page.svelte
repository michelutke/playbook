<script lang="ts">
  import { goto } from '$app/navigation';
  import { formatDate } from '$lib/utils.js';
  import type { PageData } from './$types.js';
  import type { AuditLogEntry } from '$lib/types.js';

  let { data }: { data: PageData } = $props();

  let actorId = $state(data.filters?.actorId ?? '');
  let action = $state(data.filters?.action ?? '');
  let from = $state(data.filters?.from ?? '');
  let to = $state(data.filters?.to ?? '');

  let selectedEntry = $state<AuditLogEntry | null>(null);
  let drawerOpen = $state(false);
  let exporting = $state(false);
  let exportError = $state('');

  function applyFilters() {
    const params = new URLSearchParams();
    if (actorId) params.set('actorId', actorId);
    if (action) params.set('action', action);
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    goto(`/audit-log?${params}`, { replaceState: true });
  }

  function openEntry(entry: AuditLogEntry) {
    selectedEntry = entry;
    drawerOpen = true;
  }

  async function exportCsv() {
    exporting = true;
    exportError = '';

    try {
      const startRes = await fetch('/api/audit-log/export', { method: 'POST' });
      if (!startRes.ok) throw new Error('Export failed');
      const { jobId } = await startRes.json();

      // Poll for completion
      let attempts = 0;
      while (attempts < 60) {
        await new Promise((r) => setTimeout(r, 2000));
        const statusRes = await fetch(`/api/audit-log/export/${jobId}`);
        if (!statusRes.ok) throw new Error('Status check failed');
        const job = await statusRes.json();

        if (job.status === 'completed' && job.downloadUrl) {
          const a = document.createElement('a');
          a.href = job.downloadUrl;
          a.download = `audit-log-${new Date().toISOString().slice(0, 10)}.csv`;
          a.click();
          return;
        }
        if (job.status === 'failed') throw new Error('Export job failed');
        attempts++;
      }
      throw new Error('Export timed out');
    } catch (e: unknown) {
      exportError = e instanceof Error ? e.message : 'Export failed';
    } finally {
      exporting = false;
    }
  }

  const totalPages = $derived(Math.ceil((data.result?.total ?? 0) / (data.result?.pageSize ?? 50)));
  const currentPage = $derived(data.page ?? 0);

  function goPage(p: number) {
    const params = new URLSearchParams();
    if (actorId) params.set('actorId', actorId);
    if (action) params.set('action', action);
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    params.set('page', String(p));
    goto(`/audit-log?${params}`, { replaceState: true });
  }
</script>

<svelte:head>
  <title>Audit Log — Playbook Admin</title>
</svelte:head>

<div class="p-6 max-w-7xl mx-auto">
  <div class="flex items-center justify-between mb-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Audit Log</h1>
      <p class="text-zinc-500 text-sm mt-1">
        {data.result?.total ?? 0} entries
      </p>
    </div>
    <button
      onclick={exportCsv}
      disabled={exporting}
      class="bg-zinc-800 hover:bg-zinc-700 disabled:opacity-50 text-zinc-200 text-sm font-medium px-4 py-2 rounded-lg transition-colors flex items-center gap-2"
    >
      {#if exporting}
        <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
        </svg>
        Exporting…
      {:else}
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414A1 1 0 0119 9.414V19a2 2 0 01-2 2z"/>
        </svg>
        Export CSV
      {/if}
    </button>
  </div>

  {#if exportError}
    <div class="bg-red-500/10 border border-red-500/30 rounded-lg px-4 py-3 text-sm text-red-400 mb-4">
      {exportError}
    </div>
  {/if}

  <!-- Filters -->
  <div class="bg-zinc-900 border border-zinc-800 rounded-xl p-4 mb-5">
    <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-3">
      <div>
        <label class="block text-xs font-medium text-zinc-500 mb-1">Actor ID</label>
        <input
          type="text"
          placeholder="User ID…"
          bind:value={actorId}
          class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-sm text-zinc-100 placeholder-zinc-600 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
      </div>
      <div>
        <label class="block text-xs font-medium text-zinc-500 mb-1">Action</label>
        <input
          type="text"
          placeholder="e.g. CLUB_CREATED"
          bind:value={action}
          class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-sm text-zinc-100 placeholder-zinc-600 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
      </div>
      <div>
        <label class="block text-xs font-medium text-zinc-500 mb-1">From</label>
        <input
          type="date"
          bind:value={from}
          class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-sm text-zinc-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
      </div>
      <div>
        <label class="block text-xs font-medium text-zinc-500 mb-1">To</label>
        <input
          type="date"
          bind:value={to}
          class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-sm text-zinc-100 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
      </div>
    </div>
    <button
      onclick={applyFilters}
      class="mt-3 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
    >
      Apply Filters
    </button>
  </div>

  <!-- Table -->
  <div class="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b border-zinc-800 text-zinc-500 text-xs uppercase tracking-wider">
          <th class="text-left px-4 py-3">Timestamp</th>
          <th class="text-left px-4 py-3">Actor</th>
          <th class="text-left px-4 py-3">Action</th>
          <th class="text-left px-4 py-3">Target</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-zinc-800">
        {#each data.result?.items ?? [] as entry}
          <tr
            class="hover:bg-zinc-800/50 cursor-pointer transition-colors"
            onclick={() => openEntry(entry)}
          >
            <td class="px-4 py-3 text-zinc-500 text-xs whitespace-nowrap">{formatDate(entry.createdAt)}</td>
            <td class="px-4 py-3 font-mono text-xs text-zinc-400 truncate max-w-[180px]">
              {#if entry.impersonatedAs}
                <span class="text-amber-400">[impersonating]</span>{' '}
              {/if}
              {entry.actorId.slice(0, 12)}…
            </td>
            <td class="px-4 py-3 font-mono text-xs text-zinc-200">{entry.action}</td>
            <td class="px-4 py-3 text-xs text-zinc-500">
              {#if entry.targetType}
                <span class="text-zinc-400">{entry.targetType}</span>
                {#if entry.targetId}
                  <span class="text-zinc-600 ml-1">{entry.targetId.slice(0, 8)}…</span>
                {/if}
              {:else}
                —
              {/if}
            </td>
          </tr>
        {:else}
          <tr>
            <td colspan="4" class="px-4 py-12 text-center text-zinc-600">No entries found</td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>

  <!-- Pagination -->
  {#if totalPages > 1}
    <div class="flex items-center justify-between mt-4">
      <p class="text-xs text-zinc-600">
        Page {currentPage + 1} of {totalPages}
      </p>
      <div class="flex gap-2">
        <button
          onclick={() => goPage(currentPage - 1)}
          disabled={currentPage === 0}
          class="px-3 py-1.5 bg-zinc-800 hover:bg-zinc-700 disabled:opacity-40 text-zinc-300 text-sm rounded-lg transition-colors"
        >
          Previous
        </button>
        <button
          onclick={() => goPage(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          class="px-3 py-1.5 bg-zinc-800 hover:bg-zinc-700 disabled:opacity-40 text-zinc-300 text-sm rounded-lg transition-colors"
        >
          Next
        </button>
      </div>
    </div>
  {/if}
</div>

<!-- Entry Detail Drawer -->
{#if drawerOpen && selectedEntry}
  <div
    class="fixed inset-0 z-50 flex"
    role="dialog"
    aria-modal="true"
    aria-label="Audit entry detail"
  >
    <button
      class="flex-1 bg-black/50"
      onclick={() => (drawerOpen = false)}
      aria-label="Close drawer"
    ></button>
    <div class="w-full max-w-lg bg-zinc-900 border-l border-zinc-800 overflow-y-auto">
      <div class="px-6 py-5 border-b border-zinc-800 flex items-center justify-between">
        <h2 class="font-semibold text-white">Entry Detail</h2>
        <button
          onclick={() => (drawerOpen = false)}
          class="text-zinc-500 hover:text-zinc-300"
          aria-label="Close"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="p-6 space-y-4">
        <dl class="space-y-3 text-sm">
          <div>
            <dt class="text-xs font-medium text-zinc-500 uppercase tracking-wider mb-1">Timestamp</dt>
            <dd class="text-zinc-200">{formatDate(selectedEntry.createdAt)}</dd>
          </div>
          <div>
            <dt class="text-xs font-medium text-zinc-500 uppercase tracking-wider mb-1">Action</dt>
            <dd class="font-mono text-zinc-200">{selectedEntry.action}</dd>
          </div>
          <div>
            <dt class="text-xs font-medium text-zinc-500 uppercase tracking-wider mb-1">Actor ID</dt>
            <dd class="font-mono text-zinc-400 text-xs break-all">{selectedEntry.actorId}</dd>
          </div>
          {#if selectedEntry.impersonatedAs}
            <div>
              <dt class="text-xs font-medium text-amber-500 uppercase tracking-wider mb-1">Impersonating</dt>
              <dd class="font-mono text-amber-400 text-xs break-all">{selectedEntry.impersonatedAs}</dd>
            </div>
          {/if}
          {#if selectedEntry.targetType}
            <div>
              <dt class="text-xs font-medium text-zinc-500 uppercase tracking-wider mb-1">Target</dt>
              <dd class="text-zinc-300">
                {selectedEntry.targetType}
                {#if selectedEntry.targetId}
                  <span class="font-mono text-zinc-500 ml-1 text-xs">{selectedEntry.targetId}</span>
                {/if}
              </dd>
            </div>
          {/if}
          {#if selectedEntry.payload}
            <div>
              <dt class="text-xs font-medium text-zinc-500 uppercase tracking-wider mb-1">Payload</dt>
              <dd>
                <pre class="bg-zinc-950 border border-zinc-800 rounded-lg p-3 text-xs font-mono text-zinc-300 overflow-x-auto whitespace-pre-wrap break-all">{(() => {
                  try { return JSON.stringify(JSON.parse(selectedEntry.payload!), null, 2); }
                  catch { return selectedEntry.payload; }
                })()}</pre>
              </dd>
            </div>
          {/if}
        </dl>
      </div>
    </div>
  </div>
{/if}
