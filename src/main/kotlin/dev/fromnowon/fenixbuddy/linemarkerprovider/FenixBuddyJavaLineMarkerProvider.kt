package dev.fromnowon.fenixbuddy.linemarkerprovider

import com.google.common.collect.Collections2
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.util.CommonProcessors
import com.intellij.util.ReflectionUtil
import com.intellij.util.xml.DomElement
import dev.fromnowon.fenixbuddy.xml.IdDomElement


class FenixBuddyJavaLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        // PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
        if (element is PsiNameIdentifierOwner && element is PsiClass && element.isInterface) {
            val processor = CommonProcessors.CollectProcessor<IdDomElement>()

            // TODO: 做一些处理

            val results: Collection<IdDomElement> = processor.results
            if (results.isEmpty()) {
                return
            }

            val iconPath = "/image/icon.png"
            val icon = IconLoader.getIcon(iconPath, ReflectionUtil.getGrandCallerClass() ?: error(iconPath))
            val builder = NavigationGutterIconBuilder
                .create(icon)
                .setAlignment(GutterIconRenderer.Alignment.CENTER)
                .setTargets(Collections2.transform(results, DomElement::getXmlTag))
                .setTooltipTitle("Navigation To Target In Fenix Mapper Xml")
            result.add(builder.createLineMarkerInfo(element))
        }
    }

}
