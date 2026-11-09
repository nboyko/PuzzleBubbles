package eu.bfss.puzzlebubbles

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class SoundBackupRulesTest {
    @Test fun audioAndItsSharedMetadataAreExcludedFromBackupAndTransferTogether() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (resource in listOf(R.xml.backup_rules, R.xml.data_extraction_rules)) {
            val sections = mutableMapOf<String, MutableSet<Pair<String, String>>>()
            context.resources.getXml(resource).use { parser ->
                var section = "legacy"
                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG) {
                        if (parser.name in listOf("cloud-backup", "device-transfer")) section = parser.name
                        if (parser.name == "exclude") sections.getOrPut(section) { mutableSetOf() }.add(
                            parser.getAttributeValue(null, "domain") to parser.getAttributeValue(null, "path"))
                    }
                    parser.next()
                }
            }
            val required = if (resource == R.xml.backup_rules) listOf("legacy") else listOf("cloud-backup", "device-transfer")
            for (section in required) {
                val excludes = sections[section].orEmpty()
                assertTrue("$section must exclude private audio", "file" to "library/" in excludes)
                for (suffix in listOf("", "-wal", "-shm", "-journal")) {
                    assertTrue("$section must exclude Sounds metadata", "database" to "photos.db$suffix" in excludes)
                }
            }
        }
    }
}
