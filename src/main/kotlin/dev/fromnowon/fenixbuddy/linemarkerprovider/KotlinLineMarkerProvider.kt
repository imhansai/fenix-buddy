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
        val stringTemplateExpression = ktValueArgument?.stringTemplateExpression
        if (stringTemplateExpression?.isPlain() == false) return
        var fenixId: String? = stringTemplateExpression?.plainContent

        // 类名
        val containingClass = element.containingClass()
        val fqName = containingClass?.fqName
        val namespace = fqName?.asString() ?: return
        // 方法名(叶子元素)
        val methodPsiElement = element.nameIdentifier ?: return
        val id = methodPsiElement.text
        fenixId = handleFenixId(fenixId, namespace, id)

        // 获取所有 fenix xml 文件
        val project = element.project

        // 查找 domElement 并创建行标记
        searchDomElementAndCreateLineMarkerInfo(project, fenixId, result, methodPsiElement)
    }

}
