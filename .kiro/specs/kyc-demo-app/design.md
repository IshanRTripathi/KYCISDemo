# Design Document: KYC Demo Android App

## Overview

The KYC Demo Android App is a fintech demonstration application built with modern Android development practices. It simulates a complete Know Your Customer (KYC) onboarding journey from login through document verification to completion. The app is designed to showcase realistic user flows with configurable mock behavior, making it ideal for demonstrations, testing, and as a reference implementation.

### Key Design Goals

1. **Clean Architecture**: Maintain clear separation between presentation, domain, and data layers
2. **Configurability**: Enable demo customization through external configuration files without code changes
3. **Modern UI**: Deliver a professional fintech experience using Jetpack Compose
4. **Realistic Simulation**: Provide authentic KYC flow with configurable friction points and error scenarios
5. **Maintainability**: Use dependency injection, MVVM pattern, and testable components

### Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: Clean Architecture with MVVM
- **Dependency Injection**: Hilt
- **Navigation**: Jetpack Navigation Compose
- **State Management**: StateFlow and Compose State
- **Image Loading**: Coil
- **Permissions**: Accompanist Permissions
- **Configuration Parsing**: Kotlinx Serialization (JSON) or SnakeYAML (YAML)
- **Persistence**: DataStore (feature-flagged)

## Architecture

### Layer Structure

The application follows Clean Architecture principles with three distinct layers:

```
┌─────────────────────────────────────────────────────────┐
│                  Presentation Layer                      │
│  (Composables, ViewModels, UI State, Navigation)       │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer                          │
│     (Use Cases, Domain Models, Repository Interfaces)   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                     Data Layer                           │
│  (Repository Implementations, Mock Backend, Config)     │
└─────────────────────────────────────────────────────────┘
```

### Presentation Layer

Responsible for UI rendering and user interaction handling:

- **Composables**: Screen-level and reusable UI components built with Jetpack Compose
- **ViewModels**: Manage UI state, handle user actions, coordinate use cases
- **UI State**: Immutable data classes representing screen state
- **Navigation**: NavHost and navigation graph defining screen flow

### Domain Layer

Contains business logic and is framework-agnostic:

- **Use Cases**: Single-responsibility classes encapsulating business operations
- **Domain Models**: Pure Kotlin data classes representing business entities
- **Repository Interfaces**: Contracts for data operations

### Data Layer

Handles data operations and external dependencies:

- **Repository Implementations**: Concrete implementations of repository interfaces
- **Mock Backend**: Simulates API responses with configurable delays and scenarios
- **Configuration System**: Parses and validates configuration files
- **Persistence Manager**: Handles save/resume functionality (feature-flagged)

### Dependency Flow

Dependencies flow inward: Presentation → Domain ← Data. The domain layer has no dependencies on outer layers, ensuring testability and flexibility.

## Components and Interfaces

### Core Components

#### 1. Configuration System

**Purpose**: Load, parse, and validate configuration files that define validation rules, mock responses, and error scenarios.

**Components**:
- `ConfigurationParser`: Parses JSON/YAML files into Configuration objects
- `ConfigurationValidator`: Validates configuration structure and rules
- `ConfigurationRepository`: Provides access to loaded configuration
- `Configuration`: Data class representing the complete configuration

**Key Interfaces**:

```kotlin
interface ConfigurationParser {
    fun parse(content: String): Result<Configuration>
    fun format(config: Configuration): String
}

interface ConfigurationRepository {
    suspend fun loadConfiguration(): Result<Configuration>
    fun getValidationRules(): ValidationRules
    fun getMockScenarios(): MockScenarios
    fun getErrorSimulations(): ErrorSimulations
}
```

**Configuration Structure**:
- Validation rules per field (regex patterns, length constraints, required flags)
- Mock response scenarios (success/failure conditions, response delays)
- Error simulation rules (trigger conditions, error messages)
- Feature flags (persistence enabled, specific error scenarios)

#### 2. Validation Engine

**Purpose**: Validate user inputs against configured rules and provide descriptive error messages.

**Components**:
- `ValidationEngine`: Coordinates validation across all input types
- `FieldValidator`: Validates individual fields
- `ValidationRule`: Represents a single validation constraint

**Key Interfaces**:

