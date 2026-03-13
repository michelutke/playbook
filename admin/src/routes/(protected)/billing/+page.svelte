<script lang="ts">
  import type { PageData } from './$types.js';

  let { data }: { data: PageData } = $props();

  const totalChf = $derived(
    (data.entries ?? []).reduce((s, e) => s + e.annualBillingChf, 0)
  );
  const totalMembers = $derived(
    (data.entries ?? []).reduce((s, e) => s + e.activeMemberCount, 0)
  );

  function formatChf(n: number): string {
    return new Intl.NumberFormat('de-CH', { style: 'currency', currency: 'CHF' }).format(n);
  }
</script>

<svelte:head>
  <title>Billing — Playbook Admin</title>
</svelte:head>

<div class="p-6 max-w-5xl mx-auto">
  <div class="mb-6">
    <h1 class="text-2xl font-bold text-white">Billing</h1>
    <p class="text-zinc-500 text-sm mt-1">Annual billing summary by club</p>
  </div>

  <!-- Summary cards -->
  <div class="grid sm:grid-cols-2 gap-4 mb-6">
    <div class="bg-zinc-900 border border-zinc-800 rounded-xl p-5">
      <p class="text-xs font-medium text-zinc-500 uppercase tracking-wider mb-2">Total Active Members</p>
      <p class="text-3xl font-bold text-white">{totalMembers.toLocaleString()}</p>
    </div>
    <div class="bg-zinc-900 border border-zinc-800 rounded-xl p-5">
      <p class="text-xs font-medium text-zinc-500 uppercase tracking-wider mb-2">Total Annual Revenue</p>
      <p class="text-3xl font-bold text-emerald-400">{formatChf(totalChf)}</p>
    </div>
  </div>

  <div class="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b border-zinc-800 text-zinc-500 text-xs uppercase tracking-wider">
          <th class="text-left px-4 py-3">Club</th>
          <th class="text-right px-4 py-3">Active Members</th>
          <th class="text-right px-4 py-3">Annual (CHF)</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-zinc-800">
        {#each data.entries ?? [] as entry}
          <tr class="hover:bg-zinc-800/30 transition-colors">
            <td class="px-4 py-3">
              <a
                href="/clubs/{entry.clubId}"
                class="font-medium text-zinc-100 hover:text-indigo-400 transition-colors"
              >
                {entry.clubName}
              </a>
            </td>
            <td class="px-4 py-3 text-right text-zinc-400">{entry.activeMemberCount.toLocaleString()}</td>
            <td class="px-4 py-3 text-right text-zinc-200 font-medium">{formatChf(entry.annualBillingChf)}</td>
          </tr>
        {:else}
          <tr>
            <td colspan="3" class="px-4 py-12 text-center text-zinc-600">No billing data available</td>
          </tr>
        {/each}
      </tbody>
      {#if (data.entries?.length ?? 0) > 0}
        <tfoot>
          <tr class="border-t border-zinc-700 bg-zinc-800/50 font-semibold">
            <td class="px-4 py-3 text-zinc-300">Total</td>
            <td class="px-4 py-3 text-right text-zinc-300">{totalMembers.toLocaleString()}</td>
            <td class="px-4 py-3 text-right text-emerald-400">{formatChf(totalChf)}</td>
          </tr>
        </tfoot>
      {/if}
    </table>
  </div>
</div>
