# Implementation Plan: KYC Demo Android App

## Overview

This implementation plan breaks down the KYC Demo Android App into discrete coding tasks following Clean Architecture principles. The app will be built using Kotlin and Jetpack Compose, with a focus on creating a working prototype quickly. Tasks are organized to build foundational components first, then implement screens incrementally, and finally wire everything together.

## Tasks

- [x] 1. Set up project structure and core architecture
  - Create Android project with Kotlin and Jetpack Compose
  - Set up Hilt dependency injection
  - Configure Jetpack Navigation Compose
  - Create package structure: presentation/, domain/, data/, di/, navigation/, utils/
  - Add required dependencies: Compose, Hilt, Navigation, Coil, Accompanist Permissions, Kotlinx Serialization
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

- [x] 2. Implement domain layer models and interfaces
  - [x] 2.1 Create domain models
    - Create PersonalDetails, Document, ImageData data classes
    - Create KycScreen enum with all screen types
    - Create DocumentType enum (PAN_CARD, SELFIE)
    - Create ImageFormat enum (JPEG, PNG)
    - _Requirements: 2.1_
  
  - [x] 2.2 Create validation result models
    - Create ValidationResult sealed class (Valid, Invalid)
    - Create ErrorState sealed class (ValidationError, NetworkError, UploadError, OtpError)
    - Create KycError sealed class for domain errors
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_
  
  - [x] 2.3 Create repository interfaces
    - Create KycRepository interface with all KYC operations
    - Create ConfigurationRepository interface
    - Create response data classes (SubmissionResponse, ValidationResponse, UploadResponse, OTPResponse, VerificationResponse)
    - _Requirements: 18.2, 18.3_

- [x] 3. Implement configuration system
  - [x] 3.1 Create configuration models
    - Create Configuration data class with validationRules, mockScenarios, errorSimulations, featureFlags
    - Create ValidationRules with field-specific rule classes
    - Create MockScenarios with responseDelays, successConditions, validOtpCodes
    - Create ErrorSimulations with ErrorScenario list
    - Create FeatureFlags with persistenceEnabled, errorSimulationEnabled
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  
  - [x] 3.2 Implement configuration parser
    - Create ConfigurationParser interface with parse() and format() methods
    - Implement JSON parser using Kotlinx Serialization
    - Add default configuration fallback for invalid files
    - _Requirements: 3.5, 3.6, 20.1, 20.2, 20.3_
  
  - [ ]* 3.3 Write property test for configuration parser
    - **Property 17: Configuration Serialization Round Trip**
    - **Validates: Requirements 20.4**
  
  - [x] 3.4 Implement ConfigurationRepository
    - Create ConfigurationRepositoryImpl with loadConfiguration()
    - Implement getValidationRules(), getMockScenarios(), getErrorSimulations()
    - Load configuration from assets folder on initialization
    - _Requirements: 3.5_

- [x] 4. Implement validation engine
  - [x] 4.1 Create ValidationEngine implementation
    - Implement validateEmail() with RFC 5322 simplified regex
    - Implement validatePhone() with 10-digit validation
    - Implement validatePAN() with AAAAA9999A pattern
    - Implement validateAadhaar() with 12-digit validation
    - Implement validateDateOfBirth() with past date check
    - Implement validateRequired() with non-empty/non-whitespace check
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_
  
  - [ ]* 4.2 Write property tests for validation engine
    - **Property 5: Email Validation**
    - **Property 6: Phone Number Validation**
    - **Property 7: PAN Validation**
    - **Property 8: Aadhaar Validation**
    - **Property 9: Required Field Validation**
    - **Property 10: Date of Birth Validation**
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 6.6**
  
  - [ ]* 4.3 Write unit tests for validation engine
    - Test specific valid/invalid email examples
    - Test specific valid/invalid PAN examples
    - Test specific valid/invalid Aadhaar examples
    - Test edge cases: empty strings, special characters, boundary lengths
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 5. Implement camera module and image processing
  - [x] 5.1 Create CameraManager implementation
    - Implement captureImage() using Accompanist Permissions and Camera API
    - Implement selectFromGallery() using system picker
    - Implement requestCameraPermission() and requestStoragePermission()
    - Handle permission denied scenarios with error messages
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_
  
  - [x] 5.2 Create ImageProcessor implementation
    - Implement validateImage() for format and size checks
    - Implement compressImage() for images exceeding threshold
    - Support JPEG and PNG formats
    - _Requirements: 9.1, 9.2, 9.3, 9.4_
  
  - [ ]* 5.3 Write property tests for image processing
    - **Property 11: Image Size Validation**
    - **Property 12: Image Compression Threshold**
    - **Validates: Requirements 9.2, 9.4**
  
  - [ ]* 5.4 Write unit tests for camera module
    - Test permission request flows
    - Test image validation with specific size examples
    - Test compression with specific threshold examples
    - _Requirements: 8.3, 8.4, 8.5, 9.2, 9.4_

