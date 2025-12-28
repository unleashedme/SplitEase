package com.example.splitease.ui.screens


import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitease.FCMHandler
import com.example.splitease.R
import com.example.splitease.ui.SplitEaseBottomBar
import com.example.splitease.ui.SplitEaseTopAppBar
import com.example.splitease.ui.model.ActivityExpenseDto
import com.example.splitease.ui.model.DashboardStatResponse
import com.example.splitease.ui.model.GroupDetailResponse
import com.example.splitease.ui.model.SettlementSummary
import com.example.splitease.ui.model.UserGroupResponse
import com.example.splitease.ui.navigation.NavigationDestination
import com.example.splitease.ui.viewmodel.ActivityViewModel
import com.example.splitease.ui.viewmodel.AddExpenseUiState
import com.example.splitease.ui.viewmodel.AddExpenseViewModel
import com.example.splitease.ui.viewmodel.CreateGroupUiState
import com.example.splitease.ui.viewmodel.CreateGroupViewModel
import com.example.splitease.ui.viewmodel.DashboardViewModel
import com.example.splitease.ui.viewmodel.GroupListViewModel
import com.example.splitease.ui.viewmodel.GroupViewModel
import com.example.splitease.ui.viewmodel.SettlementViewModel
import com.example.splitease.ui.viewmodel.SortPreference
import android.Manifest
import com.example.splitease.data.Repository


object DashboardDestination: NavigationDestination {

