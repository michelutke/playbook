<script lang="ts">
  import { goto } from '$app/navigation';
  import { page } from '$app/state';
  import { enhance } from '$app/forms';
  import { statusColor, formatDateOnly } from '$lib/utils.js';
  import type { PageData, ActionData } from './$types.js';
  import type { SaClub } from '$lib/types.js';

  let { data, form }: { data: PageData; form: ActionData } = $props();

  let search = $state(data.search ?? '');
  let statusFilter = $state(data.status ?? '');
  let sortKey = $state<keyof SaClub>('name');
  let sortDir = $state<'asc' | 'desc'>('asc');
  let showCreateModal = $state(false);
  let createLoading = $state(false);

  $effect(() => {
    if (form?.success) showCreateModal = false;
  });

  const filtered = $derived(() => {
    const clubs = [...(data.clubs ?? [])];
    clubs.sort((a, b) => {
      const av = String(a[sortKey] ?? '');
      const bv = String(b[sortKey] ?? '');
      return sortDir === 'asc' ? av.localeCompare(bv) : bv.localeCompare(av);
    });
    return clubs;
  });

  function toggleSort(key: keyof SaClub) {
    if (sortKey === key) sortDir = sortDir === 'asc' ? 'desc' : 'asc';
    else { sortKey = key; sortDir = 'asc'; }
  }

  function applyFilters() {
    const params = new URLSearchParams();
    if (search) params.set('search', search);
    if (statusFilter) params.set('status', statusFilter);
    goto(`/clubs?${params}`, { replaceState: true });
  }

  const statusTabs = ['', 'ACTIVE', 'INACTIVE'];

  function SortIcon(key: keyof SaClub) {
    if (sortKey !== key) return '↕';
    return sortDir === 'asc' ? '↑' : '↓';
  }
</script>

<svelte:head>
  <title>Clubs — Playbook Admin</title>
</svelte:head>