- [x] 6. Implement mock backend service
  - [x] 6.1 Create MockBackendService implementation
    - Implement submitPersonalDetails() with configurable delay
    - Implement validatePAN() with configurable delay
    - Implement uploadDocument() with configurable delay and failure simulation
    - Implement validateAadhaar() with configurable delay
    - Implement sendOTP() with mock 6-digit code generation
    - Implement verifyOTP() with configured valid codes check
    - Implement uploadSelfie() with configurable delay
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5_
  
  - [x] 6.2 Create ErrorSimulator implementation
    - Implement shouldSimulateError() based on ErrorContext
    - Implement getErrorMessage() for configured scenarios
    - Support validation, upload, network, and OTP error types
    - _Requirements: 14.1, 14.3, 14.4, 14.5, 14.6_
  
  - [ ]* 6.3 Write property test for mock backend delays
    - **Property 14: API Response Delays**
    - **Validates: Requirements 14.2**
  
  - [ ]* 6.4 Write unit tests for mock backend
    - Test OTP generation produces 6-digit codes
    - Test OTP verification with valid/invalid codes
    - Test upload simulation with specific delay values
    - Test error simulation trigger conditions
    - _Requirements: 18.4, 14.2_

- [x] 7. Implement persistence manager (feature-flagged)
  - [x] 7.1 Create PersistenceManager implementation
    - Implement saveProgress() using DataStore
    - Implement loadProgress() from DataStore
    - Implement clearProgress()
    - Implement isEnabled() checking feature flag
    - Create KycProgress data class with currentScreen, completedScreens, user data, timestamp
    - _Requirements: 15.1, 15.2, 15.3, 15.4_
  
  - [ ]* 7.2 Write property test for persistence
    - **Property 16: Progress Restoration Round Trip**
    - **Validates: Requirements 15.2**
  
  - [ ]* 7.3 Write unit tests for persistence manager
    - Test save and load with specific progress examples
    - Test feature flag disabled behavior
    - Test clear progress operation
    - _Requirements: 15.1, 15.2, 15.3_

- [x] 8. Implement KycRepository
  - [x] 8.1 Create KycRepositoryImpl
    - Inject MockBackendService and ErrorSimulator
    - Implement all repository interface methods delegating to MockBackendService
    - Apply error simulation before returning responses
    - _Requirements: 18.1, 18.2, 18.3, 18.5_
  
  - [ ]* 8.2 Write unit tests for KycRepository
    - Test repository delegates to mock backend correctly
    - Test error simulation is applied
    - Test response mapping from backend to domain models
    - _Requirements: 18.2, 18.3_

- [x] 9. Implement domain use cases
  - [x] 9.1 Create validation use cases
    - Create ValidatePersonalDetailsUseCase validating all personal fields
    - Create ValidatePANUseCase
    - Create ValidateAadhaarUseCase
    - _Requirements: 5.2, 7.2, 10.2_
  
  - [x] 9.2 Create submission use cases
    - Create SubmitPersonalDetailsUseCase
    - Create UploadDocumentUseCase with image validation
    - Create SendOTPUseCase
    - Create VerifyOTPUseCase
    - Create CaptureSelfieUseCase
    - _Requirements: 5.5, 8.9, 11.7, 12.7_
  
  - [x] 9.3 Create persistence use cases
    - Create SaveProgressUseCase
    - Create LoadProgressUseCase
    - _Requirements: 15.1, 15.2_
  
  - [ ]* 9.4 Write unit tests for use cases
    - Test ValidatePersonalDetailsUseCase with valid/invalid inputs
    - Test UploadDocumentUseCase with valid/invalid images
    - Test VerifyOTPUseCase with valid/invalid OTPs
    - _Requirements: 5.2, 8.9, 11.7_

