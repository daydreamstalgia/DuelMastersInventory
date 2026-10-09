package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color

@Composable
fun HyperlinkText(
    url: String,
    text: String,
    modifier: Modifier = Modifier,
    linkColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current

    val annotatedString = buildAnnotatedString {
        pushStringAnnotation(tag = "URL", annotation = url)
        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            append(text)
        }
        pop()
    }

    Text(
        text = annotatedString,
        modifier = modifier.clickable {
            annotatedString.getStringAnnotations(tag = "URL", start = 0, end = annotatedString.length)
                .firstOrNull()?.let { stringAnnotation ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stringAnnotation.item))
                    context.startActivity(intent)
                }
        }
    )
}
