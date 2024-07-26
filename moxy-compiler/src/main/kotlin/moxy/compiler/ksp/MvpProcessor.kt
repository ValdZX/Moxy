package moxy.compiler.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import moxy.MvpPresenter
import moxy.MvpProcessor.VIEW_STATE_SUFFIX
import moxy.MvpView

class MvpProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val codeGenerator = environment.codeGenerator
    private val logger = environment.logger
    private var isProcessed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (isProcessed) {
            return emptyList()
        }
        isProcessed = true
        val mvpView = resolver.getClassDeclarationByName(MvpView::class.qualifiedName!!)!!
            .asStarProjectedType()
        val mvpPresenter = resolver.getClassDeclarationByName(MvpPresenter::class.qualifiedName!!)!!
            .asStarProjectedType()
        val allClasses = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()

        val viewStates = allClasses
            .filter {
                mvpView.isAssignableFrom(it.asStarProjectedType())
                        && it.getConstructors().toList().isEmpty()
            }
            .toSet()
            .map {
                codeGenerator.generateViewState(it, logger)
            }
        allClasses
            .filter { mvpPresenter.isAssignableFrom(it.asStarProjectedType()) && !it.isAbstract() }
            .toSet()
            .forEach {
                it.getViewStateClass()?.let { viewName ->
                    val viewState = viewStates.find { it.simpleName == viewName }
                        ?: error("ViewState not found for ${it.qualifiedName?.getQualifier()}")
                    codeGenerator.generateViewStateProvider(it, viewState, logger)
                }
            }
        return emptyList()
    }
}

private fun KSClassDeclaration.getViewStateClass(
    superTypes: List<KSType> = getAllSuperTypes().toList(),
    childrenPairs: Map<String, KSTypeArgument?> = emptyMap()
): String? {
    val superType = superTypes.firstOrNull() ?: return null
    val superTypeDeclaration = superType.declaration
    if (superType.toClassName() == MvpPresenter::class.asClassName()) {
        val firstArgument = superType.arguments.firstOrNull()
        var resolve = firstArgument?.type?.resolve() ?: return null
        if (resolve.declaration is KSTypeParameter) {
            val parameterName = firstArgument.type.toString()
            resolve = childrenPairs[parameterName]?.type?.resolve() ?: return null
        }
        if (resolve.declaration is KSTypeParameter) return null
        val viewName = resolve.toClassName().simpleName
        return "$viewName$VIEW_STATE_SUFFIX"
    } else if (superTypeDeclaration is KSClassDeclaration) {
        val pairs = superTypeDeclaration.typeParameters.zip(superType.arguments)
            .associate { (param, arg) ->
                val paramName = param.name.getShortName()
                if (childrenPairs.contains(paramName)) {
                    paramName to childrenPairs[paramName]
                } else {
                    paramName to arg
                }
            }
        return superTypeDeclaration.getViewStateClass(
            superTypes = (superTypes - superType),
            childrenPairs = pairs
        )
    }
    return null
}