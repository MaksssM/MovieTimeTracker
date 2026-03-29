package com.example.movietime.data.repository

import com.example.movietime.data.db.CinematicUniverse
import com.example.movietime.data.db.FranchiseEntry
import com.example.movietime.data.db.FranchiseSaga

object FranchiseSeedData {

    // ─── Universe IDs ───────────────────────────────────────────────
    private const val MCU = 1L
    private const val DCEU = 2L
    private const val STAR_WARS = 3L
    private const val HARRY_POTTER = 4L
    private const val LOTR = 5L

    // ─── Saga IDs ────────────────────────────────────────────────────
    // MCU
    private const val MCU_INFINITY = 101L
    private const val MCU_MULTIVERSE = 102L

    // DCEU
    private const val DCEU_MAIN = 201L

    // Star Wars
    private const val SW_ORIGINAL = 301L
    private const val SW_PREQUEL = 302L
    private const val SW_SEQUEL = 303L
    private const val SW_ANTHOLOGY = 304L

    // Harry Potter
    private const val HP_MAIN = 401L
    private const val HP_FB = 402L

    // LOTR
    private const val LOTR_MAIN = 501L
    private const val LOTR_HOBBIT = 502L

    // ─── Universes ───────────────────────────────────────────────────
    val universes = listOf(
        CinematicUniverse(
            id = MCU, name = "Marvel Cinematic Universe",
            description = "Взаємопов'язаний кіновсесвіт Marvel Studios, що охоплює понад 30 фільмів та серіалів.",
            logoEmoji = "⚡", accentColorHex = "#E23636", isSeeded = true
        ),
        CinematicUniverse(
            id = DCEU, name = "DC Extended Universe",
            description = "Кіновсесвіт DC Films, заснований Зеком Снайдером у 2013 році.",
            logoEmoji = "🦇", accentColorHex = "#1A3A6B", isSeeded = true
        ),
        CinematicUniverse(
            id = STAR_WARS, name = "Star Wars",
            description = "Всесвіт далеко-далекої галактики від Lucasfilm та Disney.",
            logoEmoji = "⚔️", accentColorHex = "#FFE81F", isSeeded = true
        ),
        CinematicUniverse(
            id = HARRY_POTTER, name = "Wizarding World",
            description = "Чарівний світ Гаррі Поттера та Фантастичних звірів.",
            logoEmoji = "🧙", accentColorHex = "#740001", isSeeded = true
        ),
        CinematicUniverse(
            id = LOTR, name = "Середзем'я",
            description = "Кіноадаптації Толкіна від Peter Jackson — Володар Кілець та Гобіт.",
            logoEmoji = "💍", accentColorHex = "#6B4A1A", isSeeded = true
        )
    )

    // ─── Sagas ───────────────────────────────────────────────────────
    val sagas = listOf(
        // MCU
        FranchiseSaga(id = MCU_INFINITY, universeId = MCU, name = "Сага Нескінченності",
            description = "Фази 1–3 MCU, кульмінація — Месники: Завершення.", displayOrder = 0, yearRange = "2008–2019"),
        FranchiseSaga(id = MCU_MULTIVERSE, universeId = MCU, name = "Сага Мультивсесвіту",
            description = "Фази 4–6, дослідження мультивсесвіту.", displayOrder = 1, yearRange = "2021–"),

        // DCEU
        FranchiseSaga(id = DCEU_MAIN, universeId = DCEU, name = "Основний всесвіт",
            description = "Фільми DCEU від Людини зі сталі до Аквамена.", displayOrder = 0, yearRange = "2013–2023"),

        // Star Wars
        FranchiseSaga(id = SW_PREQUEL, universeId = STAR_WARS, name = "Приквел-трилогія",
            description = "Епізоди I–III: Піднесення та падіння Анакіна Скайвокера.", displayOrder = 0, yearRange = "1999–2005"),
        FranchiseSaga(id = SW_ORIGINAL, universeId = STAR_WARS, name = "Оригінальна трилогія",
            description = "Епізоди IV–VI: Люк Скайвокер і повалення Імперії.", displayOrder = 1, yearRange = "1977–1983"),
        FranchiseSaga(id = SW_SEQUEL, universeId = STAR_WARS, name = "Сиквел-трилогія",
            description = "Епізоди VII–IX: Нове покоління.", displayOrder = 2, yearRange = "2015–2019"),
        FranchiseSaga(id = SW_ANTHOLOGY, universeId = STAR_WARS, name = "Антологія",
            description = "Самостійні фільми всесвіту Star Wars.", displayOrder = 3, yearRange = "2016–2018"),

        // Harry Potter
        FranchiseSaga(id = HP_MAIN, universeId = HARRY_POTTER, name = "Гаррі Поттер",
            description = "Вісім фільмів про учня Гоґвортсу.", displayOrder = 0, yearRange = "2001–2011"),
        FranchiseSaga(id = HP_FB, universeId = HARRY_POTTER, name = "Фантастичні звірі",
            description = "Пригоди Ньюта Саламандера.", displayOrder = 1, yearRange = "2016–2022"),

        // LOTR
        FranchiseSaga(id = LOTR_MAIN, universeId = LOTR, name = "Володар Кілець",
            description = "Трилогія про знищення Персня Всевладдя.", displayOrder = 0, yearRange = "2001–2003"),
        FranchiseSaga(id = LOTR_HOBBIT, universeId = LOTR, name = "Гобіт",
            description = "Трилогія про пригоди Більбо Торбинса.", displayOrder = 1, yearRange = "2012–2014")
    )