<div class="p-6 max-w-7xl mx-auto">
  <div class="flex items-center justify-between mb-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Clubs</h1>
      <p class="text-zinc-500 text-sm mt-1">{data.clubs?.length ?? 0} clubs</p>
    </div>
    <button
      onclick={() => (showCreateModal = true)}
      class="bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors flex items-center gap-2"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
      </svg>
      New Club
    </button>
  </div>

  <!-- Filters -->
  <div class="flex flex-col sm:flex-row gap-3 mb-5">
    <div class="flex-1 relative">
      <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <circle cx="11" cy="11" r="8" stroke-width="2"/>
        <path stroke-linecap="round" stroke-width="2" d="M21 21l-4.35-4.35"/>
      </svg>
      <input
        type="text"
        placeholder="Search clubs…"
        bind:value={search}
        onkeydown={(e) => e.key === 'Enter' && applyFilters()}
        class="w-full bg-zinc-900 border border-zinc-800 rounded-lg pl-9 pr-3 py-2 text-sm text-zinc-100 placeholder-zinc-600 focus:outline-none focus:ring-2 focus:ring-indigo-500"
      />
    </div>
    <button
      onclick={applyFilters}
      class="bg-zinc-800 hover:bg-zinc-700 text-zinc-200 text-sm px-4 py-2 rounded-lg transition-colors"
    >
      Search
    </button>
  </div>

  <!-- Status tabs -->
  <div class="flex gap-1 mb-5 bg-zinc-900 border border-zinc-800 rounded-lg p-1 w-fit">
    {#each statusTabs as tab}
      <button
        onclick={() => { statusFilter = tab; applyFilters(); }}
        class="px-4 py-1.5 rounded-md text-sm font-medium transition-colors
          {statusFilter === tab ? 'bg-zinc-700 text-white' : 'text-zinc-500 hover:text-zinc-300'}"
      >
        {tab === '' ? 'All' : tab.charAt(0) + tab.slice(1).toLowerCase()}
      </button>
    {/each}
  </div>

  <!-- Table -->
  <div class="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b border-zinc-800 text-zinc-500 text-xs uppercase tracking-wider">
          <th class="text-left px-4 py-3">
            <button onclick={() => toggleSort('name')} class="hover:text-zinc-300 transition-colors flex items-center gap-1">
              Club Name <span class="text-zinc-600">{SortIcon('name')}</span>
            </button>
          </th>
          <th class="text-left px-4 py-3">Sport</th>
          <th class="text-right px-4 py-3">
            <button onclick={() => toggleSort('managerCount')} class="hover:text-zinc-300 transition-colors">
              Managers {SortIcon('managerCount')}
            </button>
          </th>
          <th class="text-right px-4 py-3">
            <button onclick={() => toggleSort('teamCount')} class="hover:text-zinc-300 transition-colors">
              Teams {SortIcon('teamCount')}
            </button>
          </th>
          <th class="text-right px-4 py-3">
            <button onclick={() => toggleSort('memberCount')} class="hover:text-zinc-300 transition-colors">
              Members {SortIcon('memberCount')}
            </button>
          </th>
          <th class="text-left px-4 py-3">Status</th>
          <th class="text-left px-4 py-3">Created</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-zinc-800">
        {#each filtered() as club}
          <tr
            class="hover:bg-zinc-800/50 cursor-pointer transition-colors"
            onclick={() => goto(`/clubs/${club.id}`)}
          >
            <td class="px-4 py-3 font-medium text-zinc-100">{club.name}</td>
            <td class="px-4 py-3 text-zinc-400">{club.sportType}</td>
            <td class="px-4 py-3 text-right text-zinc-400">{club.managerCount}</td>
            <td class="px-4 py-3 text-right text-zinc-400">{club.teamCount}</td>
            <td class="px-4 py-3 text-right text-zinc-400">{club.memberCount}</td>
            <td class="px-4 py-3">
              <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium border {statusColor(club.status)}">
                {club.status}
              </span>
            </td>
            <td class="px-4 py-3 text-zinc-500 text-xs">{formatDateOnly(club.createdAt)}</td>
          </tr>
        {:else}
          <tr>
            <td colspan="7" class="px-4 py-12 text-center text-zinc-600">No clubs found</td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
</div>

<!-- Create Club Modal -->
{#if showCreateModal}
  <div
    class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-label="Create club"
  >
    <div class="bg-zinc-900 border border-zinc-800 rounded-xl w-full max-w-md shadow-2xl">
      <div class="px-6 py-5 border-b border-zinc-800 flex items-center justify-between">
        <h2 class="font-semibold text-white">New Club</h2>
        <button
          onclick={() => (showCreateModal = false)}
          class="text-zinc-500 hover:text-zinc-300 transition-colors"
          aria-label="Close"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <form
        method="POST"
        action="?/create"
        use:enhance={() => {
          createLoading = true;
          return async ({ update }) => {
            createLoading = false;
            await update();
          };
        }}
        class="p-6 space-y-4"
      >
        {#if form?.error}
          <div class="bg-red-500/10 border border-red-500/30 rounded-lg px-3 py-2.5 text-sm text-red-400">
            {form.error}
          </div>
        {/if}

        <div>
          <label for="club-name" class="block text-sm font-medium text-zinc-300 mb-1.5">Club Name *</label>
          <input
            id="club-name"
            name="name"
            type="text"
            required
            class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2.5 text-sm text-zinc-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        <div>
          <label for="sport-type" class="block text-sm font-medium text-zinc-300 mb-1.5">Sport Type *</label>
          <input
            id="sport-type"
            name="sportType"
            type="text"
            required
            placeholder="e.g. Football, Basketball"
            class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2.5 text-sm text-zinc-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        <div>
          <label for="location" class="block text-sm font-medium text-zinc-300 mb-1.5">Location</label>
          <input
            id="location"
            name="location"
            type="text"
            placeholder="Optional"
            class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2.5 text-sm text-zinc-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        <div class="flex gap-3 pt-2">
          <button
            type="button"
            onclick={() => (showCreateModal = false)}
            class="flex-1 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-sm font-medium py-2.5 rounded-lg transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={createLoading}
            class="flex-1 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-sm font-semibold py-2.5 rounded-lg transition-colors"
          >
            {createLoading ? 'Creating…' : 'Create Club'}
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}
