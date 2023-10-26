package dev.fromnowon.fenixbuddy.xml

import com.intellij.util.xml.*

interface IdDomElement : DomElement {

    @Required
    @NameValue
    @Attribute("id")
    fun getId(): GenericAttributeValue<String?>?

    fun setValue(content: String?)

}