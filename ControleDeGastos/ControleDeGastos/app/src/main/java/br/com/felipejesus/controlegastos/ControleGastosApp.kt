package br.com.felipejesus.controlegastos

import android.app.Application
import android.content.Context
import br.com.felipejesus.controlegastos.data.AnexoStorage
import br.com.felipejesus.controlegastos.data.AppDatabase
import br.com.felipejesus.controlegastos.data.FinanceRepository
import br.com.felipejesus.controlegastos.data.Preferencias

class ControleGastosApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Injeção de dependências manual: um único lugar cria e compartilha as instâncias. */
class AppContainer(context: Context) {
    val preferencias = Preferencias(context)
    val anexos = AnexoStorage(context)
    private val database by lazy { AppDatabase.criar(context) }
    val repositorio by lazy { FinanceRepository(database, anexos) }
}
