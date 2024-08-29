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
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import moxy.MvpPresenter
import moxy.MvpProcessor.VIEW_STATE_SUFFIX
import moxy.MvpView

class MvpProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val codeGenerator = environment.codeGenerator
    private val logger = environment.logger
    var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }
        val allClasses = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
        val viewVisitor = ViewVisitor(logger, codeGenerator)
        val mvpView = resolver.getClassDeclarationByName(MvpView::class.qualifiedName!!)!!
            .asStarProjectedType()
        val viewStates = allClasses
            .filter {
                mvpView.isAssignableFrom(it.asStarProjectedType())
                        && it.getConstructors().toList().isEmpty()
            }
            .toSet()
            .mapNotNull {
                viewVisitor.visitDeclaration(it, Unit)
            }

        val presenterVisitor = PresenterVisitor(logger, codeGenerator)
        val mvpPresenter = resolver.getClassDeclarationByName(MvpPresenter::class.qualifiedName!!)!!
            .asStarProjectedType()
        allClasses
            .filter { mvpPresenter.isAssignableFrom(it.asStarProjectedType()) && !it.isAbstract() }
            .toSet()
            .forEach {
                it.getViewStateClass()?.let { viewName ->
                    logger.info("$viewName for ${it.qualifiedName?.getQualifier()}")
                    val viewState = viewStates.find { state -> state.simpleName == viewName }
                    if (viewState != null) {
                        presenterVisitor.visitDeclaration(it, viewState)
                    } else {
                        logger.error("ViewState not found for ${it.qualifiedName?.getQualifier()}")
                    }
                }
            }
        invoked = true
        return emptyList()
    }
}

private fun KSClassDeclaration.getViewStateClass(): String? {
    getAllSuperTypes().toList().forEach { superType ->
        superType.arguments.forEach { arg ->
            val declaration = arg.type?.resolve()?.declaration
            if (declaration is KSClassDeclaration) {
                val superTypes = declaration.getAllSuperTypes()
                if (superTypes.any { it.toClassName() == MvpView::class.asClassName() }) {
                    val viewName = declaration.toClassName().simpleName
                    return "$viewName$VIEW_STATE_SUFFIX"
                }
            }
        }
    }
    return null
}