- [x] 10. Create reusable UI components
  - [x] 10.1 Create form input components
    - Create KycTextField composable with validation support and error display
    - Create KycButton composable with loading state
    - Create LoadingIndicator composable
    - _Requirements: 19.1, 19.4, 19.5_
  
  - [x] 10.2 Create upload and error components
    - Create UploadCard composable with preview, retry, and continue buttons
    - Create ErrorView composable for full-screen errors with retry
    - Create InlineErrorText composable for field errors
    - _Requirements: 19.2, 19.3_
  
  - [x] 10.3 Create OTP input component
    - Create OtpInputField composable with 6 digit fields
    - Implement auto-focus on next field
    - Implement backspace handling
    - _Requirements: 11.1, 11.2_

- [x] 11. Implement UI state models
  - [x] 11.1 Create screen state data classes
    - Create PersonalDetailsState with field values and errors
    - Create PanState with panNumber, error, isValid
    - Create AadhaarState with aadhaarNumber, error, isValid
    - Create OtpState with digits, timeRemaining, canResend, error, isVerifying
    - Create UploadState with imageData, isUploading, error, success
    - _Requirements: 5.1, 7.1, 10.1, 11.1, 8.6_
  
  - [x] 11.2 Create KycUiState
    - Create KycUiState with currentScreen, isLoading, error, and all screen states
    - Create NavigationEvent sealed class
    - _Requirements: 2.1, 2.2_

- [x] 12. Implement KycViewModel
  - [x] 12.1 Create KycViewModel with state management
    - Create KycViewModel extending ViewModel
    - Initialize KycUiState as StateFlow
    - Inject all required use cases
    - Implement state update methods for each screen
    - _Requirements: 1.6, 2.2_
  
  - [x] 12.2 Implement personal details screen logic
    - Implement onPersonalDetailsChanged() updating field values
    - Implement validatePersonalDetails() calling ValidatePersonalDetailsUseCase
    - Implement submitPersonalDetails() calling SubmitPersonalDetailsUseCase
    - Update continue button enabled state based on validation
    - _Requirements: 5.2, 5.3, 5.4, 5.5_
  
  - [x] 12.3 Implement PAN screen logic
    - Implement onPanChanged() updating PAN value
    - Implement validatePAN() calling ValidatePANUseCase
    - Implement submitPAN() navigating to upload screen
    - _Requirements: 7.2, 7.4, 7.5_
  
  - [x] 12.4 Implement document upload logic
    - Implement onCaptureImage() calling CameraManager
    - Implement onSelectFromGallery() calling CameraManager
    - Implement uploadDocument() calling UploadDocumentUseCase
    - Handle permission errors and upload errors
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9_
  
  - [x] 12.5 Implement Aadhaar and OTP logic
    - Implement onAadhaarChanged() with formatting (spaces every 4 digits)
    - Implement validateAadhaar() calling ValidateAadhaarUseCase
    - Implement sendOTP() calling SendOTPUseCase
    - Implement onOtpDigitChanged() with auto-focus logic
    - Implement verifyOTP() calling VerifyOTPUseCase
    - Implement OTP timer countdown
    - Implement resendOTP() functionality
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_
  
  - [x] 12.6 Implement selfie capture logic
    - Implement captureSelfie() calling CaptureSelfieUseCase
    - Implement uploadSelfie() navigating to success screen
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_
  
  - [x] 12.7 Implement navigation and persistence logic
    - Implement navigateNext() based on current screen
    - Implement navigateBack() with Success screen prevention
    - Implement saveProgress() after each screen completion
    - Implement loadProgress() on ViewModel initialization
    - _Requirements: 2.2, 2.3, 2.4, 15.1, 15.2_
  
  - [ ]* 12.8 Write property tests for navigation
    - **Property 1: Screen Sequence Navigation**
    - **Property 2: Backward Navigation Availability**
    - **Validates: Requirements 2.2, 2.3**
  
  - [ ]* 12.9 Write property test for continue button state
    - **Property 4: Continue Button State**
    - **Validates: Requirements 5.4**
  
  - [ ]* 12.10 Write unit tests for KycViewModel
    - Test personal details validation with specific examples
    - Test PAN validation with specific examples
    - Test OTP timer countdown behavior
    - Test navigation flow through specific screen sequences
    - Test error state handling
    - _Requirements: 5.2, 7.2, 11.3, 2.2_

