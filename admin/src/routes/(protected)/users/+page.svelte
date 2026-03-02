<script lang="ts">
  import { goto } from '$app/navigation';
  import type { PageData } from './$types.js';
  import type { UserSearchResult } from '$lib/types.js';

  let { data }: { data: PageData } = $props();

  let query = $state(data.q ?? '');
  let selectedUser = $state<UserSearchResult | null>(null);
  let drawerOpen = $state(false);

  function handleSearch(e: SubmitEvent) {
    e.preventDefault();
    goto(`/users?q=${encodeURIComponent(query)}`, { replaceState: true });
  }

  function openDrawer(user: UserSearchResult) {
    selectedUser = user;
    drawerOpen = true;
  }

  function initials(user: UserSearchResult): string {
    const name = user.displayName ?? user.email;
    return name.slice(0, 2).toUpperCase();
  }
</script>

<svelte:head>
  <title>Users — Playbook Admin</title>
</svelte:head>

<div class="p-6 max-w-4xl mx-auto">
  <div class="mb-6">
    <h1 class="text-2xl font-bold text-white">Users</h1>
    <p class="text-zinc-500 text-sm mt-1">Search by name or email</p>
  </div>

  <form onsubmit={handleSearch} class="flex gap-3 mb-6">
    <div class="flex-1 relative">
      <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <circle cx="11" cy="11" r="8" stroke-width="2"/>
        <path stroke-linecap="round" stroke-width="2" d="M21 21l-4.35-4.35"/>
      </svg>
      <input
        type="text"
        placeholder="Search by name or email…"
        bind:value={query}
        class="w-full bg-zinc-900 border border-zinc-800 rounded-lg pl-9 pr-3 py-2.5 text-sm text-zinc-100 placeholder-zinc-600 focus:outline-none focus:ring-2 focus:ring-indigo-500"
      />
    </div>
    <button
      type="submit"
      class="bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold px-5 py-2.5 rounded-lg transition-colors"
    >
      Search
    </button>
  </form>

  {#if data.q && data.users?.length === 0}
    <div class="bg-zinc-900 border border-zinc-800 rounded-xl p-8 text-center">
      <p class="text-zinc-500 text-sm">No users found for "{data.q}"</p>
    </div>
  {:else if data.users?.length}
    <div class="space-y-2">
      {#each data.users as user}
        <button
          onclick={() => openDrawer(user)}
          class="w-full bg-zinc-900 border border-zinc-800 hover:border-zinc-700 rounded-xl p-4 text-left transition-colors flex items-center gap-4"
        >
          <div class="w-10 h-10 rounded-full bg-indigo-600 flex items-center justify-center text-white font-semibold text-sm shrink-0">
            {initials(user)}
          </div>
          <div class="min-w-0 flex-1">
            {#if user.displayName}
              <p class="font-medium text-zinc-100">{user.displayName}</p>
              <p class="text-sm text-zinc-500 truncate">{user.email}</p>
            {:else}
              <p class="font-medium text-zinc-100">{user.email}</p>
            {/if}
          </div>
          <div class="flex flex-wrap gap-1 justify-end">
            {#each user.clubMemberships.slice(0, 3) as membership}
              <span class="px-2 py-0.5 bg-zinc-800 text-zinc-400 rounded text-xs">{membership.clubName}</span>
            {/each}
            {#if user.clubMemberships.length > 3}
              <span class="px-2 py-0.5 bg-zinc-800 text-zinc-500 rounded text-xs">+{user.clubMemberships.length - 3}</span>
            {/if}
          </div>
          <svg class="w-4 h-4 text-zinc-600 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </button>
      {/each}
    </div>
  {:else}
    <div class="bg-zinc-900 border border-zinc-800 rounded-xl p-8 text-center">
      <svg class="w-8 h-8 text-zinc-700 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <circle cx="11" cy="11" r="8" stroke-width="2"/>
        <path stroke-linecap="round" stroke-width="2" d="M21 21l-4.35-4.35"/>
      </svg>
      <p class="text-zinc-500 text-sm">Enter a name or email to search</p>
    </div>
  {/if}
</div>

<!-- User Detail Drawer -->
{#if drawerOpen && selectedUser}
  <div
    class="fixed inset-0 z-50 flex"
    role="dialog"
    aria-modal="true"
    aria-label="User detail"
  >
    <button
      class="flex-1 bg-black/50"
      onclick={() => (drawerOpen = false)}
      aria-label="Close drawer"
    ></button>
    <div class="w-full max-w-md bg-zinc-900 border-l border-zinc-800 overflow-y-auto">
      <div class="px-6 py-5 border-b border-zinc-800 flex items-center justify-between">
        <h2 class="font-semibold text-white">User Detail</h2>
        <button
          onclick={() => (drawerOpen = false)}
          class="text-zinc-500 hover:text-zinc-300 transition-colors"
          aria-label="Close"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="p-6 space-y-6">
        <div class="flex items-center gap-4">
          <div class="w-14 h-14 rounded-full bg-indigo-600 flex items-center justify-center text-white font-bold text-lg">
            {initials(selectedUser)}
          </div>
          <div>
            {#if selectedUser.displayName}
              <p class="font-semibold text-white">{selectedUser.displayName}</p>
            {/if}
            <p class="text-zinc-400 text-sm">{selectedUser.email}</p>
            <p class="text-zinc-600 text-xs mt-1 font-mono">{selectedUser.id}</p>
          </div>
        </div>

        <div>
          <h3 class="text-xs font-medium text-zinc-500 uppercase tracking-wider mb-3">
            Club Memberships ({selectedUser.clubMemberships.length})
          </h3>
          {#if selectedUser.clubMemberships.length}
            <div class="space-y-2">
              {#each selectedUser.clubMemberships as m}
                <a
                  href="/clubs/{m.clubId}"
                  class="flex items-center justify-between p-3 bg-zinc-800 hover:bg-zinc-700 rounded-lg transition-colors"
                >
                  <span class="text-sm font-medium text-zinc-200">{m.clubName}</span>
                  <span class="text-xs text-zinc-500 capitalize">{m.role.toLowerCase()}</span>
                </a>
              {/each}
            </div>
          {:else}
            <p class="text-zinc-600 text-sm">No club memberships</p>
          {/if}
        </div>
      </div>
    </div>
  </div>
{/if}
