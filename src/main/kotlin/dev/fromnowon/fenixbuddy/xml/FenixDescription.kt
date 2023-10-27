package dev.fromnowon.fenixbuddy.xml

import com.intellij.openapi.module.Module
import com.intellij.psi.xml.XmlFile
import com.intellij.util.xml.DomFileDescription

class FenixDescription : DomFileDescription<FenixsDomElement>(FenixsDomElement::class.java, "fenixs") {

    override fun isMyFile(file: XmlFile, module: Module?): Boolean {
        val name = file.rootTag?.name
        return "fenixs" == name
    }

}