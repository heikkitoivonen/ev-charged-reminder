package com.evchargedreminder.di

import com.evchargedreminder.data.remote.NominatimApi
import com.evchargedreminder.data.remote.OpenChargeMapApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    @Named("openChargeMap")
    fun provideOpenChargeMapRetrofit(moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.openchargemap.io/v3/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideOpenChargeMapApi(@Named("openChargeMap") retrofit: Retrofit): OpenChargeMapApi =
        retrofit.create(OpenChargeMapApi::class.java)

    @Provides
    @Singleton
    @Named("nominatim")
    fun provideNominatimRetrofit(moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideNominatimApi(@Named("nominatim") retrofit: Retrofit): NominatimApi =
        retrofit.create(NominatimApi::class.java)
}
