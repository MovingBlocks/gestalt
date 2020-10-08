package org.terasology.gestalt.annotation.processing;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public class ElementUtility {
    private final Elements elements;
    private final Types types;

    public ElementUtility(Elements elements, Types types) {
        this.elements = elements;
        this.types = types;
    }

    public Types getTypes() {
        return types;
    }

    public Elements getElements() {
        return elements;
    }

    public final TypeElement classElementFor(Element element) {
        ElementKind kind = element.getKind();
        while (element != null && !(kind == ElementKind.CLASS || kind == ElementKind.INTERFACE || kind == ElementKind.ENUM)) {
            element = element.getEnclosingElement();
            kind = element.getKind();
        }
        if (element instanceof TypeElement) {
            return (TypeElement) element;
        }
        return null;
    }


    public boolean hasStereotype(Element element, List<String> stereotype) {
        if (element == null) {
            return false;
        }
        if (stereotype.contains(element.toString())) {
            return true;
        }
        for (Element ele : element.getEnclosedElements()) {
            if (ele.getKind() == ElementKind.ANNOTATION_TYPE) {
                return hasStereotype(ele, stereotype);
            }
        }
        return false;
    }


}
