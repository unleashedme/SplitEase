package com.example.splitease.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
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
import com.example.splitease.ui.model.ActivityExpenseDto
import com.example.splitease.ui.navigation.NavigationDestination
import com.example.splitease.ui.viewmodel.ActivityViewModel

object ExpenseDetailsDestination : NavigationDestination {
    override val route = "expense_details"
    const val EXPENSE_ID_ARG = "expenseId"

    val routeWithArgs = "$route/{$EXPENSE_ID_ARG}"

}

@Composable
fun ExpenseDetailsScreen(
    expenseId: String,
    activityViewModel: ActivityViewModel,
    navigateBack:() -> Unit,
    modifier: Modifier = Modifier
){

    val expenseList = activityViewModel.activityUiState?.expenseHistory?:(emptyList())
    val expense = remember(expenseId, expenseList){
        expenseList.find { it.expenseId == expenseId }
    }


    Scaffold(
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
                if(expense!=null){
                    ExpenseDetailCard(
                        navigateBack = navigateBack,
                        expense = expense,
                        modifier = Modifier
                            .padding(dimensionResource(R.dimen.smallPadding))
                    )
                }
            }
            item {
                if(expense!=null){
                    SplitDetailCard(
                        expense = expense,
                        modifier = Modifier
                            .padding(dimensionResource(R.dimen.smallPadding))
                    )
                }
            }
        }
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseDetailCard(
    navigateBack: () -> Unit,
    expense: ActivityExpenseDto,
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
                        modifier = Modifier.size(20.dp)
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
                Image(
                    painter = painterResource(R.drawable.splitease_logo_without_bottom_tag),
                    contentDescription = "Logo",
                    modifier = Modifier.size(72.dp)
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = expense.description,
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
                            text = expense.groupName,
                            modifier = Modifier.padding(start = 4.dp),
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Icon(
                            painter = painterResource(R.drawable.calendar_24),
                            contentDescription = "Calendar Icon",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = formatIsoDate(expense.date),
                            modifier = Modifier.padding(start = 4.dp),
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.mediumPadding)))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceDim),
            ){
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.largePadding))
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Amount",
                            color = Color.DarkGray
                        )
                        Text(
                            text = "₹ ${expense.amount}",
                            fontSize = 24.sp,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 60.dp))
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Paid By",
                            color = Color.DarkGray
                        )
                        Text(
                            text = expense.owesToName ?: "You",
                            fontSize = 24.sp,
                            color = Color.Black
                        )
                    }
                }
            }
            if(expense.userWasPayer){
                OutlinedButton(
                    onClick = {/*TODO*/ },
                    shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                    colors = ButtonDefaults.elevatedButtonColors(Color.White),
                    border = BorderStroke(1.dp, Color.LightGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.edit_96),
                        contentDescription = "edit Icon",
                        tint = Color.Black,
                        modifier = Modifier
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
                    Text(
                        text = "Edit Expense",
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
                    Text(
                        text = "Delete Expense",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SplitDetailCard(
    expense: ActivityExpenseDto,
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
                .spacedBy(dimensionResource(R.dimen.smallPadding))
        ) {
            Text(
                text = "Split details",
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "How this expense is divided among members.",
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            SplitCard(
                name = "You",
                expense = expense,
                modifier = Modifier.fillMaxWidth()
                    .padding(dimensionResource(R.dimen.smallPadding))
            )
            if(expense.owesToName!=null){
                SplitCard(
                    name = expense.owesToName,
                    expense = expense,
                    modifier = Modifier.fillMaxWidth()
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            else{
                SplitCard(
                    name = "Others",
                    expense = expense,
                    modifier = Modifier.fillMaxWidth()
                        .padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(Color.White),
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                border = BorderStroke(width = 0.2.dp, color = Color.Gray)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.largePadding))
                        .padding(dimensionResource(R.dimen.smallPadding)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Column {
                        Text(
                            text = "Total",
                            fontSize = 20.sp
                        )
                    }
                    Column {
                        Text(
                            text = "₹${expense.amount}",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "~ ₹${expense.userShare} each",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplitCard(
    name: String,
    expense: ActivityExpenseDto,
    modifier: Modifier = Modifier
) {
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
                name = name
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Column {
                    Text(
                        text = name,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Share: ₹${expense.userShare}"
                    )
                }
                Column {
                    if(name == "Others" || (!expense.userWasPayer && name == "You")){
                        Text(
                            text = "Owes ₹${expense.userShare}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else {
                        Text(
                            text = "Paid: ₹${expense.amount}",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Gets back: ₹ ${expense.amount - expense.userShare}",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserAvatar(
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


//@Preview
//@Composable
//fun ExpenseDetailCardPreview(){
//    ExpenseDetailCard(
//        expense = Expense(
//            expenseId = 1,
//            groupId = 1,
//            payerId = 2,
//            amount = 100.00,
//            description = "Dinner at Kaveri",
//            createdAt = LocalDate.now(),
//        ),
//        groupName = "Group 1"
//    )
//}


//@Preview
//@Composable
//fun SplitDetailCardPreview(){
//    SplitDetailCard(
//        expense = Expense(
//            expenseId = 1,
//            groupId = 1,
//            payerId = 2,
//            amount = 100.00,
//            description = "Dinner at Kaveri",
//            createdAt = LocalDate.now(),
//        )
//    )
//}


//@Preview
//@Composable
//fun SplitCardPreview(){
//    SplitCard(
//        member = User(
//            id = 3,
//            name = "Harsh Raj",
//            password = "123456",
//            email = "harsh123@gmail.com",
//            upiId = "1234@oksbi",
//            phoneNumber = "9155916131"
//        ),
//        expense = Expense(
//            expenseId = 1,
//            groupId = 1,
//            payerId = 2,
//            amount = 100.00,
//            description = "Dinner at Kaveri",
//            createdAt = LocalDate.now(),
//        )
//    )
//}

//@Preview(showBackground = true)
//@Composable
//fun ExpenseDetailScreenPreview(){
//    ExpenseDetailsScreen()
//}