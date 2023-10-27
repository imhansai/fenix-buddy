package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.google.common.collect.Collections2
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.CommonProcessors
import com.intellij.util.xml.DomElement
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

        val processor = CommonProcessors.CollectProcessor<FenixDomElement>()

        val psiAnnotationMemberValue = psiAnnotation.findAttributeValue("value")
        if (psiAnnotationMemberValue !is PsiLiteralExpressionImpl) return
        var fenixIdTag = psiAnnotationMemberValue.value as String?
        if (fenixIdTag.isNullOrBlank()) {
            // 命名空间
            val namespace = element.containingClass?.qualifiedName ?: return
            // Fenix XML 标签中的 id
            val fenixId = element.name
            fenixIdTag = "$namespace.$fenixId"
        }

        val project = element.project
        val allScope = GlobalSearchScope.allScope(project)
        val fileElements = DomService.getInstance().getFileElements(FenixsDomElement::class.java, project, allScope)
        val fenixsDomElementList = Collections2.transform(fileElements) { input -> input.rootElement }
        for (fenixsDomElement in fenixsDomElementList) {
            val namespace = fenixsDomElement.namespace.rawText
            for (fenixDomElement in fenixsDomElement.fenixDomElementList) {
                val fenixId = fenixDomElement.id.rawText
                val tempFenixIdTag = "$namespace.$fenixId"
                if (tempFenixIdTag == fenixIdTag) {
                    processor.process(fenixDomElement)
                }
            }
        }

        val results: Collection<FenixDomElement> = processor.results
        if (results.isEmpty()) return

        val iconPath = "/image/icon.png"
        val icon = IconLoader.getIcon(iconPath, this::class.java)
        val builder = NavigationGutterIconBuilder
            .create(icon)
            .setAlignment(GutterIconRenderer.Alignment.CENTER)
            .setTargets(Collections2.transform(results, DomElement::getXmlTag))
            .setTooltipTitle("Navigation To Target In Fenix Mapper Xml")
        result.add(builder.createLineMarkerInfo(element.nameIdentifier!!))
    }

}
