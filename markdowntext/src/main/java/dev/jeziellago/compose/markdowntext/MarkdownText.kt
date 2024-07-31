package dev.jeziellago.compose.markdowntext

import android.content.Context
import android.os.Build
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import androidx.annotation.FontRes
import androidx.annotation.IdRes
import androidx.compose.foundation.clickable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.TextViewCompat
import coil.ImageLoader
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import java.util.ArrayList

@Deprecated(message = "The parameters `color`, `fontSize`, `textAlign` and `lineHeight` must be part of TextStyle.")
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    linkColor: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    truncateOnTextOverflow: Boolean = false,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    isTextSelectable: Boolean = false,
    autoSizeConfig: AutoSizeConfig? = null,
    @FontRes fontResource: Int? = null,
    style: TextStyle = LocalTextStyle.current,
    @IdRes viewId: Int? = null,
    onClick: (() -> Unit)? = null,
    disableLinkMovementMethod: Boolean = false,
    imageLoader: ImageLoader? = null,
    linkifyMask: Int = Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS or Linkify.WEB_URLS,
    enableSoftBreakAddsNewLine: Boolean = true,
    onLinkClicked: ((String) -> Unit)? = null,
    onTextLayout: ((numLines: Int) -> Unit)? = null
) {

    val mergedStyle = style.merge(
        TextStyle(
            color = color,
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
            textAlign = textAlign,
            lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else style.lineHeight,
        )
    )

    MarkdownText(
        markdown = markdown,
        modifier = modifier,
        linkColor = linkColor,
        truncateOnTextOverflow = truncateOnTextOverflow,
        maxLines = maxLines,
        isTextSelectable = isTextSelectable,
        autoSizeConfig = autoSizeConfig,
        fontResource = fontResource,
        style = mergedStyle,
        viewId = viewId,
        onClick = onClick,
        disableLinkMovementMethod = disableLinkMovementMethod,
        imageLoader = imageLoader,
        linkifyMask = linkifyMask,
        enableSoftBreakAddsNewLine = enableSoftBreakAddsNewLine,
        onLinkClicked = onLinkClicked,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    linkColor: Color = Color.Unspecified,
    truncateOnTextOverflow: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    isTextSelectable: Boolean = false,
    autoSizeConfig: AutoSizeConfig? = null,
    @FontRes fontResource: Int? = null,
    style: TextStyle = LocalTextStyle.current,
    @IdRes viewId: Int? = null,
    onClick: (() -> Unit)? = null,
    // this option will disable all clicks on links, inside the markdown text
    // it also enable the parent view to receive the click event
    disableLinkMovementMethod: Boolean = false,
    imageLoader: ImageLoader? = null,
    linkifyMask: Int = Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS or Linkify.WEB_URLS,
    enableSoftBreakAddsNewLine: Boolean = true,
    onLinkClicked: ((String) -> Unit)? = null,
    onTextLayout: ((numLines: Int) -> Unit)? = null
) {
    val defaultColor: Color = LocalContentColor.current
    val context: Context = LocalContext.current
    val markdownRender: Markwon =
        remember {
            MarkdownRender.create(
                context,
                imageLoader,
                linkifyMask,
                enableSoftBreakAddsNewLine,
                onLinkClicked
            )
        }

    AndroidView(
        modifier = Modifier.clickable(enabled = onClick != null) { onClick?.let { it() } }.then(modifier),
        factory = { factoryContext ->

            if (viewId != null) {
                markdownRender.plugins.firstOrNull { it is TablePlugin }?.apply {
                    (this as TablePlugin).setTag(ArrayList<Int>(), viewId)
                }
            }

            val linkTextColor = linkColor.takeOrElse { style.color.takeOrElse { defaultColor } }

            CustomTextView(factoryContext).apply {
                viewId?.let { id = viewId }
                fontResource?.let { font -> applyFontResource(font) }

                setMaxLines(maxLines)
                setLinkTextColor(linkTextColor.toArgb())

                setTextIsSelectable(isTextSelectable)

                movementMethod = LinkMovementMethod.getInstance()

                if (truncateOnTextOverflow) enableTextOverflow()

                autoSizeConfig?.let { config ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                            this,
                            config.autoSizeMinTextSize,
                            config.autoSizeMaxTextSize,
                            config.autoSizeStepGranularity,
                            config.unit
                        )
                    }
                }
            }
        },
        update = { textView ->
            with(textView) {
                applyTextColor(style.color.takeOrElse { defaultColor }.toArgb())
                applyFontSize(style)
                applyLineHeight(style)
                applyTextDecoration(style)

                minimumHeight = measuredHeight
                minimumWidth = measuredWidth

                with(style) {
                    applyTextAlign(textAlign)
                    fontStyle?.let { applyFontStyle(it) }
                    fontWeight?.let { applyFontWeight(it) }
                    fontFamily?.let { applyFontFamily(it) }
                }
            }
            if (viewId != null) {
                markdownRender.plugins.firstOrNull { it is TablePlugin }?.apply {
                    textView.getTag(viewId)?.let { heights ->
                        if (heights != null && heights is ArrayList<*>) {
                            (this as TablePlugin).setTag(heights as ArrayList<Int>, viewId)
                        } else {
                            (this as TablePlugin).setTag(ArrayList<Int>(), viewId)
                        }
                    }

                }
            }


            markdownRender.setMarkdown(textView, markdown)
            if (disableLinkMovementMethod) {
                textView.movementMethod = null
            }
            if (onTextLayout != null) {
                textView.post {
                    onTextLayout(textView.lineCount)
                }
            }
            textView.maxLines = maxLines
        }
    )
}
