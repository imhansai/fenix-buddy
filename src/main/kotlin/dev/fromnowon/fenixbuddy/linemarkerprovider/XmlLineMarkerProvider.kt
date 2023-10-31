package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomUtil
import dev.fromnowon.fenixbuddy.xml.FenixDomElement
import dev.fromnowon.fenixbuddy.xml.FenixsDomElement


class XmlLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        // xml 文件根标签是否为 fenixs
        if (element !is XmlTag) return
        val containingFile = element.containingFile
        if (containingFile !is XmlFile) return
        val rootTag = containingFile.rootTag
        if (rootTag?.name != "fenixs") return

        // 是否为 fenix 标签
        val name = element.name
        if (name != "fenix") return

        // 是否为 fenix id 元素
        val domElement = DomUtil.getDomElement(element)
        if (domElement !is FenixDomElement) return

        // 获取命名空间
        val namespace = DomUtil.getParentOfType(domElement, FenixsDomElement::class.java, true)?.namespace?.stringValue
            ?: throw NullPointerException("未获取到命名空间")
        // 获取id
        val id = domElement.id.rawText ?: throw NullPointerException("未获取到id")

        // 找到 Java 类
        val project = element.project
        val allScope = GlobalSearchScope.allScope(project)
        // namespace 是否为完全限定类名对应了不同的查找方法
        val psiClasses = if (namespace.contains(".")) {
            JavaPsiFacade.getInstance(project).findClass(namespace, allScope)?.let { mutableListOf(it) }
        } else {
            PsiShortNamesCache.getInstance(project).getClassesByName(namespace, allScope).toMutableList()
        }
        if (psiClasses.isNullOrEmpty()) return

        // 找到对应的方法
        val psiMethods = psiClasses.flatMap { it.findMethodsByName(id, true).toMutableList() }
        if (psiMethods.isEmpty()) return

        val iconPath = "/image/icon.png"
        val icon = IconLoader.getIcon(iconPath, this::class.java)
        val builder = NavigationGutterIconBuilder
            .create(icon)
            .setAlignment(GutterIconRenderer.Alignment.CENTER)
            .setTargets(*psiMethods.toTypedArray())
            .setTooltipTitle("Navigation To Target In Fenix Java/Kotlin Class")

        // 规避性能警告,使用叶子元素,类型为 XmlToken,值为 <
        val firstChild = element.firstChild
        result.add(builder.createLineMarkerInfo(firstChild))
    }

}
