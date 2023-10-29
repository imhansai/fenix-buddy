package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiLiteralUtil
import com.intellij.util.xml.DomService
import dev.fromnowon.fenixbuddy.xml.FenixDomElement
import dev.fromnowon.fenixbuddy.xml.FenixsDomElement


/**
 * 含有特定注解的方法做标记
 */
class FenixBuddyJavaLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is PsiMethod) return
        val annotations = element.annotations
        val psiAnnotation = annotations.find { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") } ?: return

        // 获取当前Java方法的fenixId
        val psiAnnotationMemberValue = psiAnnotation.findAttributeValue("value")
        if (psiAnnotationMemberValue !is PsiLiteralExpression) return
        var fenixIdTag = PsiLiteralUtil.getStringLiteralContent(psiAnnotationMemberValue)
        // 如果 fenix 没有填写 id，就使用当前类+方法名组装 fenixId
        if (fenixIdTag.isNullOrBlank()) {
            // 命名空间
            val namespace = element.containingClass?.qualifiedName ?: return
            // Fenix XML 标签中的 id
            val fenixId = element.name
            fenixIdTag = "$namespace.$fenixId"
        }

        // 获取所有 fenix xml 文件
        val project = element.project
        val allScope = GlobalSearchScope.allScope(project)
        val fileElements = DomService.getInstance().getFileElements(FenixsDomElement::class.java, project, allScope)
        val fenixsDomElementList = fileElements.map { it.rootElement }.toMutableList()

        // 当前 fenixId 对应的 xml tag
        val fenixDomElementList: MutableList<FenixDomElement> = mutableListOf()
        for (fenixsDomElement in fenixsDomElementList) {
            val namespace = fenixsDomElement.namespace.rawText
            for (fenixDomElement in fenixsDomElement.fenixDomElementList) {
                val fenixId = fenixDomElement.id.rawText
                val tempFenixIdTag = "$namespace.$fenixId"
                if (tempFenixIdTag == fenixIdTag) {
                    fenixDomElementList.add(fenixDomElement)
                }
            }
        }

        if (fenixDomElementList.isEmpty()) return

        val iconPath = "/image/icon.png"
        val icon = IconLoader.getIcon(iconPath, this::class.java)
        val targets = fenixDomElementList.map { it.xmlTag }.toMutableList()
        val builder = NavigationGutterIconBuilder
            .create(icon)
            .setAlignment(GutterIconRenderer.Alignment.CENTER)
            .setTargets(targets)
            .setTooltipTitle("Navigation To Target In Fenix Mapper Xml")
        result.add(builder.createLineMarkerInfo(element.nameIdentifier!!))
    }

}
