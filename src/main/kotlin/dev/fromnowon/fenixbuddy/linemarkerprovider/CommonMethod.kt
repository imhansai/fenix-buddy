package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader.getIcon
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiLiteralUtil
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
    namespace: String,
    fenixId: String,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    psiElement: PsiElement
) {
    val fileElements = DomService.getInstance().getFileElements(FenixsDomElement::class.java, project, null)
    val targets = fileElements.asSequence()
        .map { it.rootElement }
        .filter { namespace == it.namespace.rawText }
        .map { it.fenixDomElementList }
        .flatten()
        .filter { fenixId == it.id.rawText }
        .mapNotNull { it.xmlTag?.getAttribute("id")?.valueElement }
        .toList()

    if (targets.isEmpty()) return

    handleLineMarkerInfo(result, targets, psiElement)
}

fun fenixToJava(
    project: Project,
    namespace: String,
    fenixId: String,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    psiElement: PsiElement
) {
    val allScope = GlobalSearchScope.allScope(project)
    // namespace 是否为完全限定类名对应了不同的查找方法
    val psiClasses = if (namespace.contains(".")) {
        // 完全限定类名查找
        JavaPsiFacade.getInstance(project).findClass(namespace, allScope)?.let { mutableListOf(it) }
    } else {
        // 类名查找
        PsiShortNamesCache.getInstance(project).getClassesByName(namespace, allScope).toMutableList()
    }
    if (psiClasses.isNullOrEmpty()) return

    // 找到对应的方法
    val psiMethods = psiClasses.flatMap { it.findMethodsByName(fenixId, true).toMutableList() }
    if (psiMethods.isEmpty()) return

    handleLineMarkerInfo(result, psiMethods, psiElement)
}

fun xmlToFenix(
    project: Project,
    namespace: String,
    fenixId: String,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    psiElement: PsiElement
) {
    fenixToJava(project, namespace, fenixId, result, psiElement)
}

fun javaToFenix(
    project: Project,
    classQualifiedName: String,
    methodName: String,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    psiElement: PsiElement
) {
    val scope = GlobalSearchScope.allScope(project)
    val annotationClass =
        JavaPsiFacade.getInstance(project).findClass("com.blinkfox.fenix.jpa.QueryFenix", scope) ?: return
    val searchPsiMethods = AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope)
    val psiMethods = searchPsiMethods.findAll()

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

        if (fenixId != methodName) continue

        targets.add(psiMethod)
    }

    handleLineMarkerInfo(result, targets, psiElement)
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