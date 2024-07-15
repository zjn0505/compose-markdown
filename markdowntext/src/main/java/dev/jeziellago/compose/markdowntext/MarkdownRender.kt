package dev.jeziellago.compose.markdowntext

import android.content.Context
import coil.ImageLoader
import coil.imageLoader
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.core.spans.LastLineSpacingSpan
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import org.commonmark.node.ListItem

internal object MarkdownRender {

    fun create(
        context: Context,
        imageLoader: ImageLoader?,
        linkifyMask: Int,
        enableSoftBreakAddsNewLine: Boolean,
        onLinkClicked: ((String) -> Unit)? = null,
    ): Markwon {
        val spacing = (15 * context.resources.displayMetrics.density + .5f).toInt()

        val coilImageLoader = imageLoader ?: context.imageLoader
        return Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(CoilImagesPlugin.create(context, coilImageLoader))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(LinkifyPlugin.create(linkifyMask))
            .apply {
                if (enableSoftBreakAddsNewLine) {
                    usePlugin(SoftBreakAddsNewLinePlugin.create())
                }
            }
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    // Setting [MarkwonConfiguration.Builder.linkResolver] overrides
                    // Markwon's default behaviour - see Markwon's [LinkResolverDef]
                    // and how it's used in [MarkwonConfiguration.Builder].
                    // Only use it if the client explicitly wants to handle link clicks.
                    onLinkClicked ?: return
                    builder.linkResolver { _, link ->
                        // handle individual clicks on Textview link
                        onLinkClicked.invoke(link)
                    }
                }

                override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                    // This is where you can add custom spans to the Markdown text
                    builder.appendFactory(
                        ListItem::class.java
                    ) { _, _ ->
                        LastLineSpacingSpan(spacing)
                    }
                }
            })
            .build()
    }
}
