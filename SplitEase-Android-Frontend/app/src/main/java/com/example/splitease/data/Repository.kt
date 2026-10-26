package com.example.splitease.data

import com.example.splitease.network.ApiService
import com.example.splitease.dto.ExpenseDto
import com.example.splitease.dto.GroupDto
import com.example.splitease.dto.SettlementDto
import com.example.splitease.dto.SplitEntryDto
import com.example.splitease.dto.UserDto
import com.example.splitease.ui.model.ActivitySummaryDto
import com.example.splitease.ui.model.AddExpenseResponse
import com.example.splitease.ui.model.CreateGroupResponse
import com.example.splitease.ui.model.DashboardStatResponse
import com.example.splitease.ui.model.GroupScreenDataResponse
import com.example.splitease.ui.model.LogInRequests
import com.example.splitease.ui.model.LogInResponse
import com.example.splitease.ui.model.SettlementSummary
import com.example.splitease.ui.model.UserGroupResponse
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response


interface Repository{
    suspend fun login(email: String, password: String): LogInResponse

    suspend fun register(userDetails: UserDto)

    suspend fun createGroup(groupName: String, members: List<String>): Response<CreateGroupResponse>

    suspend fun addExpense(groupId: String, amount: Double, description: String, splitType: String, splits: List<SplitEntryDto>) : Response<AddExpenseResponse>

    suspend fun uploadReceipt(expenseId: String, jpeg: ByteArray): Response<Unit>

    suspend fun deleteReceipt(expenseId: String): Response<Unit>

    // Address of the bill photo, loaded with Coil rather than through Retrofit
    fun receiptUrl(expenseId: String, thumbnail: Boolean): String

    suspend fun getMyGroups(): List<UserGroupResponse>

    suspend fun getMyPayables(): List<SettlementSummary>

    suspend fun recordSettlement(settlementDto: SettlementDto)

    suspend fun getActivity(): ActivitySummaryDto

    suspend fun getGroupScreenData(): GroupScreenDataResponse

    suspend fun getDashboardStat(): DashboardStatResponse

    suspend fun deleteGroup(groupId: String): Response<Unit>

    suspend fun deleteExpense(expenseId: String): Response<Unit>

    suspend fun updateFcmToken(token: String): Response<Unit>

}
class NetworkRepository(
    private val apiService: ApiService,
    private val baseUrl: HttpUrl
): Repository {

    override suspend fun login(email: String, password: String): LogInResponse = apiService.login(LogInRequests(email, password))
    override suspend fun register(userDetails: UserDto) = apiService.register(userDetails)
    override suspend fun createGroup(groupName: String, members: List<String>) = apiService.createGroup(GroupDto(groupName, members))
    override suspend fun addExpense(groupId: String, amount: Double, description: String, splitType: String, splits: List<SplitEntryDto>) = apiService.addExpense(ExpenseDto(groupId, amount, description, splitType, splits))
    override suspend fun getMyGroups(): List<UserGroupResponse>  = apiService.getMyGroups()
    override suspend fun getMyPayables(): List<SettlementSummary> = apiService.getPayables()
    override suspend fun recordSettlement(settlementDto: SettlementDto) = apiService.recordSettlement(settlementDto)
    override suspend fun getActivity(): ActivitySummaryDto = apiService.getActivity()
    override suspend fun getGroupScreenData(): GroupScreenDataResponse = apiService.getGroupScreenData()
    override suspend fun getDashboardStat(): DashboardStatResponse = apiService.getDashboardStats()
    override suspend fun deleteGroup(groupId: String) = apiService.deleteGroup(groupId)
    override suspend fun deleteExpense(expenseId: String) = apiService.deleteExpense(expenseId)
    override suspend fun updateFcmToken(token: String): Response<Unit> = apiService.updateFcmToken(token)
    override suspend fun uploadReceipt(expenseId: String, jpeg: ByteArray): Response<Unit> =
        apiService.uploadReceipt(
            expenseId,
            MultipartBody.Part.createFormData("file", "bill.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
        )
    override suspend fun deleteReceipt(expenseId: String): Response<Unit> = apiService.deleteReceipt(expenseId)
    override fun receiptUrl(expenseId: String, thumbnail: Boolean): String =
        baseUrl.newBuilder()
            .addPathSegments("expense/$expenseId/receipt")
            .addQueryParameter("size", if (thumbnail) "thumb" else "full")
            .build()
            .toString()
}
