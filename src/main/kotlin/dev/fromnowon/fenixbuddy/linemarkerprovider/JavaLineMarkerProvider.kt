package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiLiteralUtil
import com.intellij.util.xml.DomService
import dev.fromnowon.fenixbuddy.xml.FenixDomElement
import dev.fromnowon.fenixbuddy.xml.FenixsDomElement


/**
 * 含有 QueryFenix 注解的方法做标记
 */
class JavaLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is PsiMethod) return
        val annotations = element.annotations
        val psiAnnotation = annotations.find { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") } ?: return

        // 获取当前方法的 fenixId
        val psiAnnotationMemberValue = psiAnnotation.findAttributeValue("value")
        if (psiAnnotationMemberValue !is PsiLiteralExpression) return
        var fenixId = PsiLiteralUtil.getStringLiteralContent(psiAnnotationMemberValue)
        val namespace = element.containingClass?.qualifiedName ?: return
        // 如果 fenixId 为空，就使用当前类+方法名组装
        if (fenixId.isNullOrBlank()) {
            val id = element.name
            fenixId = "$namespace.$id"
        } else {
            // 如果缺少了命名空间,使用当前类名填充上
            if (!fenixId.contains(".")) {
                fenixId = "$namespace.$fenixId"
            }
        }

        // 获取所有 fenix xml 文件
        val project = element.project
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
