package com.bigon.sinema.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {

    @Binds
    @Singleton
    abstract fun bindCacheOperator(impl: DefaultCacheOperator): CacheOperator
}
