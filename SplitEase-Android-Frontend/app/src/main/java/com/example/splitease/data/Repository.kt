package com.example.splitease.data

import com.example.splitease.network.ApiService
import com.example.splitease.dto.ExpenseDto
import com.example.splitease.dto.GroupDto
import com.example.splitease.dto.SettlementDto
import com.example.splitease.dto.UserDto
import com.example.splitease.ui.model.ActivitySummaryDto
import com.example.splitease.ui.model.CreateGroupResponse
import com.example.splitease.ui.model.DashboardStatResponse
import com.example.splitease.ui.model.GroupScreenDataResponse
import com.example.splitease.ui.model.LogInRequests
import com.example.splitease.ui.model.LogInResponse
import com.example.splitease.ui.model.SettlementSummary
import com.example.splitease.ui.model.UserGroupResponse
import retrofit2.Response


interface Repository{
    suspend fun login(email: String, password: String): LogInResponse

    suspend fun register(userDetails: UserDto)

    suspend fun createGroup(groupName: String, members: List<String>): Response<CreateGroupResponse>

    suspend fun addExpense(groupId: String, amount: Double, description: String) : String

    suspend fun getMyGroups(): List<UserGroupResponse>

    suspend fun getMyPayables(): List<SettlementSummary>

    suspend fun recordSettlement(settlementDto: SettlementDto)

    suspend fun getActivity(): ActivitySummaryDto

    suspend fun getGroupScreenData(): GroupScreenDataResponse

    suspend fun getDashboardStat(): DashboardStatResponse

}
class NetworkRepository(
    private val apiService: ApiService
): Repository {

    override suspend fun login(email: String, password: String): LogInResponse = apiService.login(LogInRequests(email, password))
    override suspend fun register(userDetails: UserDto) = apiService.register(userDetails)
    override suspend fun createGroup(groupName: String, members: List<String>) = apiService.createGroup(GroupDto(groupName, members))
    override suspend fun addExpense(groupId: String, amount: Double, description: String ) = apiService.addExpense(ExpenseDto(groupId, amount, description))
    override suspend fun getMyGroups(): List<UserGroupResponse>  = apiService.getMyGroups()
    override suspend fun getMyPayables(): List<SettlementSummary> = apiService.getPayables()
    override suspend fun recordSettlement(settlementDto: SettlementDto) = apiService.recordSettlement(settlementDto)
    override suspend fun getActivity(): ActivitySummaryDto = apiService.getActivity()
    override suspend fun getGroupScreenData(): GroupScreenDataResponse = apiService.getGroupScreenData()
    override suspend fun getDashboardStat(): DashboardStatResponse = apiService.getDashboardStats()
}
