package com.kycis.demo.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kycis.demo.R
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.sdk.ui.KycEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigilockerAadhaarScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    onTryAnotherWay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var aadhaar1 by remember { mutableStateOf("") }
    var aadhaar2 by remember { mutableStateOf("") }
    var aadhaar3 by remember { mutableStateOf("") }

    val isButtonEnabled = aadhaar1.length == 4 && aadhaar2.length == 4 && aadhaar3.length == 4

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB)) // Light grayish background
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "KYC and Verification",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                Spacer(modifier = Modifier.width(48.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // DigiLocker Logo Simulation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Mock Flag Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(2.dp)
                ) {
                   Text("🇮🇳", fontSize = 24.sp)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // DigiLocker Text Logo
                Column {
                    Text(
                        text = "DigiLocker",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    Text(
                        text = "Document Wallet to Empower Citizens",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 8.sp,
                            color = Color.Gray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document verification illustration
            Image(
                painter = painterResource(id = R.drawable.img_documents),
                contentDescription = "Documents illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Sign up",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    
                    Text(
                        text = "It takes just a minute",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enter your Aadhaar Number",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF374151)
                        )
                        
                        // Aadhaar Logo Placeholder
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                           Text("🆔", fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Aadhaar Input Fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AadhaarPartField(
                            value = aadhaar1,
                            onValueChange = { 
                                if (it.length <= 4) aadhaar1 = it 
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AadhaarPartField(
                            value = aadhaar2,
                            onValueChange = { 
                                if (it.length <= 4) aadhaar2 = it 
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AadhaarPartField(
                            value = aadhaar3,
                            onValueChange = { 
                                if (it.length <= 4) aadhaar3 = it 
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "DigiLocker uses Aadhaar to enable authentic document access",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            // Send unmasked value so the agent can see what the user actually typed
                            KycEvent.componentInput(
                                componentId = "aadhaar_digilocker_field",
                                hint = aadhaar1 + aadhaar2 + aadhaar3,
                                screen = "digilocker_aadhaar",
                                componentType = "text_input"
                            )
                            onNext() 
                        },
                        enabled = isButtonEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22C55E), // Green button as in screenshot
                            disabledContainerColor = Color(0xFF22C55E).copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = "Next",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Try another way
            Text(
                text = "Try another way",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6366F1) // Purple/Blue link
                ),
                modifier = Modifier.clickable { onTryAnotherWay() }
            )
        }
    }
}

@Composable
fun AadhaarPartField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        placeholder = { Text("----", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFE5E7EB),
            unfocusedBorderColor = Color(0xFFE5E7EB)
        ),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
    )
}

@Preview(showBackground = true)
@Composable
fun DigilockerAadhaarScreenPreview() {
    KycDemoTheme {
        DigilockerAadhaarScreen(onBack = {}, onNext = {}, onTryAnotherWay = {})
    }
}
