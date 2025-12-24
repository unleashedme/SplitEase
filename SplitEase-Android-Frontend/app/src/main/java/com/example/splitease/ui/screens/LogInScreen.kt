package com.example.splitease.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitease.R
import com.example.splitease.ui.navigation.NavigationDestination
import com.example.splitease.ui.viewmodel.LogInDetails
import com.example.splitease.ui.viewmodel.LogInViewModel


object LogInDestination: NavigationDestination {
    override val route = "login"
}


@Composable
fun LogInScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: LogInViewModel = viewModel(factory = LogInViewModel.Factory)
){

    LaunchedEffect(Unit) {
        viewModel.navigateToDashboard.collect {
            navController.navigate(DashboardDestination.route) {
                popUpTo(LogInDestination.route) { inclusive = true }
            }
        }
    }


    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.mediumPadding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Spacer(modifier = Modifier.padding(top = 60.dp))
            Image(
                painter = painterResource(R.drawable.splitease_logo_without_bottom_tag),
                contentDescription = "SplitEase logo",
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
        }
        item {
            Text(
                text = "Welcome Again to SplitEase",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
        }
        item {
            Text(
                text = "Please sign in to continue",
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.padding(20.dp))
        }
        item {
            LogInCard(
                logInDetails = viewModel.logInUiState.logInDetails,
                onValueChange = viewModel::updateUiState,
                onSignUpClick = { navController.navigate(RegisterDestination.route) },
                onLogInClick = { viewModel.logIn() },
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.smallPadding))
            )
        }
    }
}

@Composable
fun LogInCard(
    logInDetails: LogInDetails,
    onValueChange: (LogInDetails) -> Unit,
    onLogInClick:() -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier
){
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement
                .spacedBy(dimensionResource(R.dimen.smallPadding)),
        ) {
            Text(
                text = "Sign In",
                fontSize = 24.sp
            )
            Text(
                text = "Enter your credentials to access your account",
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            Text(
                text = "Email",
                fontSize = 16.sp
            )
            OutlinedTextField(
                value = logInDetails.email,
                onValueChange = { onValueChange(logInDetails.copy(email = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder =  { Text("Email") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.email_96),
                        contentDescription = "Email Icon",
                        modifier = Modifier
                            .size(20.dp),
                        tint = Color.Gray
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ){
                Text(
                    text = "Password",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(horizontal = 4.dp),
                )
                TextButton(
                    onClick = { /*TODO*/ },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(48.dp),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(0.dp),
                    content = {
                        Text(
                            text = "Forgot Password?",
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.Bottom),
                            color = Color.Black
                        )
                    }
                )
            }
            OutlinedTextField(
                value = logInDetails.password,
                onValueChange = { onValueChange(logInDetails.copy(password = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = {
                    Text(
                        text = "Password",
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.password_96),
                        contentDescription = "Email Icon",
                        modifier = Modifier
                            .size(20.dp),
                        tint = Color.Gray
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            FilledIconButton(
                onClick = onLogInClick,
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                colors = IconButtonDefaults.iconButtonColors(Color.Black)
            ){
                Text(
                    text = "Sign In",
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(text = "Don't have an account? ", fontSize = 16.sp, color = Color.Gray)
                TextButton(
                    onClick = onSignUpClick,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(text = "Sign Up" ,fontSize = 16.sp, color = Color.Black)
                }
            }
        }
    }
}


//@Preview
//@Composable
//fun InputFormPreview(){
//    InputForm()
//}

//@Preview(showBackground = true)
//@Composable
//fun LogInScreenPreview(){
//    LogInScreen()
//}

