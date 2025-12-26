package com.example.splitease.network

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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("login")
    suspend fun login(@Body logInRequests: LogInRequests): LogInResponse

    @POST("register")
    suspend fun register(@Body user : UserDto)

    @POST("group")
    suspend fun createGroup(@Body group : GroupDto): Response<CreateGroupResponse>

    @POST("expense")
    suspend fun addExpense(@Body expense : ExpenseDto): String

    @GET("myGroups")
    suspend fun getMyGroups(): List<UserGroupResponse>

    @GET("settlements/payables")
    suspend fun getPayables(): List<SettlementSummary>

    @POST("settlements")
    suspend fun recordSettlement( @Body request: SettlementDto )

    @GET("activity")
    suspend fun getActivity(): ActivitySummaryDto

    @GET("group")
    suspend fun getGroupScreenData(): GroupScreenDataResponse

    @GET("dashboard")
    suspend fun getDashboardStats(): DashboardStatResponse

}

