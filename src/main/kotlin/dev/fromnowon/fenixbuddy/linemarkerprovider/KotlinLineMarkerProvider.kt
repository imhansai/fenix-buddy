package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.caches.resolve.resolveMainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.plainContent

class KotlinLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        fromQueryFenix(element, result)
        toQueryFenix(element, result)
    }

    private fun toQueryFenix(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        if (element !is KtNamedFunction) return
        val annotationEntries = element.annotationEntries
        val haveQueryFenix = annotationEntries.any {
            val typeReference = it.typeReference
            val typeElement = typeReference?.typeElement
            if (typeElement !is KtUserType) return@any false
            val referenceExpression = typeElement.referenceExpression
            val referencedName = referenceExpression?.getReferencedName()
            "QueryFenix" == referencedName
        }
        if (haveQueryFenix) return

        val psiElement = element.nameIdentifier ?: return
        val classQualifiedName = element.containingClass()?.kotlinFqName?.asString() ?: return
        val methodName = element.name ?: return
        val project = element.project

        providerToQueryFenix(project, classQualifiedName, methodName, result, psiElement)
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

        val valueArgumentList = queryFenixKtAnnotationEntry.valueArgumentList
        val arguments = valueArgumentList?.arguments

        // =========== java api 方式 ===========
        val provider = extractAttributeValue(arguments, "provider")
        var method = extractAttributeValue(arguments, "method")
        val countMethod = extractAttributeValue(arguments, "countMethod")
        if (!provider.isNullOrBlank() && provider != "java.lang.Void") {
            method = method.takeUnless { it.isNullOrBlank() } ?: methodName
            queryFenixToProvider(result, psiElement, project, provider, method!!, countMethod)
            return
        }

        // =========== xml 方式 ===========
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
        var attributeValue: String? = null
        val ktValueArgument = arguments?.find {
            // 类似 @QueryFenix("queryMyBlogs") 没有 value 的情况
            if (attributeName == "value" && it.getArgumentName() == null) {
                return@find true
            }
            val argumentName = it.getArgumentName()
            val referenceExpression = argumentName?.referenceExpression
            if (referenceExpression !is KtNameReferenceExpression) return@find false
            val referencedName = referenceExpression.getReferencedName()
            attributeName == referencedName
        }

        val stringTemplateExpression = ktValueArgument?.stringTemplateExpression
        if (stringTemplateExpression?.isPlain() == true) {
            attributeValue = stringTemplateExpression.plainContent
        }
        if (!attributeValue.isNullOrBlank()) return attributeValue

        val argumentExpression = ktValueArgument?.getArgumentExpression()
        val ktClassLiteralExpression = argumentExpression as? KtClassLiteralExpression
        val ktExpression = ktClassLiteralExpression?.receiverExpression
        val ktNameReferenceExpression = ktExpression as? KtNameReferenceExpression
        val reference = ktNameReferenceExpression?.resolveMainReference()
        attributeValue = reference?.kotlinFqName?.asString()

        return attributeValue
    }

}
