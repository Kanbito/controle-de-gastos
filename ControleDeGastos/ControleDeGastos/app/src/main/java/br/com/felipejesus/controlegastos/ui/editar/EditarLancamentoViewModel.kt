package br.com.felipejesus.controlegastos.ui.editar

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.felipejesus.controlegastos.ControleGastosApp
import br.com.felipejesus.controlegastos.data.Anexo
import br.com.felipejesus.controlegastos.data.AnexoStorage
import br.com.felipejesus.controlegastos.data.Categoria
import br.com.felipejesus.controlegastos.data.FinanceRepository
import br.com.felipejesus.controlegastos.data.Lancamento
import br.com.felipejesus.controlegastos.data.TipoLancamento
import br.com.felipejesus.controlegastos.util.Dinheiro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date

data class EstadoEdicao(
    val id: Long = 0,
    val tipo: TipoLancamento = TipoLancamento.DESPESA,
    val digitosValor: String = "",
    val descricao: String = "",
    val categoriaId: Long? = null,
    val data: LocalDate = LocalDate.now(),
    val observacao: String = "",
    val criadoEm: Long = System.currentTimeMillis(),
    val anexosExistentes: List<Anexo> = emptyList(),
    val anexosNovos: List<AnexoStorage.Copiado> = emptyList(),
    val anexosRemovidos: List<Anexo> = emptyList(),
    val carregando: Boolean = true,
    val salvando: Boolean = false,
    val copiandoAnexo: Boolean = false,
    val erro: String? = null,
    val concluido: Boolean = false,
) {
    val valorCentavos: Long get() = Dinheiro.digitosParaCentavos(digitosValor)
    val edicao: Boolean get() = id != 0L
    val anexosVisiveis: List<Anexo> get() = anexosExistentes - anexosRemovidos.toSet()
}

