package at.aau.appdev.colorpicker.hilt

import android.content.Context
import androidx.room.Room
import at.aau.appdev.colorpicker.persistence.Database
import at.aau.appdev.colorpicker.persistence.dao.ColorDao
import at.aau.appdev.colorpicker.persistence.dao.PaletteDao
import at.aau.appdev.colorpicker.persistence.dao.PhotoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): Database {
        return Room.databaseBuilder(context, Database::class.java, "database")
            .fallbackToDestructiveMigration(true).build();
    }

    @Provides
    fun provideColorDao(database: Database): ColorDao {
        return database.colorDao()
    }

    @Provides
    fun providePaletteDao(database: Database): PaletteDao {
        return database.paletteDao()
    }

    @Provides
    fun providePhotoDao(database: Database): PhotoDao {
        return database.photoDao()
    }
}
