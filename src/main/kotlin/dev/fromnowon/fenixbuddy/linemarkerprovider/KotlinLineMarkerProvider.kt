package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.util.xml.DomService
import dev.fromnowon.fenixbuddy.xml.FenixDomElement
import dev.fromnowon.fenixbuddy.xml.FenixsDomElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class KotlinLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is KtNamedFunction) return
        // 是否有 fenix 注解
        val annotations = element.annotationEntries
        val ktAnnotationEntry = annotations.find {
            val typeReference = it.typeReference
            val typeElement = typeReference?.typeElement
            if (typeElement !is KtUserType) return@find false
            val referenceExpression = typeElement.referenceExpression
            val referencedName = referenceExpression?.getReferencedName()
            "QueryFenix" == referencedName
        } ?: return
        // 找到 value 值，即 fenixId
        val valueArgumentList = ktAnnotationEntry.valueArgumentList
        val arguments = valueArgumentList?.arguments
        val ktValueArgument = arguments?.find {
            // 类似 @QueryFenix("queryMyBlogs") 没有 value 的情况
            val argumentName = it.getArgumentName() ?: return@find true
            val referenceExpression = argumentName.referenceExpression
            if (referenceExpression !is KtNameReferenceExpression) return@find false
            val referencedName = referenceExpression.getReferencedName()
            "value" == referencedName
        }

        // TODO: 从表达式中取值
        var fenixId: String? = ktValueArgument?.stringTemplateExpression?.text?.removeSurrounding("\"")

        // 类名
        val containingClass = element.containingClass()
        val fqName = containingClass?.fqName
        val namespace = fqName?.asString() ?: return

        if (fenixId.isNullOrBlank()) {
            // 方法名
            val id = element.nameIdentifier?.text
            fenixId = "$namespace.$id"
        } else {
            if (!fenixId.contains(".")) {
                fenixId = "$namespace.$fenixId"
            }
        }

        // TODO: 抽取公共逻辑

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
