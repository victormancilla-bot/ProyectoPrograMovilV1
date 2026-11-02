package bo.edu.ucb.encuentra.core.data.local

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import bo.edu.ucb.encuentra.core.data.local.dao.AppNotificationDao
import bo.edu.ucb.encuentra.core.data.local.dao.AuditLogDao
import bo.edu.ucb.encuentra.core.data.local.dao.ClaimDao
import bo.edu.ucb.encuentra.core.data.local.dao.CustodyEventDao
import bo.edu.ucb.encuentra.core.data.local.dao.FoundItemDao
import bo.edu.ucb.encuentra.core.data.local.dao.LostReportDao
import bo.edu.ucb.encuentra.core.data.local.dao.MatchDao
import bo.edu.ucb.encuentra.core.data.local.dao.UserDao
import bo.edu.ucb.encuentra.core.data.local.entity.AppNotificationEntity
import bo.edu.ucb.encuentra.core.data.local.entity.AuditLogEntity
import bo.edu.ucb.encuentra.core.data.local.entity.ClaimEntity
import bo.edu.ucb.encuentra.core.data.local.entity.CustodyEventEntity
import bo.edu.ucb.encuentra.core.data.local.entity.FoundItemEntity
import bo.edu.ucb.encuentra.core.data.local.entity.LostReportEntity
import bo.edu.ucb.encuentra.core.data.local.entity.MatchEntity
import bo.edu.ucb.encuentra.core.data.local.entity.UserEntity

/**
 * ⚠️ Dueño único: Integrante A. Agregar una tabla o cambiar una columna exige subir [version] y
 * escribir la migración: ver "Base de datos" en el README.
 */
@Database(
    entities = [
        UserEntity::class,
        LostReportEntity::class,
        FoundItemEntity::class,
        CustodyEventEntity::class,
        MatchEntity::class,
        ClaimEntity::class,
        AuditLogEntity::class,
        AppNotificationEntity::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // v2 (V01): found_items.expected_answers y claims.failed_attempts, columnas nuevas con valor por defecto.
        AutoMigration(from = 1, to = 2)
    ]
)
@ColumnTypeConverters(ListConverters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun lostReportDao(): LostReportDao
    abstract fun foundItemDao(): FoundItemDao
    abstract fun custodyEventDao(): CustodyEventDao
    abstract fun matchDao(): MatchDao
    abstract fun claimDao(): ClaimDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun appNotificationDao(): AppNotificationDao

    companion object {
        const val NAME = "ucb_encuentra.db"
    }
}

// El compilador de Room genera las implementaciones `actual`.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .build()
