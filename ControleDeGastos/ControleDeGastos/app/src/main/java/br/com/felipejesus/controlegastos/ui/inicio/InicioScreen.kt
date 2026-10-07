package br.com.felipejesus.controlegastos.ui.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.felipejesus.controlegastos.R
import br.com.felipejesus.controlegastos.data.TipoLancamento
import br.com.felipejesus.controlegastos.ui.MainViewModel
import br.com.felipejesus.controlegastos.ui.ResumoMes
import br.com.felipejesus.controlegastos.ui.componentes.BarraMes
import br.com.felipejesus.controlegastos.ui.componentes.CartaoSecao
import br.com.felipejesus.controlegastos.ui.componentes.EstadoVazio
import br.com.felipejesus.controlegastos.ui.componentes.Fatia
import br.com.felipejesus.controlegastos.ui.componentes.GraficoBarrasMensal
import br.com.felipejesus.controlegastos.ui.componentes.GraficoRosca
import br.com.felipejesus.controlegastos.ui.componentes.IconeCategoria
import br.com.felipejesus.controlegastos.ui.componentes.Legenda
import br.com.felipejesus.controlegastos.ui.componentes.LinhaLancamento
import br.com.felipejesus.controlegastos.ui.componentes.SeletorMes
import br.com.felipejesus.controlegastos.ui.theme.EstiloValor
import br.com.felipejesus.controlegastos.ui.theme.LocalCoresFinanceiras
import br.com.felipejesus.controlegastos.ui.theme.Ouro
import br.com.felipejesus.controlegastos.ui.theme.OuroClaro
import br.com.felipejesus.controlegastos.util.Datas
import br.com.felipejesus.controlegastos.util.Dinheiro
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    vm: MainViewModel,
    onNovo: () -> Unit,
    onEditar: (Long) -> Unit,
    onVerLancamentos: () -> Unit,
    onCategoria: (Long) -> Unit,
    onPendentes: () -> Unit,
) {
    val mes by vm.mes.collectAsStateWithLifecycle()
    val resumo by vm.resumo.collectAsStateWithLifecycle()
    val lancamentos by vm.lancamentos.collectAsStateWithLifecycle()
    val pendentes by vm.qtdPendentes.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.logo_simbolo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Controle de Gastos")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovo,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Lançar") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { SeletorMes(mes, vm::mesAnterior, vm::proximoMes, vm::mesAtual) }

            if (pendentes > 0) {
                item { AvisoPendentes(pendentes, onPendentes) }
            }

            val r = resumo
            if (r != null) {
                item { CartaoSaldo(r) }
                item { Comparativo(r) }
                item { CartaoCategorias(r, onCategoria) }
                item { CartaoEvolucao(r) }
            }

            item {
                CartaoSecao(
                    titulo = "Últimos lançamentos",
                    acao = { TextButton(onClick = onVerLancamentos) { Text("Ver todos") } },
                ) {
                    if (lancamentos.isEmpty()) {
                        Text(
                            "Nenhum lançamento em ${Datas.nomeMes(mes)}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column {
                            lancamentos.take(5).forEach { item ->
                                LinhaLancamento(item, onClick = { onEditar(item.lancamento.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Cartão principal com a identidade visual da logo (preto e dourado) nos dois temas. */
@Composable
private fun CartaoSaldo(r: ResumoMes) {
    val fundo = Brush.linearGradient(listOf(Color(0xFF151515), Color(0xFF2A2208)))
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .background(fundo)
                .padding(20.dp)
        ) {
            Text("Saldo atual", style = MaterialTheme.typography.labelLarge, color = OuroClaro.copy(alpha = 0.8f))
            Text(
                Dinheiro.formatar(r.saldoAtual),
                style = MaterialTheme.typography.displaySmall.merge(EstiloValor.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize)),
                color = if (r.saldoAtual >= 0) Ouro else Color(0xFFFF8A80),
                maxLines = 1,
            )
            Spacer(Modifier.height(16.dp))
            LinhaResumo("Saldo do mês anterior", Dinheiro.formatar(r.saldoAnterior), Color(0xFFE8E0CC))
            LinhaResumo("Entradas", Dinheiro.formatarComSinal(r.receitas, true), Color(0xFF7BD88F))
            LinhaResumo("Saídas", Dinheiro.formatarComSinal(r.despesas, false), Color(0xFFFF8A80))
            Box(
                Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Ouro.copy(alpha = 0.25f))
            )
            LinhaResumo(
                "Resultado de ${Datas.nomeMes(r.mes)}",
                Dinheiro.formatarComSinal(r.resultadoMes, r.resultadoMes >= 0),
                if (r.resultadoMes >= 0) Color(0xFF7BD88F) else Color(0xFFFF8A80),
            )
        }
    }
}

@Composable
private fun LinhaResumo(rotulo: String, valor: String, cor: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFBDB5A3))
        Text(valor, style = EstiloValor.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = cor)
    }
}

@Composable
private fun Comparativo(r: ResumoMes) {
    if (r.despesasMesAnterior <= 0L || r.despesas <= 0L) return
    val variacao = ((r.despesas - r.despesasMesAnterior) * 100.0 / r.despesasMesAnterior).roundToInt()
    val cores = LocalCoresFinanceiras.current
    val gastouMais = variacao > 0
    val mesAnterior = Datas.nomeMes(r.mes.minusMonths(1))
    val texto = when {
        variacao == 0 -> "Você gastou o mesmo que em $mesAnterior."
        gastouMais -> "Você gastou ${abs(variacao)}% a mais que em $mesAnterior."
        else -> "Você gastou ${abs(variacao)}% a menos que em $mesAnterior."
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (gastouMais) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
            contentDescription = null,
            tint = if (gastouMais) cores.despesa else cores.receita,
        )
        Spacer(Modifier.width(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CartaoCategorias(r: ResumoMes, onCategoria: (Long) -> Unit) {
    val despesas = r.porCategoria.filter { it.tipo == TipoLancamento.DESPESA }
    CartaoSecao(titulo = "Para onde foi o dinheiro") {
        if (despesas.isEmpty()) {
            EstadoVazio(Icons.Filled.PieChart, "Sem saídas neste mês", "Registre gastos para ver a divisão por categoria.")
            return@CartaoSecao
        }
        // Até 5 categorias; o restante vira "Outras".
        val principais = despesas.take(5)
        val outras = despesas.drop(5).sumOf { it.total }
        val fatias = principais.map { Fatia(Color(it.cor), it.total) } +
            if (outras > 0) listOf(Fatia(MaterialTheme.colorScheme.outline, outras)) else emptyList()

        Row(verticalAlignment = Alignment.CenterVertically) {
            GraficoRosca(
                fatias = fatias,
                modifier = Modifier.size(150.dp),
                descricao = "Saídas por categoria",
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Saídas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Dinheiro.formatar(r.despesas),
                        style = EstiloValor.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                principais.forEach { t ->
                    val pct = (t.total * 100.0 / r.despesas).roundToInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCategoria(t.categoriaId) },
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(t.cor))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            t.nome,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text("$pct%", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (outras > 0) {
                    Legenda(MaterialTheme.colorScheme.outline, "Outras · ${(outras * 100.0 / r.despesas).roundToInt()}%")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Maior categoria em destaque
        val maior = principais.first()
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconeCategoria(maior.icone, maior.cor, tamanho = 32.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Maior gasto: ${maior.nome} (${Dinheiro.formatar(maior.total)} em ${maior.quantidade} lançamento(s))",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoEvolucao(r: ResumoMes) {
    val cores = LocalCoresFinanceiras.current
    CartaoSecao(titulo = "Últimos 6 meses") {
        GraficoBarrasMensal(
            r.evolucao.map {
                BarraMes(Datas.mesCurto(it.mes), it.receitas, it.despesas, destaque = it.mes == r.mes)
            }
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legenda(cores.receita, "Entradas")
            Legenda(cores.despesa, "Saídas")
        }
    }
}

@Composable
private fun AvisoPendentes(qtd: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (qtd == 1) "1 valor detectado nas notificações" else "$qtd valores detectados nas notificações",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Toque para revisar e lançar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