    override val route = "dashboard"

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    createGroupViewModel: CreateGroupViewModel = viewModel(factory = CreateGroupViewModel.Factory),
    addExpenseViewModel: AddExpenseViewModel = viewModel(factory = AddExpenseViewModel.Factory),
    groupListViewModel: GroupListViewModel = viewModel(factory = GroupListViewModel.Factory),
    settlementViewModel: SettlementViewModel = viewModel(factory = SettlementViewModel.Factory),
    activityViewModel: ActivityViewModel = viewModel(factory = ActivityViewModel.Factory),
    groupViewModel: GroupViewModel = viewModel(factory = GroupViewModel.Factory),
    dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
){
    var showCreateGroupPopUp by remember { mutableStateOf(false) }
    var showAddExpensePopUp by remember { mutableStateOf(false) }
    var showSettleUpPopUp by remember { mutableStateOf(false) }

    val groups = groupListViewModel.groupListUiState.groups

    val groupDetailList = groupViewModel.groupUiState?.groups?:emptyList()
    val activeGroups: List<GroupDetailResponse> = groupDetailList.filter { it.isActive }

    Scaffold(
        topBar = {
            SplitEaseTopAppBar()
        },
        bottomBar = {
            SplitEaseBottomBar(navController = navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ){ innerPadding->
        LazyColumn(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding))
        ) {
            item {
                Text(
                    text = "Welcome to Splitease",
                    fontSize = 28.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                        .fillMaxWidth(),

                    )
            }
            item {
                Text(
                    text = "Track and split expenses effortlessly with your friends and family",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                        .fillMaxWidth()
                )
            }
            item {
                DashboardStatsList(
                    dashboardStats = dashboardViewModel.dashboardUiState,
                    activeGroupsCount = groupViewModel.groupUiState?.activeGroupsCount?:0,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                ElevatedButton(
                    onClick = { showCreateGroupPopUp = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.smallPadding)),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    colors = ButtonDefaults.elevatedButtonColors(Color.Black)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add_group_96),
                            contentDescription = "group Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = " Create Group",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                        )
                    }
                }
            }
            item{
                ElevatedButton(
                    onClick = { showAddExpensePopUp = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.smallPadding)),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    colors = ButtonDefaults.elevatedButtonColors(Color.White),
                    border = BorderStroke(0.05.dp, Color.Gray)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add_96),
                        contentDescription = "add Expense Icon",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = " Add Expense",
                        color = Color.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                    )
                }
            }
            item{
                ElevatedButton(
                    onClick = { showSettleUpPopUp = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.smallPadding)),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    colors = ButtonDefaults.elevatedButtonColors(Color.White),
                    border = BorderStroke(0.05.dp, Color.Gray)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.coin_in_hand_96),
                        contentDescription = "payment Icon",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = " Settle Up",
                        color = Color.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                    )
                }
            }
            item{
                RecentExpenses(
                    activityViewModel = activityViewModel,
                    viewAllExpenses = {navController.navigate(ActivityDestination.route)},
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                ActiveGroups(
                    activeGroupList = activeGroups,
                    viewAllGroups = {navController.navigate(GroupDestination.route)},
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
        }
    }

    if(showCreateGroupPopUp){
        Dialog(
            onDismissRequest = { showCreateGroupPopUp = false }
        ){
            CreateGroupPopUp(
                createGroupUiState = createGroupViewModel.createGroupUiState,
                onNameChange = createGroupViewModel::updateUiState,
                onEmailChange = createGroupViewModel::updateMemberEmail,
                onClose = {
                    showCreateGroupPopUp = false
                    createGroupViewModel.resetUiState()
                },
                onAddMemberClick = {createGroupViewModel.addMember()},
                onCreateGroupClick = {
                    createGroupViewModel.createGroup {
                        showCreateGroupPopUp = false
                        groupListViewModel.getGroupList()
                        groupViewModel.getGroupScreenData()
                        dashboardViewModel.getDashboardStat()
                        createGroupViewModel.resetUiState()
                    }
                }
            )
        }
    }
    if(showAddExpensePopUp){
        Dialog(
            onDismissRequest = { showAddExpensePopUp = false }
        ) {
            AddExpensePopUp(
                addExpenseUiState = addExpenseViewModel.addExpenseUiState,
                onValueChange = addExpenseViewModel::updateUiState,
                groupsList = groups,
                settlementViewModel = settlementViewModel,
                activityViewModel = activityViewModel,
                dashboardViewModel = dashboardViewModel,
                groupViewModel = groupViewModel,
                addExpenseViewModel = addExpenseViewModel,
                onClose = {
                    showAddExpensePopUp = false
                    addExpenseViewModel.resetUiState()
                }
            )
        }
    }
    if(showSettleUpPopUp){
        Dialog(
            onDismissRequest = { showSettleUpPopUp = false }
        ) {
            SettleUpPopUp(
                settlementViewModel = settlementViewModel,
                activityViewModel = activityViewModel,
                dashboardViewModel = dashboardViewModel,
                groupViewModel = groupViewModel,
                onClose = { showSettleUpPopUp = false}
            )
        }
    }
}

@Composable
fun DashboardStatsList(
    dashboardStats: DashboardStatResponse?,
    activeGroupsCount: Long,
    modifier: Modifier = Modifier
){

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement
            .spacedBy(dimensionResource(R.dimen.smallPadding)),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding)),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                cardHeading = "Total Expenses",
                cardIcon = R.drawable.rupee_96,
                cardData = "₹ ${dashboardStats?.totalUserExpense?:0.0}",
                cardDescription = "Across All Groups",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                cardHeading = "Active Groups",
                cardIcon = R.drawable.people_96,
                cardData = "$activeGroupsCount",
                cardDescription = "Sharing Expenses",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding)),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                cardHeading = "You Owe",
                cardIcon = R.drawable.decrease_96,
                cardData = "₹ ${dashboardStats?.amountUserOwes?:0.0}",
                cardDescription = "To Settle",
                cardColor = MaterialTheme.colorScheme.errorContainer,
                cardDataColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                cardHeading = "You Are Owed",
                cardIcon = R.drawable.increase_96,
                cardData = "₹ ${dashboardStats?.amountOwedToUser?:0.0}",
                cardDescription = "Across All Groups",
                cardColor = MaterialTheme.colorScheme.secondaryContainer,
                cardDataColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