```kotlin
interface ValidationEngine {
    fun validateEmail(email: String): ValidationResult
    fun validatePhone(phone: String): ValidationResult
    fun validatePAN(pan: String): ValidationResult
    fun validateAadhaar(aadhaar: String): ValidationResult
    fun validateDateOfBirth(dob: String): ValidationResult
    fun validateRequired(value: String): ValidationResult
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errorMessage: String) : ValidationResult()
}
```

**Validation Rules**:
- Email: Standard email format (RFC 5322 simplified)
- Phone: Configurable format (default: 10 digits for Indian numbers)
- PAN: Pattern AAAAA9999A (5 uppercase letters, 4 digits, 1 uppercase letter)
- Aadhaar: Exactly 12 digits
- Date of Birth: Valid date in the past
- Required: Non-empty, non-whitespace

#### 3. Camera Module

**Purpose**: Handle camera permissions, image capture, gallery selection, and image processing.

**Components**:
- `CameraManager`: Manages camera operations and permissions
- `ImageProcessor`: Handles image compression and validation
- `PermissionHandler`: Manages runtime permissions

**Key Interfaces**:

```kotlin
interface CameraManager {
    suspend fun captureImage(): Result<ImageData>
    suspend fun selectFromGallery(): Result<ImageData>
    suspend fun requestCameraPermission(): PermissionResult
    suspend fun requestStoragePermission(): PermissionResult
}

interface ImageProcessor {
    suspend fun validateImage(image: ImageData): ValidationResult
    suspend fun compressImage(image: ImageData, maxSizeKB: Int): ImageData
}

data class ImageData(
    val uri: Uri,
    val sizeBytes: Long,
    val format: ImageFormat
)

enum class ImageFormat { JPEG, PNG }
```

**Image Handling**:
- Supported formats: JPEG, PNG
- Maximum file size: Configurable (default 5MB)
- Compression: Applied if image exceeds threshold (default 2MB)
- Validation: Format check, size check, basic integrity check

#### 4. Mock Backend

**Purpose**: Simulate backend API responses without real network calls, with configurable delays and scenarios.

**Components**:
- `MockBackendService`: Simulates API endpoints
- `ResponseSimulator`: Generates mock responses based on configuration
- `DelaySimulator`: Introduces configurable delays

**Key Interfaces**:

```kotlin
interface MockBackendService {
    suspend fun submitPersonalDetails(details: PersonalDetails): Result<SubmissionResponse>
    suspend fun validatePAN(pan: String): Result<ValidationResponse>
    suspend fun uploadDocument(document: ImageData): Result<UploadResponse>
    suspend fun validateAadhaar(aadhaar: String): Result<ValidationResponse>
    suspend fun sendOTP(aadhaar: String): Result<OTPResponse>
    suspend fun verifyOTP(otp: String): Result<VerificationResponse>
    suspend fun uploadSelfie(selfie: ImageData): Result<UploadResponse>
}

data class SubmissionResponse(
    val success: Boolean,
    val message: String,
    val sessionId: String? = null
)
```

**Mock Behavior**:
- Response delays: Configurable per endpoint (default 1-3 seconds)
- Success/failure: Based on configured scenarios and input validation
- OTP generation: Mock 6-digit codes (configurable valid codes)
- Error scenarios: Configurable failure conditions

#### 5. Persistence Manager

**Purpose**: Save and restore user progress through the KYC journey (feature-flagged).

**Components**:
- `PersistenceManager`: Manages save/load operations
- `KycProgressStore`: DataStore-based storage for progress data

**Key Interfaces**:

```kotlin
interface PersistenceManager {
    suspend fun saveProgress(progress: KycProgress): Result<Unit>
    suspend fun loadProgress(): Result<KycProgress?>
    suspend fun clearProgress(): Result<Unit>
    fun isEnabled(): Boolean
}

data class KycProgress(
    val currentScreen: String,
    val completedScreens: List<String>,
    val personalDetails: PersonalDetails?,
    val panNumber: String?,
    val aadhaarNumber: String?,
    val timestamp: Long
)
```

**Persistence Behavior**:
- Enabled/disabled via configuration feature flag
- Saves after each successful screen completion
- Restores on app launch if data exists
- Clears on successful KYC completion or explicit reset

#### 6. Error Simulator

**Purpose**: Introduce configurable friction points and errors for demonstration purposes.

**Components**:
- `ErrorSimulator`: Determines when to trigger errors
- `ErrorScenario`: Represents a configured error condition

**Key Interfaces**:

