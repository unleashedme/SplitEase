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
import com.example.splitease.data.Group
import com.example.splitease.data.Settlement
import com.example.splitease.data.User
import com.example.splitease.ui.navigation.NavigationDestination
import java.time.LocalDate

object GroupDetailsDestination : NavigationDestination {
    override val route = "group_details"
    const val GROUP_ID_ARG = "groupId"

    val routeWithArgs = "$route/{$GROUP_ID_ARG}"

}

enum class GroupDetailFilter {
    EXPENSES, MEMBERS, SETTLEMENTS
}

@Composable
fun GroupDetailsScreen(
    navigateBack:() -> Unit,
    modifier: Modifier = Modifier
){
    val totalSpent = 1000.0
    val noOfMembers = 4
    val totalOwed = 250.0
    val noOfExpenses = 3
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
                    group = Group(
                        name = "Group 1",
                        creatorId = 1,
                        createdAt = 123456
                    ),
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                StatCard(
                    cardHeading = "You paid",
                    cardIcon = R.drawable.rupee_96,
                    cardData = "₹ $totalSpent",
                    cardDescription = "₹ ${totalSpent/noOfMembers} per person",
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                StatCard(
                    cardHeading = "You are owed",
                    cardIcon = R.drawable.increase_96,
                    cardData = "₹ $totalOwed",
                    cardDescription = "from ${noOfMembers-1} members",
                    cardColor = MaterialTheme.colorScheme.inverseOnSurface,
                    cardDataColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                StatCard(
                    cardHeading = "Total No. of Expenses",
                    cardIcon = R.drawable.history_96,
                    cardData = "$noOfExpenses",
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
                            modifier = Modifier
                                .padding(dimensionResource(R.dimen.smallPadding))
                        )
                    }
                    GroupDetailFilter.MEMBERS -> {
                        GroupMembersCard(
                            modifier = Modifier
                                .padding(dimensionResource(R.dimen.smallPadding))
                        )
                    }
                    else -> {
                        PaymentHistoryCard(
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
                        GroupDetailFilter.SETTLEMENTS -> "Settlements"
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
    group: Group,
    navigateBack:() -> Unit,
    modifier: Modifier = Modifier
){
    val noOfMembers = 4
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
                    name = group.name,
                    modifier = Modifier.size(72.dp)
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = group.name,
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
            Spacer(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.mediumPadding)))
            OutlinedButton(
                onClick = {/*TODO*/ },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                colors = ButtonDefaults.elevatedButtonColors(Color.White),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.coin_in_hand_96),
                    contentDescription = "edit Icon",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
                Text(
                    text = "Settle Up",
                    color = Color.Black
                )
            }
            OutlinedButton(
                onClick = {/*TODO*/ },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                colors = ButtonDefaults.elevatedButtonColors(Color.White),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.money_bag_rupee_96),
                    contentDescription = "expense Icon",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
                Text(
                    text = "Add Expense",
                    color = Color.Black
                )
            }
        }
    }
}


@Composable
fun GroupExpensesList(
    modifier: Modifier = Modifier
){

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.largePadding)),
                verticalArrangement = Arrangement
                    .spacedBy(dimensionResource(R.dimen.smallPadding))
            ) {
                GroupExpensesCard(
                    expenseName = "Dinner at Kaveri",
                    group = Group(
                        name = "Group 1",
                        creatorId = 1,
                        createdAt = 1234567890
                    ),
                    date = LocalDate.now(),
                    amount = 100.00
                )
                GroupExpensesCard(
                    expenseName = "Dinner at Kaveri",
                    group = Group(
                        name = "Group 1",
                        creatorId = 1,
                        createdAt = 1234567890
                    ),
                    date = LocalDate.now(),
                    amount = 100.00
                )
                GroupExpensesCard(
                    expenseName = "Dinner at Kaveri",
                    group = Group(
                        name = "Group 1",
                        creatorId = 1,
                        createdAt = 1234567890
                    ),
                    date = LocalDate.now(),
                    amount = 100.00
                )
            }
        }
    }
}

