package com.marknote.app.editor

import io.noties.prism4j.annotations.PrismBundle

/**
 * prism4j-bundler 注解入口：构建时生成 MarkdownGrammarLocator，
 * 为 Markwon 预览提供常用语言（Markdown/Java/Kotlin/Python/JS 等）的代码高亮语法。
 */
@PrismBundle(
    include = [
        "markdown", "markup", "css", "java", "kotlin",
        "javascript", "json", "python", "sql", "yaml",
        "c", "cpp", "go", "dart", "swift",
    ],
    grammarLocatorClassName = ".MarkdownGrammarLocator",
)
object PrismLanguages
