package io.github.fazalaali.supernoteopds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.net.URL

class OpdsTest {
    private val base = URL("https://books.example.com/opds/recent")

    private val xml = """
        <feed xmlns="http://www.w3.org/2005/Atom">
          <link rel="next" href="/opds/recent?page=2" type="application/atom+xml;profile=opds-catalog;kind=acquisition"/>
          <entry>
            <id>urn:book:13</id>
            <title>A/B: Test?</title>
            <author><name>Jane Doe</name></author>
            <link rel="http://opds-spec.org/image" href="/cover/13"/>
            <link rel="http://opds-spec.org/acquisition" href="http://books.example.com/opds/book/13/file.pdf" type="application/pdf"/>
            <link rel="http://opds-spec.org/acquisition/open-access" href="book/13/file" type="application/epub+zip; charset=binary"/>
          </entry>
          <entry>
            <id>urn:book:14</id>
            <title>Mobi only</title>
            <link rel="http://opds-spec.org/acquisition" href="https://cdn.example.org/14.mobi" type="application/x-mobipocket-ebook"/>
          </entry>
        </feed>
    """.trimIndent()

    @Test
    fun parsesFeed() {
        val feed = parseFeed(KXmlParser(), xml.byteInputStream(), base)
        assertEquals(URL("https://books.example.com/opds/recent?page=2"), feed.next)
        assertEquals(2, feed.entries.size)

        val first = feed.entries[0]
        assertEquals("urn:book:13", first.id)
        assertEquals("Jane Doe", first.author)
        assertEquals(2, first.links.size) // cover link ignored
        val (link, ext) = first.pick()!!
        assertEquals("epub", ext) // epub preferred over pdf; MIME params stripped
        assertEquals(URL("https://books.example.com/opds/book/13/file"), link.href)
        // Same-host http link upgraded to https.
        assertEquals("https", first.links[0].href.protocol)

        assertNull(feed.entries[1].pick()) // unsupported format skipped
        assertNull(feed.entries[1].author)
    }

    @Test
    fun doesNotUpgradeOtherHosts() {
        assertEquals("http", resolve(base, "http://other.example.com/x").protocol)
    }

    @Test
    fun syncKeyScopedByServerNotFeed() {
        val recent = Source("https://a.example.com/opds/recent")
        assertEquals(recent.syncKey("urn:1"), Source("https://a.example.com/opds/all").syncKey("urn:1"))
        assertEquals(false, recent.syncKey("urn:1") == Source("https://b.example.com/opds/recent").syncKey("urn:1"))
    }

    @Test
    fun sanitizesFileNames() {
        assertEquals("A_B_ Test_", fileName("A/B: Test?"))
        assertEquals("book", fileName(" ... "))
    }
}
