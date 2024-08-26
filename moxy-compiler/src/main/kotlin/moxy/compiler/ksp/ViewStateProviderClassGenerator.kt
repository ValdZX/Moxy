package moxy.compiler.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import moxy.MvpProcessor
import moxy.MvpView
import moxy.ViewStateProvider
import moxy.viewstate.MvpViewState

fun generateViewStateProvider(
    ksClassDeclaration: KSClassDeclaration,
    viewState: ClassName,
    logger: KSPLogger
): FileSpec {
    val className =
        ksClassDeclaration.simpleName.getShortName() + MvpProcessor.VIEW_STATE_PROVIDER_SUFFIX
    logger.info(
        "Generate provider: ${ksClassDeclaration.packageName.getShortName().ifEmpty { "ROOT" }}.$className",
        ksClassDeclaration
    )
    val typeSpec = TypeSpec
        .classBuilder(className)
        .addOriginatingKSFile(ksFile = ksClassDeclaration.containingFile!!)
        .superclass(ViewStateProvider::class)
        .addFunction(viewState.generateGetViewStateMethod())
        .build()
    val packageName = ksClassDeclaration.packageName.asString()
    return FileSpec
        .builder(packageName, className)
        .addType(typeSpec)
        .build()
}

private fun ClassName.generateGetViewStateMethod(): FunSpec {
    val viewState = this
    return FunSpec.builder("getViewState")
        .addAnnotation(Override::class)
        .addModifiers(KModifier.OVERRIDE)
        .returns(
            MvpViewState::class.asClassName()
                .parameterizedBy(WildcardTypeName.producerOf(MvpView::class))
        )
        .apply {
            val packageName= if(viewState.packageName.isNotEmpty()){
                viewState.packageName + "."
            } else ""
            addStatement("return $packageName`${viewState.simpleName}`()")
        }
        .build()
}
