<script lang="ts">
  import '../app.css';
  import { page } from '$app/state';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  import {
    loadImpersonation,
    clearImpersonation,
    formatCountdown,
    isExpired,
    secondsRemaining,
    type ImpersonationState
  } from '$lib/stores.js';
  import { createApiClient } from '$lib/api.js';
  import type { LayoutData } from './$types.js';

  let { data, children }: { data: LayoutData; children: import('svelte').Snippet } = $props();

  const isLoginPage = $derived(page.url.pathname === '/login');

  // Impersonation state
  let impersonation = $state<ImpersonationState | null>(null);
  let countdown = $state('00:00');
  let warned = $state(false);
  let toastMessage = $state('');
  let toastVisible = $state(false);
  let toastType = $state<'info' | 'warn' | 'error'>('info');

  function showToast(msg: string, type: 'info' | 'warn' | 'error' = 'info', duration = 4000) {
    toastMessage = msg;
    toastType = type;
    toastVisible = true;
    setTimeout(() => (toastVisible = false), duration);
  }

  async function endImpersonation(reason?: string) {
    if (!impersonation) return;
    const api = createApiClient(data.token ?? '');
    try {
      await api.impersonation.end(impersonation.sessionId, impersonation.token);
    } catch {
      // best-effort
    }
    clearImpersonation();
    impersonation = null;
    warned = false;
    if (reason) showToast(reason, 'info');
  }

  let intervalId: ReturnType<typeof setInterval> | null = null;

  function startTimer(state: ImpersonationState) {
    if (intervalId) clearInterval(intervalId);
    intervalId = setInterval(() => {
      if (!impersonation) {
        clearInterval(intervalId!);
        return;
      }
      countdown = formatCountdown(state.expiresAt);
      const secs = secondsRemaining(state.expiresAt);

      if (secs <= 0) {
        clearInterval(intervalId!);
        endImpersonation('Impersonation session expired');
        return;
      }

      if (secs <= 300 && !warned) {
        warned = true;
        showToast('Impersonation expires in 5 minutes', 'warn', 10000);
      }
    }, 1000);
    countdown = formatCountdown(state.expiresAt);
  }

  onMount(() => {
    const state = loadImpersonation();
    if (state && !isExpired(state.expiresAt)) {
      impersonation = state;
      startTimer(state);
    } else if (state) {
      clearImpersonation();
    }

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  });

  async function handleLogout() {
    await fetch('/api/login', { method: 'DELETE' });
    goto('/login');
  }

  const navItems = [
    { href: '/', label: 'Dashboard', icon: 'grid' },
    { href: '/clubs', label: 'Clubs', icon: 'building' },
    { href: '/users', label: 'Users', icon: 'users' },
    { href: '/audit-log', label: 'Audit Log', icon: 'scroll' },
    { href: '/billing', label: 'Billing', icon: 'credit-card' }
  ];

  function isActive(href: string): boolean {
    const p = page.url.pathname;
    if (href === '/') return p === '/' || p === '/dashboard';
    return p.startsWith(href);
  }
</script>

{#if isLoginPage}
  {@render children()}
{:else}
  <!-- Impersonation banner -->
  {#if impersonation}
    <div class="fixed top-0 left-0 right-0 z-50 bg-amber-500 text-amber-950 px-4 py-2 flex items-center justify-between text-sm font-medium">
      <span>
        Impersonating <strong>{impersonation.managerEmail}</strong> — session expires in
        <strong class="font-mono">{countdown}</strong>
      </span>
      <button
        onclick={() => endImpersonation('Impersonation ended')}
        class="bg-amber-950 text-amber-100 px-3 py-1 rounded text-xs font-semibold hover:bg-amber-900 transition-colors"
      >
        Exit
      </button>
    </div>
  {/if}

  <div class="flex min-h-screen" class:pt-10={!!impersonation}>
    <!-- Sidebar -->
    <aside class="w-60 shrink-0 bg-zinc-900 border-r border-zinc-800 flex flex-col">
      <div class="px-4 py-5 border-b border-zinc-800">
        <h1 class="text-lg font-bold text-white tracking-tight">Playbook</h1>
        <p class="text-xs text-zinc-500 mt-0.5">Super Admin</p>
      </div>

      <nav class="flex-1 px-2 py-4 space-y-0.5">
        {#each navItems as item}
          <a
            href={item.href}
            class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors
              {isActive(item.href)
                ? 'bg-indigo-600 text-white'
                : 'text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100'}"
          >
            <NavIcon name={item.icon} />
            {item.label}
          </a>
        {/each}
      </nav>

      <div class="px-4 py-4 border-t border-zinc-800">
        <p class="text-xs text-zinc-500 truncate mb-2">{data.userId ?? ''}</p>
        <button
          onclick={handleLogout}
          class="w-full text-left px-3 py-2 rounded-lg text-sm text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100 transition-colors flex items-center gap-2"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
          Logout
        </button>
      </div>
    </aside>

    <!-- Main content -->
    <main class="flex-1 overflow-auto">
      {@render children()}
    </main>
  </div>

  <!-- Toast -->
  {#if toastVisible}
    <div
      class="fixed bottom-4 right-4 z-50 px-4 py-3 rounded-lg shadow-xl text-sm font-medium max-w-sm
        {toastType === 'warn'
          ? 'bg-amber-500 text-amber-950'
          : toastType === 'error'
            ? 'bg-red-600 text-white'
            : 'bg-zinc-800 text-zinc-100 border border-zinc-700'}"
    >
      {toastMessage}
    </div>
  {/if}
{/if}

<!-- Inline icon component -->
{#snippet NavIcon({ name }: { name: string })}
  {#if name === 'grid'}
    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <rect x="3" y="3" width="7" height="7" rx="1" stroke-width="2"/>
      <rect x="14" y="3" width="7" height="7" rx="1" stroke-width="2"/>
      <rect x="3" y="14" width="7" height="7" rx="1" stroke-width="2"/>
      <rect x="14" y="14" width="7" height="7" rx="1" stroke-width="2"/>
    </svg>
  {:else if name === 'building'}
    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
        d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0H5m14 0h2M5 21H3M9 7h1m-1 4h1m4-4h1m-1 4h1M9 21v-4a1 1 0 011-1h4a1 1 0 011 1v4" />
    </svg>
  {:else if name === 'users'}
    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
        d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
    </svg>
  {:else if name === 'scroll'}
    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
    </svg>
  {:else if name === 'credit-card'}
    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <rect x="1" y="4" width="22" height="16" rx="2" ry="2" stroke-width="2"/>
      <path stroke-linecap="round" stroke-width="2" d="M1 10h22"/>
    </svg>
  {/if}
{/snippet}
