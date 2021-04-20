// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.gestalt.naming;

import com.github.zafarkhaja.semver.expr.ExpressionParser;
import com.google.common.base.MoreObjects;

import java.util.function.Predicate;

public class SemverExpression implements Predicate<Version> {

    private final Predicate<com.github.zafarkhaja.semver.Version> expression;
    private final String source;

    public SemverExpression(String source) {
        this.source = source;
        this.expression = ExpressionParser.newInstance().parse(source);
    }

    @Override
    public boolean test(Version version) {
        return expression.test(version.semver);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(source)
                .toString();
    }
}
