package br.com.felipejesus.controlegastos.ui.controle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.felipejesus.controlegastos.data.Categoria
import br.com.felipejesus.controlegastos.data.CategoriasPadrao
import br.com.felipejesus.controlegastos.data.TipoLancamento
import br.com.felipejesus.controlegastos.ui.Icones
import br.com.felipejesus.controlegastos.ui.MainViewModel
import br.com.felipejesus.controlegastos.ui.componentes.IconeCategoria
import br.com.felipejesus.controlegastos.ui.componentes.SeletorMes
import br.com.felipejesus.controlegastos.ui.componentes.corDoTipo
import br.com.felipejesus.controlegastos.ui.theme.EstiloValor
import br.com.felipejesus.controlegastos.util.Datas
import br.com.felipejesus.controlegastos.util.Dinheiro
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Controle das definições: mostra quanto foi movimentado em cada categoria
 * no mês (ex.: "R$ 89,70 em Assinaturas, 3 lançamentos") e permite criar,
 * editar e excluir as definições de entradas e saídas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControleScreen(vm: MainViewModel, onCategoria: (Long) -> Unit) {
    val mes by vm.mes.collectAsStateWithLifecycle()
    val resumo by vm.resumo.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    var aba by rememberSaveable { mutableStateOf(0) }
    val tipo = if (aba == 0) TipoLancamento.DESPESA else TipoLancamento.RECEITA
    var editando by remember { mutableStateOf<Categoria?>(null) }
    var dialogoAberto by remember { mutableStateOf(false) }

    val totais = resumo?.porCategoria.orEmpty().associateBy { it.categoriaId }
    val doTipo = categorias.filter { it.tipo == tipo }
        .sortedByDescending { totais[it.id]?.total ?: 0L }
    val totalTipo = doTipo.sumOf { totais[it.id]?.total ?: 0L }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Controle por categoria") },
                actions = {
                    IconButton(onClick = { editando = null; dialogoAberto = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nova categoria")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            PrimaryTabRow(selectedTabIndex = aba) {
                Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Saídas") })
                Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Entradas") })
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item { SeletorMes(mes, vm::mesAnterior, vm::proximoMes, vm::mesAtual) }
                item {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            if (tipo == TipoLancamento.DESPESA) "Total de saídas em ${Datas.nomeMes(mes)}"
                            else "Total de entradas em ${Datas.nomeMes(mes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            Dinheiro.formatar(totalTipo),
                            style = MaterialTheme.typography.headlineMedium.merge(EstiloValor.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize)),
                            color = corDoTipo(tipo),
                        )
                    }
                }
                items(doTipo, key = { it.id }) { c ->
                    val t = totais[c.id]
                    val valor = t?.total ?: 0L
                    val fracao = if (totalTipo > 0) valor.toFloat() / totalTipo else 0f
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onCategoria(c.id) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconeCategoria(c.icone, c.cor)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    c.nome,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(Dinheiro.formatar(valor), style = EstiloValor)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fracao },
                                color = Color(c.cor),
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when (val q = t?.quantidade ?: 0) {
                                    0 -> "Nenhum lançamento"
                                    1 -> "1 lançamento · ${(fracao * 100).roundToInt()}%"
                                    else -> "$q lançamentos · ${(fracao * 100).roundToInt()}%"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { editando = c; dialogoAberto = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar ${c.nome}")
                        }
                    }
                }
            }
        }
    }

    if (dialogoAberto) {
        DialogoCategoria(
            inicial = editando,
            tipoPadrao = tipo,
            onFechar = { dialogoAberto = false },
            onSalvar = { c ->
                vm.salvarCategoria(c) { erro ->
                    if (erro == null) dialogoAberto = false
                    else escopo.launch { snackbar.showSnackbar(erro) }
                }
            },
            onExcluir = { c ->
                vm.excluirCategoria(c) { erro ->
                    dialogoAberto = false
                    escopo.launch { snackbar.showSnackbar(erro ?: "Categoria excluída.") }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DialogoCategoria(
    inicial: Categoria?,
    tipoPadrao: TipoLancamento,
    onFechar: () -> Unit,
    onSalvar: (Categoria) -> Unit,
    onExcluir: (Categoria) -> Unit,
) {
    var nome by rememberSaveable { mutableStateOf(inicial?.nome.orEmpty()) }
    var tipo by rememberSaveable { mutableStateOf(inicial?.tipo ?: tipoPadrao) }
    var cor by rememberSaveable { mutableStateOf(inicial?.cor ?: CategoriasPadrao.cores.first()) }
    var icone by rememberSaveable { mutableStateOf(inicial?.icone ?: "outros") }
    var confirmarExclusao by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(if (inicial == null) "Nova categoria" else "Editar categoria") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(40) },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (inicial == null) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        val opcoes = listOf(TipoLancamento.DESPESA to "Saída", TipoLancamento.RECEITA to "Entrada")
                        opcoes.forEachIndexed { i, (t, rotulo) ->
                            SegmentedButton(
                                selected = tipo == t,
                                onClick = { tipo = t },
                                shape = SegmentedButtonDefaults.itemShape(i, opcoes.size),
                            ) { Text(rotulo) }
                        }
                    }
                }
                Text("Cor", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoriasPadrao.cores.forEach { c ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (c == cor) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { cor = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (c == cor) Icon(Icons.Filled.Check, contentDescription = "Selecionada", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Text("Ícone", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icones.mapa.forEach { (chave, vetor) ->
                        val selecionado = chave == icone
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (selecionado) Color(cor).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { icone = chave },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                vetor,
                                contentDescription = chave,
                                tint = if (selecionado) Color(cor) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                if (inicial != null) {
                    TextButton(onClick = { confirmarExclusao = true }) {
                        Text("Excluir categoria", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSalvar(
                    (inicial ?: Categoria(nome = "", tipo = tipo, cor = cor, icone = icone))
                        .copy(nome = nome, tipo = tipo, cor = cor, icone = icone)
                )
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )

    if (confirmarExclusao && inicial != null) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir \"${inicial.nome}\"?") },
            text = { Text("Só é possível excluir categorias sem lançamentos.") },
            confirmButton = {
                TextButton(onClick = { confirmarExclusao = false; onExcluir(inicial) }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }
}
