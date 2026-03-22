@file:OptIn(ExperimentalMaterial3Api::class)

package com.kycis.demo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

// Validation functions
fun validatePan(pan: String): String? {
    return when {
        pan.isEmpty() -> null
        pan.length != 10 -> "PAN must be exactly 10 characters"
        !pan.matches(Regex("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")) -> "Invalid PAN format (e.g., ABCDE1234F)"
        else -> null
    }
}

fun validateAadhaar(aadhaar: String): String? {
    return when {
        aadhaar.isEmpty() -> null
        aadhaar.length != 12 -> "Aadhaar must be exactly 12 digits"
        !aadhaar.matches(Regex("^\\d{12}$")) -> "Aadhaar must contain only digits"
        else -> null
    }
}

fun validateIfsc(ifsc: String): String? {
    return when {
        ifsc.isEmpty() -> null
        ifsc.length != 11 -> "IFSC must be exactly 11 characters"
        !ifsc.matches(Regex("^[A-Z]{4}0[A-Z0-9]{6}$")) -> "Invalid IFSC format (e.g., ABCD0123456)"
        else -> null
    }
}

fun validateDateOfBirth(dob: String): String? {
    if (dob.isEmpty()) return null
    val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")
    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)
            sdf.isLenient = false
            val date = sdf.parse(dob) ?: continue
            val now = Calendar.getInstance()
            val dobCal = Calendar.getInstance().apply { time = date }
            
            // Check not born in future
            if (date.after(now.time)) return "Date of birth cannot be in the future"
            
            // Check age > 18
            val age = now.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
            if (now.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                if (age <= 18) return "Must be at least 18 years old"
            }
            return null
        } catch (e: Exception) {
            continue
        }
    }
    return "Invalid date format (use DD/MM/YYYY)"
}

fun validateEmail(email: String): String? {
    if (email.isEmpty()) return null
    if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
        return "Invalid email format"
    }
    return null
}

fun validatePincode(pincode: String): String? {
    if (pincode.isEmpty()) return null
    if (!pincode.matches(Regex("^\\d{6}$"))) return "PIN code must be 6 digits"
    return null
}

