package com.github.evgzakharov.kmapper

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.originatingKSFiles
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

class BuilderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("start resolving")
        val converterElements = getElements(resolver, MapperConverter::class, "dynamicFields")
        val implemented = converterElements
            .associateBy { (it.srcType to it.dstType) }

        val files = converterElements.filter { it.implemented }.map {
            genImplementedCoverterFile(it)
        } + converterElements.filter { it.implemented.not() }.map {
            genCoverterFile(implemented, it, resolver)
        }

        val dependencies = files.flatMap { it.originatingKSFiles() }
        files.forEach { it.writeTo(codeGenerator, Dependencies(true, *dependencies.toTypedArray())) }

        getElements(resolver, MapperSetter::class, "ignoreFields")
            .forEach {
                genSetterFile(implemented, it, resolver).writeTo(codeGenerator, Dependencies(true, *dependencies.toTypedArray()))
            }

        return emptyList()
    }

    private fun genSetterFile(
        implemented: Map<Pair<KSClassDeclaration, KSClassDeclaration>, MapperData>,
        data: MapperData,
        resolver: Resolver
    ): FileSpec {
        val packageName = data.type.packageName
        val className = data.type.simpleName + "Impl"
        val srcType = data.srcType.toClassName()
        val dstType = data.dstType.toClassName()

        val converterClass = TypeSpec.objectBuilder(className)
            .superclass(SetterBuilder::class.asClassName().parameterizedBy(srcType, dstType))
            .addSuperinterface(data.type)
            .addFunction(
                FunSpec.builder("build")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("source", srcType)
                    .addParameter("target", dstType)
                    .let {
                        data.srcProperties.forEach { srcProperty ->
                            val srcName = srcProperty.simpleName.getShortName()

                            if (srcName !in data.fields) {
                                val dstProperty = data.dstProperties.find { it.simpleName == srcProperty.simpleName }
                                    ?: throw RuntimeException("not find configuration for property: $srcName")
                                val dstName = dstProperty.simpleName.getShortName()

                                if (dstProperty.setter != null) {
                                    val statement = "target.$dstName = " + prepareValue(
                                        srcProperty.type,
                                        dstProperty.type,
                                        "source.",
                                        dstName,
                                        implemented,
                                        resolver,
                                        data
                                    )
                                    it.addStatement(statement)
                                } else {
                                    logger.info("not find setter for $dstProperty")
                                }
                            }
                        }
                        it
                    }
                    .addStatement(
                        """
                            return target
                        """.trimIndent(),
                        dstType
                    )
                    .returns(dstType)
                    .build()
            ).build()

        return FileSpec.builder(packageName, className)
            .addType(converterClass)
            .build()
    }

    private fun genImplementedCoverterFile(data: MapperData): FileSpec {
        val packageName = data.type.packageName
        val className = data.type.simpleName + "Impl"
        val srcType = data.srcType.toClassName()
        val dstType = data.dstType.toClassName()

        val converterClass = TypeSpec.objectBuilder(className)
            .superclass(MappingBuilder::class.asClassName().parameterizedBy(srcType, dstType))
            .addSuperinterface(data.type)
            .addOriginatingKSFile(data.originalFile)
            .build()

        return FileSpec.builder(packageName, className)
            .addType(converterClass)
            .build()
    }

    private fun genCoverterFile(
        implemented: Map<Pair<KSClassDeclaration, KSClassDeclaration>, MapperData>,
        data: MapperData,
        resolver: Resolver
    ): FileSpec {
        val packageName = data.type.packageName
        val className = data.type.simpleName + "Impl"
        val srcType = data.srcType.toClassName()
        val dstType = data.dstType.toClassName()
        val parameters = prepareParameters(implemented, data, resolver)

        val converterClass = TypeSpec.objectBuilder(className)
            .superclass(MappingBuilder::class.asClassName().parameterizedBy(srcType, dstType))
            .addSuperinterface(data.type)
            .addOriginatingKSFile(data.originalFile)
            .addFunction(
                FunSpec.builder("convert")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("source", srcType)
                    .addStatement("return build(source, MappingBuilder.Configuration())")
                    .returns(dstType)
                    .build()
            )
            .addFunction(
                FunSpec.builder("build")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("source", srcType)
                    .addParameter(
                        "configuration",
                        MappingBuilder.Configuration::class.asClassName()
                            .parameterizedBy(srcType, dstType)
                    )
                    .apply {
                        parameters.forEach { parameter ->
                            parameter.apply { builder() }
                        }
                    }
                    .addStatement(
                        """
                            return %T(${parameters.joinToString(",") { "${it.name} = _${it.name}" }})
                        """.trimIndent(),
                        dstType
                    )
                    .returns(dstType)
                    .build()
            ).build()

        return FileSpec.builder(packageName, className)
            .addType(converterClass)
            .build()
    }

    private data class PreparedParameter(
        val name: String,
        val builder: FunSpec.Builder.() -> FunSpec.Builder
    )

    private fun prepareParameters(
        implemented: Map<Pair<KSClassDeclaration, KSClassDeclaration>, MapperData>,
        data: MapperData,
        resolver: Resolver
    ): List<PreparedParameter> {
        val srcType = data.srcType.toClassName()

        return data.dstConstrParams.mapNotNull { dstParam ->
            val dstName = dstParam.name!!.getShortName()
            val srcProperty = data.srcProperties.find { srcProperty ->
                srcProperty.simpleName == dstParam.name
            }

            if (srcProperty != null && dstName !in data.fields) {
                PreparedParameter(dstName) {
                    addStatement("val _$dstName = " + prepareValue(
                        srcProperty.type,
                        dstParam.type,
                        "source.",
                        dstName,
                        implemented,
                        resolver,
                        data
                    ))
                }
            } else if (dstName in data.fields) {
                val resultType = dstParam.type.toTypeName()

                PreparedParameter(dstName) {
                    beginControlFlow("val _$dstName = if (\"$dstName\" in configuration.dynamicFunctions)")
                    addStatement(
                        "(configuration.dynamicFunctions[\"$dstName\"] as (%T) -> %T).invoke(source)",
                        srcType,
                        resultType
                    )
                    endControlFlow()
                    beginControlFlow("else")
                    addStatement(
                        """throw RuntimeException("couldn't find getter for \"$dstName\"")"""
                    )
                    endControlFlow()
                }
            } else {
                val defaultParameter = data.dstType.primaryConstructor!!.parameters.find {
                    it.name == dstParam.name &&
                        it.type == dstParam.type &&
                        it.hasDefault
                }
                if (defaultParameter != null) {
                    return@mapNotNull null
                } else {
                    throw RuntimeException("not find configuration for property: property=$dstName, data=$data")
                }
            }
        }
    }

    private fun prepareValue(
        srcType: KSTypeReference,
        dstType: KSTypeReference,
        sourceName: String,
        dstName: String,
        implemented: Map<Pair<KSClassDeclaration, KSClassDeclaration>, MapperData>,
        resolver: Resolver,
        data: MapperData
    ): String {
        val listClass = resolver.getClassDeclarationByName<List<Any>>()!!.asStarProjectedType()
        val nullable = srcType.resolve().nullability == Nullability.NULLABLE
        val npSym = if (nullable) "?" else ""

        return when {
            // simplest case when all types are equal
            srcType == dstType || srcType.resolve().isAssignableFrom(dstType.resolve()) -> {
                "$sourceName$dstName"
            }

            // case for list
            (srcType.toClassDeclaration().asStarProjectedType().isAssignableFrom(listClass) ||
                srcType.toClassDeclaration().superTypes.any { it.toClassDeclaration().asStarProjectedType().isAssignableFrom(listClass) }) &&
                dstType.toClassDeclaration().asStarProjectedType().isAssignableFrom(listClass) -> {
                val srcListType = srcType.element!!.typeArguments[0]
                val dstListType = dstType.element!!.typeArguments[0]

                val subName = "${dstName}_it"
                "$sourceName$dstName$npSym.map { $subName -> " + prepareValue(
                    srcType = srcListType.type!!,
                    dstType = dstListType.type!!,
                    sourceName = "",
                    dstName = subName,
                    implemented = implemented,
                    resolver = resolver,
                    data = data
                ) + " }"
            }

            // case for implemented converter
            implemented[srcType.toClassDeclaration() to dstType.toClassDeclaration()] != null -> {
                val converterName = implemented[srcType.toClassDeclaration() to dstType.toClassDeclaration()]!!.type.canonicalName.prepare()
                "$sourceName$dstName$npSym.let{ ${converterName}Impl.convert(it) }"
            }

            // case for enum
            srcType.toClassDeclaration().classKind == ClassKind.ENUM_CLASS && dstType.toClassDeclaration().classKind == ClassKind.ENUM_CLASS -> {
                val dstCanonicalName = dstType.toClassDeclaration().toClassName().canonicalName.prepare()
                "$sourceName$dstName$npSym.name$npSym.let{ $dstCanonicalName.valueOf(it) }"
            }

            // case for dst wrapper
            dstType.toClassDeclaration().primaryConstructor?.parameters?.size == 1 -> {
                val canonicalName = dstType.toClassDeclaration().toClassName().canonicalName.prepare()
                "$canonicalName(${prepareValue(
                    srcType = srcType,
                    dstType = dstType.toClassDeclaration().primaryConstructor?.parameters?.first()?.type!!,
                    sourceName = sourceName,
                    dstName = dstName,
                    implemented = implemented,
                    resolver = resolver,
                    data = data
                )})"
            }

            // case for src wrapper
            srcType.toClassDeclaration().primaryConstructor?.parameters?.size == 1 -> {
                val firstParameter = srcType.toClassDeclaration().primaryConstructor?.parameters?.first()

                prepareValue(
                    srcType = firstParameter?.type!!,
                    dstType = dstType,
                    sourceName = sourceName,
                    dstName = dstName + "." + firstParameter.name?.getShortName(),
                    implemented = implemented,
                    resolver = resolver,
                    data = data
                )
            }

            else ->  throw RuntimeException("not implemented converter for ($sourceName$dstName) $srcType to $dstType, data=$data")
        }
    }

    private data class MapperData(
        val originalFile: KSFile,
        val declaration: KSClassDeclaration,
        val type: ClassName,
        val fields: Set<String>,

        val srcType: KSClassDeclaration,
        val srcProperties: List<KSPropertyDeclaration>,

        val dstType: KSClassDeclaration,
        val dstConstrParams: List<KSValueParameter>,
        val dstProperties: List<KSPropertyDeclaration>,

        val implemented: Boolean
    )

    private fun <T : Any> getElements(resolver: Resolver, kClass: KClass<T>, fieldsName: String): List<MapperData> {
        val annotationName = kClass.qualifiedName.orEmpty()

        return resolver.getSymbolsWithAnnotation(annotationName, false)
            .filterIsInstance<KSClassDeclaration>()
            .filter(KSNode::validate)
            .map { d ->
                val parent = d.superTypes.first()
                logger.info("parent: $parent")

                val srcType =
                    parent.element!!.typeArguments.first().type!!.resolve().declaration.closestClassDeclaration()!!
                logger.info("srcType: $srcType ${srcType::class}")
                val dstType =
                    parent.element!!.typeArguments.get(1).type!!.resolve().declaration.closestClassDeclaration()!!
                logger.info("dstType: $dstType ${dstType!!::class}")

                if (d.getDeclaredFunctions().find { it.simpleName.getShortName() == "convert" } != null) {
                    return@map MapperData(
                        originalFile = d.containingFile!!,
                        declaration = d,
                        type = d.toClassName(),
                        fields = emptySet(),
                        srcType = srcType,
                        srcProperties = emptyList(),
                        dstType = dstType,
                        dstConstrParams = emptyList(),
                        dstProperties = emptyList(),
                        implemented = true
                    )
                }

                val fields =
                    d.annotations.first { it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationName }
                        .let { it.arguments.find { it.name == resolver.getKSNameFromString(fieldsName) } }
                        .let { (it?.value as? List<String>)?.toSet() ?: emptySet() }
                logger.info("dynamicFields: $fields")

                val srcProperties = srcType.getAllProperties()
                    .map { srcProperty ->
                        logger.info("srcProperty: $srcProperty ${srcProperty::class}")
                        srcProperty
                    }
                val dstProperties = dstType.getAllProperties()
                    .map { srcProperty ->
                        logger.info("dstProperty: $srcProperty ${srcProperty::class}")
                        srcProperty
                    }

                logger.info("found one: $d ${d::class}")

                MapperData(
                    originalFile = d.containingFile!!,
                    declaration = d,
                    type = d.toClassName(),
                    fields = fields,
                    srcType = srcType,
                    srcProperties = srcProperties.toList(),
                    dstType = dstType,
                    dstConstrParams = dstType.primaryConstructor?.parameters ?: emptyList(),
                    dstProperties = dstProperties.toList(),
                    implemented = false
                )
            }.toList()
    }

    private fun KSTypeReference.toClassDeclaration(): KSClassDeclaration {
        return resolve().declaration.closestClassDeclaration()!!
    }

    private fun String.prepare(): String {
        return this.replace("object", "`object`")
            .replace("enum", "`enum`")
    }
}
