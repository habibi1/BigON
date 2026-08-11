package com.bigon.core.update

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The module's own wiring. A host app gets the whole feature by depending on
 * this module and calling [ForceUpdateGate] — there is nothing to bind on the
 * app side, which is the difference between a reusable module and a snippet to
 * copy.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdateModule {

    @Binds
    @Singleton
    abstract fun bindUpdateSource(impl: PlayUpdateSource): UpdateSource
}
