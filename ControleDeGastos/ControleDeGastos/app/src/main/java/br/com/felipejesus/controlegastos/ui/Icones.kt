package br.com.felipejesus.controlegastos.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** Ícones disponíveis para categorias; a chave é o que fica salvo no banco. */
object Icones {
    val mapa: Map<String, ImageVector> = linkedMapOf(
        "trabalho" to Icons.Filled.Work,
        "ferias" to Icons.Filled.BeachAccess,
        "presente" to Icons.Filled.CardGiftcard,
        "notebook" to Icons.Filled.Laptop,
        "investimento" to Icons.Filled.Savings,
        "reembolso" to Icons.Filled.Replay,
        "dinheiro" to Icons.Filled.AttachMoney,
        "pagamentos" to Icons.Filled.Payments,
        "mercado" to Icons.Filled.ShoppingCart,
        "casa" to Icons.Filled.Home,
        "energia" to Icons.Filled.Bolt,
        "assinatura" to Icons.Filled.Subscriptions,
        "carro" to Icons.Filled.DirectionsCar,
        "combustivel" to Icons.Filled.LocalGasStation,
        "restaurante" to Icons.Filled.Restaurant,
        "saude" to Icons.Filled.LocalHospital,
        "academia" to Icons.Filled.FitnessCenter,
        "educacao" to Icons.Filled.School,
        "lazer" to Icons.Filled.SportsEsports,
        "compras" to Icons.Filled.ShoppingBag,
        "cartao" to Icons.Filled.CreditCard,
        "internet" to Icons.Filled.Wifi,
        "celular" to Icons.Filled.PhoneAndroid,
        "pet" to Icons.Filled.Pets,
        "viagem" to Icons.Filled.Flight,
        "recibo" to Icons.Filled.Receipt,
        "outros" to Icons.Filled.Category,
    )

    fun de(chave: String): ImageVector = mapa[chave] ?: Icons.Filled.Category
}
