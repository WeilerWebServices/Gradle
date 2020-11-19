/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.transform;

import groovy.transform.NullCheck;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;

import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.ifS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.isNullX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.throwS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;

/**
 * Handles generation of code for the @AutoImplement annotation.
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class NullCheckASTTransformation extends AbstractASTTransformation {
    private static final Class MY_CLASS = NullCheck.class;
    private static final ClassNode MY_TYPE = make(MY_CLASS);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final ClassNode EXCEPTION = ClassHelper.make(IllegalArgumentException.class);

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode anno = (AnnotationNode) nodes[0];
        if (!MY_TYPE.equals(anno.getClassNode())) return;

        if (parent instanceof ClassNode) {
            ClassNode cNode = (ClassNode) parent;
            if (!checkNotInterface(cNode, MY_TYPE_NAME)) return;
            for (ConstructorNode cn : cNode.getDeclaredConstructors()) {
                adjustMethod(cn);
            }
            for (MethodNode mn : cNode.getAllDeclaredMethods()) {
                adjustMethod(mn);
            }
        } else if (parent instanceof MethodNode) {
            // handles constructor case too
            adjustMethod((MethodNode) parent);
        }
    }

    private void adjustMethod(MethodNode mn) {
        Statement origCode = mn.getCode();
        BlockStatement newCode = new BlockStatement();
        if (mn.getParameters().length == 0) return;
        for (Parameter p : mn.getParameters()) {
            newCode.addStatement(ifS(isNullX(varX(p)),
                    throwS(ctorX(EXCEPTION, constX(p.getName() + " cannot be null")))));
        }
        if (origCode instanceof BlockStatement) {
            for (Statement s : ((BlockStatement) origCode).getStatements()) {
                newCode.addStatement(s);
            }
        } else {
            newCode.addStatement(origCode);
        }
        mn.setCode(newCode);
    }
}
