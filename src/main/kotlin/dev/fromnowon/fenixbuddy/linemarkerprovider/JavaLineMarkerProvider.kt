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
        fromFenix(element, result)
        toFenix(element, result)
    }

    /**
     * 从 java 跳转到 fenix
     */
    private fun toFenix(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        if (element !is PsiMethod) return
        // 有 QueryFenix 注解的不需要处理
        val annotations = element.annotations
        if (annotations.any { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") }) return

        val psiElement = element.nameIdentifier ?: return
        val classQualifiedName = element.containingClass?.qualifiedName ?: return
        val methodName = element.name
        val project = element.project

        javaToFenix(project, classQualifiedName, methodName, result, psiElement)
    }

    /**
     * 从 fenix 跳转到 java/xml
     */
    private fun fromFenix(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        if (element !is PsiMethod) return
        val annotations = element.annotations
        val psiAnnotation = annotations.find { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") } ?: return

        // 方法名(叶子元素)
        val methodPsiElement = element.nameIdentifier ?: return
        val project = element.project

        val classQualifiedName = element.containingClass?.qualifiedName
        val methodName = element.name

        // =========== java api 方式 ===========
        // provider
        val providerPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("provider")
        val psiJavaCodeReferenceElement =
            (providerPsiAnnotationMemberValue as? PsiClassObjectAccessExpression)?.operand?.innermostComponentReferenceElement
        // 命名空间
        val namespace = psiJavaCodeReferenceElement?.qualifiedName
        // fenixId
        var fenixId: String? = null
        // method
        val methodPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("method")
        (methodPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            fenixId = PsiLiteralUtil.getStringLiteralContent(it)
        }
        // countMethod
        var countMethod: String? = null
        val countMethodPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("countMethod")
        (countMethodPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            countMethod = PsiLiteralUtil.getStringLiteralContent(it)
        }

        if (!namespace.isNullOrBlank() && namespace != "java.lang.Void") {
            // 获取当前方法的名称作为id
            fenixId = fenixId.takeUnless { it.isNullOrBlank() } ?: methodName
            fenixToJava(project, namespace, fenixId!!, countMethod, result, methodPsiElement)
            return
        }

        // =========== xml 方式 ===========
        var completeFenixId: String? = null
        // value
        val valuePsiAnnotationMemberValue = psiAnnotation.findAttributeValue("value")
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
        val countQueryPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("countQuery")
        (countQueryPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            countQuery = PsiLiteralUtil.getStringLiteralContent(it)
        }
        val (tempNameSpaceForTempCountQuery, tempCountQuery) = extractTempInfo(
            countQuery,
            classQualifiedName,
            methodName
        )

        fenixToXml(
            project,
            tempNameSpaceForTempFenixId,
            tempFenixId,
            tempNameSpaceForTempCountQuery,
            tempCountQuery,
            result,
            methodPsiElement
        )
    }

}
