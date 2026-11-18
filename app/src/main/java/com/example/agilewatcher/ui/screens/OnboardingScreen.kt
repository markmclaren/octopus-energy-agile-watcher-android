package com.example.agilewatcher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agilewatcher.data.UK_GSP_REGIONS
import com.example.agilewatcher.data.findGspRegion
import com.example.agilewatcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    initialApiKey: String,
    initialAccountNumber: String,
    initialRegionCode: String,
    isDiscovering: Boolean,
    onConnectApiKey: (apiKey: String, accountNumber: String) -> Unit,
    onSkipWithPublicRegion: (regionCode: String) -> Unit
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var accountNumber by remember { mutableStateOf(initialAccountNumber) }
    var showKeyText by remember { mutableStateOf(false) }
    var showAdvancedAccountField by remember { mutableStateOf(initialAccountNumber.isNotBlank()) }
    var selectedRegion by remember { mutableStateOf(findGspRegion(initialRegionCode)) }
    var regionDropdownExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Main Title matching 01-onboarding.png
            Text(
                text = "Welcome to Agile Watcher",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            // Primary Onboarding Card matching 01-onboarding.png
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyCardDark),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Enter your Octopus API key",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Copy your key from Developer settings in your Octopus account. We'll use it to find your home and meter.",
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = TextSecondaryDark
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = {
                            Text("sk_live_...", color = TextMutedDark)
                        },
                        singleLine = true,
                        visualTransformation = if (showKeyText) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showKeyText = !showKeyText }) {
                                Icon(
                                    imageVector = if (showKeyText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API key visibility",
                                    tint = TextSecondaryDark
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NavyInputDark,
                            unfocusedContainerColor = NavyInputDark,
                            focusedBorderColor = AgilePinkOnboardingBtn,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = AgilePinkOnboardingBtn
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Optional Account Number toggle
                    Row(
                        modifier = Modifier
                            .clickable { showAdvancedAccountField = !showAdvancedAccountField }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showAdvancedAccountField) "Hide account number (optional)" else "Have your Account Number (A-XXXX)? Optional",
                            fontSize = 13.sp,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showAdvancedAccountField) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = showAdvancedAccountField) {
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            placeholder = { Text("A-AAAA1111 (optional — auto-detected if blank)", color = TextMutedDark) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NavyInputDark,
                                unfocusedContainerColor = NavyInputDark,
                                focusedBorderColor = AgilePinkOnboardingBtn,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Pink "Find my details ->" Button
                    Button(
                        onClick = { onConnectApiKey(apiKey, accountNumber) },
                        enabled = !isDiscovering,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AgilePinkOnboardingBtn,
                            contentColor = NavyBackgroundDark
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        if (isDiscovering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = NavyBackgroundDark,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Finding your home & meters...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyBackgroundDark
                            )
                        } else {
                            Text(
                                text = "Find my details",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyBackgroundDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = NavyBackgroundDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // "Where do I find my key? ↗"
                    Row(
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://octopus.energy/dashboard/new/accounts/personal-details/api-access")
                                )
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Where do I find my key?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.NorthEast,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Public Rates Quick-Start Card (No API Key required for live Agile prices)
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyCardDark.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AgilePinkSoftPill,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Or watch public Agile prices right away",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Public half-hourly Agile rates don't require an API key. Pick your UK electricity region below and add your key later in Settings:",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )

                    ExposedDropdownMenuBox(
                        expanded = regionDropdownExpanded,
                        onExpandedChange = { regionDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "Region ${selectedRegion.code} — ${selectedRegion.name}",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionDropdownExpanded)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NavyInputDark,
                                unfocusedContainerColor = NavyInputDark,
                                focusedBorderColor = AgilePinkSoftPill,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = regionDropdownExpanded,
                            onDismissRequest = { regionDropdownExpanded = false },
                            containerColor = NavyCardDark
                        ) {
                            UK_GSP_REGIONS.forEach { reg ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                "Region ${reg.code} — ${reg.name}",
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                reg.areaDescription,
                                                color = TextSecondaryDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedRegion = reg
                                        regionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onSkipWithPublicRegion(selectedRegion.code) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Continue with Region ${selectedRegion.code} (${selectedRegion.name}) →",
                            color = AgilePinkSoftPill,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Footer Disclaimer matching 01-onboarding.png
            Text(
                text = "Agile Watcher is an independent app. It connects directly to Octopus Energy without an Agile Watcher server.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
        }
    }
}