```kotlin
interface ErrorSimulator {
    fun shouldSimulateError(context: ErrorContext): Boolean
    fun getErrorMessage(context: ErrorContext): String
}

data class ErrorContext(
    val screen: String,
    val action: String,
    val attemptCount: Int,
    val inputData: Map<String, Any>
)

data class ErrorScenario(
    val enabled: Boolean,
    val triggerCondition: TriggerCondition,
    val errorMessage: String,
    val errorType: ErrorType
)

enum class ErrorType {
    VALIDATION_ERROR,
    UPLOAD_ERROR,
    NETWORK_ERROR,
    OTP_ERROR
}
```

### ViewModels

#### KycViewModel

Central ViewModel managing the KYC journey state and coordinating use cases.

**Responsibilities**:
- Maintain current screen and navigation state
- Coordinate validation, submission, and navigation use cases
- Manage loading and error states
- Expose UI state via StateFlow

**State**:

```kotlin
data class KycUiState(
    val currentScreen: KycScreen,
    val isLoading: Boolean = false,
    val error: ErrorState? = null,
    val personalDetails: PersonalDetailsState = PersonalDetailsState(),
    val panState: PanState = PanState(),
    val aadhaarState: AadhaarState = AadhaarState(),
    val otpState: OtpState = OtpState(),
    val uploadStates: Map<DocumentType, UploadState> = emptyMap()
)
```

#### Screen-Specific ViewModels

Each complex screen may have its own ViewModel:
- `PersonalDetailsViewModel`: Manages personal details form state
- `DocumentUploadViewModel`: Manages document upload flow
- `OtpViewModel`: Manages OTP entry and timer

### Use Cases

Use cases encapsulate single business operations:

1. `ValidatePersonalDetailsUseCase`: Validates all personal detail fields
2. `SubmitPersonalDetailsUseCase`: Submits personal details to mock backend
3. `ValidatePANUseCase`: Validates PAN format
4. `UploadDocumentUseCase`: Handles document upload with validation
5. `ValidateAadhaarUseCase`: Validates Aadhaar format
6. `SendOTPUseCase`: Triggers OTP generation
7. `VerifyOTPUseCase`: Validates entered OTP
8. `CaptureSelfieUseCase`: Handles selfie capture flow
9. `SaveProgressUseCase`: Saves current progress
10. `LoadProgressUseCase`: Loads saved progress

### Repositories

#### KycRepository

```kotlin
interface KycRepository {
    suspend fun submitPersonalDetails(details: PersonalDetails): Result<SubmissionResponse>
    suspend fun validatePAN(pan: String): Result<ValidationResponse>
    suspend fun uploadDocument(document: ImageData, type: DocumentType): Result<UploadResponse>
    suspend fun validateAadhaar(aadhaar: String): Result<ValidationResponse>
    suspend fun sendOTP(aadhaar: String): Result<OTPResponse>
    suspend fun verifyOTP(otp: String): Result<VerificationResponse>
    suspend fun uploadSelfie(selfie: ImageData): Result<UploadResponse>
}
```

Implementation delegates to MockBackendService and applies error simulation.

## Data Models

### Domain Models

#### PersonalDetails

```kotlin
data class PersonalDetails(
    val fullName: String,
    val dateOfBirth: String,
    val phoneNumber: String,
    val email: String
)
```

#### Document

```kotlin
data class Document(
    val type: DocumentType,
    val imageData: ImageData,
    val uploadedAt: Long
)

enum class DocumentType {
    PAN_CARD,
    SELFIE
}
```

#### KycScreen

```kotlin
enum class KycScreen {
    LOGIN,
    PERSONAL_DETAILS,
    PAN_ENTRY,
    PAN_UPLOAD,
    AADHAAR_ENTRY,
    OTP_VERIFICATION,
    SELFIE_CAPTURE,
    SUCCESS
}
```

### UI State Models

#### PersonalDetailsState

```kotlin
data class PersonalDetailsState(
    val fullName: String = "",
    val fullNameError: String? = null,
    val dateOfBirth: String = "",
    val dateOfBirthError: String? = null,
    val phoneNumber: String = "",
    val phoneNumberError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val isValid: Boolean = false
)
```

#### PanState

```kotlin
data class PanState(
    val panNumber: String = "",
    val panError: String? = null,
    val isValid: Boolean = false
)
```

#### UploadState