@Composable
fun MutualFundKycScreen(
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    // Form state
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var maritalStatus by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var panNumber by remember { mutableStateOf("") }
    var aadhaarNumber by remember { mutableStateOf("") }
    
    // Address state
    var addressLine1 by remember { mutableStateOf("") }
    var addressLine2 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    
    // Bank state
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var branchAddress by remember { mutableStateOf("") }
    
    // Declarations
    var fatcaDeclaration by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Validation errors
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var panError by remember { mutableStateOf<String?>(null) }
    var aadhaarError by remember { mutableStateOf<String?>(null) }
    var pincodeError by remember { mutableStateOf<String?>(null) }
    var ifscError by remember { mutableStateOf<String?>(null) }

    // Real-time validation
    LaunchedEffect(fullName) { fullNameError = if (fullName.isBlank()) "Full name is required" else null }
    LaunchedEffect(email) { emailError = validateEmail(email) }
    LaunchedEffect(mobile) { mobileError = if (mobile.isNotBlank() && mobile.length != 10) "Mobile must be 10 digits" else null }
    LaunchedEffect(dateOfBirth) { dobError = validateDateOfBirth(dateOfBirth) }
    LaunchedEffect(panNumber) { panError = validatePan(panNumber) }
    LaunchedEffect(aadhaarNumber) { aadhaarError = validateAadhaar(aadhaarNumber) }
    LaunchedEffect(pincode) { pincodeError = validatePincode(pincode) }
    LaunchedEffect(ifscCode) { ifscError = validateIfsc(ifscCode) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mutual Fund KYC") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / totalSteps },
                modifier = Modifier.fillMaxWidth()
            )

            // Step indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Personal", "ID", "Address", "Bank", "Review").forEachIndexed { index, step ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (index <= currentStep) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (index <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (index <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (currentStep) {
                    0 -> PersonalDetailsStep(
                        fullName = fullName, onFullNameChange = { fullName = it },
                        fullNameError = fullNameError,
                        email = email, onEmailChange = { email = it },
                        emailError = emailError,
                        mobile = mobile, onMobileChange = { mobile = it },
                        mobileError = mobileError,
                        dateOfBirth = dateOfBirth, onDateOfBirthChange = { dateOfBirth = it },
                        dobError = dobError,
                        gender = gender, onGenderChange = { gender = it },
                        maritalStatus = maritalStatus, onMaritalStatusChange = { maritalStatus = it },
                        occupation = occupation, onOccupationChange = { occupation = it }
                    )
                    1 -> IdVerificationStep(
                        panNumber = panNumber, onPanChange = { panNumber = it },
                        panError = panError,
                        aadhaarNumber = aadhaarNumber, onAadhaarChange = { aadhaarNumber = it },
                        aadhaarError = aadhaarError
                    )
                    2 -> AddressStep(
                        addressLine1 = addressLine1, onAddressLine1Change = { addressLine1 = it },
                        addressLine2 = addressLine2, onAddressLine2Change = { addressLine2 = it },
                        city = city, onCityChange = { city = it },
                        state = state, onStateChange = { state = it },
                        pincode = pincode, onPincodeChange = { pincode = it },
                        pincodeError = pincodeError
                    )
                    3 -> BankDetailsStep(
                        bankName = bankName, onBankNameChange = { bankName = it },
                        accountNumber = accountNumber, onAccountNumberChange = { accountNumber = it },
                        ifscCode = ifscCode, onIfscCodeChange = { ifscCode = it },
                        ifscError = ifscError,
                        branchAddress = branchAddress, onBranchAddressChange = { branchAddress = it }
                    )
                    4 -> ReviewStep(
                        fullName = fullName, email = email, mobile = mobile,
                        dateOfBirth = dateOfBirth, gender = gender, occupation = occupation,
                        addressLine1 = addressLine1, addressLine2 = addressLine2,
                        city = city, state = state, pincode = pincode,
                        panNumber = panNumber, aadhaarNumber = aadhaarNumber,
                        bankName = bankName, accountNumber = accountNumber, ifscCode = ifscCode,
                        fatcaDeclaration = fatcaDeclaration, onFatcaChange = { fatcaDeclaration = it },
                        termsAccepted = termsAccepted, onTermsChange = { termsAccepted = it }
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            isSubmitting = true
                            onSubmit()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting && when (currentStep) {
                        0 -> fullName.isNotBlank() && dateOfBirth.isNotBlank() && gender.isNotBlank() && 
                             occupation.isNotBlank() && fullNameError == null && dobError == null
                        1 -> panNumber.length == 10 && aadhaarNumber.length == 12 && panError == null && aadhaarError == null
                        2 -> addressLine1.isNotBlank() && city.isNotBlank() && state.isNotBlank() && pincode.length == 6
                        3 -> bankName.isNotBlank() && accountNumber.isNotBlank() && ifscCode.length == 11 && ifscError == null
                        4 -> fatcaDeclaration && termsAccepted
                        else -> true
                    }
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (currentStep < totalSteps - 1) "Continue" else "Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalDetailsStep(
    fullName: String, onFullNameChange: (String) -> Unit, fullNameError: String?,
    email: String, onEmailChange: (String) -> Unit, emailError: String?,
    mobile: String, onMobileChange: (String) -> Unit, mobileError: String?,
    dateOfBirth: String, onDateOfBirthChange: (String) -> Unit, dobError: String?,
    gender: String, onGenderChange: (String) -> Unit,
    maritalStatus: String, onMaritalStatusChange: (String) -> Unit,
    occupation: String, onOccupationChange: (String) -> Unit
) {
    var genderExpanded by remember { mutableStateOf(false) }
    var maritalExpanded by remember { mutableStateOf(false) }
    var occupationExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Personal Details", style = MaterialTheme.typography.headlineSmall)
        Text("Please provide your personal information as per your official documents",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = fullName, onValueChange = onFullNameChange,
            label = { Text("Full Name (as per PAN) *") },
            modifier = Modifier.fillMaxWidth(), isError = fullNameError != null,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            supportingText = fullNameError?.let { { Text(it) } }
        )

        OutlinedTextField(
            value = email, onValueChange = onEmailChange,
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(), isError = emailError != null,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            supportingText = emailError?.let { { Text(it) } }
        )

        OutlinedTextField(
            value = mobile, onValueChange = { if (it.length <= 10) onMobileChange(it.filter { c -> c.isDigit() }) },
            label = { Text("Mobile Number *") },
            modifier = Modifier.fillMaxWidth(), isError = mobileError != null,
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            supportingText = mobileError?.let { { Text(it) } }
        )

        OutlinedTextField(
            value = dateOfBirth, onValueChange = onDateOfBirthChange,
            label = { Text("Date of Birth (DD/MM/YYYY) *") },
            modifier = Modifier.fillMaxWidth(), isError = dobError != null,
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            supportingText = dobError?.let { { Text(it) } }
        )

        ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = it }) {
            OutlinedTextField(
                value = gender, onValueChange = {}, readOnly = true,
                label = { Text("Gender *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                listOf("Male", "Female", "Other").forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onGenderChange(option); genderExpanded = false })
                }
            }
        }

        ExposedDropdownMenuBox(expanded = maritalExpanded, onExpandedChange = { maritalExpanded = it }) {
            OutlinedTextField(
                value = maritalStatus, onValueChange = {}, readOnly = true,
                label = { Text("Marital Status") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = maritalExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = maritalExpanded, onDismissRequest = { maritalExpanded = false }) {
                listOf("Single", "Married", "Divorced", "Widowed").forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onMaritalStatusChange(option); maritalExpanded = false })
                }
            }
        }

        ExposedDropdownMenuBox(expanded = occupationExpanded, onExpandedChange = { occupationExpanded = it }) {
            OutlinedTextField(
                value = occupation, onValueChange = {}, readOnly = true,
                label = { Text("Occupation *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = occupationExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = occupationExpanded, onDismissRequest = { occupationExpanded = false }) {
                listOf("Salaried", "Self-Employed", "Business", "Professional", "Retired", "Student", "Housewife", "Other").forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onOccupationChange(option); occupationExpanded = false })
                }
            }
        }
    }
}

