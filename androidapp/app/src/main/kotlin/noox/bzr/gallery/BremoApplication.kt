package noox.bzr.gallery

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import noox.bzr.network.AppLanguage
import noox.bzr.network.BremoHttp

class BremoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BremoHttp.language = {
            val selected = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            AppLanguage.resolve((selected.ifBlank { resources.configuration.locales.toLanguageTags() }).split(','))
        }
    }
}
