package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType
import com.intellij.util.xml.DomUtil
import dev.fromnowon.fenixbuddy.xml.FenixDomElement
import dev.fromnowon.fenixbuddy.xml.FenixsDomElement

class XmlLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is XmlTag) return
        // 是否为 fenix 标签
        val name = element.name
        if (name != "fenix") return

        // 是否为 fenix id 元素
        val domElement = DomUtil.getDomElement(element)
        if (domElement !is FenixDomElement) return

        // 获取 namespace
        val namespace = DomUtil.getParentOfType(domElement, FenixsDomElement::class.java, true)?.namespace?.stringValue
            ?: throw NullPointerException("未获取到命名空间")
        // 获取 id
        val id = domElement.id.rawText ?: throw NullPointerException("未获取到id")

        val project = element.project
        val xmlAttributeValue = element.getAttribute("id")?.valueElement ?: return

        val psiElements = xmlAttributeValue.children
        val psiElement = psiElements.find {
            it is XmlToken && it.elementType == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
        } ?: return

        xmlToFenix(project, namespace, id, result, psiElement)
    }

}
