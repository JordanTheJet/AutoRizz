package com.autorizz.dating.di

import android.content.Context
import androidx.room.Room
import com.autorizz.dating.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatingModule {

    @Provides
    @Singleton
    fun provideDatingDb(@ApplicationContext context: Context): DatingDb {
        return Room.databaseBuilder(
            context,
            DatingDb::class.java,
            "autorizz_dating"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSwipePreferenceDao(db: DatingDb): SwipePreferenceDao = db.swipePreferenceDao()

    @Provides
    fun provideMatchDao(db: DatingDb): MatchDao = db.matchDao()

    @Provides
    fun provideConversationMessageDao(db: DatingDb): ConversationMessageDao = db.conversationMessageDao()

    @Provides
    fun provideScheduledDateDao(db: DatingDb): ScheduledDateDao = db.scheduledDateDao()

    @Provides
    fun provideSwipeSessionDao(db: DatingDb): SwipeSessionDao = db.swipeSessionDao()
}
