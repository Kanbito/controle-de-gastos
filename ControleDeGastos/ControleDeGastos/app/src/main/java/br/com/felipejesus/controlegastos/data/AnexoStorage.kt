package br.com.felipejesus.controlegastos.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Guarda os anexos dentro do armazenamento privado do app (filesDir/anexos).
 * Assim eles continuam disponíveis mesmo que o arquivo original seja apagado,
 * e nenhum outro app consegue lê-los sem permissão explícita (FileProvider).
 */
class AnexoStorage(private val context: Context) {

    data class Copiado(
        val nomeOriginal: String,
        val arquivo: String,
        val mimeType: String,
        val tamanho: Long,
    )

    private val pasta: File
        get() = File(context.filesDir, "anexos").apply { mkdirs() }

    fun arquivo(nome: String): File = File(pasta, nome)

    fun excluir(nome: String) {
        arquivo(nome).delete()
    }

    fun uri(nome: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo(nome))

    /** Cria um arquivo vazio para a câmera gravar a foto. */
    fun novoArquivoFoto(): String {
        val nome = "foto_${UUID.randomUUID()}.jpg"
        arquivo(nome).createNewFile()
        return nome
    }

    suspend fun copiar(uri: Uri): Copiado = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var nome = "anexo"
        var tamanhoInformado = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val iNome = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (iNome >= 0 && !c.isNull(iNome)) nome = c.getString(iNome)
                    val iTam = c.getColumnIndex(OpenableColumns.SIZE)
                    if (iTam >= 0 && !c.isNull(iTam)) tamanhoInformado = c.getLong(iTam)
                }
            }
        if (tamanhoInformado > LIMITE_BYTES) throw IOException("O arquivo passa de 20 MB.")

        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val extensao = nome.substringAfterLast('.', "").take(8).lowercase()
        val destino = arquivo(UUID.randomUUID().toString() + if (extensao.isNotEmpty()) ".$extensao" else "")

        val entrada = resolver.openInputStream(uri) ?: throw IOException("Não foi possível ler o arquivo.")
        entrada.use { input -> destino.outputStream().use { input.copyTo(it) } }

        if (destino.length() > LIMITE_BYTES) {
            destino.delete()
            throw IOException("O arquivo passa de 20 MB.")
        }
        Copiado(nome, destino.name, mime, destino.length())
    }

    companion object {
        const val LIMITE_BYTES = 20L * 1024 * 1024
    }
}