    // ─── Entries ─────────────────────────────────────────────────────
    val entries = listOf(

        // ══ MCU — Сага Нескінченності ══

        FranchiseEntry(id = 1001, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Залізна Людина", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 131292, displayOrder = 0,
            movieIds = "1726,10138,68721", totalCount = 3),

        FranchiseEntry(id = 1002, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Неймовірний Халк", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 1724, mediaType = "movie", displayOrder = 1,
            movieIds = "1724", totalCount = 1),

        FranchiseEntry(id = 1003, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Тор", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 131296, displayOrder = 2,
            movieIds = "10195,76338,284053", totalCount = 3),

        FranchiseEntry(id = 1004, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Капітан Америка", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 131295, displayOrder = 3,
            movieIds = "9776,100402,259693", totalCount = 3),

        FranchiseEntry(id = 1005, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Месники", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 86311, displayOrder = 4,
            movieIds = "24428,99861,299536,299534", totalCount = 4),

        FranchiseEntry(id = 1006, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Вартові Галактики", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 284433, displayOrder = 5,
            movieIds = "286217,283995,447365", totalCount = 3),

        FranchiseEntry(id = 1007, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Людина-Павук (MCU)", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 531241, displayOrder = 6,
            movieIds = "315635,429617,634649", totalCount = 3),

        FranchiseEntry(id = 1008, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Доктор Стренж", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 566986, displayOrder = 7,
            movieIds = "284052,566985", totalCount = 2),

        FranchiseEntry(id = 1009, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Чорна Пантера", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 529892, displayOrder = 8,
            movieIds = "284054,505642", totalCount = 2),

        FranchiseEntry(id = 1010, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Мурашник", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 398352, displayOrder = 9,
            movieIds = "102899,343611,519182", totalCount = 3),

        FranchiseEntry(id = 1011, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Капітан Марвел", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 623911, displayOrder = 10,
            movieIds = "299537,577922", totalCount = 2),

        FranchiseEntry(id = 1012, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Вічні", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 524434, mediaType = "movie", displayOrder = 11,
            movieIds = "524434", totalCount = 1),

        FranchiseEntry(id = 1013, universeId = MCU, sagaId = MCU_INFINITY,
            name = "Шан-Чі", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 566525, mediaType = "movie", displayOrder = 12,
            movieIds = "566525", totalCount = 1),

        // ══ MCU — Сага Мультивсесвіту ══

        FranchiseEntry(id = 1101, universeId = MCU, sagaId = MCU_MULTIVERSE,
            name = "Чорна Вдова", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 497698, mediaType = "movie", displayOrder = 0,
            movieIds = "497698", totalCount = 1),

        FranchiseEntry(id = 1102, universeId = MCU, sagaId = MCU_MULTIVERSE,
            name = "Локі", entryType = "STANDALONE_TV",
            tmdbMediaId = 84958, mediaType = "tv", displayOrder = 1,
            movieIds = "84958", totalCount = 1),

        FranchiseEntry(id = 1103, universeId = MCU, sagaId = MCU_MULTIVERSE,
            name = "Ванда/Візіон", entryType = "STANDALONE_TV",
            tmdbMediaId = 85271, mediaType = "tv", displayOrder = 2,
            movieIds = "85271", totalCount = 1),

        FranchiseEntry(id = 1104, universeId = MCU, sagaId = MCU_MULTIVERSE,
            name = "Сокіл і Зимовий Солдат", entryType = "STANDALONE_TV",
            tmdbMediaId = 88396, mediaType = "tv", displayOrder = 3,
            movieIds = "88396", totalCount = 1),

        FranchiseEntry(id = 1105, universeId = MCU, sagaId = MCU_MULTIVERSE,
            name = "Людина-Павук: Немає шляху додому", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 634649, mediaType = "movie", displayOrder = 4,
            movieIds = "634649", totalCount = 1),

        FranchiseEntry(id = 1106, universeId = MCU, sagaId = MCU_MULTIVERSE,
            name = "Тор: Любов і Грім", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 616037, mediaType = "movie", displayOrder = 5,
            movieIds = "616037", totalCount = 1),

        // ══ MCU — Пов'язане (без саги) ══

        FranchiseEntry(id = 1200, universeId = MCU, sagaId = null,
            name = "Люди Ікс (Fox)", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 748, displayOrder = 0, relationshipType = "RELATED",
            movieIds = "36657,36658,67878,127585,127585,246655,320288,337339,372058,407448",
            totalCount = 10,
            note = "Не входять до офіційного MCU, але є частиною спадщини Marvel"),

        FranchiseEntry(id = 1201, universeId = MCU, sagaId = null,
            name = "Людина-Павук (Sony)", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 556, displayOrder = 1, relationshipType = "SPIN_OFF",
            movieIds = "1930,1931,559,116745,429617",
            totalCount = 5,
            note = "Оригінальна та Amazing Spider-Man серії Sony"),

        // ══ DCEU ══

        FranchiseEntry(id = 2001, universeId = DCEU, sagaId = DCEU_MAIN,
            name = "Людина зі сталі", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 49521, mediaType = "movie", displayOrder = 0,
            movieIds = "49521", totalCount = 1),

        FranchiseEntry(id = 2002, universeId = DCEU, sagaId = DCEU_MAIN,
            name = "Бетмен проти Супермена", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 209112, mediaType = "movie", displayOrder = 1,
            movieIds = "209112", totalCount = 1),

        FranchiseEntry(id = 2003, universeId = DCEU, sagaId = DCEU_MAIN,
            name = "Загін самогубців", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 698652, displayOrder = 2,
            movieIds = "297761,436969", totalCount = 2),

        FranchiseEntry(id = 2004, universeId = DCEU, sagaId = DCEU_MAIN,
            name = "Диво-жінка", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 468552, displayOrder = 3,
            movieIds = "297762,381284", totalCount = 2),

        FranchiseEntry(id = 2005, universeId = DCEU, sagaId = DCEU_MAIN,
            name = "Ліга Справедливості", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 791373, mediaType = "movie", displayOrder = 4,
            movieIds = "791373", totalCount = 1,
            note = "Snyder Cut (2021)"),

        FranchiseEntry(id = 2006, universeId = DCEU, sagaId = DCEU_MAIN,
            name = "Аквамен", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 573693, displayOrder = 5,
            movieIds = "297802,572802", totalCount = 2),

        FranchiseEntry(id = 2007, universeId = DCEU, sagaId = DCEU_MAIN,
            name = "Шазам!", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 726987, displayOrder = 6,
            movieIds = "287947,594767", totalCount = 2),

        FranchiseEntry(id = 2008, universeId = DCEU, sagaId = DCEU_MAIN,
            name = "Джокер", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 475557, mediaType = "movie", displayOrder = 7,
            movieIds = "475557", totalCount = 1,
            note = "Не пов'язаний з основним DCEU"),

        // ══ Star Wars — Приквели ══

        FranchiseEntry(id = 3001, universeId = STAR_WARS, sagaId = SW_PREQUEL,
            name = "Епізод I — Прихована загроза", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 1893, mediaType = "movie", displayOrder = 0,
            movieIds = "1893", totalCount = 1),

        FranchiseEntry(id = 3002, universeId = STAR_WARS, sagaId = SW_PREQUEL,
            name = "Епізод II — Атака клонів", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 1894, mediaType = "movie", displayOrder = 1,
            movieIds = "1894", totalCount = 1),

        FranchiseEntry(id = 3003, universeId = STAR_WARS, sagaId = SW_PREQUEL,
            name = "Епізод III — Помста ситхів", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 1895, mediaType = "movie", displayOrder = 2,
            movieIds = "1895", totalCount = 1),

        // ══ Star Wars — Оригінальна трилогія ══

        FranchiseEntry(id = 3011, universeId = STAR_WARS, sagaId = SW_ORIGINAL,
            name = "Епізод IV — Нова надія", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 11, mediaType = "movie", displayOrder = 0,
            movieIds = "11", totalCount = 1),

        FranchiseEntry(id = 3012, universeId = STAR_WARS, sagaId = SW_ORIGINAL,
            name = "Епізод V — Імперія завдає удару у відповідь", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 1891, mediaType = "movie", displayOrder = 1,
            movieIds = "1891", totalCount = 1),

        FranchiseEntry(id = 3013, universeId = STAR_WARS, sagaId = SW_ORIGINAL,
            name = "Епізод VI — Повернення Джедая", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 1892, mediaType = "movie", displayOrder = 2,
            movieIds = "1892", totalCount = 1),

        // ══ Star Wars — Сиквели ══

        FranchiseEntry(id = 3021, universeId = STAR_WARS, sagaId = SW_SEQUEL,
            name = "Епізод VII — Пробудження Сили", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 140607, mediaType = "movie", displayOrder = 0,
            movieIds = "140607", totalCount = 1),

        FranchiseEntry(id = 3022, universeId = STAR_WARS, sagaId = SW_SEQUEL,
            name = "Епізод VIII — Останні Джедаї", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 181808, mediaType = "movie", displayOrder = 1,
            movieIds = "181808", totalCount = 1),

        FranchiseEntry(id = 3023, universeId = STAR_WARS, sagaId = SW_SEQUEL,
            name = "Епізод IX — Підйом Скайвокера", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 181812, mediaType = "movie", displayOrder = 2,
            movieIds = "181812", totalCount = 1),

        // ══ Star Wars — Антологія ══

        FranchiseEntry(id = 3031, universeId = STAR_WARS, sagaId = SW_ANTHOLOGY,
            name = "Вигнанець: Зоряні Війни", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 330459, mediaType = "movie", displayOrder = 0,
            movieIds = "330459", totalCount = 1),

        FranchiseEntry(id = 3032, universeId = STAR_WARS, sagaId = SW_ANTHOLOGY,
            name = "Соло: Зоряні Війни", entryType = "STANDALONE_MOVIE",
            tmdbMediaId = 348350, mediaType = "movie", displayOrder = 1,
            movieIds = "348350", totalCount = 1),

        // ══ Harry Potter ══

        FranchiseEntry(id = 4001, universeId = HARRY_POTTER, sagaId = HP_MAIN,
            name = "Гаррі Поттер (серія 8 фільмів)", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 1241, displayOrder = 0,
            movieIds = "671,672,674,675,767,12445,5765,116705", totalCount = 8),

        FranchiseEntry(id = 4002, universeId = HARRY_POTTER, sagaId = HP_FB,
            name = "Фантастичні звірі", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 435259, displayOrder = 0,
            movieIds = "259316,338952,624860", totalCount = 3),

        // ══ LOTR ══

        FranchiseEntry(id = 5001, universeId = LOTR, sagaId = LOTR_MAIN,
            name = "Володар Кілець (трилогія)", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 119, displayOrder = 0,
            movieIds = "120,121,122", totalCount = 3),

        FranchiseEntry(id = 5002, universeId = LOTR, sagaId = LOTR_HOBBIT,
            name = "Гобіт (трилогія)", entryType = "TMDB_COLLECTION",
            tmdbCollectionId = 121938, displayOrder = 0,
            movieIds = "49051,57158,122917", totalCount = 3)
    )
}
