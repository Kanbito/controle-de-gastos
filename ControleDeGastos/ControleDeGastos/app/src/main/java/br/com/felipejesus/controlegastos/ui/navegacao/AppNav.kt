package br.com.felipejesus.controlegastos.ui.navegacao

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.felipejesus.controlegastos.data.TipoLancamento
import br.com.felipejesus.controlegastos.ui.MainViewModel
import br.com.felipejesus.controlegastos.ui.ajustes.AjustesScreen
import br.com.felipejesus.controlegastos.ui.controle.ControleScreen
import br.com.felipejesus.controlegastos.ui.editar.EditarLancamentoScreen
import br.com.felipejesus.controlegastos.ui.inicio.InicioScreen
import br.com.felipejesus.controlegastos.ui.lancamentos.LancamentosScreen
import br.com.felipejesus.controlegastos.ui.pendentes.PendentesScreen

private enum class Aba(val rota: String, val rotulo: String, val icone: ImageVector) {
    INICIO("inicio", "Início", Icons.Filled.Home),
    LANCAMENTOS("lancamentos", "Lançamentos", Icons.AutoMirrored.Filled.ReceiptLong),
    CONTROLE("controle", "Controle", Icons.Filled.PieChart),
    AJUSTES("ajustes", "Ajustes", Icons.Filled.Settings),
}

private const val ROTA_EDITAR = "editar?id={id}&valor={valor}&tipo={tipo}&desc={desc}&pendente={pendente}"
private const val ROTA_CATEGORIA = "categoria/{id}"
private const val ROTA_PENDENTES = "pendentes"

private fun rotaEditar(
    id: Long = -1,
    valor: Long = 0,
    tipo: TipoLancamento = TipoLancamento.DESPESA,
    desc: String = "",
    pendente: Long = -1,
) = "editar?id=$id&valor=$valor&tipo=${tipo.name}&desc=${Uri.encode(desc)}&pendente=$pendente"

@Composable
fun AppNav() {
    val nav = rememberNavController()
    // ViewModel compartilhado pelas abas (escopo da Activity): mantém o mês selecionado.
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)
    val entrada by nav.currentBackStackEntryAsState()
    val rotaAtual = entrada?.destination?.route
    val mostrarBarra = Aba.entries.any { it.rota == rotaAtual }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar {
                    Aba.entries.forEach { aba ->
                        NavigationBarItem(
                            selected = rotaAtual == aba.rota,
                            onClick = { nav.irParaAba(aba.rota) },
                            icon = { Icon(aba.icone, contentDescription = null) },
                            label = { Text(aba.rotulo) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Aba.INICIO.rota,
            modifier = Modifier.padding(padding),
        ) {
            composable(Aba.INICIO.rota) {
                InicioScreen(
                    vm = vm,
                    onNovo = { nav.navigate(rotaEditar()) },
                    onEditar = { nav.navigate(rotaEditar(id = it)) },
                    onVerLancamentos = { nav.irParaAba(Aba.LANCAMENTOS.rota) },
                    onCategoria = { nav.navigate("categoria/$it") },
                    onPendentes = { nav.navigate(ROTA_PENDENTES) },
                )
            }
            composable(Aba.LANCAMENTOS.rota) {
                LancamentosScreen(
                    vm = vm,
                    onEditar = { nav.navigate(rotaEditar(id = it)) },
                    onNovo = { nav.navigate(rotaEditar()) },
                )
            }
            composable(Aba.CONTROLE.rota) {
                ControleScreen(vm = vm, onCategoria = { nav.navigate("categoria/$it") })
            }
            composable(Aba.AJUSTES.rota) {
                AjustesScreen(vm = vm, onPendentes = { nav.navigate(ROTA_PENDENTES) })
            }
            composable(
                ROTA_CATEGORIA,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { backStack ->
                val id = backStack.arguments?.getLong("id") ?: -1L
                LancamentosScreen(
                    vm = vm,
                    onEditar = { nav.navigate(rotaEditar(id = it)) },
                    onNovo = { nav.navigate(rotaEditar()) },
                    categoriaId = id,
                    onVoltar = { nav.popBackStack() },
                )
            }
            composable(ROTA_PENDENTES) {
                PendentesScreen(
                    vm = vm,
                    onVoltar = { nav.popBackStack() },
                    onLancar = { p ->
                        nav.navigate(
                            rotaEditar(
                                valor = p.valorCentavos,
                                tipo = p.tipoSugerido,
                                desc = p.app,
                                pendente = p.id,
                            )
                        )
                    },
                )
            }
            composable(
                ROTA_EDITAR,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("valor") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("tipo") { type = NavType.StringType; defaultValue = TipoLancamento.DESPESA.name },
                    navArgument("desc") { type = NavType.StringType; defaultValue = "" },
                    navArgument("pendente") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) {
                EditarLancamentoScreen(
                    anexos = vm.anexos,
                    onVoltar = { nav.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.irParaAba(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
