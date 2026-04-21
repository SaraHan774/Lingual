package com.august.spiritscribe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    // 화면/VM 수명과 분리해 fire-and-forget 백그라운드 작업을 돌릴 수 있게 해주는 앱 전역 스코프.
    // 번역 저장 직후 popBackStack 으로 WriteDiaryViewModel 이 즉시 clear 되면 viewModelScope 가
    // 취소돼 translateAll 의 후속 언어가 PENDING 상태로 남는 문제가 있었다. SupervisorJob 으로
    // 개별 작업 실패가 다른 작업을 취소하지 않도록 한다.
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
