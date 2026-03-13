<script lang="ts">
  import { goto } from '$app/navigation';
  import { formatDate } from '$lib/utils.js';
  import type { PageData } from './$types.js';

  let { data }: { data: PageData } = $props();

  const stats = $derived(data.stats);
  const auditLog = $derived(data.auditLog);
</script>

<svelte:head>
  <title>Dashboard — Playbook Admin</title>
</svelte:head>

<div class="p-6 max-w-6xl mx-auto">
  <div class="mb-6">
    <h1 class="text-2xl font-bold text-white">Dashboard</h1>
    <p class="text-zinc-500 text-sm mt-1">Platform overview</p>
  </div>

  <!-- Metric Cards -->
  {#if stats}
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
      <MetricCard
        label="Total Clubs"
        value={stats.totalClubs}
        icon="building"
        color="indigo"
      />
      <MetricCard
        label="Total Users"
        value={stats.totalUsers}
        icon="users"
        color="emerald"
      />
      <MetricCard
        label="Active Events Today"
        value={stats.activeEventsToday}
        icon="calendar"
        color="amber"
      />
      <MetricCard
        label="Sign-ups (7d)"
        value={stats.signUpsLast7Days}
        icon="trending-up"
        color="sky"
      />
    </div>
  {:else}
    <div class="bg-zinc-900 border border-zinc-800 rounded-xl p-6 mb-8 text-zinc-500 text-sm">
      Stats unavailable
    </div>
  {/if}

  <div class="grid lg:grid-cols-3 gap-6">
    <!-- Recent Activity -->
    <div class="lg:col-span-2 bg-zinc-900 border border-zinc-800 rounded-xl">
      <div class="px-5 py-4 border-b border-zinc-800 flex items-center justify-between">
        <h2 class="font-semibold text-white text-sm">Recent Activity</h2>
        <a href="/audit-log" class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
          View all
        </a>
      </div>
      <div class="divide-y divide-zinc-800">
        {#if auditLog?.items?.length}
          {#each auditLog.items as entry}
            <div class="px-5 py-3">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-sm text-zinc-200 font-mono truncate">{entry.action}</p>
                  {#if entry.targetType}
                    <p class="text-xs text-zinc-500 truncate mt-0.5">
                      {entry.targetType}{entry.targetId ? ` · ${entry.targetId.slice(0, 8)}` : ''}
                    </p>
                  {/if}
                </div>
                <time class="text-xs text-zinc-600 shrink-0 whitespace-nowrap">
                  {formatDate(entry.createdAt)}
                </time>
              </div>
            </div>
          {/each}
        {:else}
          <div class="px-5 py-8 text-center text-zinc-600 text-sm">No activity yet</div>
        {/if}
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="bg-zinc-900 border border-zinc-800 rounded-xl h-fit">
      <div class="px-5 py-4 border-b border-zinc-800">
        <h2 class="font-semibold text-white text-sm">Quick Actions</h2>
      </div>
      <div class="p-4 space-y-2">
        <a
          href="/clubs?new=1"
          class="flex items-center gap-3 w-full px-4 py-3 bg-indigo-600 hover:bg-indigo-500 rounded-lg transition-colors text-sm font-medium text-white"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          New Club
        </a>
        <a
          href="/users"
          class="flex items-center gap-3 w-full px-4 py-3 bg-zinc-800 hover:bg-zinc-700 rounded-lg transition-colors text-sm font-medium text-zinc-200"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <circle cx="11" cy="11" r="8" stroke-width="2"/>
            <path stroke-linecap="round" stroke-width="2" d="M21 21l-4.35-4.35"/>
          </svg>
          Search User
        </a>
        <a
          href="/audit-log"
          class="flex items-center gap-3 w-full px-4 py-3 bg-zinc-800 hover:bg-zinc-700 rounded-lg transition-colors text-sm font-medium text-zinc-200"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
          </svg>
          View Audit Log
        </a>
        <a
          href="/billing"
          class="flex items-center gap-3 w-full px-4 py-3 bg-zinc-800 hover:bg-zinc-700 rounded-lg transition-colors text-sm font-medium text-zinc-200"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <rect x="1" y="4" width="22" height="16" rx="2" ry="2" stroke-width="2"/>
            <path stroke-linecap="round" stroke-width="2" d="M1 10h22"/>
          </svg>
          View Billing
        </a>
      </div>
    </div>
  </div>
</div>

{#snippet MetricCard({ label, value, icon, color }: { label: string; value: number; icon: string; color: string })}
  {@const colorMap: Record<string, string> = {
    indigo: 'bg-indigo-500/10 text-indigo-400',
    emerald: 'bg-emerald-500/10 text-emerald-400',
    amber: 'bg-amber-500/10 text-amber-400',
    sky: 'bg-sky-500/10 text-sky-400'
  }}
  <div class="bg-zinc-900 border border-zinc-800 rounded-xl p-5">
    <div class="flex items-center justify-between mb-3">
      <p class="text-xs font-medium text-zinc-500 uppercase tracking-wider">{label}</p>
      <div class="w-8 h-8 rounded-lg flex items-center justify-center {colorMap[color]}">
        {#if icon === 'building'}
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16M5 21h14"/>
          </svg>
        {:else if icon === 'users'}
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/>
          </svg>
        {:else if icon === 'calendar'}
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2" stroke-width="2"/>
            <path stroke-linecap="round" stroke-width="2" d="M16 2v4M8 2v4M3 10h18"/>
          </svg>
        {:else if icon === 'trending-up'}
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <polyline points="17 6 23 6 23 12" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        {/if}
      </div>
    </div>
    <p class="text-3xl font-bold text-white">{value.toLocaleString()}</p>
  </div>
{/snippet}
