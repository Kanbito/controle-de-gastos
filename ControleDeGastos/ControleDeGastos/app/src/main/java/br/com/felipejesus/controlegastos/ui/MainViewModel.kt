package br.com.felipejesus.controlegastos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.felipejesus.controlegastos.ControleGastosApp
import br.com.felipejesus.controlegastos.data.Categoria
import br.com.felipejesus.controlegastos.data.FinanceRepository
import br.com.felipejesus.controlegastos.data.LancamentoDetalhado
import br.com.felipejesus.controlegastos.data.NotificacaoPendente
import br.com.felipejesus.controlegastos.data.Preferencias
import br.com.felipejesus.controlegastos.data.Tema
import br.com.felipejesus.controlegastos.data.TipoLancamento
import br.com.felipejesus.controlegastos.data.TotalCategoria
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class PontoMensal(val mes: YearMonth, val receitas: Long, val despesas: Long)

data class ResumoMes(
    val mes: YearMonth,
    /** Saldo acumulado de todos os meses anteriores. */
    val saldoAnterior: Long,
    val receitas: Long,
    val despesas: Long,
    val receitasMesAnterior: Long,
    val despesasMesAnterior: Long,
    val porCategoria: List<TotalCategoria>,
    val evolucao: List<PontoMensal>,
) {
    val resultadoMes: Long get() = receitas - despesas
    val saldoAtual: Long get() = saldoAnterior + resultadoMes
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repo: FinanceRepository,
    private val preferencias: Preferencias,
) : ViewModel() {

    private val _mes = MutableStateFlow(YearMonth.now())
    val mes: StateFlow<YearMonth> = _mes.asStateFlow()

    fun mesAnterior() = _mes.update { it.minusMonths(1) }
    fun proximoMes() = _mes.update { it.plusMonths(1) }
    fun mesAtual() { _mes.value = YearMonth.now() }

    val categorias: StateFlow<List<Categoria>> = repo.categorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lancamentos: StateFlow<List<LancamentoDetalhado>> = _mes
        .flatMapLatest { repo.lancamentosDoMes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val resumo: StateFlow<ResumoMes?> = _mes
        .flatMapLatest { m ->
            combine(
                repo.totaisPorCategoria(m),
                repo.saldoAnterior(m),
                repo.totaisMensais(m, 6),
            ) { totais, saldoAnterior, mensais ->
                val chave = DateTimeFormatter.ofPattern("yyyy-MM")
                val porMes = mensais.associateBy { it.mes }
                val evolucao = (5 downTo 0).map { i ->
                    val ym = m.minusMonths(i.toLong())
                    val t = porMes[ym.format(chave)]
                    PontoMensal(ym, t?.receitas ?: 0L, t?.despesas ?: 0L)
                }
                val anterior = evolucao[evolucao.size - 2]
                ResumoMes(
                    mes = m,
                    saldoAnterior = saldoAnterior,
                    receitas = totais.filter { it.tipo == TipoLancamento.RECEITA }.sumOf { it.total },
                    despesas = totais.filter { it.tipo == TipoLancamento.DESPESA }.sumOf { it.total },
                    receitasMesAnterior = anterior.receitas,
                    despesasMesAnterior = anterior.despesas,
                    porCategoria = totais,
                    evolucao = evolucao,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val pendentes: StateFlow<List<NotificacaoPendente>> = repo.notificacoesPendentes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val qtdPendentes: StateFlow<Int> = repo.quantidadePendentes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val tema: StateFlow<Tema> = preferencias.tema
    val capturaNotificacoes: StateFlow<Boolean> = preferencias.capturaNotificacoes

    fun definirTema(tema: Tema) = preferencias.definirTema(tema)
    fun definirCaptura(ativa: Boolean) = preferencias.definirCapturaNotificacoes(ativa)

    fun excluirLancamento(id: Long) {
        viewModelScope.launch { repo.excluirLancamento(id) }
    }

    fun salvarCategoria(categoria: Categoria, aoTerminar: (erro: String?) -> Unit) {
        viewModelScope.launch { aoTerminar(repo.salvarCategoria(categoria)) }
    }

    fun excluirCategoria(categoria: Categoria, aoTerminar: (erro: String?) -> Unit) {
        viewModelScope.launch { aoTerminar(repo.excluirCategoria(categoria)) }
    }

    fun descartarPendente(id: Long) {
        viewModelScope.launch { repo.descartarNotificacao(id) }
    }

    fun limparPendentes() {
        viewModelScope.launch { repo.limparNotificacoes() }
    }

    suspend fun dadosExportacao(mes: YearMonth?) = repo.dadosExportacao(mes)

    val anexos get() = repo.anexos

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ControleGastosApp
                MainViewModel(app.container.repositorio, app.container.preferencias)
            }
        }
    }
}
