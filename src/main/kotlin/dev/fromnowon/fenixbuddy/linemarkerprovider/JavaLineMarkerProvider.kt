package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
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
        val notContinue = annotations.any { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") }
        if (notContinue) return

        val psiElement = element.nameIdentifier ?: return

        val classQualifiedName = element.containingClass?.qualifiedName
        val methodName = element.name

        val project = element.project
        val scope = GlobalSearchScope.allScope(project)
        val annotationClass =
            JavaPsiFacade.getInstance(project).findClass("com.blinkfox.fenix.jpa.QueryFenix", scope) ?: return
        val searchPsiMethods = AnnotatedElementsSearch.searchPsiMethods(annotationClass, scope)
        val psiMethods = searchPsiMethods.findAll()

        val targets: MutableList<PsiMethod> = mutableListOf()
        for (psiMethod in psiMethods) {
            // 获取是否有 provider 注解，然后判断有没有 method 属性
            val psiAnnotations = psiMethod.annotations
            val psiAnnotation =
                psiAnnotations.find { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") } ?: continue
            // provider
            val providerPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("provider")
            val psiJavaCodeReferenceElement =
                (providerPsiAnnotationMemberValue as? PsiClassObjectAccessExpression)?.operand?.innermostComponentReferenceElement
            val qualifiedName = psiJavaCodeReferenceElement?.qualifiedName
            if (qualifiedName.isNullOrBlank() || qualifiedName == "java.lang.Void" || qualifiedName != classQualifiedName) continue

            // fenixId
            var fenixId: String? = null
            // method
            val methodPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("method")
            (methodPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
                fenixId = PsiLiteralUtil.getStringLiteralContent(it)
            }
            // 获取当前方法的名称作为id
            fenixId = fenixId.takeUnless { it.isNullOrBlank() } ?: element.name

            if (fenixId != methodName) continue

            targets.add(psiMethod)
        }

        handleLineMarkerInfo(result, targets, psiElement)
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

        // provider
        val providerPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("provider")
        val psiJavaCodeReferenceElement =
            (providerPsiAnnotationMemberValue as? PsiClassObjectAccessExpression)?.operand?.innermostComponentReferenceElement
        namespace = psiJavaCodeReferenceElement?.qualifiedName
        // method
        val methodPsiAnnotationMemberValue = psiAnnotation.findAttributeValue("method")
        (methodPsiAnnotationMemberValue as? PsiLiteralExpression)?.let {
            fenixId = PsiLiteralUtil.getStringLiteralContent(it)
        }
        if (!namespace.isNullOrBlank() && namespace != "java.lang.Void") {
            // 获取当前方法的名称作为id
            fenixId = fenixId.takeUnless { it.isNullOrBlank() } ?: element.name
            // println("跳转到 $namespace $fenixId 方法")
            searchPsiMethodsAndCreateLineMarkerInfo(project, namespace, fenixId!!, result, methodPsiElement)
            return
        }

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
        searchDomElementAndCreateLineMarkerInfo(project, namespace, fenixId!!, result, methodPsiElement)
    }

}
