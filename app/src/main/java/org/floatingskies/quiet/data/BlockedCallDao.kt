package org.floatingskies.quiet.data

import androidx.room.*

@Dao
interface BlockedCallDao {

    @Query("SELECT * FROM chamadas_bloqueadas ORDER BY dataHora DESC")
    suspend fun listarTodos(): List<BlockedCallEntity>

    @Query("SELECT * FROM chamadas_bloqueadas WHERE telefone = :telefone LIMIT 1")
    suspend fun buscarPorTelefone(telefone: String): BlockedCallEntity?

    @Insert
    suspend fun inserir(call: BlockedCallEntity): Long

    @Update
    suspend fun atualizar(call: BlockedCallEntity)

    /** Registra uma chamada bloqueada: cria o registro se for novo, ou incrementa o contador. */
    suspend fun registrarBloqueio(telefone: String, telefoneOriginal: String) {
        val existente = buscarPorTelefone(telefone)
        if (existente == null) {
            inserir(BlockedCallEntity(
                telefone = telefone,
                telefoneOriginal = telefoneOriginal,
                dataHora = System.currentTimeMillis(),
                vezes = 1
            ))
        } else {
            atualizar(existente.copy(
                vezes = existente.vezes + 1,
                dataHora = System.currentTimeMillis()
            ))
        }
    }

    @Query("DELETE FROM chamadas_bloqueadas")
    suspend fun limparTudo()

    @Query("SELECT COUNT(*) FROM chamadas_bloqueadas")
    suspend fun contar(): Int

    @Query("DELETE FROM chamadas_bloqueadas WHERE id = :id")
    suspend fun deletarPorId(id: Long)

    @Query("SELECT COUNT(*) FROM chamadas_bloqueadas WHERE dataHora >= :startOfDay")
    suspend fun contarHoje(startOfDay: Long = getStartOfDay()): Int

    companion object {
        fun getStartOfDay(): Long {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    }
}