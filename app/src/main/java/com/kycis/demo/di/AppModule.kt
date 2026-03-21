package com.kycis.demo.di

import android.content.Context
import com.kycis.demo.data.camera.CameraManager
import com.kycis.demo.data.camera.CameraManagerImpl
import com.kycis.demo.data.camera.ImageProcessor
import com.kycis.demo.data.camera.ImageProcessorImpl
import com.kycis.demo.data.config.ConfigurationParser
import com.kycis.demo.data.config.ErrorSimulations
import com.kycis.demo.data.config.JsonConfigurationParser
import com.kycis.demo.data.config.MockScenarios
import com.kycis.demo.data.mock.ErrorSimulator
import com.kycis.demo.data.mock.ErrorSimulatorImpl
import com.kycis.demo.data.mock.MockBackendService
import com.kycis.demo.data.mock.MockBackendServiceImpl
import com.kycis.demo.data.persistence.PersistenceManager
import com.kycis.demo.data.persistence.PersistenceManagerImpl
import com.kycis.demo.data.repository.ConfigurationRepositoryImpl
import com.kycis.demo.data.repository.KycRepositoryImpl
import com.kycis.demo.domain.repository.ConfigurationRepository
import com.kycis.demo.domain.repository.KycRepository
import com.kycis.demo.domain.validation.ValidationEngine
import com.kycis.demo.domain.validation.ValidationEngineImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindConfigurationRepository(
        impl: ConfigurationRepositoryImpl
    ): ConfigurationRepository

    @Binds
    @Singleton
    abstract fun bindKycRepository(
        impl: KycRepositoryImpl
    ): KycRepository

    @Binds
    @Singleton
    abstract fun bindCameraManager(
        impl: CameraManagerImpl
    ): CameraManager

    @Binds
    @Singleton
    abstract fun bindImageProcessor(
        impl: ImageProcessorImpl
    ): ImageProcessor

    @Binds
    @Singleton
    abstract fun bindPersistenceManager(
        impl: PersistenceManagerImpl
    ): PersistenceManager

    @Binds
    @Singleton
    abstract fun bindMockBackendService(
        impl: MockBackendServiceImpl
    ): MockBackendService

    @Binds
    @Singleton
    abstract fun bindErrorSimulator(
        impl: ErrorSimulatorImpl
    ): ErrorSimulator

    @Binds
    @Singleton
    abstract fun bindValidationEngine(
        impl: ValidationEngineImpl
    ): ValidationEngine
}

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideConfigurationParser(): ConfigurationParser {
        return JsonConfigurationParser()
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideMockScenarios(): MockScenarios = MockScenarios()

    @Provides
    @Singleton
    fun provideErrorSimulations(): ErrorSimulations = ErrorSimulations()
}