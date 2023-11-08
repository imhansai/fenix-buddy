package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiLiteralUtil


/**
 * 含有 QueryFenix 注解的方法做标记
 */
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

        // 命名空间
        var namespace: String?
        // fenixId
        var fenixId: String? = null

        // provider java api 方式
        val providerPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("provider")
        val psiJavaCodeReferenceElement =
            (providerPsiAnnotationMemberValue as? PsiClassObjectAccessExpression)?.operand?.innermostComponentReferenceElement
        namespace = psiJavaCodeReferenceElement?.qualifiedName
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
            fenixId = fenixId.takeUnless { it.isNullOrBlank() } ?: element.name

            val methodNames: MutableList<String> = mutableListOf(fenixId!!)
            countMethod?.let { methodNames.add(it) }
            fenixToJava(project, namespace, methodNames, result, methodPsiElement)
            return
        }

        // xml 方式
        var completeFenixId: String? = null
        // value
        val valuePsiAnnotationMemberValue = psiAnnotation.findAttributeValue("value")
        (valuePsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            completeFenixId = PsiLiteralUtil.getStringLiteralContent(it)
        }
        val classQualifiedName = element.containingClass?.qualifiedName
        when {
            completeFenixId.isNullOrBlank() -> {
                // 当前类名作为 namespace，方法名作为 fenixId
                namespace = classQualifiedName
                fenixId = element.name
            }

            else -> {
                // 需要提取出 namespace 和 fenixId
                if (completeFenixId!!.contains(".")) {
                    namespace = completeFenixId!!.substringBeforeLast(".")
                    fenixId = completeFenixId!!.substringAfterLast(".")
                } else {
                    namespace = classQualifiedName
                    fenixId = completeFenixId
                }
            }
        }
        if (namespace.isNullOrBlank() || fenixId.isNullOrBlank()) return
        // 查找 domElement 并创建行标记
        fenixToXml(project, namespace, fenixId!!, result, methodPsiElement)
    }

}
