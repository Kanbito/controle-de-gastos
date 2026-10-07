package br.com.felipejesus.controlegastos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

private const val SELECT_DETALHADO = """
    SELECT l.*, c.nome AS cat_nome, c.cor AS cat_cor, c.icone AS cat_icone,
           (SELECT COUNT(*) FROM anexos a WHERE a.lancamentoId = l.id) AS qtd_anexos
    FROM lancamentos l
    INNER JOIN categorias c ON c.id = l.categoriaId
"""

private const val SELECT_TOTAIS = """
    SELECT c.id AS categoriaId, c.nome AS nome, c.cor AS cor, c.icone AS icone, c.tipo AS tipo,
           SUM(l.valorCentavos) AS total, COUNT(l.id) AS quantidade
    FROM lancamentos l
    INNER JOIN categorias c ON c.id = l.categoriaId
    WHERE l.data BETWEEN :inicio AND :fim
    GROUP BY c.id
    ORDER BY total DESC
"""

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY tipo, nome COLLATE NOCASE")
    fun todas(): Flow<List<Categoria>>

    @Query("SELECT * FROM categorias WHERE id = :id")
    suspend fun porId(id: Long): Categoria?

    @Insert
    suspend fun inserir(categoria: Categoria): Long

    @Update
    suspend fun atualizar(categoria: Categoria)

    @Delete
    suspend fun excluir(categoria: Categoria)

    @Query("SELECT COUNT(*) FROM lancamentos WHERE categoriaId = :categoriaId")
    suspend fun qtdLancamentos(categoriaId: Long): Int
}

@Dao
interface LancamentoDao {
    @Query("$SELECT_DETALHADO WHERE l.data BETWEEN :inicio AND :fim ORDER BY l.data DESC, l.id DESC")
    fun detalhadosNoPeriodo(inicio: LocalDate, fim: LocalDate): Flow<List<LancamentoDetalhado>>

    @Query("$SELECT_DETALHADO WHERE l.data BETWEEN :inicio AND :fim ORDER BY l.data, l.id")
    suspend fun listaParaExportar(inicio: LocalDate, fim: LocalDate): List<LancamentoDetalhado>

    @Query("SELECT * FROM lancamentos WHERE id = :id")
    suspend fun porId(id: Long): Lancamento?

    @Query(SELECT_TOTAIS)
    fun totaisPorCategoria(inicio: LocalDate, fim: LocalDate): Flow<List<TotalCategoria>>

    @Query(SELECT_TOTAIS)
    suspend fun totaisPorCategoriaLista(inicio: LocalDate, fim: LocalDate): List<TotalCategoria>

    /** Saldo acumulado (receitas - despesas) de tudo o que aconteceu antes da data. */
    @Query(
        """SELECT COALESCE(SUM(CASE WHEN tipo = 'RECEITA' THEN valorCentavos ELSE -valorCentavos END), 0)
           FROM lancamentos WHERE data < :antesDe"""
    )
    fun saldoAntesDe(antesDe: LocalDate): Flow<Long>

    /** A coluna "data" guarda o dia em epochDay; multiplicando por 86400 vira timestamp Unix. */
    @Query(
        """SELECT strftime('%Y-%m', data * 86400, 'unixepoch') AS mes,
                  COALESCE(SUM(CASE WHEN tipo = 'RECEITA' THEN valorCentavos ELSE 0 END), 0) AS receitas,
                  COALESCE(SUM(CASE WHEN tipo = 'DESPESA' THEN valorCentavos ELSE 0 END), 0) AS despesas
           FROM lancamentos
           WHERE data BETWEEN :inicio AND :fim
           GROUP BY mes
           ORDER BY mes"""
    )
    fun totaisMensais(inicio: LocalDate, fim: LocalDate): Flow<List<TotalMensal>>

    @Insert
    suspend fun inserir(lancamento: Lancamento): Long

    @Update
    suspend fun atualizar(lancamento: Lancamento)

    @Query("DELETE FROM lancamentos WHERE id = :id")
    suspend fun excluir(id: Long)
}

@Dao
interface AnexoDao {
    @Query("SELECT * FROM anexos WHERE lancamentoId = :lancamentoId ORDER BY id")
    suspend fun doLancamento(lancamentoId: Long): List<Anexo>

    @Insert
    suspend fun inserir(anexo: Anexo): Long

    @Query("DELETE FROM anexos WHERE id = :id")
    suspend fun excluir(id: Long)
}

@Dao
interface NotificacaoDao {
    @Query("SELECT * FROM notificacoes_pendentes ORDER BY recebidaEm DESC")
    fun todas(): Flow<List<NotificacaoPendente>>

    @Query("SELECT COUNT(*) FROM notificacoes_pendentes")
    fun quantidade(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notificacoes_pendentes WHERE texto = :texto AND recebidaEm > :desde")
    suspend fun repetidas(texto: String, desde: Long): Int

    @Insert
    suspend fun inserir(notificacao: NotificacaoPendente): Long

    @Query("DELETE FROM notificacoes_pendentes WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query("DELETE FROM notificacoes_pendentes")
    suspend fun limpar()
}