@Composable
fun GroupExpensesCard(
    modifier: Modifier = Modifier,
    expenseName: String,
    group: Group,
    date: LocalDate,
    amount: Double
){
    val memberCount = 10
    val split = amount/memberCount
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
                        text = group.name,
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
                        text = date.toString(),
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
                        text = "₹$split per person",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun GroupMembersCard(
    modifier: Modifier = Modifier
){
    val members = listOf(
        User(
            id = 1,
            name = "Nirmal Bhunwal",
            password = "123456",
            email = "nirmal123@gmail.com",
            upiId = "1234@oksbi",
            phoneNumber = "9155916131"
        ),
        User(
            id = 2,
            name = "Abhinav Kumar",
            password = "123456",
            email = "abhinav123@gmail.com",
            upiId = "1234@oksbi",
            phoneNumber = "9155916131"
        ),
        User(
            id = 3,
            name = "Harsh Raj",
            password = "123456",
            email = "harsh123@gmail.com",
            upiId = "1234@oksbi",
            phoneNumber = "9155916131"
        ),
        User(
            id = 4,
            name = "Affan Hussain",
            password = "123456",
            email = "affan123@gmail.com",
            upiId = "1234@oksbi",
            phoneNumber = "9155916131"
        )
    )
    val balance = listOf(100, 500, -300, -150)
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
                text = "Balances and member information.",
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
                    member = members[i],
                    balance = balance[i].toDouble(),
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun MemberCard(
    member: User,
    balance: Double,
    modifier: Modifier = Modifier
){
    val userId = 2L
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
                name = if(userId!=member.id) member.name else "Me"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Column {
                    Text(
                        text = if(userId != member.id) member.name else "Me",
                        fontSize = 20.sp
                    )
                    Text(
                        text = if(balance>0) {
                            "is Owed"
                        } else if(balance==0.0) {
                            ""
                        }else {
                            "is Owing"
                        }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (balance>0) {
                        Icon(
                            painter = painterResource(R.drawable.increase_96),
                            contentDescription = "increase Icon",
                            modifier = Modifier
                                .padding(4.dp)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "₹ $balance",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 20.sp
                        )
                    }
                    else{
                        Icon(
                            painter = painterResource(R.drawable.decrease_96),
                            contentDescription = "decrease Icon",
                            modifier = Modifier
                                .padding(4.dp)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "₹ ${-balance}",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryCard(
    modifier: Modifier = Modifier
){
    val settlements = listOf(
        Settlement(
            id = 1,
            groupId = 1,
            fromUser = 1,
            toUser = 2,
            amount = 100.0,
            paymentDescription = "Paid via GPay",
            createdAt = LocalDate.now()
        ),
        Settlement(
            id = 2,
            groupId = 1,
            fromUser = 2,
            toUser = 3,
            amount = 500.0,
            paymentDescription = "Paid via PhonePe",
            createdAt = LocalDate.now()
        )
    )
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
                text = "Payment History",
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "All recorded payment and settlements",
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.mediumPadding))
            )
            settlements.forEach {settlement ->
                SettlementCard(
                    settlement = settlement
                )
            }
        }
    }
}

@Composable
fun SettlementCard(
    settlement: Settlement,
    modifier: Modifier = Modifier
){
    val fromUser = "Nirmal"
    val toUser = "Abhinav"
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
                Row {
                    Text(
                        text = fromUser,
                        fontSize = 20.sp
                    )
                    Icon(
                        painter = painterResource(R.drawable.arrow_right_100),
                        contentDescription = "right Arrow",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = toUser,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.calendar_24),
                        contentDescription = "Calendar Icon",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = settlement.createdAt.toString(),
                        modifier = Modifier.padding(start = 4.dp),
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    if(settlement.paymentDescription!=null){
                        Icon(
                            painter = painterResource(R.drawable.dot_96),
                            contentDescription = "group Icon",
                            modifier = Modifier.size(12.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = settlement.paymentDescription,
                            modifier = Modifier.padding(start = 4.dp),
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${settlement.amount}",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}


//@Preview
//@Composable
//fun GroupDetailsCardPreview(){
//    GroupDetailsCard(
//        group = Group(
//            name = "Group 1",
//            creatorId = 1,
//            createdAt = 123456
//        )
//    )
//}

//@Preview
//@Composable
//fun MemberCardPreview(){
//    MemberCard(
//        member = User(
//            id = 3,
//            name = "Harsh Raj",
//            password = "123456",
//            email = "harsh123@gmail.com",
//            upiId = "1234@oksbi",
//            phoneNumber = "9155916131"
//        ),
//        balance = -100.00
//    )
//}

//@Preview
//@Composable
//fun GroupMembersCardPreview(){
//    GroupMembersCard()
//}

//@Preview
//@Composable
//fun PaymentHistoryCardPreview(){
//    PaymentHistoryCard()
//}
//
//@Preview
//@Composable
//fun SettlementCardPreview() {
//    SettlementCard(
//        settlement = Settlement(
//            id = 1,
//            groupId = 1,
//            fromUser = 1,
//            toUser = 2,
//            amount = 100.0,
//            paymentDescription = "Paid via GPay",
//            createdAt = LocalDate.now()
//        )
//    )
//}


//@Preview
//@Composable
//fun GroupExpensesListPreview(){
//    GroupExpensesList()
//}

//@Preview(showBackground = true)
//@Composable
//fun GroupDetailScreenPreview(){
//    GroupDetailsScreen()
//}