package com.plcoding.dictionary.feature_dictionary.di

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.plcoding.dictionary.feature_dictionary.data.local.Converters
import com.plcoding.dictionary.feature_dictionary.data.local.WordInfoDatabase
import com.plcoding.dictionary.feature_dictionary.data.remote.DictionaryApi
import com.plcoding.dictionary.feature_dictionary.data.repository.WordInfoRepositoryImpl
import com.plcoding.dictionary.feature_dictionary.data.util.GsonParser
import com.plcoding.dictionary.feature_dictionary.domain.repository.WordInfoRepository
import com.plcoding.dictionary.feature_dictionary.domain.use_case.GetWordInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WordInfoModule {

    @Provides
    @Singleton
    fun provideGetWordInfoUseCase(repository: WordInfoRepository): GetWordInfo {
        return GetWordInfo(repository)
    }

    @Provides
    @Singleton
    fun provideWordInfoRepository(
        db: WordInfoDatabase,
        api: DictionaryApi
    ): WordInfoRepository {
        return WordInfoRepositoryImpl(api, db.dao)
    }

    @Provides
    @Singleton
    fun provideWordInfoDatabase(app: Application): WordInfoDatabase {
        return Room.databaseBuilder(
            app, WordInfoDatabase::class.java, "word_db"
        ).addMigrations(object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Perform the migration by renaming the existing table
                database.execSQL("ALTER TABLE `WordInfoEntity` RENAME TO `WordInfoEntity_temp`")

                // Create the new table with the modified schema (without 'origin')
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `WordInfoEntity` " +
                            "(`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "`word` TEXT NOT NULL, " +
                            "`phonetic` TEXT NOT NULL, " +
                            "`meanings` TEXT NOT NULL)"
                )

                // Copy data from the temporary table to the new table, excluding the 'origin' column
                database.execSQL(
                    "INSERT INTO `WordInfoEntity` (`id`, `word`, `phonetic`, `meanings`) " +
                            "SELECT `id`, `word`, `phonetic`, `meanings` FROM `WordInfoEntity_temp`"
                )

                // Drop the temporary table
                database.execSQL("DROP TABLE IF EXISTS `WordInfoEntity_temp`")
            }
        })
            .addTypeConverter(Converters(GsonParser(Gson())))
            .build()
    }

    @Provides
    @Singleton
    fun provideDictionaryApi(): DictionaryApi {
        return Retrofit.Builder()
            .baseUrl(DictionaryApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DictionaryApi::class.java)
    }
}