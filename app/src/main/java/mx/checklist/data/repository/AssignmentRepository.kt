package mx.checklist.data.repository

import mx.checklist.data.api.Api
import mx.checklist.data.api.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentRepository @Inject constructor(
    private val api: Api
) {
    suspend fun getAssignableUsers(): List<AssignableUserDto> {
        return api.getAssignableUsers()
    }
    
    suspend fun getAssignmentSummary(): List<AssignmentSummaryDto> {
        val response = api.getAssignmentSummary()
        return response.data
    }
    
    suspend fun assignUserToSectors(userId: Long, sectors: List<Int>): AssignmentResponse {
        return api.assignUserToSectors(
            AssignUserToSectorsRequest(
                userId = userId.toString(),
                sectors = sectors
            )
        )
    }
    
    suspend fun getUserAssignedStores(userId: Long): List<AssignedStoreDto> {
        return api.getUserAssignedStores(userId.toString())
    }

    suspend fun getAssignmentSectors(): List<Int> {
        return api.getAssignmentSectors()
    }
}
