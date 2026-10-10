package dz.netdiag

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val kind: String,   // ping | dns | port | lan | wifi
    val target: String,
    val summary: String
)

@Dao
interface ReportDao {
    @Insert suspend fun insert(r: Report): Long
    @Query("SELECT * FROM reports ORDER BY timestamp DESC LIMIT 200")
    fun observe(): Flow<List<Report>>
    @Query("SELECT * FROM reports ORDER BY timestamp DESC LIMIT 200")
    suspend fun list(): List<Report>
    @Query("DELETE FROM reports") suspend fun clear()
}

@Database(entities = [Report::class], version = 1, exportSchema = false)
abstract class AppDb : androidx.room.RoomDatabase() {
    abstract fun reports(): ReportDao
}
