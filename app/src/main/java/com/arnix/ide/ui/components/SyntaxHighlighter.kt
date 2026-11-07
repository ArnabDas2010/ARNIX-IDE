package com.arnix.ide.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.arnix.ide.ui.theme.*
import java.util.regex.Pattern

object SyntaxHighlighter {

    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "interface", "object", "val", "var", "fun",
        "return", "if", "else", "when", "for", "while", "do", "try", "catch", "finally",
        "throw", "override", "public", "private", "protected", "internal", "data", "sealed",
        "open", "abstract", "enum", "companion", "by", "lazy", "null", "true", "false",
        "is", "in", "as", "this", "super", "suspend", "inline", "tailrec"
    )

    private val PYTHON_KEYWORDS = setOf(
        "def", "class", "import", "from", "as", "return", "if", "elif", "else",
        "for", "while", "in", "try", "except", "finally", "with", "yield", "pass",
        "break", "continue", "lambda", "global", "nonlocal", "True", "False", "None"
    )

    private val JS_KEYWORDS = setOf(
        "const", "let", "var", "function", "return", "if", "else", "switch", "case",
        "for", "while", "do", "import", "export", "default", "from", "async", "await",
        "class", "extends", "try", "catch", "finally", "throw", "new", "this", "null",
        "undefined", "true", "false"
    )

    fun highlightCode(code: String, extension: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            val keywords = when (extension.lowercase()) {
                "py" -> PYTHON_KEYWORDS
                "js", "ts", "jsx", "tsx" -> JS_KEYWORDS
                else -> KOTLIN_KEYWORDS
            }

            // Highlight Comments: // or #
            val commentPattern = Pattern.compile("(//.*)|(#.*)|(/\\*[\\s\\S]*?\\*/)")
            val commentMatcher = commentPattern.matcher(code)
            while (commentMatcher.find()) {
                addStyle(
                    SpanStyle(color = SyntaxComment),
                    commentMatcher.start(),
                    commentMatcher.end()
                )
            }

            // Highlight Strings: "..." or '...'
            val stringPattern = Pattern.compile("\".*?\"|'.*?'")
            val stringMatcher = stringPattern.matcher(code)
            while (stringMatcher.find()) {
                addStyle(
                    SpanStyle(color = SyntaxString),
                    stringMatcher.start(),
                    stringMatcher.end()
                )
            }

            // Highlight Numbers: [0-9]+
            val numberPattern = Pattern.compile("\\b\\d+(\\.\\d+)?\\b")
            val numberMatcher = numberPattern.matcher(code)
            while (numberMatcher.find()) {
                addStyle(
                    SpanStyle(color = SyntaxNumber),
                    numberMatcher.start(),
                    numberMatcher.end()
                )
            }

            // Highlight Annotations: @Composable, @Test
            val annotationPattern = Pattern.compile("@[a-zA-Z0-9_]+")
            val annotationMatcher = annotationPattern.matcher(code)
            while (annotationMatcher.find()) {
                addStyle(
                    SpanStyle(color = SyntaxAnnotation, fontWeight = FontWeight.SemiBold),
                    annotationMatcher.start(),
                    annotationMatcher.end()
                )
            }

            // Highlight Keywords
            val wordPattern = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
            val wordMatcher = wordPattern.matcher(code)
            while (wordMatcher.find()) {
                val word = wordMatcher.group()
                if (keywords.contains(word)) {
                    addStyle(
                        SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold),
                        wordMatcher.start(),
                        wordMatcher.end()
                    )
                } else if (word.firstOrNull()?.isUpperCase() == true) {
                    addStyle(
                        SpanStyle(color = SyntaxType),
                        wordMatcher.start(),
                        wordMatcher.end()
                    )
                }
            }
        }
    }
}