```kotlin
data class UploadState(
    val imageData: ImageData? = null,
    val isUploading: Boolean = false,
    val uploadError: String? = null,
    val uploadSuccess: Boolean = false
)
```

#### OtpState

```kotlin
data class OtpState(
    val digits: List<String> = List(6) { "" },
    val timeRemaining: Int = 60,
    val canResend: Boolean = false,
    val otpError: String? = null,
    val isVerifying: Boolean = false
)
```

#### AadhaarState

```kotlin
data class AadhaarState(
    val aadhaarNumber: String = "",
    val aadhaarError: String? = null,
    val isValid: Boolean = false
)
```

### Configuration Models

#### Configuration

```kotlin
data class Configuration(
    val validationRules: ValidationRules,
    val mockScenarios: MockScenarios,
    val errorSimulations: ErrorSimulations,
    val featureFlags: FeatureFlags
)

data class ValidationRules(
    val email: EmailValidationRule,
    val phone: PhoneValidationRule,
    val pan: PanValidationRule,
    val aadhaar: AadhaarValidationRule,
    val dateOfBirth: DateValidationRule
)

data class MockScenarios(
    val responseDelays: Map<String, IntRange>,
    val successConditions: Map<String, SuccessCondition>,
    val validOtpCodes: List<String>
)

data class ErrorSimulations(
    val scenarios: List<ErrorScenario>
)

data class FeatureFlags(
    val persistenceEnabled: Boolean,
    val errorSimulationEnabled: Boolean
)
```

### Navigation Models

#### NavigationEvent

```kotlin
sealed class NavigationEvent {
    object NavigateBack : NavigationEvent()
    data class NavigateTo(val screen: KycScreen) : NavigationEvent()
    object NavigateToSuccess : NavigationEvent()
}
```

### Error Models

#### ErrorState

