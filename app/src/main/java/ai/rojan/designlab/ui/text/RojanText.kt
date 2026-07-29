package ai.rojan.designlab.ui.text

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text as Material3Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * ROJAN Global RTL/LTR Typography — one centralized text-direction system.
 *
 * Resolves RTL-vs-LTR and alignment from each string's own content (first
 * strong Unicode character — the same rule HTML's `dir="auto"` and the
 * Unicode Bidi Algorithm use), not from whatever [androidx.compose.ui.unit.LayoutDirection]
 * happens to be ambient. Before this, every `Text` in the app inherited
 * plain [TextAlign.Start], which resolves to right/left purely off the
 * *device's system locale* RTL-ness — so a Persian string would render
 * left-aligned on an English-locale device, and (more subtly) there was no
 * mechanism at all for a genuinely Latin/English string to render
 * left-aligned inside this Persian-first, RTL-by-locale app. Mixed-script
 * runs within one string (e.g. "استودیو luxe") already shape correctly
 * character-by-character regardless — that's the platform text-shaping
 * layer's job, not this component's; this only decides the paragraph's
 * base direction and alignment.
 *
 * Same name and parameter surface as [androidx.compose.material3.Text] by
 * design: every call site across the app switches to this system by
 * changing one import line (`ai.rojan.designlab.ui.text.Text` instead of
 * `androidx.compose.material3.Text`) — no call site itself needs to
 * change, so the fix is genuinely centralized rather than a per-screen
 * patch.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    val directionality = resolveTextDirectionality(text)
    Material3Text(
        text = text,
        modifier = modifier,
        color = color,
        autoSize = autoSize,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign ?: directionality.textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style.copy(textDirection = directionality.textDirection),
    )
}

/** Resolved base direction + alignment for one piece of text. */
internal data class TextDirectionality(val textDirection: TextDirection, val textAlign: TextAlign)

// Unicode blocks that contain RTL letters relevant to this app (Persian +
// Arabic, per the RTL/LTR requirement — Hebrew/Syriac included for the same
// underlying script family, at no extra cost). Built from explicit hex
// codepoints (Char(0x....)) rather than literal characters or \u escapes,
// so the exact ranges are unambiguous regardless of source-file encoding.
private val RTL_BLOCKS = listOf(
    Char(0x0590)..Char(0x05FF), // Hebrew
    Char(0x0600)..Char(0x06FF), // Arabic (covers core Persian letters + Arabic-Indic digits)
    Char(0x0700)..Char(0x074F), // Syriac
    Char(0x0750)..Char(0x077F), // Arabic Supplement
    Char(0x08A0)..Char(0x08FF), // Arabic Extended-A
    Char(0xFB50)..Char(0xFDFF), // Arabic Presentation Forms-A
    Char(0xFE70)..Char(0xFEFF), // Arabic Presentation Forms-B
)

private fun Char.isStrongRtl(): Boolean = RTL_BLOCKS.any { this in it }

/**
 * First-strong-character direction detection: digits, punctuation, and
 * whitespace are directionally neutral and skipped; the first letter found
 * decides the whole string's base direction. A string with no strong
 * character at all (pure numbers, e.g. a price) defaults RTL/right — this
 * app is Persian-first, and that default matches how numerals already read
 * inside the surrounding Persian UI ("Preserve RTL Persian-first
 * experience", per this project's CLAUDE.md rules).
 */
internal fun resolveTextDirectionality(text: String): TextDirectionality {
    val firstStrong = text.firstOrNull { it.isLetter() }
    return when {
        firstStrong == null || firstStrong.isStrongRtl() -> TextDirectionality(TextDirection.Rtl, TextAlign.Right)
        else -> TextDirectionality(TextDirection.Ltr, TextAlign.Left)
    }
}

/**
 * Same centralized direction system as [Text], for the app's few live text
 * inputs (search box, phone/OTP/name fields) — [androidx.compose.material3.OutlinedTextField]
 * and [androidx.compose.foundation.text.BasicTextField] have no drop-in
 * shadow here (their parameter surfaces are far larger and none of this
 * app's ~5 call sites need one), so those call sites opt in explicitly with
 * `textStyle = LocalTextStyle.current.withDirectionFor(value)` instead of
 * an import swap. Recomputed on every recomposition from the field's
 * current value, so direction follows what the customer is typing right
 * now, not just static labels.
 */
fun TextStyle.withDirectionFor(text: String): TextStyle {
    val directionality = resolveTextDirectionality(text)
    return merge(TextStyle(textDirection = directionality.textDirection, textAlign = directionality.textAlign))
}
