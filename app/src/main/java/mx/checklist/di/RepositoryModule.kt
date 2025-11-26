package mx.checklist.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.checklist.data.api.Api
import mx.checklist.data.api.ApiClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideApi(): Api {
        return ApiClient.api
    }
}

