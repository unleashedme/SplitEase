package com.example.splitease.ui.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.HorizontalDivider
import com.example.splitease.ui.rupees
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitease.R
import com.example.splitease.ui.model.ExpenseDTO
import com.example.splitease.ui.navigation.NavigationDestination
import com.example.splitease.viewmodel.GroupViewModel

object GroupDetailsDestination : NavigationDestination {
    override val route = "group_details"
    const val GROUP_ID_ARG = "groupId"

    val routeWithArgs = "$route/{$GROUP_ID_ARG}"

}

enum class GroupDetailFilter {
    EXPENSES, MEMBERS
}

@Composable
fun GroupDetailsScreen(
    groupId: String,
    groupViewModel: GroupViewModel,
    navigateBack:() -> Unit,
    modifier: Modifier = Modifier
){

    val groupList = groupViewModel.groupUiState?.groups?:emptyList()
    val groupDetail = remember(groupId, groupList){
        groupList.find { it.groupId == groupId }
    }

    var selectedDetail by remember { mutableStateOf(GroupDetailFilter.EXPENSES) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ){innerPadding->
        LazyColumn(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                GroupDetailsCard(
                    navigateBack = navigateBack,
                    groupName = groupDetail?.groupName?:"",
                    noOfMembers = groupDetail?.memberCount?:0,
                    onDeleteGroupClicked = {
                        groupViewModel.deleteGroup(groupId){
                            navigateBack()

                        }
                    },
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                StatCard(
                    cardHeading = "Total Expense",
                    cardIcon = R.drawable.rupee_96,
                    cardData = rupees(groupDetail?.totalGroupExpense ?: 0.0),
                    cardDescription = "Your share ${rupees(groupDetail?.userShare ?: 0.0)}",
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                StatCard(
                    cardHeading = "Total No. of Expenses",
                    cardIcon = R.drawable.history_96,
                    cardData = "${groupDetail?.expenseCount}",
                    cardDescription = "Transactions",
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
                GroupDetailFilterTabs(
                    selected = selectedDetail,
                    onSelected = { selectedDetail = it },
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            }
            item{
                when (selectedDetail) {
                    GroupDetailFilter.EXPENSES -> {
                        GroupExpensesList(
                            expenseList = groupDetail?.expenses?:emptyList(),
                            receiptUrl = groupViewModel::receiptUrl,
                            modifier = Modifier
                                .padding(dimensionResource(R.dimen.smallPadding))
                        )
                    }
                    GroupDetailFilter.MEMBERS -> {
                        GroupMembersList(
                            members = groupDetail?.memberNames?:emptyList(),
                            modifier = Modifier
                                .padding(dimensionResource(R.dimen.smallPadding))
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun GroupDetailFilterTabs(
    selected: GroupDetailFilter,
    onSelected: (GroupDetailFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color(0xFFF1F1F1),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GroupDetailFilter.entries.forEach { filter ->
            val isSelected = filter == selected

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) Color.White else Color.Transparent
                    )
                    .clickable { onSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = when (filter) {
                        GroupDetailFilter.EXPENSES -> "Expenses"
                        GroupDetailFilter.MEMBERS -> "Members"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupDetailsCard(
    onDeleteGroupClicked:() -> Unit,
    groupName: String,
    noOfMembers: Int,
    navigateBack:() -> Unit,
    modifier: Modifier = Modifier
){
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ){
                TextButton(
                    onClick = navigateBack,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.left_96),
                        contentDescription = "back Icon",
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = dimensionResource(R.dimen.smallPadding)),
                        tint = Color.Black
                    )
                    Text(
                        text = "Back",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }
            FlowRow (
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement
                    .spacedBy(dimensionResource(R.dimen.smallPadding))
            ) {
                GroupAvatar(
                    name = groupName,
                    modifier = Modifier.size(72.dp)
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = groupName,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.people_96),
                            contentDescription = "group Icon",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = "$noOfMembers members",
                            modifier = Modifier.padding(start = 4.dp),
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { showDeleteDialog = true } ,
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                colors = ButtonDefaults.elevatedButtonColors(Color.White),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Delete Group",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "Delete Group?",
            content = "This will permanently remove this group from everyone's groupList. This cannot be undone.",
            onConfirm = {
                showDeleteDialog = false
                onDeleteGroupClicked()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}


@Composable
fun GroupExpensesList(
    expenseList: List<ExpenseDTO>,
    receiptUrl: (expenseId: String, thumbnail: Boolean) -> String,
    modifier: Modifier = Modifier
){
    var billToShow by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        modifier = modifier.fillMaxWidth()
    ){
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.largePadding)),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "All Expenses",
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                modifier = Modifier.padding(4.dp)
            )
            Text(
                text = "Complete expense list of this group.",
                modifier = Modifier.padding(4.dp),
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            if (expenseList.isEmpty()) {
                Text(
                    text = "No expenses yet.",
                    color = Color.Gray,
                    modifier = Modifier.padding(4.dp)
                )
            }
            // Rows separated by dividers rather than a card per expense, so the list stays light
            expenseList.forEachIndexed { index, expense ->
                if (index > 0) {
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE0E0E0))
                }
                GroupExpensesCard(
                    expenseName = expense.description,
                    date = expense.date,
                    amount = expense.amount,
                    splitLabel = expense.splitPerPerson?.let { "${rupees(it)} each" }
                        ?: "Your share ${rupees(expense.userShare)}",
                    payerName = if(!expense.userPaid) expense.payerName else "You",
                    onViewBill = expense.expenseId
                        ?.takeIf { expense.hasReceipt }
                        ?.let { id -> { billToShow = id } }
                )
            }
        }
    }

    billToShow?.let { id ->
        FullScreenBillDialog(
            url = receiptUrl(id, false),
            cacheVersion = 0,
            onDismiss = { billToShow = null }
        )
    }
}

@Composable
fun GroupExpensesCard(
    modifier: Modifier = Modifier,
    expenseName: String,
    date: String,
    amount: Double,
    splitLabel: String,
    payerName: String,
    onViewBill: (() -> Unit)? = null // null when the expense has no bill photo
){
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserAvatar(
            name = expenseName,
            modifier = Modifier.size(40.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = expenseName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Paid by $payerName · ${formatIsoDate(date)}",
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onViewBill != null) {
                Text(
                    text = "View bill",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onViewBill)
                        .padding(vertical = 2.dp)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = rupees(amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = splitLabel,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun GroupMembersList(
    members: List<String>,
    modifier: Modifier = Modifier
){
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.mediumPadding))
        ) {
            Text(
                text = "Group Members",
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Member information.",
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.mediumPadding))
            )
            for(i in members.indices){
                MemberCard(
                    memberName = members[i],
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun MemberCard(
    memberName: String,
    modifier: Modifier = Modifier
){
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.White),
        shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.mediumPadding))
        ) {
            UserAvatar(
                name = memberName
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(
                    text = memberName,
                    fontSize = 20.sp
                )
            }
        }
    }
}