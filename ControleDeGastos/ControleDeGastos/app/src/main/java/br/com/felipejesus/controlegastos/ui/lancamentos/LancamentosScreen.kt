package br.com.felipejesus.controlegastos.ui.lancamentos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.felipejesus.controlegastos.data.LancamentoDetalhado
import br.com.felipejesus.controlegastos.data.TipoLancamento
import br.com.felipejesus.controlegastos.ui.MainViewModel
import br.com.felipejesus.controlegastos.ui.componentes.EstadoVazio
import br.com.felipejesus.controlegastos.ui.componentes.LinhaLancamento
import br.com.felipejesus.controlegastos.ui.componentes.SeletorMes
import br.com.felipejesus.controlegastos.ui.theme.EstiloValor
import br.com.felipejesus.controlegastos.ui.theme.LocalCoresFinanceiras
import br.com.felipejesus.controlegastos.util.Datas
import br.com.felipejesus.controlegastos.util.Dinheiro
import br.com.felipejesus.controlegastos.util.ExportadorExcel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Filtro(val rotulo: String) { TODOS("Todos"), ENTRADAS("Entradas"), SAIDAS("Saídas") }

/**
 * Lista de lançamentos do mês. Quando [categoriaId] é informado, vira a tela
 * de detalhe de uma categoria (com botão de voltar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LancamentosScreen(
    vm: MainViewModel,
    onEditar: (Long) -> Unit,
    onNovo: () -> Unit,
    categoriaId: Long? = null,
    onVoltar: (() -> Unit)? = null,
) {
    val mes by vm.mes.collectAsStateWithLifecycle()
    val todos by vm.lancamentos.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var filtro by rememberSaveable { mutableStateOf(Filtro.TODOS) }
    var busca by rememberSaveable { mutableStateOf("") }
    var excluirId by rememberSaveable { mutableStateOf<Long?>(null) }

    val categoria = categorias.firstOrNull { it.id == categoriaId }
    val lista = remember(todos, filtro, busca, categoriaId) {
        todos.filter { d ->
            val l = d.lancamento
            (categoriaId == null || l.categoriaId == categoriaId) &&
                when (filtro) {
                    Filtro.TODOS -> true
                    Filtro.ENTRADAS -> l.tipo == TipoLancamento.RECEITA
                    Filtro.SAIDAS -> l.tipo == TipoLancamento.DESPESA
                } &&
                (busca.isBlank() || l.descricao.contains(busca, ignoreCase = true) ||
                    d.categoriaNome.contains(busca, ignoreCase = true) ||
                    l.observacao.contains(busca, ignoreCase = true))
        }
    }

    val exportar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportadorExcel.MIME_XLSX)
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        escopo.launch {
            val ok = runCatching {
                val dados = vm.dadosExportacao(mes)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { ExportadorExcel.escrever(it, dados) }
                        ?: error("sem acesso")
                }
            }.isSuccess
            snackbar.showSnackbar(if (ok) "Planilha exportada com sucesso." else "Não foi possível exportar.")
        }
    }

    Scaffold(
        contentWindowInsets = if (onVoltar == null) WindowInsets(0, 0, 0, 0) else androidx.compose.material3.ScaffoldDefaults.contentWindowInsets,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(categoria?.nome ?: "Lançamentos") },
                navigationIcon = {
                    if (onVoltar != null) {
                        IconButton(onClick = onVoltar) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                },
                actions = {
                    if (categoriaId == null) {
                        IconButton(onClick = { exportar.launch("gastos-$mes.xlsx") }) {
                            Icon(Icons.Filled.FileDownload, contentDescription = "Exportar mês para Excel")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNovo) {
                Icon(Icons.Filled.Add, contentDescription = "Novo lançamento")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
        ) {
            item {
                SeletorMes(mes, vm::mesAnterior, vm::proximoMes, vm::mesAtual)
            }
            if (categoriaId == null) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Filtro.entries.forEach { f ->
                            FilterChip(selected = filtro == f, onClick = { filtro = f }, label = { Text(f.rotulo) })
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    placeholder = { Text("Buscar por descrição ou categoria") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (busca.isNotEmpty()) {
                            IconButton(onClick = { busca = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Limpar busca") }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
            item { TotaisLista(lista) }

            if (lista.isEmpty()) {
                item {
                    EstadoVazio(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        "Nada por aqui",
                        if (todos.isEmpty()) "Toque em + para registrar seu primeiro lançamento de ${Datas.nomeMes(mes)}."
                        else "Nenhum lançamento corresponde aos filtros.",
                    )
                }
            }

            lista.groupBy { it.lancamento.data }.forEach { (data, doDia) ->
                item(key = "dia-$data") {
                    Text(
                        Datas.diaLongo(data),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(doDia, key = { it.lancamento.id }) { item ->
                    LinhaLancamento(
                        item = item,
                        onClick = { onEditar(item.lancamento.id) },
                        trailing = { MenuItem(onEditar = { onEditar(item.lancamento.id) }, onExcluir = { excluirId = item.lancamento.id }) },
                    )
                }
            }
        }
    }

    excluirId?.let { id ->
        AlertDialog(
            onDismissRequest = { excluirId = null },
            title = { Text("Excluir lançamento?") },
            text = { Text("O lançamento e seus anexos serão apagados.") },
            confirmButton = {
                TextButton(onClick = { vm.excluirLancamento(id); excluirId = null }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { excluirId = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun MenuItem(onEditar: () -> Unit, onExcluir: () -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { aberto = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Opções") }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DropdownMenuItem(text = { Text("Editar") }, onClick = { aberto = false; onEditar() })
            DropdownMenuItem(text = { Text("Excluir") }, onClick = { aberto = false; onExcluir() })
        }
    }
}

@Composable
private fun TotaisLista(lista: List<LancamentoDetalhado>) {
    if (lista.isEmpty()) return
    val cores = LocalCoresFinanceiras.current
    val entradas = lista.filter { it.lancamento.tipo == TipoLancamento.RECEITA }.sumOf { it.lancamento.valorCentavos }
    val saidas = lista.filter { it.lancamento.tipo == TipoLancamento.DESPESA }.sumOf { it.lancamento.valorCentavos }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${lista.size} lançamento(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (entradas > 0) Text(Dinheiro.formatarComSinal(entradas, true), style = EstiloValor.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = cores.receita)
            if (saidas > 0) Text(Dinheiro.formatarComSinal(saidas, false), style = EstiloValor.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = cores.despesa)
        }
    }
}
