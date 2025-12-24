package com.example.splitease.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitease.R
import com.example.splitease.data.Expense
import com.example.splitease.ui.SplitEaseBottomBar
import com.example.splitease.ui.SplitEaseTopAppBar
import com.example.splitease.ui.navigation.NavigationDestination
import com.example.splitease.ui.viewmodel.AddExpenseViewModel
import com.example.splitease.ui.viewmodel.GroupListViewModel
import java.time.LocalDate

object ActivityDestination: NavigationDestination {

    override val route = "activity"

}

enum class ExpenseFilter {
    ALL, YOU_PAID, YOU_OWE, SETTLED
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    navController: NavHostController,
    navigateToExpenseDetails: (Long) -> Unit,
    modifier: Modifier = Modifier,
    addExpenseViewModel: AddExpenseViewModel = viewModel(factory = AddExpenseViewModel.Factory),
    groupListViewModel: GroupListViewModel = viewModel(factory = GroupListViewModel.Factory),
){
    var query by remember { mutableStateOf("") }
    var showAddExpensePopUp by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(ExpenseFilter.ALL) }


    val groups = listOf("All Groups","Trip", "Roommates", "Office", "Family")
    var selectedGroup by remember { mutableStateOf(groups[0]) }
    var groupListExpanded by remember { mutableStateOf(false) }

    val sortingPreferences = listOf("Newest First", "Oldest First", "Highest Amount", "Lowest Amount")
    var selectedSortingPreference by remember { mutableStateOf(sortingPreferences[0]) }
    var preferenceListExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        groupListViewModel.getGroupList()
    }

    val groupList = groupListViewModel.groupListUiState.groups

    Scaffold(
        topBar = {
            SplitEaseTopAppBar()
        },
        bottomBar = {
            SplitEaseBottomBar(navController = navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ){innerPadding ->
        LazyColumn(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "Activities",
                    fontSize = 28.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                        .fillMaxWidth()
                )
            }
            item {
                Text(
                    text = "View and manage all your expenses and transactions",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                        .fillMaxWidth()
                )
            }
            item{
                ActivityStatsList(
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                OutlinedTextField(
                    value = query,
                    onValueChange = { newText: String -> query = newText },
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    placeholder = {Text(text = "Search expenses...")},
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search_96),
                            contentDescription = "Search Icon",
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    keyboardActions = KeyboardActions(
                        onDone = {/*TODO*/}
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.smallPadding))
                        .height(56.dp)
                )
            }
            item{
                ElevatedButton(
                    onClick = { showAddExpensePopUp = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.smallPadding)),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    colors = ButtonDefaults.elevatedButtonColors(Color.White),
                    border = BorderStroke(width = 0.05.dp, color = Color.Gray)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.money_bag_rupee_96),
                            contentDescription = "add expense Icon",
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
                        painter = painterResource(R.drawable.filter_96),
                        contentDescription = "filter Icon",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                    ){
                        ExposedDropdownMenuBox(
                            expanded = groupListExpanded,
                            onExpandedChange = { groupListExpanded = !groupListExpanded }
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
                                    text = selectedGroup,
                                    color = Color.Black
                                )
                                Icon(
                                    painter = painterResource(R.drawable.expand_arrow_96),
                                    contentDescription = "Expand Icon",
                                    tint = Color.Gray
                                )
                            }
                            ExposedDropdownMenu(
                                expanded = groupListExpanded,
                                onDismissRequest = { groupListExpanded = false },
                                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                                containerColor = Color.White
                            ) {
                                groups.forEach { group ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = group,
                                                fontSize = 16.sp
                                            )
                                        },
                                        onClick = {
                                            selectedGroup = group
                                            groupListExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
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
                                    text = selectedSortingPreference,
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
                                sortingPreferences.forEach { preference ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = preference,
                                                fontSize = 16.sp
                                            )
                                        },
                                        onClick = {
                                            selectedSortingPreference = preference
                                            preferenceListExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
                ExpenseFilterTabs(
                    selected = selectedFilter,
                    onSelected = {selectedFilter = it}
                )
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            }
            item{
                ExpenseHistory(
                    onExpenseClick = navigateToExpenseDetails,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
        }
    }

    if(showAddExpensePopUp){
        Dialog(
            onDismissRequest = { showAddExpensePopUp = false }
        ) {
            AddExpensePopUp(
                addExpenseUiState = addExpenseViewModel.addExpenseUiState,
                onValueChange = addExpenseViewModel::updateUiState,
                groupsList = groupList,
                onAddExpenseClick = {
                    addExpenseViewModel.addExpense(it)
                    showAddExpensePopUp = false
                },
                onClose = {showAddExpensePopUp = false}
            )
        }
    }
}


