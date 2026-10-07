package br.com.felipejesus.controlegastos.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class TipoLancamento { RECEITA, DESPESA }

/** Definição de entrada (Salário, Férias...) ou saída (Mercado, Aluguel...). */
@Entity(
    tableName = "categorias",
    indices = [Index(value = ["nome", "tipo"], unique = true)]
)
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val tipo: TipoLancamento,
    /** Cor ARGB usada nos gráficos. */
    val cor: Int,
    /** Chave do ícone (ver ui/Icones.kt). */
    val icone: String,
)

/** Valores são guardados em centavos (Long) para evitar erros de arredondamento. */
@Entity(
    tableName = "lancamentos",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("categoriaId"), Index("data")]
)
data class Lancamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val valorCentavos: Long,
    val tipo: TipoLancamento,
    val categoriaId: Long,
    val descricao: String,
    val data: LocalDate,
    val observacao: String = "",
    val criadoEm: Long = System.currentTimeMillis(),
)

/** Arquivo (imagem, PDF, documento) copiado para o armazenamento interno do app. */
@Entity(
    tableName = "anexos",
    foreignKeys = [
        ForeignKey(
            entity = Lancamento::class,
            parentColumns = ["id"],
            childColumns = ["lancamentoId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("lancamentoId")]
)
data class Anexo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lancamentoId: Long,
    val nomeOriginal: String,
    /** Nome do arquivo dentro de filesDir/anexos. */
    val arquivo: String,
    val mimeType: String,
    val tamanhoBytes: Long,
)

/** Valor detectado em uma notificação, aguardando confirmação do usuário. */
@Entity(tableName = "notificacoes_pendentes")
data class NotificacaoPendente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val valorCentavos: Long,
    val tipoSugerido: TipoLancamento,
    val texto: String,
    val app: String,
    val recebidaEm: Long,
)

// ---------- Resultados de consultas ----------

data class LancamentoDetalhado(
    @Embedded val lancamento: Lancamento,
    @ColumnInfo(name = "cat_nome") val categoriaNome: String,
    @ColumnInfo(name = "cat_cor") val categoriaCor: Int,
    @ColumnInfo(name = "cat_icone") val categoriaIcone: String,
    @ColumnInfo(name = "qtd_anexos") val qtdAnexos: Int,
)

data class TotalCategoria(
    val categoriaId: Long,
    val nome: String,
    val cor: Int,
    val icone: String,
    val tipo: TipoLancamento,
    val total: Long,
    val quantidade: Int,
)

data class TotalMensal(
    /** Formato "yyyy-MM". */
    val mes: String,
    val receitas: Long,
    val despesas: Long,
)
