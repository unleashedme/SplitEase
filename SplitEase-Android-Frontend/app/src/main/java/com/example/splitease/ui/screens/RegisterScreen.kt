package com.example.splitease.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.splitease.ui.viewmodel.RegisterViewModel
import com.example.splitease.ui.viewmodel.UserDetails


object RegisterDestination: NavigationDestination {
    override val route = "register"
}

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory)
){

    LaunchedEffect(Unit) {
        viewModel.navigateToLoginScreen.collect {
            navController.navigate(LogInDestination.route) {
                popUpTo(RegisterDestination.route) { inclusive = true }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.mediumPadding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.largePadding)))
            Image(
                painter = painterResource(R.drawable.splitease_logo_without_bottom_tag),
                contentDescription = "SplitEase logo",
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
        }
        item{
            Text(
                text = "Join SplitEase",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
        }
        item{
            Text(
                text = "Create an account to start splitting expenses",
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.padding(20.dp))
        }
        item{
            RegisterCard(
                userDetails = viewModel.registerUiState.userDetails,
                onValueChange = viewModel::updateUiState,
                onSignUpClick = { viewModel.register() },
                onSignInClick = { navController.navigate(LogInDestination.route) },
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.smallPadding))
            )
        }
    }
}


@Composable
fun RegisterCard(
    userDetails: UserDetails,
    onValueChange: (UserDetails) -> Unit,
    onSignUpClick:() -> Unit,
    onSignInClick: () -> Unit,
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
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Create Account",
                fontSize = 24.sp
            )
            Text(
                text = "Get started with your new account",
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.mediumPadding)))
            Text(
                text = "Name",
                fontSize = 16.sp
            )
            OutlinedTextField(
                value = userDetails.name,
                onValueChange = { onValueChange(userDetails.copy(name = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = { Text("Name") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.person_96),
                        contentDescription = "Email Icon",
                        modifier = Modifier
                            .size(20.dp),
                        tint = Color.Gray
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Email",
                fontSize = 16.sp
            )
            OutlinedTextField(
                value = userDetails.email,
                onValueChange = { onValueChange(userDetails.copy(email = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = { Text("Email") },
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
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Mobile Number",
                fontSize = 16.sp
            )
            OutlinedTextField(
                value = userDetails.mobileNumber,
                onValueChange = { onValueChange(userDetails.copy(mobileNumber = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = { Text("Phone No.") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.call_96),
                        contentDescription = "Email Icon",
                        modifier = Modifier
                            .size(20.dp),
                        tint = Color.Gray
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Upi Id",
                fontSize = 16.sp
            )
            OutlinedTextField(
                value = userDetails.upiId,
                onValueChange = { onValueChange(userDetails.copy(upiId = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = { Text("Upi Id") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.account_96),
                        contentDescription = "Upi Icon",
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
            Text(
                text = "Password",
                fontSize = 16.sp
            )
            OutlinedTextField(
                value = userDetails.password,
                onValueChange = { onValueChange(userDetails.copy(password = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = { Text("Password") },
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
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            Text(
                text = "Confirm Password",
                fontSize = 16.sp
            )
            OutlinedTextField(
                value = userDetails.confirmPassword,
                onValueChange = { onValueChange(userDetails.copy(confirmPassword = it)) },
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                placeholder = { Text("Confirm Password") },
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
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(dimensionResource(R.dimen.smallPadding)))
            FilledIconButton(
                onClick = onSignUpClick,
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
                colors = IconButtonDefaults.iconButtonColors(Color.Black)
            ){
                Text(
                    text = "Sign Up",
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
                Text(text = "Already have an account?", fontSize = 16.sp, color = Color.Gray)
                TextButton(
                    onClick = onSignInClick
                ) {
                    Text(text = "Sign In", fontSize = 16.sp, color = Color.Black)
                }
            }
        }
    }
}


//@Preview
//@Composable
//fun RegisterFormPreview(){
//    RegisterForm()
//}

//@Preview(showBackground = true)
//@Composable
//fun RegisterScreenPreview(){
//    RegisterScreen()
//}