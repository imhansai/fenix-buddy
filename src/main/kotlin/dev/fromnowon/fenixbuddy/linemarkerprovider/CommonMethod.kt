package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader.getIcon
import com.intellij.psi.PsiElement
import com.intellij.util.xml.DomService
import dev.fromnowon.fenixbuddy.xml.FenixDomElement
import dev.fromnowon.fenixbuddy.xml.FenixsDomElement
import dev.fromnowon.fenixbuddy.xml.FenixsDomFileDescription


/**
 * 处理 fenixId
 */
fun handleFenixId(
    fenixId: String?,
    namespace: String,
    id: String?
): String {
    var finalFenixId = fenixId
    if (finalFenixId.isNullOrBlank()) {
        // 方法名
        finalFenixId = "$namespace.$id"
    } else {
        if (!finalFenixId.contains(".")) {
            finalFenixId = "$namespace.$finalFenixId"
        }
    }
    return finalFenixId
}

/**
 * 查找 domElement 并创建行标记
 *
 * https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#companion-object-extensions
 */
fun searchDomElementAndCreateLineMarkerInfo(
    project: Project,
    fenixId: String?,
    result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    methodPsiElement: PsiElement
) {
    val fileElements = DomService.getInstance().getFileElements(FenixsDomElement::class.java, project, null)
    val fenixsDomElementList = fileElements.map { it.rootElement }.toMutableList()

    // 当前 fenixId 对应的 xml tag
    val fenixDomElementList: MutableList<FenixDomElement> = mutableListOf()
    for (fenixsDomElement in fenixsDomElementList) {
        val tempNamespace = fenixsDomElement.namespace.rawText
        for (fenixDomElement in fenixsDomElement.fenixDomElementList) {
            val tempId = fenixDomElement.id.rawText
            val tempFenixId = "$tempNamespace.$tempId"
            if (tempFenixId == fenixId) {
                fenixDomElementList.add(fenixDomElement)
            }
        }
    }

    if (fenixDomElementList.isEmpty()) return

    val iconPath = "/image/icon.png"
    val icon = getIcon(iconPath, FenixsDomFileDescription::class.java.classLoader)
    val targets = fenixDomElementList.map { it.xmlTag }.toMutableList()
    val builder = NavigationGutterIconBuilder
        .create(icon)
        .setAlignment(GutterIconRenderer.Alignment.CENTER)
        .setTargets(targets)
        .setTooltipTitle("Navigation To Target In Fenix Mapper Xml")
    result.add(builder.createLineMarkerInfo(methodPsiElement))
}