- [x] 13. Implement Login screen
  - [x] 13.1 Create LoginScreen composable
    - Create LoginScreen with app branding and title
    - Add "Start KYC Journey" button
    - Implement button click navigating to Personal Details
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 14. Implement Personal Details screen
  - [x] 14.1 Create PersonalDetailsScreen composable
    - Create PersonalDetailsScreen observing PersonalDetailsState
    - Add KycTextField for Full Name with validation
    - Add KycTextField for Date of Birth with date picker
    - Add KycTextField for Phone Number with validation
    - Add KycTextField for Email with validation
    - Display inline errors for each field
    - Add Continue button enabled when fields have content
    - Implement Continue button click calling submitPersonalDetails()
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [ ]* 14.2 Write property test for field validation
    - **Property 3: Field Validation on Input**
    - **Validates: Requirements 5.2**

- [x] 15. Implement PAN Entry screen
  - [x] 15.1 Create PanEntryScreen composable
    - Create PanEntryScreen observing PanState
    - Add KycTextField for PAN number with uppercase transformation
    - Display format guidance text (AAAAA9999A)
    - Display inline error for invalid PAN
    - Add Continue button enabled when PAN has content
    - Implement Continue button click navigating to PAN Upload
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 16. Implement PAN Upload screen
  - [x] 16.1 Create PanUploadScreen composable
    - Create PanUploadScreen observing UploadState
    - Add "Capture Photo" button calling onCaptureImage()
    - Add "Select from Gallery" button calling onSelectFromGallery()
    - Display image preview when image selected
    - Add Retry button to capture/select different image
    - Add Continue button to proceed with upload
    - Display loading indicator during upload
    - Display upload errors with retry option
    - Implement Continue button click navigating to Aadhaar Entry
    - _Requirements: 8.1, 8.2, 8.6, 8.7, 8.8, 8.9_
  
  - [ ]* 16.2 Write unit tests for upload screen
    - Test permission request flows
    - Test image preview display
    - Test error display and retry
    - _Requirements: 8.3, 8.4, 8.5_

- [x] 17. Implement Aadhaar Entry screen
  - [x] 17.1 Create AadhaarEntryScreen composable
    - Create AadhaarEntryScreen observing AadhaarState
    - Add KycTextField for Aadhaar number with digit-only input
    - Display formatted Aadhaar with spaces (XXXX XXXX XXXX)
    - Display inline error for invalid Aadhaar
    - Add Continue button enabled when Aadhaar has content
    - Implement Continue button click calling sendOTP() and navigating to OTP screen
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [ ]* 17.2 Write property test for Aadhaar formatting
    - **Property 13: Aadhaar Number Formatting**
    - **Validates: Requirements 10.3**

- [x] 18. Implement OTP Verification screen
  - [x] 18.1 Create OtpVerificationScreen composable
    - Create OtpVerificationScreen observing OtpState
    - Add OtpInputField component with 6 digit fields
    - Display countdown timer showing time remaining
    - Add Resend OTP button enabled when timer expires
    - Display loading indicator during OTP verification
    - Display OTP error messages
    - Implement auto-verify when 6 digits entered
    - Implement Resend button click calling resendOTP()
    - Navigate to Selfie screen on successful verification
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_
  
  - [ ]* 18.2 Write unit tests for OTP screen
    - Test auto-focus behavior
    - Test timer countdown
    - Test resend button enable/disable
    - Test auto-verify on 6 digits
    - _Requirements: 11.2, 11.3, 11.6_

- [x] 19. Implement Selfie Capture screen
  - [x] 19.1 Create SelfieCaptureScreen composable
    - Create SelfieCaptureScreen with camera preview
    - Use front-facing camera by default
    - Add Capture button to take selfie
    - Display selfie preview after capture
    - Add Retry button to capture different selfie
    - Add Continue button to proceed with selfie
    - Display loading indicator during upload
    - Implement Continue button click uploading selfie and navigating to Success
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_
  
  - [ ]* 19.2 Write unit tests for selfie screen
    - Test camera preview initialization
    - Test capture and preview flow
    - Test retry functionality
    - _Requirements: 12.2, 12.3, 12.5_