class EditarLancamentoViewModel(
    private val repo: FinanceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val idArg: Long = savedStateHandle.get<Long>("id") ?: -1L
    private val pendenteId: Long = savedStateHandle.get<Long>("pendente") ?: -1L

    private val _estado = MutableStateFlow(EstadoEdicao())
    val estado: StateFlow<EstadoEdicao> = _estado.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = repo.categorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Foto sendo tirada pela câmera (arquivo já criado, aguardando o resultado). */
    private var fotoEmAndamento: String? = null

    init {
        viewModelScope.launch {
            if (idArg > 0) {
                val l = repo.lancamento(idArg)
                if (l == null) {
                    _estado.update { it.copy(carregando = false, erro = "Lançamento não encontrado.", concluido = true) }
                    return@launch
                }
                _estado.value = EstadoEdicao(
                    id = l.id,
                    tipo = l.tipo,
                    digitosValor = l.valorCentavos.toString(),
                    descricao = l.descricao,
                    categoriaId = l.categoriaId,
                    data = l.data,
                    observacao = l.observacao,
                    criadoEm = l.criadoEm,
                    anexosExistentes = repo.anexosDe(l.id),
                    carregando = false,
                )
            } else {
                // Novo lançamento, possivelmente pré-preenchido por uma notificação capturada.
                val valor = savedStateHandle.get<Long>("valor") ?: 0L
                val tipo = runCatching {
                    TipoLancamento.valueOf(savedStateHandle.get<String>("tipo") ?: "DESPESA")
                }.getOrDefault(TipoLancamento.DESPESA)
                val desc = savedStateHandle.get<String>("desc").orEmpty()
                _estado.value = EstadoEdicao(
                    tipo = tipo,
                    digitosValor = if (valor > 0) valor.toString() else "",
                    descricao = desc,
                    carregando = false,
                )
            }
        }
    }

    fun alterarTipo(tipo: TipoLancamento) = _estado.update {
        if (it.tipo == tipo) it else {
            val categoriaValida = categorias.value.any { c -> c.id == it.categoriaId && c.tipo == tipo }
            it.copy(tipo = tipo, categoriaId = if (categoriaValida) it.categoriaId else null)
        }
    }

    fun alterarValor(textoDigitado: String) = _estado.update {
        it.copy(digitosValor = textoDigitado.filter(Char::isDigit).trimStart('0').take(13))
    }

    fun alterarDescricao(v: String) = _estado.update { it.copy(descricao = v.take(120)) }
    fun alterarCategoria(id: Long) = _estado.update { it.copy(categoriaId = id) }
    fun alterarData(d: LocalDate) = _estado.update { it.copy(data = d) }
    fun alterarObservacao(v: String) = _estado.update { it.copy(observacao = v.take(1000)) }
    fun limparErro() = _estado.update { it.copy(erro = null) }

    fun adicionarAnexo(uri: Uri) {
        viewModelScope.launch {
            _estado.update { it.copy(copiandoAnexo = true) }
            try {
                val copiado = repo.anexos.copiar(uri)
                _estado.update { it.copy(anexosNovos = it.anexosNovos + copiado, copiandoAnexo = false) }
            } catch (e: Exception) {
                _estado.update { it.copy(erro = e.message ?: "Não foi possível anexar o arquivo.", copiandoAnexo = false) }
            }
        }
    }

    /** Retorna a Uri onde a câmera deve salvar a foto. */
    fun prepararFoto(): Uri {
        val nome = repo.anexos.novoArquivoFoto()
        fotoEmAndamento = nome
        return repo.anexos.uri(nome)
    }

    fun resultadoFoto(sucesso: Boolean) {
        val nome = fotoEmAndamento ?: return
        fotoEmAndamento = null
        val arquivo = repo.anexos.arquivo(nome)
        if (!sucesso || arquivo.length() == 0L) {
            arquivo.delete()
            return
        }
        val rotulo = "Foto " + SimpleDateFormat("dd-MM-yyyy HH'h'mm", java.util.Locale.ROOT).format(Date()) + ".jpg"
        val copiado = AnexoStorage.Copiado(rotulo, nome, "image/jpeg", arquivo.length())
        _estado.update { it.copy(anexosNovos = it.anexosNovos + copiado) }
    }

    fun removerAnexoExistente(anexo: Anexo) = _estado.update { it.copy(anexosRemovidos = it.anexosRemovidos + anexo) }

    fun removerAnexoNovo(c: AnexoStorage.Copiado) {
        repo.anexos.excluir(c.arquivo)
        _estado.update { it.copy(anexosNovos = it.anexosNovos - c) }
    }

    fun salvar() {
        val e = _estado.value
        if (e.salvando) return
        when {
            e.valorCentavos <= 0 -> { _estado.update { it.copy(erro = "Informe um valor maior que zero.") }; return }
            e.categoriaId == null -> { _estado.update { it.copy(erro = "Escolha uma categoria.") }; return }
        }
        val categoriaNome = categorias.value.firstOrNull { it.id == e.categoriaId }?.nome.orEmpty()
        _estado.update { it.copy(salvando = true) }
        viewModelScope.launch {
            try {
                repo.salvarLancamento(
                    Lancamento(
                        id = e.id,
                        valorCentavos = e.valorCentavos,
                        tipo = e.tipo,
                        categoriaId = e.categoriaId!!,
                        descricao = e.descricao.trim().ifBlank { categoriaNome },
                        data = e.data,
                        observacao = e.observacao.trim(),
                        criadoEm = e.criadoEm,
                    ),
                    novosAnexos = e.anexosNovos,
                    anexosRemovidos = e.anexosRemovidos,
                )
                if (pendenteId > 0) repo.descartarNotificacao(pendenteId)
                _estado.update { it.copy(salvando = false, anexosNovos = emptyList(), concluido = true) }
            } catch (ex: Exception) {
                _estado.update { it.copy(salvando = false, erro = "Erro ao salvar: ${ex.message}") }
            }
        }
    }

    fun excluir() {
        val id = _estado.value.id
        if (id == 0L) return
        viewModelScope.launch {
            repo.excluirLancamento(id)
            _estado.update { it.copy(concluido = true) }
        }
    }

    override fun onCleared() {
        // Saiu sem salvar: apaga os arquivos copiados que não foram vinculados.
        if (!_estado.value.concluido) {
            _estado.value.anexosNovos.forEach { repo.anexos.excluir(it.arquivo) }
        }
        fotoEmAndamento?.let { repo.anexos.excluir(it) }
        super.onCleared()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ControleGastosApp
                EditarLancamentoViewModel(app.container.repositorio, createSavedStateHandle())
            }
        }
    }
}
