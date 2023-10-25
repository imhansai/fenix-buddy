package dev.fromnowon.fenixbuddy

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.psi.PsiElement

class FenixBuddyJavaLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        println("执行了 FenixBuddyJavaLineMarkerProvider")
        super.collectNavigationMarkers(element, result)
    }

}
