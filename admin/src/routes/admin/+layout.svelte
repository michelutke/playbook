<script lang="ts">
	import { page } from '$app/stores';
	import { LayoutDashboard, Building2, Users, ScrollText, LogOut } from 'lucide-svelte';
	import type { Snippet } from 'svelte';
	import type { LayoutData } from './$types';

	interface Props {
		data: LayoutData;
		children: Snippet;
	}

	let { data, children }: Props = $props();

	const isLoginPage = $derived($page.url.pathname === '/admin/login');

	const navItems = [
		{ href: '/admin/dashboard', label: 'Dashboard', icon: LayoutDashboard },
		{ href: '/admin/clubs', label: 'Clubs', icon: Building2 },
		{ href: '/admin/users', label: 'Users', icon: Users },
		{ href: '/admin/audit-log', label: 'Audit Log', icon: ScrollText }
	];

	function isActive(href: string): boolean {
		return $page.url.pathname === href || $page.url.pathname.startsWith(href + '/');
	}
</script>

{#if isLoginPage}
	{@render children()}
{:else}
	<div class="flex min-h-screen" style="background-color: #090912;">
		<!-- Sidebar -->
		<aside
			class="fixed top-0 left-0 h-full flex flex-col"
			style="width: 240px; background-color: #13131F; border-right: 1px solid #2A2A40;"
		>
			<!-- Logo -->
			<div class="p-6" style="border-bottom: 1px solid #2A2A40;">
				<span class="font-semibold" style="font-size: 20px; color: #F0F0FF;">TeamOrg</span>
				<span class="block font-semibold" style="font-size: 12px; color: #9090B0; margin-top: 2px;">
					Admin Panel
				</span>
			</div>

			<!-- Nav items -->
			<nav class="flex-1 py-4">
				{#each navItems as item}
					{@const active = isActive(item.href)}
					<a
						href={item.href}
						class="flex items-center gap-3 no-underline"
						style="
							padding: 10px 16px;
							font-size: 14px;
							font-weight: {active ? '600' : '400'};
							color: {active ? '#4F8EF7' : '#F0F0FF'};
							background-color: {active ? 'rgba(79,142,247,0.08)' : 'transparent'};
							border-left: 3px solid {active ? '#4F8EF7' : 'transparent'};
							text-decoration: none;
							display: flex;
							align-items: center;
							gap: 12px;
							transition: background-color 0.15s;
						"
						onmouseenter={(e) => { if (!active) (e.currentTarget as HTMLElement).style.backgroundColor = '#1C1C2E'; }}
						onmouseleave={(e) => { if (!active) (e.currentTarget as HTMLElement).style.backgroundColor = 'transparent'; }}
					>
						<item.icon size={18} />
						{item.label}
					</a>
				{/each}
			</nav>

			<!-- Logout -->
			<div class="p-4" style="border-top: 1px solid #2A2A40;">
				{#if data.user}
					<div class="mb-3" style="font-size: 12px; color: #9090B0; padding: 0 4px;">
						{data.user.displayName}
					</div>
				{/if}
				<form method="POST" action="/admin/logout">
					<button
						type="submit"
						class="flex items-center gap-3 w-full"
						style="
							background: transparent;
							border: 1px solid #2A2A40;
							color: #9090B0;
							font-size: 14px;
							padding: 8px 12px;
							border-radius: 6px;
							cursor: pointer;
							width: 100%;
							text-align: left;
						"
					>
						<LogOut size={16} />
						Sign Out
					</button>
				</form>
			</div>
		</aside>

		<!-- Main content -->
		<main class="flex-1" style="margin-left: 240px; padding: 32px; min-height: 100vh; overflow-y: auto;">
			{@render children()}
		</main>
	</div>
{/if}
