package br.com.felipejesus.controlegastos.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

class FinanceRepository(
    private val db: AppDatabase,
    val anexos: AnexoStorage,
) {
    private val categoriaDao = db.categoriaDao()
    private val lancamentoDao = db.lancamentoDao()
    private val anexoDao = db.anexoDao()
    private val notificacaoDao = db.notificacaoDao()

    // ---------- Consultas ----------

    fun categorias(): Flow<List<Categoria>> = categoriaDao.todas()

    fun lancamentosDoMes(mes: YearMonth): Flow<List<LancamentoDetalhado>> =
        lancamentoDao.detalhadosNoPeriodo(mes.atDay(1), mes.atEndOfMonth())

    fun totaisPorCategoria(mes: YearMonth): Flow<List<TotalCategoria>> =
        lancamentoDao.totaisPorCategoria(mes.atDay(1), mes.atEndOfMonth())

    /** Saldo que "vem" dos meses anteriores (tudo antes do dia 1 do mês). */
    fun saldoAnterior(mes: YearMonth): Flow<Long> = lancamentoDao.saldoAntesDe(mes.atDay(1))

    fun totaisMensais(ate: YearMonth, quantidadeMeses: Int): Flow<List<TotalMensal>> =
        lancamentoDao.totaisMensais(
            ate.minusMonths(quantidadeMeses - 1L).atDay(1),
            ate.atEndOfMonth()
        )

    suspend fun lancamento(id: Long): Lancamento? = lancamentoDao.porId(id)

    suspend fun anexosDe(lancamentoId: Long): List<Anexo> = anexoDao.doLancamento(lancamentoId)

    // ---------- Lançamentos ----------

    suspend fun salvarLancamento(
        lancamento: Lancamento,
        novosAnexos: List<AnexoStorage.Copiado>,
        anexosRemovidos: List<Anexo>,
    ): Long {
        val id = db.withTransaction {
            val id = if (lancamento.id == 0L) {
                lancamentoDao.inserir(lancamento)
            } else {
                lancamentoDao.atualizar(lancamento)
                lancamento.id
            }
            anexosRemovidos.forEach { anexoDao.excluir(it.id) }
            novosAnexos.forEach {
                anexoDao.inserir(
                    Anexo(
                        lancamentoId = id,
                        nomeOriginal = it.nomeOriginal,
                        arquivo = it.arquivo,
                        mimeType = it.mimeType,
                        tamanhoBytes = it.tamanho,
                    )
                )
            }
            id
        }
        anexosRemovidos.forEach { anexos.excluir(it.arquivo) }
        return id
    }

    suspend fun excluirLancamento(id: Long) {
        val arquivos = anexoDao.doLancamento(id)
        lancamentoDao.excluir(id) // anexos no banco saem em cascata
        arquivos.forEach { anexos.excluir(it.arquivo) }
    }

    // ---------- Categorias (definições de entradas e saídas) ----------

    /** Retorna null em caso de sucesso, ou a mensagem de erro. */
    suspend fun salvarCategoria(categoria: Categoria): String? {
        if (categoria.nome.isBlank()) return "Informe um nome."
        return try {
            val limpa = categoria.copy(nome = categoria.nome.trim())
            if (limpa.id == 0L) categoriaDao.inserir(limpa) else categoriaDao.atualizar(limpa)
            null
        } catch (e: SQLiteConstraintException) {
            "Já existe uma categoria com esse nome."
        }
    }

    /** Só exclui se não houver lançamentos usando a categoria. */
    suspend fun excluirCategoria(categoria: Categoria): String? {
        val qtd = categoriaDao.qtdLancamentos(categoria.id)
        if (qtd > 0) return "Existem $qtd lançamento(s) nesta categoria. Mova-os ou exclua-os antes."
        categoriaDao.excluir(categoria)
        return null
    }

    // ---------- Notificações capturadas ----------

    fun notificacoesPendentes(): Flow<List<NotificacaoPendente>> = notificacaoDao.todas()

    fun quantidadePendentes(): Flow<Int> = notificacaoDao.quantidade()

    suspend fun registrarNotificacao(
        valorCentavos: Long,
        tipo: TipoLancamento,
        texto: String,
        app: String,
        recebidaEm: Long,
    ) {
        // Bancos costumam reenviar a mesma notificação; ignora repetições em 10 minutos.
        if (notificacaoDao.repetidas(texto, recebidaEm - 10 * 60_000L) > 0) return
        notificacaoDao.inserir(
            NotificacaoPendente(
                valorCentavos = valorCentavos,
                tipoSugerido = tipo,
                texto = texto.take(500),
                app = app,
                recebidaEm = recebidaEm,
            )
        )
    }

    suspend fun descartarNotificacao(id: Long) = notificacaoDao.excluir(id)

    suspend fun limparNotificacoes() = notificacaoDao.limpar()

    // ---------- Exportação ----------

    data class DadosExportacao(
        val lancamentos: List<LancamentoDetalhado>,
        val totais: List<TotalCategoria>,
    )

    /** mes == null exporta todo o histórico. */
    suspend fun dadosExportacao(mes: YearMonth?): DadosExportacao {
        val inicio = mes?.atDay(1) ?: LocalDate.of(1900, 1, 1)
        val fim = mes?.atEndOfMonth() ?: LocalDate.of(2999, 12, 31)
        return DadosExportacao(
            lancamentoDao.listaParaExportar(inicio, fim),
            lancamentoDao.totaisPorCategoriaLista(inicio, fim),
        )
    }
}
