package eu.kanade.tachiyomi.extension.ar.dilar

import android.app.Application
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.multisrc.gmanga.Gmanga
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val MIRROR_PREF_KEY = "MIRROR"
private const val MIRROR_PREF_TITLE = "Dilar : Mirror Urls"
private val MIRROR_PREF_ENTRY_VALUES = arrayOf("https://dilar.tube", "https://golden.rest")
private val MIRROR_PREF_DEFAULT_VALUE = MIRROR_PREF_ENTRY_VALUES[0]
private const val RESTART_TACHIYOMI = ".لتطبيق الإعدادات الجديدة Tachiyomi أعد تشغيل"

class Dilar :
    ConfigurableSource, Gmanga(
    "Dilar",
    MIRROR_PREF_DEFAULT_VALUE,
    "ar",
) {
    override fun chaptersRequest(manga: SManga): Request {
        val mangaId = manga.url.substringAfterLast("/")
        return GET("$baseUrl/api/mangas/$mangaId/releases", headers)
    }

    override fun chaptersParse(response: Response): List<SChapter> {
        val releases = response.parseAs<ChapterListDto>().releases
            .filterNot { it.isMonetized }

        return releases.map { it.toSChapter() }
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val mirrorPref = ListPreference(screen.context).apply {
            key = MIRROR_PREF_KEY
            title = MIRROR_PREF_TITLE
            entries = MIRROR_PREF_ENTRY_VALUES
            entryValues = MIRROR_PREF_ENTRY_VALUES
            setDefaultValue(MIRROR_PREF_DEFAULT_VALUE)
            summary = "%s"

            setOnPreferenceChangeListener { _, _ ->
                Toast.makeText(screen.context, RESTART_TACHIYOMI, Toast.LENGTH_LONG).show()
                true
            }
        }
        screen.addPreference(mirrorPref)
    }

    private fun mirrorPref() = when {
        System.getenv("CI") == "true" -> MIRROR_PREF_ENTRY_VALUES.joinToString("#, ")
        else -> preferences.getString(MIRROR_PREF_KEY, MIRROR_PREF_DEFAULT_VALUE)!!
    }

    override val baseUrl = mirrorPref()

    private val preferences: SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)

    override val cdnUrl = baseUrl
}
