package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.plainContent

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

        // 命名空间
        var namespace: String?
        // fenixId
        var fenixId: String?

        // 找到 value 值，即 completeFenixId
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
        val stringTemplateExpression = ktValueArgument?.stringTemplateExpression
        if (stringTemplateExpression?.isPlain() == false) return
        var completeFenixId: String? = stringTemplateExpression?.plainContent


        // 当前类名作为 namespace，方法名作为 fenixId
        val containingClass = element.containingClass()
        val fqName = containingClass?.fqName
        val methodPsiElement = element.nameIdentifier ?: return
        when {
            completeFenixId.isNullOrBlank() -> {
                namespace = (fqName?.asString() ?: return)
                fenixId = methodPsiElement.text
            }

            else -> {
                // 需要提取出 namespace 和 fenixId
                if (completeFenixId.contains(".")) {
                    namespace = completeFenixId.substringBeforeLast(".")
                    fenixId = completeFenixId.substringAfterLast(".")
                } else {
                    // 当前类名作为 namespace，方法名作为 fenixId
                    namespace = (fqName?.asString() ?: return)
                    fenixId = completeFenixId
                }
            }
        }

        if (namespace.isBlank() || fenixId.isNullOrBlank()) return

        // 获取所有 fenix xml 文件
        val project = element.project

        // 查找 domElement 并创建行标记
        fenixToXml(project, namespace, fenixId, null, result, methodPsiElement)
    }

}