```kotlin
sealed class ErrorState {
    data class ValidationError(
        val field: String,
        val message: String
    ) : ErrorState()
    
    data class NetworkError(
        val message: String,
        val retryable: Boolean = true
    ) : ErrorState()
    
    data class UploadError(
        val message: String,
        val retryable: Boolean = true
    ) : ErrorState()
    
    data class OtpError(
        val message: String,
        val canResend: Boolean = false
    ) : ErrorState()
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, I identified the following redundancies:
- Validation properties (6.1-6.6) cover the specific screen validation requirements (7.2, 10.2)
- Navigation properties can be generalized rather than testing each screen individually
- Configuration parsing round-trip (20.4) is the comprehensive test that subsumes individual parse/format tests
- Error simulation delay property (14.2) covers mock backend delay requirement (18.1)

The following properties represent the unique, non-redundant correctness guarantees:

### Property 1: Screen Sequence Navigation

*For any* screen in the KYC journey (except the Success screen), when the user successfully completes that screen, the app should navigate to the next screen in the defined sequence: Login → Personal Details → PAN Entry → PAN Upload → Aadhaar Entry → OTP Verification → Selfie Capture → Success.

**Validates: Requirements 2.2**

### Property 2: Backward Navigation Availability

*For any* screen in the KYC journey (except Login and Success screens), the user should be able to navigate back to the previous screen.

**Validates: Requirements 2.3**

### Property 3: Field Validation on Input

*For any* input field and any user-entered data, the Validation Engine should validate the data according to the configured rules for that field type.

**Validates: Requirements 5.2**

### Property 4: Continue Button State

*For any* form screen with multiple fields, the continue button should be enabled if and only if all required fields contain valid data.

**Validates: Requirements 5.4**

### Property 5: Email Validation

*For any* string, the Validation Engine should validate it as a valid email if and only if it matches standard email format rules (contains @ symbol, has valid domain structure, no invalid characters).

**Validates: Requirements 6.1**

### Property 6: Phone Number Validation

*For any* string, the Validation Engine should validate it as a valid phone number if and only if it matches the configured phone format rules (default: exactly 10 digits).

**Validates: Requirements 6.2**

### Property 7: PAN Validation

*For any* string, the Validation Engine should validate it as a valid PAN if and only if it matches the pattern: five uppercase letters, followed by four digits, followed by one uppercase letter (e.g., ABCDE1234F).

**Validates: Requirements 6.3**

### Property 8: Aadhaar Validation

*For any* string, the Validation Engine should validate it as a valid Aadhaar if and only if it contains exactly twelve digits.

**Validates: Requirements 6.4**

### Property 9: Required Field Validation

*For any* string composed entirely of whitespace characters (spaces, tabs, newlines) or empty string, the Validation Engine should reject it as invalid for required fields.

**Validates: Requirements 6.5**

### Property 10: Date of Birth Validation

*For any* date string, the Validation Engine should validate it as a valid date of birth if and only if it represents a valid date in the past (before the current date).

**Validates: Requirements 6.6**

### Property 11: Image Size Validation

*For any* image with a file size, the Camera Module should validate that the image is acceptable if and only if its size does not exceed the configured maximum size.

**Validates: Requirements 9.2**

### Property 12: Image Compression Threshold

*For any* image with a file size exceeding the configured compression threshold, the Camera Module should compress the image before proceeding.

**Validates: Requirements 9.4**

### Property 13: Aadhaar Number Formatting

*For any* valid 12-digit Aadhaar number, the formatting function should insert spaces to display it in groups (e.g., "1234 5678 9012") for readability.

**Validates: Requirements 10.3**

### Property 14: API Response Delays

*For any* mock API endpoint, when called, the Mock Backend should introduce a delay according to the configured delay range for that endpoint before returning a response.

**Validates: Requirements 14.2**

### Property 15: Progress Persistence on Completion

*For any* screen in the KYC journey, when persistence is enabled and the user successfully completes that screen, the Persistence Manager should save the current progress including the completed screen identifier.

**Validates: Requirements 15.1**

### Property 16: Progress Restoration Round Trip

*For any* saved KYC progress state, when persistence is enabled, saving the progress and then loading it should restore an equivalent progress state with the same completed screens and user data.

**Validates: Requirements 15.2**

### Property 17: Configuration Serialization Round Trip

*For any* valid Configuration object, serializing it to a configuration file format (JSON/YAML) and then parsing it back should produce an equivalent Configuration object with the same validation rules, mock scenarios, error simulations, and feature flags.

**Validates: Requirements 20.4**

## Error Handling

### Error Categories

The application handles four primary categories of errors:

1. **Validation Errors**: Input data that fails validation rules
2. **Permission Errors**: Denied camera or storage permissions
3. **Upload Errors**: Image upload failures (size, format, simulated failures)
4. **Processing Errors**: Mock backend errors and OTP validation failures

### Error Handling Strategy

#### Validation Errors

- **Detection**: Immediate validation on field blur or input change
- **Display**: Inline error messages below the relevant field
- **Recovery**: User corrects input, error clears automatically
- **State**: Error state stored in ViewModel, exposed via StateFlow

#### Permission Errors

- **Detection**: Permission check before camera/gallery access
- **Display**: Dialog or full-screen message explaining permission need
- **Recovery**: User can grant permission or cancel operation
- **Fallback**: Alternative input method if available (gallery vs camera)

#### Upload Errors

- **Detection**: Image validation or simulated upload failure
- **Display**: Error message on upload card with retry button
- **Recovery**: User can retry upload or select different image
- **Logging**: Error details logged for debugging

#### Processing Errors

- **Detection**: Mock backend returns error response
- **Display**: Full-screen error view with descriptive message
- **Recovery**: Retry button for transient errors, back navigation for permanent errors
- **Simulation**: Configurable error scenarios for demo purposes

### Error State Management

All errors are represented as sealed classes in the domain layer:

```kotlin
sealed class KycError {
    data class ValidationError(val field: String, val message: String) : KycError()
    data class PermissionError(val permission: String, val rationale: String) : KycError()
    data class UploadError(val message: String, val retryable: Boolean) : KycError()
    data class ProcessingError(val message: String, val retryable: Boolean) : KycError()
}
```

ViewModels expose error state via StateFlow, and Composables observe and render appropriate error UI.

### Error Recovery Patterns

1. **Automatic Retry**: For transient network-like errors (simulated delays)
2. **User-Initiated Retry**: For upload failures and processing errors
3. **Input Correction**: For validation errors
4. **Alternative Path**: For permission denials (camera → gallery)
5. **Graceful Degradation**: Default configuration if config file invalid

### Error Simulation for Demo

The Error Simulator introduces configurable friction points:

- **Validation Failures**: Force specific inputs to fail validation
- **Upload Delays**: Simulate slow upload processing
- **Upload Failures**: Randomly fail uploads based on probability
- **OTP Failures**: Reject specific OTP codes
- **API Delays**: Introduce realistic response times

Configuration example:

```json
{
  "errorSimulations": {
    "scenarios": [
      {
        "enabled": true,
        "screen": "pan_upload",
        "errorType": "upload_failure",
        "triggerCondition": {
          "probability": 0.3
        },
        "errorMessage": "Document image is unclear. Please capture again."
      }
    ]
  }
}
```

## Testing Strategy

### Dual Testing Approach

The KYC Demo App requires both unit testing and property-based testing for comprehensive coverage:

- **Unit Tests**: Verify specific examples, edge cases, error conditions, and integration points
- **Property Tests**: Verify universal properties across all inputs through randomized testing

Both approaches are complementary and necessary. Unit tests catch concrete bugs and verify specific scenarios, while property tests verify general correctness across a wide input space.

### Unit Testing

#### Scope

Unit tests focus on:
- Specific validation examples (valid/invalid email, PAN, Aadhaar)
- Navigation flow examples (Login → Personal Details, Success screen back prevention)
- Permission handling scenarios (granted, denied, rationale)
- Configuration parsing examples (valid JSON, valid YAML, invalid format)
- Error handling examples (specific error scenarios)
- Mock backend behavior (OTP generation, upload simulation)

#### Test Organization

```
test/
├── domain/
│   ├── usecases/
│   │   ├── ValidatePersonalDetailsUseCaseTest.kt
│   │   ├── UploadDocumentUseCaseTest.kt
│   │   └── VerifyOTPUseCaseTest.kt
│   └── models/
│       └── ConfigurationTest.kt
├── data/
│   ├── repository/
│   │   └── KycRepositoryImplTest.kt
│   ├── config/
│   │   └── ConfigurationParserTest.kt
│   └── mock/
│       └── MockBackendServiceTest.kt
└── presentation/
    └── viewmodels/
        └── KycViewModelTest.kt
