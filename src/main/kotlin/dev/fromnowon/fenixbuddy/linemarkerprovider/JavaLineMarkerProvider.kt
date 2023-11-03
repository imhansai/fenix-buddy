package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
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
        if (element !is PsiMethod) return
        val annotations = element.annotations
        val psiAnnotation = annotations.find { it.hasQualifiedName("com.blinkfox.fenix.jpa.QueryFenix") } ?: return

        // 获取当前方法的 fenixId
        val psiAnnotationMemberValue = psiAnnotation.findAttributeValue("value")
        if (psiAnnotationMemberValue !is PsiLiteralExpression) return
        var fenixId = PsiLiteralUtil.getStringLiteralContent(psiAnnotationMemberValue)
        val namespace = element.containingClass?.qualifiedName ?: return
        val id = element.name
        fenixId = handleFenixId(fenixId, namespace, id)

        // 获取所有 fenix xml 文件
        val project = element.project
        // 方法名(叶子元素)
        val methodPsiElement = element.nameIdentifier ?: return

        // 查找 domElement 并创建行标记
        searchDomElementAndCreateLineMarkerInfo(project, fenixId, result, methodPsiElement)
    }

}
