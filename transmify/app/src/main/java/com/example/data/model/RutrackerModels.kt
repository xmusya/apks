package com.example.data.model

data class RutrackerTopic(
    val topicId: Long,
    val title: String,
    val category: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val seeds: Int,
    val leeches: Int,
    val date: String,
    val downloadsCount: Int = 0,
    val author: String = "",
    val magnetUrl: String = "",
    val torrentFileUrl: String = ""
)

data class RutrackerCategory(
    val id: Int,
    val nameRu: String,
    val iconName: String = "folder"
)

object RutrackerCategories {
    val ALL = listOf(
        RutrackerCategory(0, "Все разделы"),
        RutrackerCategory(1, "Фильмы и Видео"),
        RutrackerCategory(2, "Сериалы"),
        RutrackerCategory(3, "Музыка (Lossless/MP3)"),
        RutrackerCategory(4, "Игры для ПК"),
        RutrackerCategory(5, "Консольные игры"),
        RutrackerCategory(6, "Программы и Дистрибутивы"),
        RutrackerCategory(7, "Книги и Аудиокниги"),
        RutrackerCategory(8, "Обучающие видео и Курсы"),
        RutrackerCategory(9, "Аниме и Манга")
    )
}
