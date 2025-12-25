package com.example.splitease.ui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitease.R
import com.example.splitease.ui.SplitEaseBottomBar
import com.example.splitease.ui.SplitEaseTopAppBar
import com.example.splitease.ui.navigation.NavigationDestination
import com.example.splitease.ui.viewmodel.ProfileUiState
import com.example.splitease.ui.viewmodel.ProfileViewModel


object ProfileDestination: NavigationDestination {

    override val route = "profile"

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
){
    val profileUiState by viewModel.profileUiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToLoginScreen.collect {
            navController.navigate(LogInDestination.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

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
                    text = "Profile",
                    fontSize = 28.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                        .fillMaxWidth()
                )
            }
            item {
                Text(
                    text = "Manage your account settings and preferences",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.smallPadding))
                        .fillMaxWidth()
                )
            }
            item{
                PersonalInfoCard(
                    profileUiState = profileUiState,
                    modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                PreferencesCard(
                    modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                StatsCard(
                    modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                DeleteProfileCard(
                    onLogOutClick = { viewModel.logOut() },
                    modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                )
            }
        }
    }

}
@Composable
fun ProfilePicture(
    username: String,
    modifier: Modifier = Modifier
) {
    val initials = username
        .trim()
        .take(2)
        .uppercase()

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.inverseSurface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp
        )
    }
}



@Composable
fun PersonalInfoCard(
    profileUiState: ProfileUiState,
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
                text = "Personal Information",
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Update your personal Details",
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            if(profileUiState.name != null){
                ProfilePicture(
                    username = profileUiState.name,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                )
            }
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            Text(
                text = "Full Name",
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ){
                Icon(
                    painter = painterResource(R.drawable.person_96),
                    contentDescription = "person Icon",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
                profileUiState.name?.let {
                    Text(
                        text = it,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Email",
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ){
                Icon(
                    painter = painterResource(R.drawable.email_96),
                    contentDescription = "email Icon",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
                profileUiState.email?.let {
                    Text(
                        text = it,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Phone",
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ){
                Icon(
                    painter = painterResource(R.drawable.call_96),
                    contentDescription = "call Icon",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
                Text(
                    text = "+91 "+profileUiState.phone,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "UPI Id",
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ){
                Icon(
                    painter = painterResource(R.drawable.payment_96),
                    contentDescription = "payment Icon",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
                profileUiState.upiId?.let {
                    Text(
                        text = it,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
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
                    text = "Edit Your Profile",
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun PreferencesCard(
    modifier: Modifier = Modifier
){
    var emailNotification by remember { mutableStateOf(true) }
    var pushNotification by remember { mutableStateOf(true) }
    var expenseReminder by remember { mutableStateOf(true) }
    var paymentReminder by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        elevation = CardDefaults.elevatedCardElevation()
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding))
        ) {
            Text(
                text = "Preferences",
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Manage your notifications and privacy settings",
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            Row(
                horizontalArrangement = Arrangement
                    .spacedBy(dimensionResource(R.dimen.smallPadding)),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ){
                Icon(
                    painter = painterResource(R.drawable.notification_96),
                    contentDescription = "notification Icon",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                )
                Text(
                    text = "Notification",
                    fontSize = 16.sp
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Email Notifications",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Receive email updates about your expense",
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = emailNotification,
                    onCheckedChange = { emailNotification = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Push Notifications",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Get instant alerts on your device",
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = pushNotification,
                    onCheckedChange = { pushNotification = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Expense Reminders",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Weekly summary of you expenses",
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = expenseReminder,
                    onCheckedChange = { expenseReminder = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Payment Reminders",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reminder for pending payments",
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = paymentReminder,
                    onCheckedChange = { paymentReminder = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            OutlinedButton(
                onClick = {/*TODO*/ },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                colors = ButtonDefaults.elevatedButtonColors(Color.White),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement
                        .spacedBy(dimensionResource(R.dimen.mediumPadding)),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.password_96),
                        contentDescription = "password Icon",
                        tint = Color.Black,
                        modifier = Modifier
                            .size(16.dp)
                    )
                    Text(
                        text = "Change Password",
                        color = Color.Black
                    )
                }
            }
            OutlinedButton(
                onClick = {/*TODO*/ },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                colors = ButtonDefaults.elevatedButtonColors(Color.White),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement
                        .spacedBy(dimensionResource(R.dimen.mediumPadding)),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.painting_96),
                        contentDescription = "appearance Icon",
                        tint = Color.Black,
                        modifier = Modifier
                            .size(16.dp)
                    )
                    Text(
                        text = "Appearance",
                        color = Color.Black
                    )
                }
            }
            OutlinedButton(
                onClick = {/*TODO*/ },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                colors = ButtonDefaults.elevatedButtonColors(Color.White),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement
                        .spacedBy(dimensionResource(R.dimen.mediumPadding)),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.globe_96),
                        contentDescription = "Language Icon",
                        tint = Color.Black,
                        modifier = Modifier
                            .size(16.dp)
                    )
                    Text(
                        text = "Language & Region",
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    modifier: Modifier = Modifier
){
    val totalExpense = 100.00
    val noOfGroups = 5
    val noOfTransaction = 50

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        elevation = CardDefaults.elevatedCardElevation()
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.mediumPadding))
        ) {
            Text(
                text = "Your Stats",
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Activity Overview",
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier
                .padding(dimensionResource(R.dimen.mediumPadding)))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.largePadding))
                        .fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallPadding)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rupee_96),
                            contentDescription = "Rupee Icon",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .size(20.dp)
                        )
                        Text(
                            text = "Total Expenses",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = "₹ $totalExpense",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.largePadding))
                        .fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallPadding)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.people_96),
                            contentDescription = "Rupee Icon",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .size(20.dp)
                        )
                        Text(
                            text = "Groups",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = "$noOfGroups",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.largePadding))
                        .fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallPadding)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.transaction_96),
                            contentDescription = "Rupee Icon",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .size(20.dp)
                        )
                        Text(
                            text = "Transactions",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                    Text(
                        text = "$noOfTransaction",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteProfileCard(
    onLogOutClick: () -> Unit,
    modifier: Modifier = Modifier
){
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(0.5.dp,MaterialTheme.colorScheme.error),
        colors = CardDefaults.cardColors(Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation()
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.mediumPadding)),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallPadding))
        ) {
            Text(
                text = "Danger Zone",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Start,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Bye Bye!!",
                color = Color.Gray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            OutlinedButton(
                onClick = onLogOutClick,
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                colors = ButtonDefaults.elevatedButtonColors(Color.White),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Log Out",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


//@Preview
//@Composable
//fun PersonalInfoPreview(){
//    PersonalInfoCard(
//        user = User(
//            id = 1,
//            name = "Abhinav Kumar",
//            password = "abhi",
//            email = "baranwalabhi.24@gmail.com",
//            upiId = "9155916131@oksbi",
//            phoneNumber = "9155916131",
//            createdAt = 123456
//        )
//    )
//}

//@Preview
//@Composable
//fun PreferencesCardPreview(){
//    PreferencesCard()
//}

//@Preview
//@Composable
//fun StatsCardPreview(){
//    StatsCard()
//}

//@Preview
//@Composable
//fun DeleteProfileCardPreview(){
//    DeleteProfileCard()
//}

//@Preview(showBackground = true)
//@Composable
//fun ProfileScreenPreview(){
//    ProfileScreen()
//}