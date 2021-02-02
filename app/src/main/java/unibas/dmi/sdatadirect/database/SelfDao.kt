package unibas.dmi.sdatadirect.database

import androidx.room.*

/**
 * Intefaces defines functions for queries in order to retrieve data about self from the database
 */

@Dao
interface SelfDao {
    @Query("SELECT * FROM self LIMIT 1")
    fun getSelf(): Self

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg: Self)
}