```

#### Key Unit Tests

1. **Configuration Parsing**
   - Valid JSON configuration → successful parse
   - Valid YAML configuration → successful parse
   - Invalid configuration → error with default fallback
   - Missing required fields → descriptive error

2. **Navigation Flow**
   - Login initiation → navigates to Personal Details
   - Personal Details completion → navigates to PAN Entry
   - Success screen → back navigation prevented

3. **Permission Handling**
   - Camera permission not granted → request permission
   - Storage permission not granted → request permission
   - Permission denied → display error message

4. **Mock Backend**
   - Send OTP → generates 6-digit code
   - Upload document → simulates processing delay
   - Verify OTP with valid code → success response
   - Verify OTP with invalid code → error response

### Property-Based Testing

#### Framework Selection

For Kotlin/Android, we'll use **Kotest Property Testing** (formerly KotlinTest), which provides:
- Comprehensive property testing support
- Integration with JUnit
- Rich set of generators for common types
- Custom generator support

#### Configuration

Each property test must:
- Run minimum 100 iterations (configured via `PropertyTesting.defaultIterationCount`)
- Include a comment tag referencing the design property
- Use appropriate generators for input data

Tag format:
```kotlin
// Feature: kyc-demo-app, Property 5: Email Validation
```

#### Property Test Organization

```
test/
└── properties/
    ├── ValidationPropertiesTest.kt
    ├── NavigationPropertiesTest.kt
    ├── PersistencePropertiesTest.kt
    ├── ImageProcessingPropertiesTest.kt
    └── ConfigurationPropertiesTest.kt
