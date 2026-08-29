package com.example.splitease.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitease.R
import com.example.splitease.ui.SplitEaseBottomBar
import com.example.splitease.ui.SplitEaseTopAppBar
import com.example.splitease.ui.model.GroupDetailResponse
import com.example.splitease.ui.model.GroupScreenDataResponse
import com.example.splitease.ui.navigation.NavigationDestination
import com.example.splitease.viewmodel.ActivityViewModel
import com.example.splitease.viewmodel.CreateGroupViewModel
import com.example.splitease.viewmodel.DashboardViewModel
import com.example.splitease.viewmodel.GroupListViewModel
import com.example.splitease.viewmodel.GroupSortOrder
import com.example.splitease.viewmodel.GroupViewModel

object GroupDestination: NavigationDestination {

    override val route = "group"

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    navController: NavHostController,
    groupViewModel: GroupViewModel,
    onGroupViewDetailClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    createGroupViewModel: CreateGroupViewModel = viewModel(factory = CreateGroupViewModel.Factory),
    groupListViewModel: GroupListViewModel = viewModel(factory = GroupListViewModel.Factory),
    dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
    activityViewModel: ActivityViewModel = viewModel(factory = ActivityViewModel.Factory)
){
    var showCreateGroupPopUp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        groupViewModel.getGroupScreenData()
    }

    val groupUiData = groupViewModel.groupUiState

    val selectedPreference by groupViewModel.groupSortOrder.collectAsState()
    val sortedGroupList by groupViewModel.sortedGroupList.collectAsState()
    var preferenceListExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SplitEaseTopAppBar()
        },
        bottomBar = {
            SplitEaseBottomBar(navController = navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ){ innerPadding ->
        LazyColumn(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "Groups",
                    fontSize = 28.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                        .fillMaxWidth()
                )
            }
            item {
                Text(
                    text = "Manage your expense sharing groups and members",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                        .fillMaxWidth()
                )
            }
            item{
                GroupStatsList(
                    groupUiData = groupUiData,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                Row(
                    horizontalArrangement = Arrangement
                        .spacedBy(dimensionResource(R.dimen.smallPadding)),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.smallPadding))
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
                            .fillMaxWidth(0.5f)
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
                                GroupSortOrder.entries.forEach { preference ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = preference.displayName,
                                                fontSize = 16.sp
                                            )
                                        },
                                        onClick = {
                                            groupViewModel.updateSortOrder(preference.displayName)
                                            preferenceListExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    FilledIconButton(
                        onClick = { showCreateGroupPopUp = true },
                        modifier = Modifier
                            .padding(dimensionResource(R.dimen.smallPadding))
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                        colors = IconButtonDefaults.filledIconButtonColors(Color.Black)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add_group_96),
                                contentDescription = "group Icon",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = " Create Group",
                                color = Color.White
                            )
                        }
                    }
                }
            }
            item{
                GroupList(
                    groups = sortedGroupList,
                    onGroupViewDetailClick = onGroupViewDetailClick,
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
            CreateGroupPopUp(createGroupUiState = createGroupViewModel.createGroupUiState,
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
                        groupViewModel.getGroupScreenData()
                        activityViewModel.getActivity()
                        dashboardViewModel.getDashboardStat()
                        groupListViewModel.getGroupList()
                        createGroupViewModel.resetUiState()
                    }
                }
            )
        }
    }
}

@Composable
fun GroupStatsList(
    groupUiData: GroupScreenDataResponse? = null,
    modifier: Modifier = Modifier
){

    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                cardHeading = "Total Groups",
                cardIcon = R.drawable.people_96,
                cardData = "${groupUiData?.totalGroups?:0}",
                cardDescription = "${groupUiData?.activeGroupsCount} active",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                cardHeading = "Total Members",
                cardIcon = R.drawable.person_96,
                cardData = "${groupUiData?.totalMembersAcrossGroups?:0}",
                cardDescription = "Across all groups",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                cardHeading = "Total Expenses",
                cardIcon = R.drawable.rupee_96,
                cardData = "₹ ${groupUiData?.totalExpenses?:0.0}",
                cardDescription = "Combined spending",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                cardHeading = "Active Groups",
                cardIcon = R.drawable.increase_96,
                cardData = "${groupUiData?.activeGroupsCount?:0}",
                cardDescription = "Currently tracking",
                modifier = Modifier.weight(1f),
                cardColor = MaterialTheme.colorScheme.inverseOnSurface,
                cardDataColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun GroupList(
    groups: List<GroupDetailResponse>,
    onGroupViewDetailClick: (String) -> Unit,
    modifier: Modifier = Modifier
){
    groups.forEach { group ->
        GroupCard(
            groupName = group.groupName,
            noOfMembers = group.memberCount,
            isActive = group.isActive,
            totalExpense = group.totalGroupExpense,
            userShare = group.userShare,
            onViewDetailClick = {
                onGroupViewDetailClick(group.groupId)
            },
            modifier = modifier
        )
    }
}

@Composable
fun GroupCard(
    groupName: String,
    noOfMembers: Int,
    isActive: Boolean,
    totalExpense: Double,
    userShare: Double,
    onViewDetailClick:() -> Unit,
    modifier: Modifier = Modifier
){
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray)
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding))
        ){
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement
                        .spacedBy(dimensionResource(R.dimen.smallPadding))
                ){
                    Text(
                        text = groupName,
                        fontSize = 20.sp
                    )
                    Row {
                        Icon(
                            painter = painterResource(R.drawable.people_96),
                            contentDescription = "group Icon",
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "$noOfMembers members",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(Color.Black),
                        modifier = Modifier
                            .width(52.dp)
                            .height(28.dp)
                            .padding(top = dimensionResource(R.dimen.smallPadding))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = if (isActive) "active" else "settled",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                GroupAvatar(name = groupName, modifier = Modifier.padding( 4.dp))
            }

            Spacer(modifier = Modifier.padding(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Total expenses",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "₹ $totalExpense",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your Share",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "₹ $userShare",
                    fontSize = 12.sp,
                    color = Color.Blue,
                )
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.padding(4.dp))
            FilledIconButton(
                onClick = { onViewDetailClick() },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(),
                colors = IconButtonDefaults.iconButtonColors(Color.Black)
            ){
                Text(
                    text = "View Details",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}
