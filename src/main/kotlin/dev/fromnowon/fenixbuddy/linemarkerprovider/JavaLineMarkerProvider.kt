package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiLiteralUtil

open class JavaLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        fromQueryFenix(element, result)
        toQueryFenix(element, result)
    }

    /**
     * 从 java 跳转到 fenix
     */
    private fun toQueryFenix(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        if (element !is PsiMethod) return
        // 有 QueryFenix 注解的不需要处理
        val annotations = element.annotations
        if (annotations.any { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") }) return

        val psiElement = element.nameIdentifier ?: return
        val classQualifiedName = element.containingClass?.qualifiedName ?: return
        val methodName = element.name
        val project = element.project

        providerToQueryFenix(project, classQualifiedName, methodName, result, psiElement)
    }

    /**
     * 从 fenix 跳转到 java/xml
     */
    private fun fromQueryFenix(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        if (element !is PsiMethod) return
        val psiAnnotations = element.annotations
        val queryFenixAnnotation =
            psiAnnotations.find { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") } ?: return

        // 方法名(叶子元素)
        val psiElement = element.nameIdentifier ?: return
        val project = element.project
        val classQualifiedName = element.containingClass?.qualifiedName
        val methodName = element.name

        // =========== java api 方式 ===========
        // provider
        val providerPsiAnnotationMemberValue = queryFenixAnnotation.findAttributeValue("provider")
        val psiJavaCodeReferenceElement =
            (providerPsiAnnotationMemberValue as? PsiClassObjectAccessExpression)?.operand?.innermostComponentReferenceElement
        val provider = psiJavaCodeReferenceElement?.qualifiedName
        // method
        var method: String? = null
        val methodPsiAnnotationMemberValue = queryFenixAnnotation.findAttributeValue("method")
        (methodPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            method = PsiLiteralUtil.getStringLiteralContent(it)
        }
        // countMethod
        var countMethod: String? = null
        val countMethodPsiAnnotationMemberValue = queryFenixAnnotation.findAttributeValue("countMethod")
        (countMethodPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            countMethod = PsiLiteralUtil.getStringLiteralContent(it)
        }

        if (!provider.isNullOrBlank() && provider != "java.lang.Void") {
            // 获取当前方法的名称作为id
            method = method.takeUnless { it.isNullOrBlank() } ?: methodName
            queryFenixToProvider(result, psiElement, project, provider, method!!, countMethod)
            return
        }

        // =========== xml 方式 ===========
        var completeFenixId: String? = null
        // value
        val valuePsiAnnotationMemberValue = queryFenixAnnotation.findAttributeValue("value")
        (valuePsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            completeFenixId = PsiLiteralUtil.getStringLiteralContent(it)
        }
        val (tempNameSpaceForTempFenixId, tempFenixId) = extractTempInfo(
            completeFenixId,
            classQualifiedName,
            methodName
        )

        // countQuery
        var countQuery: String? = null
        val countQueryPsiAnnotationMemberValue = queryFenixAnnotation.findAttributeValue("countQuery")
        (countQueryPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            countQuery = PsiLiteralUtil.getStringLiteralContent(it)
        }
        val (tempNameSpaceForTempCountQuery, tempCountQuery) = extractTempInfo(
            countQuery,
            classQualifiedName,
            methodName
        )

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

}
