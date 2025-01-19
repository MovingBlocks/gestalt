// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gestalt.annotation.processing;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

/**
 * Utilities for discovering and distinguishing element types.
 */
public class ElementUtility {
    private final Elements elements;
    private final Types types;

    /**
     * Creates an ElementUtility, with given elements and types.
     * @param elements the elements.
     * @param types the types.
     */
    public ElementUtility(Elements elements, Types types) {
        this.elements = elements;
        this.types = types;
    }

    /**
     * {@return the types}
     */
    public Types getTypes() {
        return types;
    }

    /**
     * {@return the elements}
     */
    public Elements getElements() {
        return elements;
    }

    /**
     * {@return the enclosing TypeElement}
     * @param element the element
     */
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

    /**
     * {@return true, if stereotype contains element, false otherwise.}
     * @param element the element.
     * @param stereotype the stereotype.
     */
    public boolean hasStereotype(Element element, List<String> stereotype) {
        if (element == null) {
            return false;
        }
        if (stereotype.contains(element.toString())) {
            return true;
        }
        for (AnnotationMirror ele : element.getAnnotationMirrors()) {
            DeclaredType declaredType = ele.getAnnotationType();
            if (stereotype.contains(declaredType.toString())) {
                return hasStereotype(declaredType.asElement(), stereotype);
            }
        }
        return false;
    }


}