@Composable
fun IdVerificationStep(
    panNumber: String, onPanChange: (String) -> Unit, panError: String?,
    aadhaarNumber: String, onAadhaarChange: (String) -> Unit, aadhaarError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ID Verification", style = MaterialTheme.typography.headlineSmall)
        Text("Enter your PAN and Aadhaar details for identity verification",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = panNumber, onValueChange = { if (it.length <= 10) onPanChange(it.uppercase().filter { c -> c.isLetterOrDigit() }) },
            label = { Text("PAN Number *") }, placeholder = { Text("ABCDE1234F") },
            modifier = Modifier.fillMaxWidth(), isError = panError != null,
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
            supportingText = panError?.let { { Text(it) } }
        )

        OutlinedTextField(
            value = aadhaarNumber, onValueChange = { if (it.length <= 12) onAadhaarChange(it.filter { c -> c.isDigit() }) },
            label = { Text("Aadhaar Number *") }, placeholder = { Text("123456789012") },
            modifier = Modifier.fillMaxWidth(), isError = aadhaarError != null,
            leadingIcon = { Icon(Icons.Default.AssignmentInd, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = aadhaarError?.let { { Text(it) } }
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Your Aadhaar details will be verified via UIDAI", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AddressStep(
    addressLine1: String, onAddressLine1Change: (String) -> Unit,
    addressLine2: String, onAddressLine2Change: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    state: String, onStateChange: (String) -> Unit,
    pincode: String, onPincodeChange: (String) -> Unit, pincodeError: String?
) {
    var stateExpanded by remember { mutableStateOf(false) }
    val states = listOf("Andhra Pradesh", "Bihar", "Delhi", "Goa", "Gujarat", "Haryana",
        "Karnataka", "Kerala", "Maharashtra", "Punjab", "Rajasthan", "Tamil Nadu", "Telangana", "Uttar Pradesh", "West Bengal")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Address Details", style = MaterialTheme.typography.headlineSmall)
        Text("Enter your current residential address",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = addressLine1, onValueChange = onAddressLine1Change,
            label = { Text("Address Line 1 *") }, placeholder = { Text("House No., Street Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = addressLine2, onValueChange = onAddressLine2Change,
            label = { Text("Address Line 2") }, placeholder = { Text("Landmark, Area") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = city, onValueChange = onCityChange,
                label = { Text("City *") }, modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = pincode, onValueChange = { if (it.length <= 6) onPincodeChange(it.filter { c -> c.isDigit() }) },
                label = { Text("PIN Code *") }, modifier = Modifier.weight(1f), isError = pincodeError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = pincodeError?.let { { Text(it) } }
            )
        }

        ExposedDropdownMenuBox(expanded = stateExpanded, onExpandedChange = { stateExpanded = it }) {
            OutlinedTextField(
                value = state, onValueChange = {}, readOnly = true,
                label = { Text("State *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = stateExpanded, onDismissRequest = { stateExpanded = false }) {
                states.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onStateChange(option); stateExpanded = false })
                }
            }
        }
    }
}

@Composable
fun BankDetailsStep(
    bankName: String, onBankNameChange: (String) -> Unit,
    accountNumber: String, onAccountNumberChange: (String) -> Unit,
    ifscCode: String, onIfscCodeChange: (String) -> Unit, ifscError: String?,
    branchAddress: String, onBranchAddressChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bank Details", style = MaterialTheme.typography.headlineSmall)
        Text("Enter your bank account details for investment transactions",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = bankName, onValueChange = onBankNameChange,
            label = { Text("Bank Name *") }, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
        )

        OutlinedTextField(
            value = accountNumber, onValueChange = { if (it.length <= 18) onAccountNumberChange(it.filter { c -> c.isDigit() }) },
            label = { Text("Account Number *") }, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = ifscCode, onValueChange = { if (it.length <= 11) onIfscCodeChange(it.uppercase().filter { c -> c.isLetterOrDigit() }) },
            label = { Text("IFSC Code *") }, placeholder = { Text("ABCD0123456") },
            modifier = Modifier.fillMaxWidth(), isError = ifscError != null,
            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
            supportingText = ifscError?.let { { Text(it) } }
        )

        OutlinedTextField(
            value = branchAddress, onValueChange = onBranchAddressChange,
            label = { Text("Branch Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Your bank details are encrypted and secure", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
fun ReviewStep(
    fullName: String, email: String, mobile: String,
    dateOfBirth: String, gender: String, occupation: String,
    addressLine1: String, addressLine2: String, city: String, state: String, pincode: String,
    panNumber: String, aadhaarNumber: String,
    bankName: String, accountNumber: String, ifscCode: String,
    fatcaDeclaration: Boolean, onFatcaChange: (Boolean) -> Unit,
    termsAccepted: Boolean, onTermsChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Review & Confirm", style = MaterialTheme.typography.headlineSmall)
        Text("Please review your details before submission",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Personal Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ReviewRow("Name", fullName)
                ReviewRow("Email", email.ifBlank { "-" })
                ReviewRow("Mobile", mobile.ifBlank { "-" })
                ReviewRow("DOB", dateOfBirth)
                ReviewRow("Gender", gender.ifBlank { "-" })
                ReviewRow("Occupation", occupation)
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Address", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ReviewRow("Address", "$addressLine1${if (addressLine2.isNotBlank()) ", $addressLine2" else ""}")
                ReviewRow("City", city.ifBlank { "-" })
                ReviewRow("State", state.ifBlank { "-" })
                ReviewRow("PIN", pincode.ifBlank { "-" })
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ID Details", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ReviewRow("PAN", panNumber)
                ReviewRow("Aadhaar", if (aadhaarNumber.length >= 4) "XXXX-XXXX-${aadhaarNumber.takeLast(4)}" else "-")
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bank Details", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ReviewRow("Bank", bankName)
                ReviewRow("Account", if (accountNumber.length >= 4) "XXXX${accountNumber.takeLast(4)}" else "-")
                ReviewRow("IFSC", ifscCode)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("FATCA Declaration", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("I declare that I am not a US citizen or resident for tax purposes, and I do not have any US tax identification number.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = fatcaDeclaration, onCheckedChange = onFatcaChange)
                    Text("I accept FATCA declaration")
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = termsAccepted, onCheckedChange = onTermsChange)
            Text("I agree to the Terms & Conditions and Privacy Policy", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}