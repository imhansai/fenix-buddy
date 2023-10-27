package dev.fromnowon.fenixbuddy.xml

import com.intellij.util.xml.*

interface FenixsDomElement : DomElement {

    @get:SubTagList("fenix")
    val fenixDomElementList: List<FenixDomElement>

    @get:Attribute("namespace")
    @get:NameValue
    @get:Required
    val namespace: GenericAttributeValue<String>

}