- [x] 20. Implement Success screen
  - [x] 20.1 Create SuccessScreen composable
    - Create SuccessScreen with success icon/animation
    - Display "KYC Complete" success message
    - Add "Done" button to exit or restart
    - Prevent back navigation from Success screen
    - _Requirements: 13.1, 13.2, 13.3, 13.4_

- [x] 21. Implement navigation graph
  - [x] 21.1 Create navigation setup
    - Create NavGraph.kt with NavHost
    - Define Routes object with all screen routes
    - Add navigation composable for each screen
    - Implement navigation actions in KycViewModel
    - Handle back press for each screen
    - Prevent back navigation from Success screen
    - _Requirements: 1.5, 2.1, 2.2, 2.3, 2.4_
  
  - [ ]* 21.2 Write integration tests for navigation
    - Test complete flow from Login to Success
    - Test back navigation through screens
    - Test Success screen back prevention
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 22. Implement dependency injection modules
  - [x] 22.1 Create Hilt modules
    - Create AppModule providing Application-level dependencies
    - Create DataModule providing Repository implementations
    - Create DomainModule providing Use Cases
    - Create ConfigModule providing ConfigurationRepository
    - Annotate Application class with @HiltAndroidApp
    - Annotate ViewModels with @HiltViewModel
    - _Requirements: 1.4_

- [x] 23. Create default configuration file
  - [x] 23.1 Create demo configuration JSON
    - Create kyc_config.json in assets folder
    - Define validation rules for all field types
    - Define mock scenarios with response delays (1-3 seconds)
    - Define error simulations for demo friction points
    - Define feature flags (persistence enabled, error simulation enabled)
    - Add valid OTP codes for testing (e.g., "123456", "000000")
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 14.1, 14.2, 14.3, 14.4, 14.6_

- [x] 24. Implement UI theme and styling
  - [x] 24.1 Create Material3 theme
    - Create Color.kt with fintech color palette (blues, greens for trust)
    - Create Type.kt with typography scale
    - Create Theme.kt with Material3 theme configuration
    - Apply theme to MainActivity
    - _Requirements: 16.1, 16.2_
  
  - [x] 24.2 Add loading and transition animations
    - Add loading indicators to all async operations
    - Add smooth screen transitions using Navigation animations
    - Add button press animations
    - _Requirements: 16.3, 16.4_
  
  - [x] 24.3 Implement responsive layout
    - Test layouts on different screen sizes
    - Add appropriate padding and spacing
    - Ensure scrollable content for small screens
    - _Requirements: 16.5_

- [x] 25. Implement error handling UI
  - [x] 25.1 Create error display components
    - Enhance ErrorView with retry and back buttons
    - Add error icons and colors
    - Implement descriptive error messages for each error type
    - Add error recovery suggestions in messages
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_
  
  - [ ]* 25.2 Write unit tests for error handling
    - Test validation error display
    - Test network error display with retry
    - Test upload error display with retry
    - Test error clearing on resolution
    - _Requirements: 17.1, 17.2, 17.3, 17.5_

- [x] 26. Final integration and polish
  - [x] 26.1 Wire all components together
    - Verify all screens navigate correctly
    - Verify all validations work end-to-end
    - Verify camera and gallery integration works
    - Verify OTP flow works with configured codes
    - Verify persistence saves and restores correctly
    - Verify error simulations trigger as configured
    - _Requirements: 2.1, 2.2, 5.2, 8.1, 8.2, 11.7, 15.1, 15.2, 14.5_
  
  - [x] 26.2 Test complete KYC journey
    - Test happy path: Login → Success with valid inputs
    - Test error scenarios: invalid inputs, upload failures, OTP failures
    - Test back navigation through all screens
    - Test app restart with saved progress
    - _Requirements: 2.1, 2.2, 2.3, 15.2_
  
  - [x] 26.3 Polish UI and UX
    - Review all screens for consistent styling
    - Verify loading states display correctly
    - Verify error messages are clear and helpful
    - Add any missing animations or transitions
    - _Requirements: 16.1, 16.2, 16.3, 16.4_

- [x] 27. Checkpoint - Ensure all tests pass
  - Build successful, app ready for testing

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP delivery
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties across all inputs
- Unit tests validate specific examples and edge cases
- The implementation follows Clean Architecture: domain layer is built first, then data layer, then presentation layer
- Configuration system enables demo customization without code changes
- Mock backend simulates realistic API behavior with configurable delays and errors
- Persistence is feature-flagged and can be enabled/disabled via configuration
