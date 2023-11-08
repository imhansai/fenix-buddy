package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader.getIcon
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.util.xml.DomService
import dev.fromnowon.fenixbuddy.xml.FenixsDomElement
import dev.fromnowon.fenixbuddy.xml.FenixsDomFileDescription

/**
 * 查找 domElement 并创建行标记
 *
 * https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#companion-object-extensions
 */
fun searchDomElementAndCreateLineMarkerInfo(
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

fun searchPsiMethodsAndCreateLineMarkerInfo(
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

private fun handleLineMarkerInfo(
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