package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProductBatch
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductBatchDao {

    @Query("SELECT * FROM product_batches ORDER BY createdAt ASC")
    fun getAllBatches(): Flow<List<ProductBatch>>

    @Query("SELECT * FROM product_batches WHERE productId = :productId AND quantity > 0.0001 ORDER BY createdAt ASC")
    fun getActiveBatchesForProduct(productId: Long): Flow<List<ProductBatch>>

    @Query("SELECT * FROM product_batches WHERE productId = :productId AND quantity > 0.0001 ORDER BY createdAt ASC")
    suspend fun getActiveBatchesForProductDirect(productId: Long): List<ProductBatch>

    @Query("SELECT * FROM product_batches WHERE productId = :productId ORDER BY createdAt ASC")
    suspend fun getAllBatchesForProductDirect(productId: Long): List<ProductBatch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: ProductBatch): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<ProductBatch>): List<Long>

    @Update
    suspend fun updateBatch(batch: ProductBatch)

    @Update
    suspend fun updateBatches(batches: List<ProductBatch>)

    @Delete
    suspend fun deleteBatch(batch: ProductBatch)

    @Query("DELETE FROM product_batches WHERE productId = :productId")
    suspend fun deleteBatchesByProductId(productId: Long)
}
