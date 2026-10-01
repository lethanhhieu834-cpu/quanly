package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.InvoiceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceItemDao {
    @Query("SELECT * FROM invoice_items ORDER BY id DESC")
    fun getAllInvoiceItems(): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getItemsByInvoiceId(invoiceId: Long): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceCode = :invoiceCode")
    suspend fun getItemsByInvoiceCodeSync(invoiceCode: String): List<InvoiceItem>

    @Query("SELECT * FROM invoice_items WHERE invoiceCode = :invoiceCode")
    fun getItemsByInvoiceCode(invoiceCode: String): Flow<List<InvoiceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoiceCode = :invoiceCode")
    suspend fun deleteByInvoiceCode(invoiceCode: String)
}
