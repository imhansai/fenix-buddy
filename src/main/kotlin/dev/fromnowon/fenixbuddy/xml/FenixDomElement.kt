package dev.fromnowon.fenixbuddy.xml

import com.intellij.util.xml.*

interface FenixDomElement : DomElement {

    @get:Attribute("id")
    @get:NameValue
    @get:Required
    val id: GenericAttributeValue<String>

}