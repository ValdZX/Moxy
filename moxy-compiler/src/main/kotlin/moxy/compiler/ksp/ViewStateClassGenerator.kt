package moxy.compiler.ksp

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo
import moxy.MvpProcessor
import moxy.viewstate.MvpViewState
import moxy.viewstate.ViewCommand
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType

data class ViewStateFun(
    val declaration: KSFunctionDeclaration,
    var uniqueSuffix: String = ""
)

fun CodeGenerator.generateViewState(
    ksClassDeclaration: KSClassDeclaration,
    logger: KSPLogger
): ClassName {
    val packageName = ksClassDeclaration.packageName.asString()
    val typeVariables = ksClassDeclaration.typeParameters.map { it.toTypeVariableName() }
    val nameWithTypeVariables = if (typeVariables.isEmpty()) {
        ksClassDeclaration.toClassName()
    } else {
        ksClassDeclaration.toClassName().parameterizedBy(typeVariables)
    }
    val typeName = ksClassDeclaration.simpleName.getShortName() + MvpProcessor.VIEW_STATE_SUFFIX
    logger.info(
        "Generate state: ${ksClassDeclaration.packageName.getShortName()}.$typeName",
        ksClassDeclaration
    )
    val defaultStrategy = ksClassDeclaration.annotations.find {
        it.annotationType.element.toString() == StateStrategyType::class.java.name
    }?.annotationType?.resolve()?.toClassName() ?: AddToEndSingleStrategy::class.asClassName()
    val classBuilder = TypeSpec.classBuilder(typeName)
        .addOriginatingKSFile(ksFile = ksClassDeclaration.containingFile!!)
        .superclass(MvpViewState::class.asClassName().parameterizedBy(nameWithTypeVariables))
        .addSuperinterface(nameWithTypeVariables)
        .addTypeVariables(typeVariables)
    val strategyToImport = mutableSetOf<ClassName>()
    val funks = ksClassDeclaration.getAllFunctions()
        .filter { !it.isConstructor() && it.returnType?.resolve()?.toClassName() == Unit::class.asClassName() }
        .map { ViewStateFun(it) }.toList()
    addUniqueSuffixToMethodsWithTheSameName(funks)
    funks.forEach { funk ->
        val declaration = funk.declaration
        val uniqueSuffix = funk.uniqueSuffix
        val commandClassName = declaration.simpleName.getShortName()
            .replaceFirstChar { it.uppercase() } + uniqueSuffix + "Command"
        val commandClass = declaration.generateCommandClass(
            viewTypeName = nameWithTypeVariables,
            defaultStrategy = defaultStrategy,
            classTypeVariables = typeVariables,
            commandClassName = commandClassName,
            strategyToImport = strategyToImport,
            logger = logger
        )
        classBuilder.addType(commandClass)
        val commandFun =
            declaration.generateFun(typeVariables, commandClassName, uniqueSuffix, logger)
        classBuilder.addFunction(commandFun)
    }

    FileSpec
        .builder(packageName, typeName)
        .addStrategyImports(strategyToImport)
        .addType(classBuilder.build())
        .build()
        .writeTo(this, Dependencies(true))
    return ClassName(packageName, typeName)
}

private fun FileSpec.Builder.addStrategyImports(strategyToImport: MutableSet<ClassName>): FileSpec.Builder {
    strategyToImport.forEach { addImport(it.packageName, it.simpleName) }
    return this
}

private fun KSFunctionDeclaration.generateCommandClass(
    viewTypeName: TypeName,
    defaultStrategy: ClassName,
    classTypeVariables: List<TypeVariableName>,
    commandClassName: String,
    strategyToImport: MutableSet<ClassName>,
    logger: KSPLogger
): TypeSpec {
    logger.info("Generate command class: $commandClassName", this)
    val argumentsString = parameters.joinToString { it.name!!.getShortName() }
    val name = this.simpleName.getShortName()
    val applyMethod = FunSpec.builder("apply")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("mvpView", viewTypeName)
        .addStatement("mvpView.${name}($argumentsString)")
        .build()

    val typeVariables = typeParameters.map { it.toTypeVariableName() }
    val classBuilder = TypeSpec.classBuilder(commandClassName)
        .addModifiers(KModifier.INNER)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameters(parameters.toParameterSpec(classTypeVariables))
                .build()
        )
        .addTypeVariables(typeVariables)
        .superclass(ViewCommand::class.asClassName().parameterizedBy(viewTypeName))
        .addSuperclassConstructorParameter(
            generateCommandConstructor(defaultStrategy, strategyToImport)
        )
        .addFunction(applyMethod)
    parameters.toPropertySpec(classTypeVariables).forEach {
        classBuilder.addProperty(it)
    }
    return classBuilder.build()
}

