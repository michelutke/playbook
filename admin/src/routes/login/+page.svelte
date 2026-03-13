<script lang="ts">
  import { goto } from '$app/navigation';

  let email = $state('');
  let password = $state('');
  let error = $state('');
  let loading = $state(false);

  async function handleSubmit(e: SubmitEvent) {
    e.preventDefault();
    error = '';
    loading = true;

    try {
      const res = await fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      const data = await res.json();

      if (!res.ok) {
        error = data.error ?? 'Login failed';
        return;
      }

      await goto('/');
    } catch {
      error = 'Network error — check your connection';
    } finally {
      loading = false;
    }
  }
</script>

<svelte:head>
  <title>Login — Playbook Admin</title>
</svelte:head>

<div class="min-h-screen bg-zinc-950 flex items-center justify-center p-4">
  <div class="w-full max-w-sm">
    <div class="mb-8 text-center">
      <h1 class="text-2xl font-bold text-white">Playbook</h1>
      <p class="text-zinc-500 text-sm mt-1">Super Admin Panel</p>
    </div>

    <div class="bg-zinc-900 border border-zinc-800 rounded-xl p-6 shadow-2xl">
      <form onsubmit={handleSubmit} class="space-y-4">
        <div>
          <label for="email" class="block text-sm font-medium text-zinc-300 mb-1.5">
            Email address
          </label>
          <input
            id="email"
            type="email"
            bind:value={email}
            required
            autocomplete="email"
            placeholder="admin@playbook.com"
            class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2.5 text-sm text-zinc-100 placeholder-zinc-600
              focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors"
          />
        </div>

        <div>
          <label for="password" class="block text-sm font-medium text-zinc-300 mb-1.5">
            Password
          </label>
          <input
            id="password"
            type="password"
            bind:value={password}
            required
            autocomplete="current-password"
            placeholder="••••••••"
            class="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2.5 text-sm text-zinc-100 placeholder-zinc-600
              focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors"
          />
        </div>

        {#if error}
          <div class="bg-red-500/10 border border-red-500/30 rounded-lg px-3 py-2.5 text-sm text-red-400">
            {error}
          </div>
        {/if}

        <button
          type="submit"
          disabled={loading}
          class="w-full bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed
            text-white font-semibold py-2.5 px-4 rounded-lg transition-colors text-sm"
        >
          {#if loading}
            <span class="inline-flex items-center gap-2">
              <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
              </svg>
              Signing in…
            </span>
          {:else}
            Sign in
          {/if}
        </button>
      </form>
    </div>

    <p class="text-center text-xs text-zinc-600 mt-6">
      Super admin access only. Unauthorized access is prohibited.
    </p>
  </div>
</div>
