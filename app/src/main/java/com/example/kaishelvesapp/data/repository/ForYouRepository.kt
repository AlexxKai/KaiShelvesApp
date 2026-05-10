package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.model.Libro
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class BookRecommendation(
    val book: Libro,
    val reason: String
)

class ForYouRepository(
    private val userListsRepository: UserListsRepository = UserListsRepository(),
    private val bookRepository: BookRepository = BookRepository()
) {

    suspend fun loadRecommendations(): Result<List<BookRecommendation>> {
        return try {
            val sourceBooks = loadSourceBooks()
            if (sourceBooks.isEmpty()) {
                return Result.success(emptyList())
            }

            val existingKeys = sourceBooks.map(::bookKey).toSet()
            val seeds = buildSeeds(sourceBooks)

            val recommendations = coroutineScope {
                seeds.map { seed ->
                    async {
                        val result = bookRepository.searchBooks(genero = null, query = seed.query)
                        result.getOrDefault(emptyList())
                            .filter { candidate ->
                                candidate.titulo.isNotBlank() &&
                                    candidate.autor.isNotBlank() &&
                                    bookKey(candidate) !in existingKeys
                            }
                            .take(6)
                            .map { candidate ->
                                BookRecommendation(
                                    book = candidate,
                                    reason = seed.reason
                                )
                            }
                    }
                }.flatMap { it.await() }
            }

            Result.success(
                recommendations
                    .distinctBy { recommendation -> bookKey(recommendation.book) }
                    .take(24)
            )
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun loadSourceBooks(): List<Libro> {
        val lists = userListsRepository.getUserLists().getOrThrow()
        return lists
            .flatMap { list ->
                userListsRepository.getBooksInList(list.id).getOrDefault(emptyList())
            }
            .distinctBy(::bookKey)
    }

    private fun buildSeeds(books: List<Libro>): List<RecommendationSeed> {
        val genreSeeds = books
            .map { it.genero.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (genre) ->
                RecommendationSeed(
                    query = "subject:$genre",
                    reason = "Mismo género"
                )
            }

        val authorSeeds = books
            .map { it.autor.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (author) ->
                RecommendationSeed(
                    query = "inauthor:$author",
                    reason = "Autor relacionado"
                )
            }

        val eraSeeds = books
            .mapNotNull { it.fechaPublicacion.takeIf { year -> year > 0 } }
            .map { year -> (year / 10) * 10 }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(2)
            .map { (decade) ->
                RecommendationSeed(
                    query = "$decade literature fiction",
                    reason = "Epoca parecida"
                )
            }

        val titleThemeSeeds = books
            .map { it.titulo.trim() }
            .filter { it.isNotBlank() }
            .take(2)
            .map { title ->
                RecommendationSeed(
                    query = title,
                    reason = "Tematica cercana"
                )
            }

        return (genreSeeds + authorSeeds + eraSeeds + titleThemeSeeds)
            .distinctBy { it.query.lowercase() }
            .take(8)
    }

    private fun bookKey(book: Libro): String {
        return book.id.ifBlank { book.isbn }
            .ifBlank { "${book.titulo.lowercase()}-${book.autor.lowercase()}" }
    }
}

private data class RecommendationSeed(
    val query: String,
    val reason: String
)

