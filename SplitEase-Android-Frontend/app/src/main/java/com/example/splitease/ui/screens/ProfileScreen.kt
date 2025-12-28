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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch


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
                    profileViewModel = viewModel,
                    modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding))
                )
            }
            item{
                LogOutCard(
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
            .background(Color.DarkGray),
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
        }
    }
}

@Composable
fun PreferencesCard(
    profileViewModel: ProfileViewModel,
    modifier: Modifier = Modifier
){
    val profileUiState by profileViewModel.profileUiState.collectAsState()
    val scope = rememberCoroutineScope()

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
                text = "Preference",
                fontSize = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Manage your notification settings",
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
                        text = "Push Notifications",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Get instant alerts on your device",
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = profileUiState.isNotificationsEnabled,
                    onCheckedChange = { isChecked ->
                        scope.launch {
                            profileViewModel.updateNotificationPreference(isChecked)
                        }
                    },
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
        }
    }
}

@Composable
fun LogOutCard(
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
                text = "Sayonara!!",
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
