package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader.getIcon
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiLiteralUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType
import com.intellij.util.xml.DomFileElement
import com.intellij.util.xml.DomService
import dev.fromnowon.fenixbuddy.xml.FenixsDomElement
import dev.fromnowon.fenixbuddy.xml.FenixsDomFileDescription

/**
 * 查找 domElement 并创建行标记
 *
 * https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#companion-object-extensions
 */
fun fenixToXml(
    project: Project,
    tempNameSpaceForTempFenixId: String?,
    tempFenixId: String?,
    tempNameSpaceForTempCountQuery: String?,
    tempCountQuery: String?,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    psiElement: PsiElement
) {
    val fileElements = DomService.getInstance().getFileElements(FenixsDomElement::class.java, project, null)
    val targetsForFenixId = xmlAttributeValues(fileElements, tempNameSpaceForTempFenixId, tempFenixId)
    val targetsForCountQuery = xmlAttributeValues(fileElements, tempNameSpaceForTempCountQuery, tempCountQuery)
    val targets = targetsForFenixId + targetsForCountQuery
    if (targets.isEmpty()) return

    handleLineMarkerInfo(result, targets, psiElement)
}

private fun xmlAttributeValues(
    fileElements: List<DomFileElement<FenixsDomElement>>,
    tempNameSpace: String?,
    tempFenixIdOrCountQuery: String?
): List<PsiElement> {
    if (tempNameSpace.isNullOrBlank()) return mutableListOf()
    return fileElements.asSequence()
        .map { it.rootElement }
        .filter { tempNameSpace == it.namespace.rawText }
        .flatMap { it.fenixDomElementList }
        .filter { tempFenixIdOrCountQuery == it.id.rawText }
        .mapNotNull {
            val valueElement = it.xmlTag?.getAttribute("id")?.valueElement
            valueElement?.children?.find { element ->
                element is XmlToken && element.elementType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
            }
        }
        .toList()
}

fun fenixToJava(
    project: Project,
    namespace: String,
    fenixId: String,
    countMethod: String?,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    psiElement: PsiElement
) {
    // 找到类
    val allScope = GlobalSearchScope.allScope(project)
    val psiClass = JavaPsiFacade.getInstance(project).findClass(namespace, allScope) ?: return

    // 找到方法
    val fenixIdPsiMethods = psiClass.findMethodsByName(fenixId, true).toList()
    val countMethodPsiMethods = countMethod?.let { psiClass.findMethodsByName(it, true).toList() }
    val psiMethods = fenixIdPsiMethods + (countMethodPsiMethods ?: emptyList())
    if (psiMethods.isEmpty()) return

    handleLineMarkerInfo(result, psiMethods, psiElement)
}

