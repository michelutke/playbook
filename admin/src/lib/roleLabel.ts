/**
 * Maps a member role string to a human-readable label.
 * Roles are defined in UserSearchResult.clubMemberships.role in types.ts.
 */
export function roleLabel(role: string): string {
  switch (role.toLowerCase()) {
    case 'coach':
      return 'Coach'
    case 'player':
      return 'Player'
    case 'admin':
      return 'Admin'
    case 'manager':
      return 'Manager'
    default:
      return role.charAt(0).toUpperCase() + role.slice(1).toLowerCase()
  }
}
