package ch.teamorg.data.repository

import ch.teamorg.domain.InviteDetails
import ch.teamorg.repository.InviteRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode

class InviteRepositoryImpl(private val client: HttpClient) : InviteRepository {
    override suspend fun getInviteDetails(token: String): Result<InviteDetails> {
        return try {
            val response = client.get("/invites/$token")
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to fetch invite details: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun redeemInvite(token: String): Result<Unit> {
        return try {
            val response = client.post("/invites/$token/redeem")
            if (response.status == HttpStatusCode.OK) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to redeem invite: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
