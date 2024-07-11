package moxy.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.symbolProcessorProviders
import moxy.compiler.ksp.MvpProcessorProvider
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
fun SourceFile.compile(): KotlinCompilation.Result {
    val compilation = KotlinCompilation().apply {
        sources = listOf(this@compile)
        symbolProcessorProviders = listOf(MvpProcessorProvider())
        inheritClassPath = true
        verbose = false
        kspIncremental = true
    }
    return compilation.compile()
}