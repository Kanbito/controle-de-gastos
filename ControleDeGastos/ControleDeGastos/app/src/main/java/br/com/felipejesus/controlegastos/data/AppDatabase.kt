package br.com.felipejesus.controlegastos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

class Conversores {
    @TypeConverter
    fun deData(data: LocalDate?): Long? = data?.toEpochDay()

    @TypeConverter
    fun paraData(valor: Long?): LocalDate? = valor?.let(LocalDate::ofEpochDay)
}

@Database(
    entities = [Categoria::class, Lancamento::class, Anexo::class, NotificacaoPendente::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoriaDao(): CategoriaDao
    abstract fun lancamentoDao(): LancamentoDao
    abstract fun anexoDao(): AnexoDao
    abstract fun notificacaoDao(): NotificacaoDao

    companion object {
        fun criar(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "controle_gastos.db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Primeira abertura: cadastra as definições de entradas e saídas padrão.
                        CategoriasPadrao.lista.forEach { c ->
                            db.execSQL(
                                "INSERT INTO categorias (nome, tipo, cor, icone) VALUES (?, ?, ?, ?)",
                                arrayOf<Any>(c.nome, c.tipo.name, c.cor, c.icone)
                            )
                        }
                    }
                })
                .build()
    }
}
