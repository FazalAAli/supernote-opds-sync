package io.github.fazalaali.supernoteopds

import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.URL

data class Link(val href: URL, val type: String)
data class Entry(val id: String, val title: String, val author: String?, val links: List<Link>)
data class Feed(val entries: List<Entry>, val next: URL?)

private const val ATOM = "http://www.w3.org/2005/Atom"
private const val ACQUISITION = "http://opds-spec.org/acquisition"

/** Formats the Supernote reader opens, in order of preference. */
val FORMATS = linkedMapOf("application/epub+zip" to "epub", "application/pdf" to "pdf")

/** Best downloadable link for this entry and its file extension, or null if no supported format. */
fun Entry.pick(): Pair<Link, String>? = FORMATS.firstNotNullOfOrNull { (type, ext) ->
    links.find { it.type == type }?.let { it to ext }
}

/**
 * Parses one page of an OPDS 1.x acquisition feed. Only reads what sync needs:
 * entry id/title/author, acquisition links, and the feed's rel="next".
 */
fun parseFeed(p: XmlPullParser, input: InputStream, base: URL): Feed {
    p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
    p.setInput(input, null)
    val entries = mutableListOf<Entry>()
    var next: URL? = null
    var inEntry = false
    var id = ""; var title = ""; var author: String? = null
    val links = mutableListOf<Link>()

    while (p.next() != XmlPullParser.END_DOCUMENT) {
        if (p.namespace != ATOM) continue
        if (p.eventType == XmlPullParser.END_TAG) {
            if (p.name == "entry") {
                if (id.isNotBlank()) entries += Entry(id, title, author, links.toList())
                inEntry = false
            }
            continue
        }
        if (p.eventType != XmlPullParser.START_TAG) continue
        when (p.name) {
            "entry" -> { inEntry = true; id = ""; title = ""; author = null; links.clear() }
            "id" -> if (inEntry) id = p.nextText().trim()
            "title" -> if (inEntry) title = p.nextText().trim()
            "name" -> if (inEntry && author == null) author = p.nextText().trim()
            "link" -> {
                val rel = p.getAttributeValue(null, "rel") ?: ""
                val href = p.getAttributeValue(null, "href") ?: continue
                if (inEntry && rel.startsWith(ACQUISITION)) {
                    // Strip MIME parameters (e.g. "application/epub+zip; charset=...").
                    val type = (p.getAttributeValue(null, "type") ?: "").substringBefore(';').trim()
                    links += Link(resolve(base, href), type)
                } else if (!inEntry && rel == "next") {
                    next = resolve(base, href)
                }
            }
        }
    }
    return Feed(entries, next)
}

/**
 * Resolves [href] against [base]. Some servers behind a TLS proxy emit http:// links to their own
 * host; upgrade those to https rather than leak credentials over cleartext.
 */
fun resolve(base: URL, href: String): URL {
    val url = URL(base, href)
    return if (url.protocol == "http" && base.protocol == "https" && url.host == base.host)
        URL("https", url.host, if (url.port == 80) -1 else url.port, url.file)
    else url
}
