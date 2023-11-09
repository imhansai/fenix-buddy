package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.plainContent

class KotlinLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        fromQueryFenix(element, result)
    }

    private fun fromQueryFenix(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is KtNamedFunction) return
        // 是否有 fenix 注解
        val annotations = element.annotationEntries
        val queryFenixKtAnnotationEntry = annotations.find {
            val typeReference = it.typeReference
            val typeElement = typeReference?.typeElement
            if (typeElement !is KtUserType) return@find false
            val referenceExpression = typeElement.referenceExpression
            val referencedName = referenceExpression?.getReferencedName()
            "QueryFenix" == referencedName
        } ?: return

        val psiElement = element.nameIdentifier ?: return
        val containingClass = element.containingClass()
        val classQualifiedName = containingClass?.fqName?.asString() ?: return
        val methodName = psiElement.text
        val project = element.project

        // =========== xml 方式 ===========
        val valueArgumentList = queryFenixKtAnnotationEntry.valueArgumentList
        val arguments = valueArgumentList?.arguments
        // completeFenixId
        val completeFenixId = extractAttributeValue(arguments, "value")
        val (tempNameSpaceForTempFenixId, tempFenixId) = extractTempInfo(
            completeFenixId,
            classQualifiedName,
            methodName
        )

        // countQuery
        val countQuery = extractAttributeValue(arguments, "countQuery")
        val (tempNameSpaceForTempCountQuery, tempCountQuery) = extractTempInfo(
            countQuery,
            classQualifiedName,
            methodName
        )

        // 查找 domElement 并创建行标记
        queryFenixToXml(
            result,
            psiElement,
            project,
            tempNameSpaceForTempFenixId,
            tempFenixId,
            tempNameSpaceForTempCountQuery,
            tempCountQuery
        )
    }

    private fun extractAttributeValue(arguments: MutableList<KtValueArgument>?, attributeName: String): String? {
        var completeFenixId: String? = null
        val ktValueArgument = arguments?.find {
            // 类似 @QueryFenix("queryMyBlogs") 没有 value 的情况
            val argumentName = it.getArgumentName() ?: return@find true
            val referenceExpression = argumentName.referenceExpression
            if (referenceExpression !is KtNameReferenceExpression) return@find false
            val referencedName = referenceExpression.getReferencedName()
            attributeName == referencedName
        }
        val stringTemplateExpression = ktValueArgument?.stringTemplateExpression
        if (stringTemplateExpression?.isPlain() == true) {
            completeFenixId = stringTemplateExpression.plainContent
        }
        return completeFenixId
    }

}