@Composable
fun ActivityStatsList(
    modifier: Modifier = Modifier
){
    val expenseCount = 10
    val totalExpense = 1000
    val youPaid = 3
    val settled = 5

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
                cardHeading = "Total Expenses",
                cardIcon = R.drawable.clock_96,
                cardData = "$expenseCount",
                cardDescription = "All time",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                cardHeading = "Total Amount",
                cardIcon = R.drawable.rupee_96,
                cardData = "₹ $totalExpense",
                cardDescription = "Combined spending",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                cardHeading = "You paid",
                cardIcon = R.drawable.increase_96,
                cardData = "$youPaid",
                cardDescription = "Combined spending",
                modifier = Modifier.weight(1f),
                cardColor = MaterialTheme.colorScheme.inverseOnSurface,
                cardDataColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            StatCard(
                cardHeading = "Settled",
                cardIcon = R.drawable.done_96,
                cardData = "$settled",
                cardDescription = "Completed",
                modifier = Modifier.weight(1f),
                cardColor = MaterialTheme.colorScheme.secondaryContainer,
                cardDataColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun ExpenseFilterTabs(
    selected: ExpenseFilter,
    onSelected: (ExpenseFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = Color(0xFFF1F1F1),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ExpenseFilter.entries.forEach { filter ->
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
                        ExpenseFilter.ALL -> "All"
                        ExpenseFilter.YOU_PAID -> "You Paid"
                        ExpenseFilter.YOU_OWE -> "You Owe"
                        ExpenseFilter.SETTLED -> "Settled"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ExpenseHistory(
    onExpenseClick:(Long) -> Unit,
    modifier: Modifier = Modifier
){
    val noOfExpenses = 3
    val totalNoOfExpenses = 3
    val expenses = listOf(
        Expense(
            groupId = 1,
            payerId = 1,
            amount = 100.00,
            description = "Dinner at Kaveri",
            createdAt = LocalDate.now(),
        ),
        Expense(
            groupId = 1,
            payerId = 1,
            amount = 200.00,
            description = "Dinner at Kaveri",
            createdAt = LocalDate.now(),
        ),
        Expense(
            groupId = 1,
            payerId = 1,
            amount = 300.00,
            description = "Dinner at Kaveri",
            createdAt = LocalDate.now(),
        )
    )
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        modifier = modifier.width(400.dp)
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding))
        ){
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Expense History",
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            )
            Text(
                text = "showing $noOfExpenses out of $totalNoOfExpenses expenses",
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            ExpenseList(
                expenseList = expenses,
                onExpenseClick = {onExpenseClick(it.expenseId)}
            )
        }
    }
}

@Composable
private fun ExpenseList(
    expenseList: List<Expense>,
    onExpenseClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier
    ) {
        expenseList.forEach {expense ->
            ExpenseCard(
                expense = expense,
                groupName = "group1",
                isSettled = true,
                modifier = Modifier
                    .clickable(onClick = { onExpenseClick(expense) })
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseCard(
    expense: Expense,
    groupName: String,
    isSettled: Boolean,
    modifier: Modifier = Modifier
){
    val memberCount = 10
    val split = expense.amount/memberCount
    val userId = 1L
    val payerName = "Abhinav"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
            .padding(8.dp)
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
                FlowRow {
                    Text(
                        text = expense.description,
                        fontSize = 20.sp
                    )
                    if(userId == expense.payerId){
                        Card(
                            colors = CardDefaults.cardColors(Color.LightGray),
                            modifier = Modifier.padding(start=4.dp)
                        ){
                            Text(
                                text = "You paid",
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                    if(isSettled){
                        Card(
                            colors = CardDefaults.cardColors(Color(
                                red = 80,
                                green = 200,
                                blue = 120
                            )),
                            modifier = Modifier.padding(start=4.dp)
                        ){
                            Text(
                                text = "Settled",
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                }
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
                        text = expense.createdAt.toString(),
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
                        text = "₹ ${expense.amount}",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Your share: ₹ $split",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if(userId!=expense.payerId){
                        Text(
                            text = "You owe $payerName",
                            fontSize = 12.sp,
                            color = Color(
                                red = 200,
                                green = 10,
                                blue = 20
                            )
                        )
                    }
                }
            }
        }
    }
}

//@Preview
//@Composable
//fun ActivityStatsListPreview(){
//    ActivityStatsList()
//}

//@Preview
//@Composable
//fun ExpenseCardPreview() {
//    ExpenseCard(
//        expense = Expense(
//            expenseId = 1,
//            groupId = 1,
//            payerId = 1,
//            amount = 100.00,
//            description = "Dinner at Kaveri",
//            createdAt = LocalDate.now(),
//        ),
//        groupName = "Group 1",
//        isSettled = true
//    )
//}

//@Preview
//@Composable
//fun ExpenseFilterTabsPreview(){
//    ExpenseFilterTabs(
//        selected = ExpenseFilter.ALL,
//        onSelected = {}
//    )
//}

//@Preview(showBackground = true)
//@Composable
//fun ActivityScreenPreview(){
//    ActivityScreen()
//}