private fun KSFunctionDeclaration.generateCommandConstructor(
    defaultStrategy: ClassName,
    strategyToImport: MutableSet<ClassName>
): CodeBlock {
    val findAnnotation = annotations
        .find {
            it.annotationType.resolve().toTypeName()
                .toString() == StateStrategyType::class.java.name
        }
        ?: annotations.flatMap {
            annotations.first().annotationType.resolve().declaration.annotations
        }.find {
            it.annotationType.resolve().toTypeName()
                .toString() == StateStrategyType::class.java.name
        }
    val (strategy, tag) = findAnnotation
        ?.let {
            val value = (it.arguments[0].value as KSType).toClassName()
            val tag = it.arguments[1].value
            value to tag.toString().ifEmpty { simpleName.getShortName() }
        }
        ?: (defaultStrategy to simpleName.getShortName())
    strategyToImport.add(strategy)
    return CodeBlock.Builder()
        .addStatement("\"$tag\", $strategy::class.java")
        .build()
}

private fun List<KSValueParameter>.toPropertySpec(classTypeVariables: List<TypeVariableName>): List<PropertySpec> {
    val typeVars = classTypeVariables.map { it.name }
    return map {
        val type = if (it.type.toString() in typeVars) {
            TypeVariableName(it.type.toString())
        } else {
            it.type.toTypeName()
        }
        val name = it.name!!.getShortName()
        PropertySpec.builder(name, type)
            .initializer(name)
            .build()
    }
}

private fun List<KSValueParameter>.toParameterSpec(classTypeVariables: List<TypeVariableName>): List<ParameterSpec> {
    val typeVars = classTypeVariables.map { it.name }
    return map {
        val type = if (it.type.toString() in typeVars) {
            TypeVariableName(it.type.toString())
        } else {
            it.type.toTypeName()
        }
        ParameterSpec.builder(
            it.name!!.getShortName(),
            type
        ).build()
    }
}

private fun addUniqueSuffixToMethodsWithTheSameName(funks: List<ViewStateFun>) {
    // Allow methods to have equal names
    val methodsCounter = mutableMapOf<String, Int>()
    for (funk in funks) {
        val name = funk.declaration.simpleName.getShortName()
        val counter = methodsCounter[name] ?: 0
        if (counter > 0) {
            funk.uniqueSuffix = counter.toString()
        }
        methodsCounter[name] = counter + 1
    }
}

private fun KSFunctionDeclaration.generateFun(
    classTypeVariables: List<TypeVariableName>,
    commandClassName: String,
    uniqueSuffix: String,
    logger: KSPLogger,
): FunSpec {
    val name = this.simpleName.getShortName()
    logger.info("Generate function: $name", this)
    var commandFieldName = name.replaceFirstChar { it.lowercase() } + uniqueSuffix + "Command"
    var iterationVariableName = "view"
    val argumentsString = parameters.joinToString { it.name!!.getShortName() }
    while (argumentsString.contains(commandFieldName)) {
        commandFieldName += commandFieldName.hashCode() % 10
    }
    while (argumentsString.contains(iterationVariableName)) {
        iterationVariableName += iterationVariableName.hashCode() % 10
    }

    return overriding(this, classTypeVariables)
        .addStatement("val $commandFieldName = $commandClassName(${argumentsString})")
        .addStatement("viewCommands.beforeApply($commandFieldName)")
        .addCode("\n")
        .beginControlFlow("if (hasNotView())")
        .addStatement("return")
        .endControlFlow()
        .addCode("\n")
        .beginControlFlow("for ($iterationVariableName in views)")
        .addStatement("$iterationVariableName.${name}(${argumentsString})")
        .endControlFlow()
        .addCode("\n")
        .addStatement("viewCommands.afterApply($commandFieldName)")
        .build()
}

fun overriding(
    function: KSFunctionDeclaration,
    classTypeVariables: List<TypeVariableName>
): FunSpec.Builder {
    val methodName = function.simpleName.getShortName()
    val funBuilder = FunSpec.builder(methodName)
    funBuilder.addModifiers(KModifier.OVERRIDE)
    function.typeParameters
        .map { it.toTypeVariableName() }
        .forEach { funBuilder.addTypeVariable(it) }
    funBuilder.returns(function.returnType!!.toTypeName())
    funBuilder.addParameters(function.parameters.toParameterSpec(classTypeVariables))
    return funBuilder
}
