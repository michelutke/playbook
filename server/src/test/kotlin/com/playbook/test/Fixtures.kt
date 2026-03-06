package com.playbook.test

import org.jetbrains.exposed.sql.Transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.OffsetDateTime
import java.time.ZoneOffset

object Fixtures {
    const val TEST_COACH_ID = "11111111-1111-1111-1111-111111111111"
    const val TEST_PLAYER_ID = "22222222-2222-2222-2222-222222222222"
    const val TEST_MANAGER_ID = "33333333-3333-3333-3333-333333333333"
    const val TEST_SA_ID = "44444444-4444-4444-4444-444444444444"
    const val TEST_CLUB_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    const val TEST_TEAM_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"

    const val COACH_EMAIL = "coach@playbook.test"
    const val PLAYER_EMAIL = "player@playbook.test"
    const val MANAGER_EMAIL = "manager@playbook.test"
    const val TEST_PASSWORD = "testPassword123"

    /**
     * Seeds a coach user, a player user, a club, a team, and memberships.
     * Coach has role "coach" on TEST_TEAM_ID.
     * Manager user is a club_manager for TEST_CLUB_ID.
     */
    fun seedCoachAndTeam(tx: Transaction) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val hash = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt())

        // Coach user
        tx.exec(
            "INSERT INTO users (id, email, display_name, password_hash, super_admin, created_at, updated_at) " +
                "VALUES ('$TEST_COACH_ID', '$COACH_EMAIL', 'Test Coach', '$hash', false, '$now', '$now') " +
                "ON CONFLICT (id) DO NOTHING"
        )

        // Player user
        tx.exec(
            "INSERT INTO users (id, email, display_name, password_hash, super_admin, created_at, updated_at) " +
                "VALUES ('$TEST_PLAYER_ID', '$PLAYER_EMAIL', 'Test Player', '$hash', false, '$now', '$now') " +
                "ON CONFLICT (id) DO NOTHING"
        )

        // Manager user
        tx.exec(
            "INSERT INTO users (id, email, display_name, password_hash, super_admin, created_at, updated_at) " +
                "VALUES ('$TEST_MANAGER_ID', '$MANAGER_EMAIL', 'Test Manager', '$hash', false, '$now', '$now') " +
                "ON CONFLICT (id) DO NOTHING"
        )

        // SA user
        tx.exec(
            "INSERT INTO users (id, email, display_name, password_hash, super_admin, created_at, updated_at) " +
                "VALUES ('$TEST_SA_ID', 'sa@playbook.test', 'Super Admin', '$hash', true, '$now', '$now') " +
                "ON CONFLICT (id) DO NOTHING"
        )

        // Club
        tx.exec(
            "INSERT INTO clubs (id, name, sport_type, status, created_at, updated_at) " +
                "VALUES ('$TEST_CLUB_ID', 'Test Club', 'football', 'active', '$now', '$now') " +
                "ON CONFLICT (id) DO NOTHING"
        )

        // Club manager link
        tx.exec(
            "INSERT INTO club_managers (id, club_id, user_id, invited_email, status, added_at) " +
                "VALUES (gen_random_uuid(), '$TEST_CLUB_ID', '$TEST_MANAGER_ID', '$MANAGER_EMAIL', 'active', '$now') " +
                "ON CONFLICT DO NOTHING"
        )

        // Team (active)
        tx.exec(
            "INSERT INTO teams (id, club_id, name, status, created_at, updated_at) " +
                "VALUES ('$TEST_TEAM_ID', '$TEST_CLUB_ID', 'Test Team', 'active', '$now', '$now') " +
                "ON CONFLICT (id) DO NOTHING"
        )

        // Coach membership
        tx.exec(
            "INSERT INTO team_memberships (id, team_id, user_id, role, joined_at) " +
                "VALUES (gen_random_uuid(), '$TEST_TEAM_ID', '$TEST_COACH_ID', 'coach', '$now') " +
                "ON CONFLICT DO NOTHING"
        )

        // Player membership
        tx.exec(
            "INSERT INTO team_memberships (id, team_id, user_id, role, joined_at) " +
                "VALUES (gen_random_uuid(), '$TEST_TEAM_ID', '$TEST_PLAYER_ID', 'player', '$now') " +
                "ON CONFLICT DO NOTHING"
        )
    }
}