```

#### Property Test Implementation Examples

**Property 5: Email Validation**

```kotlin
class ValidationPropertiesTest : StringSpec({
    
    // Feature: kyc-demo-app, Property 5: Email Validation
    "email validation should accept valid emails and reject invalid ones" {
        checkAll(100, Arb.email()) { validEmail ->
            val result = validationEngine.validateEmail(validEmail)
            result shouldBe ValidationResult.Valid
        }
        
        checkAll(100, Arb.invalidEmail()) { invalidEmail ->
            val result = validationEngine.validateEmail(invalidEmail)
            result shouldBe instanceOf<ValidationResult.Invalid>()
        }
    }
    
    // Feature: kyc-demo-app, Property 7: PAN Validation
    "PAN validation should accept only AAAAA9999A pattern" {
        checkAll(100, Arb.validPAN()) { validPAN ->
            val result = validationEngine.validatePAN(validPAN)
            result shouldBe ValidationResult.Valid
        }
        
        checkAll(100, Arb.string()) { randomString ->
            val result = validationEngine.validatePAN(randomString)
            if (randomString.matches(Regex("[A-Z]{5}\\d{4}[A-Z]"))) {
                result shouldBe ValidationResult.Valid
            } else {
                result shouldBe instanceOf<ValidationResult.Invalid>()
            }
        }
    }
    
    // Feature: kyc-demo-app, Property 9: Required Field Validation
    "required field validation should reject whitespace-only strings" {
        checkAll(100, Arb.whitespaceString()) { whitespaceString ->
            val result = validationEngine.validateRequired(whitespaceString)
            result shouldBe instanceOf<ValidationResult.Invalid>()
        }
    }
})
```

**Property 17: Configuration Serialization Round Trip**

```kotlin
class ConfigurationPropertiesTest : StringSpec({
    
    // Feature: kyc-demo-app, Property 17: Configuration Serialization Round Trip
    "configuration serialization round trip should preserve data" {
        checkAll(100, Arb.configuration()) { config ->
            val serialized = configurationParser.format(config)
            val deserialized = configurationParser.parse(serialized)
            
            deserialized.isSuccess shouldBe true
            deserialized.getOrNull() shouldBe config
        }
    }
})
```

**Property 16: Progress Restoration Round Trip**

```kotlin
class PersistencePropertiesTest : StringSpec({
    
    // Feature: kyc-demo-app, Property 16: Progress Restoration Round Trip
    "progress persistence round trip should restore equivalent state" {
        checkAll(100, Arb.kycProgress()) { progress ->
            persistenceManager.saveProgress(progress).shouldBeSuccess()
            
            val restored = persistenceManager.loadProgress()
            restored.shouldBeSuccess()
            restored.getOrNull() shouldBe progress
        }
    }
})
```

#### Custom Generators

Property tests require custom generators for domain-specific types:

```kotlin
object KycArbitraries {
    fun Arb.Companion.validPAN(): Arb<String> = arbitrary {
        val letters1 = String(CharArray(5) { ('A'..'Z').random() })
        val digits = String(CharArray(4) { ('0'..'9').random() })
        val letter2 = ('A'..'Z').random()
        "$letters1$digits$letter2"
    }
    
    fun Arb.Companion.validAadhaar(): Arb<String> = arbitrary {
        String(CharArray(12) { ('0'..'9').random() })
    }
    
    fun Arb.Companion.whitespaceString(): Arb<String> = arbitrary {
        val whitespaceChars = listOf(' ', '\t', '\n', '\r')
        val length = (1..20).random()
        String(CharArray(length) { whitespaceChars.random() })
    }
    
    fun Arb.Companion.configuration(): Arb<Configuration> = arbitrary {
        Configuration(
            validationRules = Arb.validationRules().bind(),
            mockScenarios = Arb.mockScenarios().bind(),
            errorSimulations = Arb.errorSimulations().bind(),
            featureFlags = Arb.featureFlags().bind()
        )
    }
    
    fun Arb.Companion.kycProgress(): Arb<KycProgress> = arbitrary {
        KycProgress(
            currentScreen = Arb.enum<KycScreen>().bind().name,
            completedScreens = Arb.list(Arb.enum<KycScreen>()).bind().map { it.name },
            personalDetails = Arb.personalDetails().orNull().bind(),
            panNumber = Arb.validPAN().orNull().bind(),
            aadhaarNumber = Arb.validAadhaar().orNull().bind(),
            timestamp = Arb.long(0L..System.currentTimeMillis()).bind()
        )
    }
}
```

### Integration Testing

Integration tests verify component interactions:

- ViewModel + Use Cases + Repository
- Configuration loading on app startup
- Navigation flow across multiple screens
- Camera module + Image processor + Upload use case

### UI Testing

Compose UI tests verify:
- Screen rendering with different states
- User interactions (button clicks, text input)
- Navigation between screens
- Error state display

### Test Coverage Goals

- Unit test coverage: >80% for domain and data layers
- Property test coverage: All 17 correctness properties implemented
- Integration test coverage: Critical user flows
- UI test coverage: All screens with key interaction paths

### Continuous Testing

- Run unit tests on every commit
- Run property tests (100 iterations) on every PR
- Run integration tests before merge
- Run UI tests on release candidates