fun StatCard(
    cardHeading: String,
    cardDescription: String,
    @DrawableRes cardIcon: Int,
    cardData: String,
    modifier: Modifier = Modifier,
    cardColor: Color = Color.White,
    cardDataColor: Color = Color.Black
){
    Card(shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.elevatedCardElevation(),
        modifier = modifier
            .padding(dimensionResource(R.dimen.smallPadding))
            .height(132.dp),
        border = BorderStroke(0.2.dp, cardDataColor)
    ){
        Column(
            modifier = Modifier
                .padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ){
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = cardHeading,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(4.dp),
                    color = Color.Black
                )
                Icon(
                    painter = painterResource(cardIcon),
                    contentDescription = "Card Icon",
                    modifier = Modifier
                        .padding(4.dp)
                        .size(24.dp),
                    tint = cardDataColor
                )
            }
            Spacer(modifier = modifier.padding(4.dp))
            Text(
                text = cardData,
                fontSize = 20.sp,
                modifier = Modifier.padding(4.dp),
                color = cardDataColor
            )
            Text(
                text = cardDescription,
                fontSize = 12.sp,
                modifier = Modifier.padding(4.dp),
                color = Color.DarkGray
            )

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentExpenses(
    activityViewModel: ActivityViewModel,
    viewAllExpenses: () -> Unit,
    modifier: Modifier = Modifier
){
    var preferenceListExpanded by remember { mutableStateOf(false) }
    val selectedPreference by activityViewModel.sortPreference.collectAsState()
    val sortedExpenseList by activityViewModel.sortedExpenses.collectAsState()


    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        modifier = modifier.fillMaxWidth()
    ){
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.largePadding)),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recent Expenses",
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                modifier = Modifier.padding(4.dp)
            )
            Text(
                text = "your latest expenses across all groups",
                modifier = Modifier.padding(4.dp),
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Row(
                horizontalArrangement = Arrangement
                    .spacedBy(dimensionResource(R.dimen.smallPadding)),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.sorting_arrows_96),
                    contentDescription = "sorting Icon",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Card(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ){
                    ExposedDropdownMenuBox(
                        expanded = preferenceListExpanded,
                        onExpandedChange = { preferenceListExpanded = !preferenceListExpanded }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensionResource(R.dimen.mediumPadding))
                                .size(20.dp)
                                .menuAnchor(
                                    type = MenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                )
                        ) {
                            Text(
                                text = selectedPreference.displayName,
                                color = Color.Black
                            )
                            Icon(
                                painter = painterResource(R.drawable.expand_arrow_96),
                                contentDescription = "Expand Icon",
                                tint = Color.Gray
                            )
                        }
                        ExposedDropdownMenu(
                            expanded = preferenceListExpanded,
                            onDismissRequest = { preferenceListExpanded = false },
                            shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                            containerColor = Color.White
                        ) {
                            SortPreference.entries.forEach { preference ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = preference.displayName,
                                            fontSize = 16.sp
                                        )
                                    },
                                    onClick = {
                                        activityViewModel.updateSortPreference(preference.displayName)
                                        preferenceListExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))

            RecentExpensesList(
                expenseList = sortedExpenseList
            )

            FilledIconButton(
                onClick = viewAllExpenses,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                colors = IconButtonDefaults.iconButtonColors(Color.White)
            ){
                Row {
                    Text(
                        text = "View All Expenses",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(
                        painter = painterResource(R.drawable.arrow_right_50),
                        contentDescription = "Arrow Icon",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentExpensesList(
    expenseList: List<ActivityExpenseDto>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        expenseList.take(4).forEach {expense ->
            RecentExpensesCard(
                expenseName = expense.description,
                amount =  expense.amount,
                date = expense.date,
                groupName = expense.groupName,
                userSplit = expense.userShare
            )
        }
    }
}

@Composable
fun RecentExpensesCard(
    modifier: Modifier = Modifier,
    expenseName: String,
    groupName: String,
    date: String,
    amount: Double,
    userSplit: Double
){
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(0.2.dp, Color.Gray)
    ){
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = painterResource(R.drawable.splitease_logo_without_bottom_tag),
                contentDescription = "Logo",
                modifier = Modifier.size(60.dp)
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = expenseName,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Icon(
                        painter = painterResource(R.drawable.people_96),
                        contentDescription = "group Icon",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = groupName,
                        modifier = Modifier.padding(start = 4.dp),
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Icon(
                        painter = painterResource(R.drawable.calendar_24),
                        contentDescription = "Calendar Icon",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = formatIsoDate(date),
                        modifier = Modifier.padding(start = 4.dp),
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹$amount",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "₹$userSplit per person",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
@Composable
fun ActiveGroups(
    activeGroupList: List<GroupDetailResponse>,
    viewAllGroups:() -> Unit,
    modifier: Modifier = Modifier
){
    Card(shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        modifier = modifier.width(400.dp)
    ){
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Active Groups",
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
                text = "your expense sharing groups",
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                fontSize = 16.sp,
                color = Color.Gray
            )
            ActiveGroupList(
                groupList = activeGroupList
            )
            FilledIconButton(
                onClick = viewAllGroups,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = IconButtonDefaults.iconButtonColors(Color.White)
            ){
                Row {
                    Text(
                        text = "View All Groups",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(
                        painter = painterResource(R.drawable.arrow_right_50),
                        contentDescription = "Arrow Icon",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveGroupList(
    groupList: List<GroupDetailResponse>,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier
    ) {
        groupList.take(4).forEach { group ->
            ActiveGroupsCard(
                groupName = group.groupName,
                noOfMembers = group.memberCount,
                totalExpense = group.totalGroupExpense,
                userShare = group.userShare,
                modifier = Modifier
            )
        }
    }
}

@Composable
fun ActiveGroupsCard(
    groupName: String,
    noOfMembers: Int,
    totalExpense: Double,
    userShare: Double,
    modifier: Modifier = Modifier
){
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .padding(dimensionResource(R.dimen.smallPadding))
            .fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(R.dimen.largeCornerRoundedness)),
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(0.2.dp, Color.Gray)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ){
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                GroupAvatar(name = groupName, modifier = Modifier.padding(end = 4.dp))
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                ) {
                    Text(
                        text = groupName,
                        textAlign = TextAlign.Start,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "$noOfMembers members",
                        color = Color.Gray,
                        textAlign = TextAlign.Start,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ){
                Card(
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    colors = CardDefaults.cardColors(Color.Gray),
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp)
                ){
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(start = 4.dp)
                    ){
                        Text(
                            text = "Total expenses",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                        )
                        Text(
                            text = "₹$totalExpense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
                Card(
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    colors = CardDefaults.cardColors(Color.DarkGray),
                    border = BorderStroke(0.2.dp, Color.Blue),
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp)
                ){
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(start = 4.dp)
                    ) {
                        Text(
                            text = "Your share",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                        )
                        Text(
                            text = "₹$userShare",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                        )

                    }
                }
            }
        }
    }
}

@Composable
fun CreateGroupPopUp(
    createGroupUiState: CreateGroupUiState,
    onNameChange: (CreateGroupUiState) -> Unit,
    onEmailChange:(Int, String) -> Unit,
    onClose:() -> Unit,
    onAddMemberClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    modifier: Modifier = Modifier
){

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding)),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .align(Alignment.End)
                    .size(20.dp),
                shape = RectangleShape,
                contentPadding = PaddingValues(0.dp),
                content = {
                    Icon(
                        painter = painterResource(R.drawable.cross_96),
                        contentDescription = "close Icon",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
            Row(
                horizontalArrangement = Arrangement
                    .spacedBy(dimensionResource(R.dimen.smallPadding))
            ) {
                Icon(
                    painter = painterResource(R.drawable.people_96),
                    contentDescription = "group Icon",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Create New Group",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = "Create a group to split expenses with friends and family.",
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            Text(
                text = "Group Name",
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = createGroupUiState.name,
                onValueChange = { onNameChange(createGroupUiState.copy(name = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = { Text("e.g. Le Pebble") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            createGroupUiState.members.forEachIndexed { index, email ->
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
                Text(
                    text = "Add Member",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { onEmailChange(index, it) },
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    placeholder = { Text("Member Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
            TextButton(
                onClick = onAddMemberClick,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .align(Alignment.End)
                    .height(20.dp),
                shape = RectangleShape,
                contentPadding = PaddingValues(0.dp),
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add_96),
                            contentDescription = "add Icon",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = " Add Another Member",
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.Top),
                            color = Color.Black
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            FilledIconButton(
                onClick = onCreateGroupClick,
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(),
                colors = IconButtonDefaults.iconButtonColors(Color.Black)
            ){
                Text(
                    text = "Create Group",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            ElevatedButton(
                onClick = onClose,
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(),
                border = BorderStroke(1.dp, Color.Gray),
                elevation = ButtonDefaults.elevatedButtonElevation(),
                colors = ButtonDefaults.elevatedButtonColors(Color.White)
            ){
                Text(
                    text = "Cancel",
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpensePopUp(
    groupsList: List<UserGroupResponse>,
    addExpenseUiState: AddExpenseUiState,
    onValueChange: (AddExpenseUiState) -> Unit,
    settlementViewModel: SettlementViewModel,
    activityViewModel: ActivityViewModel,
    dashboardViewModel: DashboardViewModel,
    groupViewModel: GroupViewModel,
    addExpenseViewModel: AddExpenseViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
){
    var expanded by remember { mutableStateOf(false) }
    var selectedGroup by remember {  mutableStateOf<UserGroupResponse?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding)),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .align(Alignment.End)
                    .size(20.dp),
                shape = RectangleShape,
                contentPadding = PaddingValues(0.dp),
                content = {
                    Icon(
                        painter = painterResource(R.drawable.cross_96),
                        contentDescription = "add Icon",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
            Row(
                horizontalArrangement = Arrangement
                    .spacedBy(dimensionResource(R.dimen.smallPadding))
            ) {
                Icon(
                    painter = painterResource(R.drawable.money_bag_rupee_96),
                    contentDescription = "group Icon",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Add New Expense",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = "Add an expense to track and split with your group.",
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            Text(
                text = "Description",
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = addExpenseUiState.description,
                onValueChange = { onValueChange(addExpenseUiState.copy(description = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = { Text("e.g. Dinner At Kaveri") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Amount",
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = addExpenseUiState.amount,
                onValueChange = { onValueChange(addExpenseUiState.copy(amount = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Group",
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedGroup?.groupName?: "",
                    onValueChange = {},
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    placeholder = { Text("Select a group") },
                    trailingIcon = {
                        if(expanded){
                            Icon(
                                painter = painterResource(R.drawable.collapse_arrow_96),
                                contentDescription = "expand Icon",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        else{
                            Icon(
                                painter = painterResource(R.drawable.expand_arrow_96),
                                contentDescription = "expand Icon",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(
                            type = MenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        )
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    containerColor = Color.White
                ) {
                    groupsList.forEach { group ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = group.groupName,
                                    fontSize = 16.sp
                                )
                            },
                            onClick = {
                                selectedGroup = group
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            FilledIconButton(
                onClick = {
                    addExpenseViewModel.addExpense(selectedGroup?.groupId?:""){
                        onClose()
                        groupViewModel.getGroupScreenData()
                        dashboardViewModel.getDashboardStat()
                        settlementViewModel.loadSettlements()
                        activityViewModel.getActivity()
                        addExpenseViewModel.resetUiState()
                    }
                },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(),
                colors = IconButtonDefaults.iconButtonColors(Color.Black)
            ){
                Text(
                    text = "Add Expense",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            ElevatedButton(
                onClick = onClose,
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(),
                border = BorderStroke(1.dp, Color.Gray),
                elevation = ButtonDefaults.elevatedButtonElevation(),
                colors = ButtonDefaults.elevatedButtonColors(Color.White)
            ){
                Text(
                    text = "Cancel",
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpPopUp(
    onClose: () -> Unit,
    settlementViewModel: SettlementViewModel,
    activityViewModel: ActivityViewModel,
    dashboardViewModel: DashboardViewModel,
    groupViewModel: GroupViewModel,
    modifier: Modifier = Modifier
){
    var note by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedDebt by remember { mutableStateOf<SettlementSummary?>(null) }

    val settlements = settlementViewModel.settlements


    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding)),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .align(Alignment.End)
                    .size(20.dp),
                shape = RectangleShape,
                contentPadding = PaddingValues(0.dp),
                content = {
                    Icon(
                        painter = painterResource(R.drawable.cross_96),
                        contentDescription = "add Icon",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
            Row(
                horizontalArrangement = Arrangement
                    .spacedBy(dimensionResource(R.dimen.smallPadding))
            ) {
                Icon(
                    painter = painterResource(R.drawable.coin_in_hand_96),
                    contentDescription = "payment Icon",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Record Payment",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = "Record a payment to settle your balance.",
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            Text(
                text = "Select debt to settle",
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                BasicTextField(
                    value = "",
                    onValueChange = {},
                    readOnly = true,
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ){
                            if(selectedDebt == null){
                                Text(
                                    text = "Choose who you are paying",
                                    color = Color.DarkGray
                                )
                            }
                            else{
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallPadding)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${selectedDebt?.toUserName} - ₹${selectedDebt?.amount}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (expanded) {
                                Icon(
                                    painter = painterResource(R.drawable.collapse_arrow_96),
                                    contentDescription = "expand Icon",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.expand_arrow_96),
                                    contentDescription = "expand Icon",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        innerTextField()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .menuAnchor(
                            type = MenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    containerColor = Color.White
                ) {
                    settlements.forEach { debt ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${debt.toUserName} - ₹${debt.amount}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                selectedDebt = debt
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Payment Amount",
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = if(selectedDebt==null) "0.00" else selectedDebt?.amount.toString(),
                onValueChange = { },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                singleLine = true,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Note (optional)",
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = { Text("e.g. Paid via GPay") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            FilledIconButton(
                onClick = {
                    settlementViewModel.recordPayment(
                        toUserId = selectedDebt?.toUserId?:"",
                        amount = selectedDebt?.amount?:0.0,
                        note = note
                    ) {
                        onClose()
                        groupViewModel.getGroupScreenData()
                        dashboardViewModel.getDashboardStat()
                        settlementViewModel.loadSettlements()
                        activityViewModel.getActivity()
                    }
                },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(),
                colors = IconButtonDefaults.iconButtonColors(Color.Black)
            ){
                Text(
                    text = "Record Payment",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            ElevatedButton(
                onClick = onClose,
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(),
                border = BorderStroke(1.dp, Color.Gray),
                elevation = ButtonDefaults.elevatedButtonElevation(),
                colors = ButtonDefaults.elevatedButtonColors(Color.White)
            ){
                Text(
                    text = "Cancel",
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun GroupAvatar(
    name: String,
    modifier: Modifier = Modifier
) {
    val initials = name
        .trim()
        .take(2)
        .uppercase()

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.Black,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp
        )
    }
}

@Composable
fun NotificationSetup(repository: Repository) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) FCMHandler.syncTokenWithServer(repository)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (status != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                FCMHandler.syncTokenWithServer(repository)
            }
        } else {
            FCMHandler.syncTokenWithServer(repository)
        }
    }
}