fun xmlToFenix(
    project: Project,
    namespace: String,
    id: String,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    psiElement: PsiElement
) {
    val psiMethods = searchPsiMethodsByAnnotationClass(project)
    if (psiMethods.isNullOrEmpty()) return

    val targets: MutableList<PsiMethod> = mutableListOf()
    for (psiMethod in psiMethods) {
        val annotations = psiMethod.annotations
        val psiAnnotation = annotations.find { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") } ?: return

        val classQualifiedName = psiMethod.containingClass?.qualifiedName
        val methodName = psiMethod.name

        var completeFenixId: String? = null
        // value
        val valuePsiAnnotationMemberValue = psiAnnotation.findAttributeValue("value")
        (valuePsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            completeFenixId = PsiLiteralUtil.getStringLiteralContent(it)
        }
        val (tempNameSpaceForTempFenixId, tempFenixId) = extractTempInfo(
            completeFenixId,
            classQualifiedName,
            methodName
        )
        if (namespace == tempNameSpaceForTempFenixId && id == tempFenixId) {
            targets.add(psiMethod)
            continue
        }

        // countQuery
        var countQuery: String? = null
        val countQueryPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("countQuery")
        (countQueryPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            countQuery = PsiLiteralUtil.getStringLiteralContent(it)
        }
        val (tempNameSpaceForTempCountQuery, tempCountQuery) = extractTempInfo(
            countQuery,
            classQualifiedName,
            methodName
        )
        if (namespace == tempNameSpaceForTempCountQuery && id == tempCountQuery) {
            targets.add(psiMethod)
            continue
        }
    }

    if (targets.isEmpty()) return

    handleLineMarkerInfo(result, targets, psiElement)
}

fun extractTempInfo(
    completeFenixIdOrCountQuery: String?,
    classQualifiedName: String?,
    methodName: String
): Pair<String?, String?> {
    val tempNameSpace: String?
    val tempFenixId: String?
    if (completeFenixIdOrCountQuery.isNullOrBlank()) {
        tempNameSpace = classQualifiedName
        tempFenixId = methodName
    } else {
        if (completeFenixIdOrCountQuery.contains(".")) {
            tempNameSpace = completeFenixIdOrCountQuery.substringBeforeLast(".")
            tempFenixId = completeFenixIdOrCountQuery.substringAfterLast(".")
        } else {
            tempNameSpace = classQualifiedName
            tempFenixId = completeFenixIdOrCountQuery
        }
    }
    return Pair(tempNameSpace, tempFenixId)
}

fun javaToFenix(
    project: Project,
    classQualifiedName: String,
    methodName: String,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    psiElement: PsiElement
) {
    val psiMethods = searchPsiMethodsByAnnotationClass(project)
    if (psiMethods.isNullOrEmpty()) return

    val targets: MutableList<PsiMethod> = mutableListOf()
    for (psiMethod in psiMethods) {
        // 获取是否有 provider 注解，然后判断有没有 method 属性
        val psiAnnotations = psiMethod.annotations
        val psiAnnotation =
            psiAnnotations.find { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") } ?: continue
        // provider
        val providerPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("provider")
        val psiJavaCodeReferenceElement =
            (providerPsiAnnotationMemberValue as? PsiClassObjectAccessExpression)?.operand?.innermostComponentReferenceElement
        val qualifiedName = psiJavaCodeReferenceElement?.qualifiedName
        if (qualifiedName.isNullOrBlank() || qualifiedName == "java.lang.Void" || qualifiedName != classQualifiedName) continue

        // fenixId
        var fenixId: String? = null
        // method
        val methodPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("method")
        (methodPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            fenixId = PsiLiteralUtil.getStringLiteralContent(it)
        }
        // 获取当前方法的名称作为id
        fenixId = fenixId.takeUnless { it.isNullOrBlank() } ?: psiMethod.name

        // countMethod
        var countMethod: String? = null
        val countMethodPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("countMethod")
        (countMethodPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            countMethod = PsiLiteralUtil.getStringLiteralContent(it)
        }

        if (fenixId != methodName && countMethod != methodName) continue

        targets.add(psiMethod)
    }

    if (targets.isEmpty()) return

    handleLineMarkerInfo(result, targets, psiElement)
}

fun searchPsiMethodsByAnnotationClass(
    project: Project,
    annotationQualifiedName: String = "com.blinkfox.fenix.jpa.QueryFenix"
): MutableCollection<PsiMethod>? {
    val scope = GlobalSearchScope.allScope(project)
    val annotationClass = JavaPsiFacade.getInstance(project).findClass(annotationQualifiedName, scope)
    val searchPsiMethods = annotationClass?.let { AnnotatedElementsSearch.searchPsiMethods(it, scope) }
    return searchPsiMethods?.findAll()
}

fun handleLineMarkerInfo(
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    targets: List<PsiElement>,
    psiElement: PsiElement
) {
    val iconPath = "/image/icon.png"
    val icon = getIcon(iconPath, FenixsDomFileDescription::class.java.classLoader)
    val builder = NavigationGutterIconBuilder
        .create(icon)
        .setAlignment(GutterIconRenderer.Alignment.CENTER)
        .setTargets(*targets.toTypedArray())
        .setTooltipTitle("Navigation To Target In Fenix Java/Kotlin Class Or Mapper Xml")
    result.add(builder.createLineMarkerInfo(psiElement))
}