package com.example.kaishelvesapp.ui.screen.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color

@Composable
fun BookDetailScreen(
    libro: Libro,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onMarkAsRead: (Libro) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, TarnishedGold)
            ) {
                Text("Volver", color = TarnishedGold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Obsidian
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Ficha del volumen",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TarnishedGold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        BookCover(
                            imageUrl = libro.imagen,
                            title = libro.titulo,
                            modifier = Modifier
                                .width(110.dp)
                                .height(160.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = libro.titulo,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TarnishedGold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            DetailLine("Autor", libro.autor)
                            DetailLine("Editorial", libro.editorial)
                            DetailLine("Género", libro.genero)

                            if (libro.fechaPublicacion != 0) {
                                DetailLine("Publicación", libro.fechaPublicacion.toString())
                            }

                            if (libro.paginas != 0) {
                                DetailLine("Páginas", libro.paginas.toString())
                            }

                            if (libro.isbn.isNotBlank()) {
                                DetailLine("ISBN", libro.isbn)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = BloodWine),
                        border = BorderStroke(1.dp, TarnishedGold),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Nota del archivo",
                                style = MaterialTheme.typography.titleMedium,
                                color = TarnishedGold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Puedes incorporar este volumen a tu registro personal de lecturas para conservarlo y puntuarlo más tarde.",
                                color = OldIvory,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onMarkAsRead(libro)
                            scope.launch {
                                snackbarHostState.showSnackbar("Libro añadido a tus lecturas")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = KaiShelvesThemeDefaults.primaryButtonColors()
                    ) {
                        Text("Marcar como leído")
                    }

                    if (libro.pdf.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(libro.pdf))
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("No se pudo abrir el PDF")
                                    }
                                } catch (_: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Enlace PDF no válido")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = KaiShelvesThemeDefaults.secondaryButtonColors()
                        ) {
                            Text("Abrir PDF")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TarnishedGold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory
        )
    }
}