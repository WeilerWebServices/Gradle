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
package org.codehaus.groovy.transform.stc;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.SourceUnit;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

public class TypeCheckingContext {
    protected SourceUnit source;
    protected Set<MethodNode> methodsToBeVisited = Collections.emptySet();
    protected boolean isInStaticContext = false;

    protected final LinkedList<ErrorCollector> errorCollectors = new LinkedList<ErrorCollector>();
    protected final LinkedList<ClassNode> enclosingClassNodes = new LinkedList<ClassNode>();
    protected final LinkedList<MethodNode> enclosingMethods = new LinkedList<MethodNode>();
    protected final LinkedList<Expression> enclosingMethodCalls = new LinkedList<Expression>();
    protected final LinkedList<ConstructorCallExpression> enclosingConstructorCalls = new LinkedList<ConstructorCallExpression>();
    protected final LinkedList<BlockStatement> enclosingBlocks = new LinkedList<BlockStatement>();
    protected final LinkedList<ReturnStatement> enclosingReturnStatements = new LinkedList<ReturnStatement>();
    protected final LinkedList<PropertyExpression> enclosingPropertyExpressions = new LinkedList<PropertyExpression>();


    // used for closure return type inference
    protected final LinkedList<EnclosingClosure> enclosingClosures = new LinkedList<EnclosingClosure>();

    // whenever a method using a closure as argument (typically, "with") is detected, this list is updated
    // with the receiver type of the with method
    protected DelegationMetadata delegationMetadata;
    /**
     * The type of the last encountered "it" implicit parameter
     */
    protected ClassNode lastImplicitItType;
    /**
     * This field is used to track assignments in if/else branches, for loops and while loops. For example, in the
     * following code: if (cond) { x = 1 } else { x = '123' } the inferred type of x after the if/else statement should
     * be the LUB of (int, String)
     */
    protected Map<VariableExpression, List<ClassNode>> ifElseForWhileAssignmentTracker = null;


    /**
     * This field used for type derivation
     * Check IfStatement matched pattern :
     * Object var1;
     * if (!(var1 instanceOf Runnable)){
     * return
     * }
     * // Here var1 instance of Runnable
     */
    protected final IdentityHashMap<BlockStatement, Map<VariableExpression, List<ClassNode>>> blockStatements2Types = new IdentityHashMap<BlockStatement, Map<VariableExpression, List<ClassNode>>>();


    /**
     * Stores information which is only valid in the "if" branch of an if-then-else statement. This is used when the if
     * condition expression makes use of an instanceof check
     */
    protected Stack<Map<Object, List<ClassNode>>> temporaryIfBranchTypeInformation = new Stack<Map<Object, List<ClassNode>>>();
    protected Set<MethodNode> alreadyVisitedMethods = new HashSet<MethodNode>();
    /**
     * Some expressions need to be visited twice, because type information may be insufficient at some point. For
     * example, for closure shared variables, we need a first pass to collect every type which is assigned to a closure
     * shared variable, then a second pass to ensure that every method call on such a variable is made on a LUB.
     */
    protected final LinkedHashSet<SecondPassExpression> secondPassExpressions = new LinkedHashSet<SecondPassExpression>();
    /**
     * A map used to store every type used in closure shared variable assignments. In a second pass, we will compute the
     * LUB of each type and check that method calls on those variables are valid.
     */
    protected final Map<VariableExpression, List<ClassNode>> closureSharedVariablesAssignmentTypes = new HashMap<VariableExpression, List<ClassNode>>();
    protected Map<Parameter, ClassNode> controlStructureVariables = new HashMap<Parameter, ClassNode>();

    // this map is used to ensure that two errors are not reported on the same line/column
    protected final Set<Long> reportedErrors = new TreeSet<Long>();

    // stores the current binary expression. This is used when assignments are made with a null object, for type
    // inference
    protected final LinkedList<BinaryExpression> enclosingBinaryExpressions = new LinkedList<BinaryExpression>();

    protected final StaticTypeCheckingVisitor visitor;

    protected CompilationUnit compilationUnit;

    public TypeCheckingContext(final StaticTypeCheckingVisitor staticTypeCheckingVisitor) {
        this.visitor = staticTypeCheckingVisitor;
    }

    /**
     * Pushes a binary expression into the binary expression stack.
     * @param binaryExpression the binary expression to be pushed
     */
    public void pushEnclosingBinaryExpression(BinaryExpression binaryExpression) {
        enclosingBinaryExpressions.addFirst(binaryExpression);
    }

    /**
     * Pops a binary expression from the binary expression stack.
     * @return the popped binary expression
     */
    public BinaryExpression popEnclosingBinaryExpression() {
        return enclosingBinaryExpressions.removeFirst();
    }

    /**
     * Returns the binary expression which is on the top of the stack, or null
     * if there's no such element.
     * @return the binary expression on top of the stack, or null if no such element.
     */
    public BinaryExpression getEnclosingBinaryExpression() {
        if (enclosingBinaryExpressions.isEmpty()) return null;
        return enclosingBinaryExpressions.getFirst();
    }

    /**
     * Returns the current stack of enclosing binary expressions. The first
     * element is the top of the stack.
     * @return an immutable list of binary expressions.
     */
    public List<BinaryExpression> getEnclosingBinaryExpressionStack() {
        return Collections.unmodifiableList(enclosingBinaryExpressions);
    }

    /**
     * Pushes a closure expression into the closure expression stack.
     * @param closureExpression the closure expression to be pushed
     */
    public void pushEnclosingClosureExpression(ClosureExpression closureExpression) {
        enclosingClosures.addFirst(new EnclosingClosure(closureExpression));
    }

    /**
     * Pops a closure expression from the closure expression stack.
     * @return the popped closure expression
     */
    public EnclosingClosure popEnclosingClosure() {
        return enclosingClosures.removeFirst();
    }

    /**
     * Returns the closure expression which is on the top of the stack, or null
     * if there's no such element.
     * @return the closure expression on top of the stack, or null if no such element.
     */
    public EnclosingClosure getEnclosingClosure() {
        if (enclosingClosures.isEmpty()) return null;
        return enclosingClosures.getFirst();
    }

    /**
     * Returns the current stack of enclosing closure expressions. The first
     * element is the top of the stack.
     * @return an immutable list of closure expressions.
     */
    public List<EnclosingClosure> getEnclosingClosureStack() {
        return Collections.unmodifiableList(enclosingClosures);
    }

    /**
     * Pushes a method into the method stack.
     * @param methodNode the method to be pushed
     */
    public void pushEnclosingMethod(MethodNode methodNode) {
        enclosingMethods.addFirst(methodNode);
    }

    /**
     * Pops a method from the enclosing methods stack.
     * @return the popped method
     */
    public MethodNode popEnclosingMethod() {
        return enclosingMethods.removeFirst();
    }

    /**
     * Returns the method node which is on the top of the stack, or null
     * if there's no such element.
     * @return the enclosing method on top of the stack, or null if no such element.
     */
    public MethodNode getEnclosingMethod() {
        if (enclosingMethods.isEmpty()) return null;
        return enclosingMethods.getFirst();
    }


    /**
     * Pushes a return statement into the return statement stack.
     * @param returnStatement the return statement to be pushed
     */
    public void pushEnclosingReturnStatement(ReturnStatement returnStatement) {
        enclosingReturnStatements.addFirst(returnStatement);
    }

    /**
     * Pops a return statement from the enclosing return statements stack.
     * @return the popped return statement
     */
    public ReturnStatement popEnclosingReturnStatement() {
        return enclosingReturnStatements.removeFirst();
    }

    /**
     * Returns the return statement which is on the top of the stack, or null
     * if there's no such element.
     * @return the enclosing return statement on top of the stack, or null if no such element.
     */
    public ReturnStatement getEnclosingReturnStatement() {
        if (enclosingReturnStatements.isEmpty()) return null;
        return enclosingReturnStatements.getFirst();
    }

    /**
     * Returns the current stack of enclosing methods. The first
     * element is the top of the stack, that is to say the last visited method.
     * @return an immutable list of method nodes.
     */
    public List<MethodNode> getEnclosingMethods() {
        return Collections.unmodifiableList(enclosingMethods);
    }

    /**
     * Pushes a class into the classes stack.
     * @param classNode the class to be pushed
     */
    public void pushEnclosingClassNode(ClassNode classNode) {
        enclosingClassNodes.addFirst(classNode);
    }

    /**
     * Pops a class from the enclosing classes stack.
     * @return the popped class
     */
    public ClassNode popEnclosingClassNode() {
        return enclosingClassNodes.removeFirst();
    }

    /**
     * Returns the class node which is on the top of the stack, or null
     * if there's no such element.
     * @return the enclosing class on top of the stack, or null if no such element.
     */
    public ClassNode getEnclosingClassNode() {
        if (enclosingClassNodes.isEmpty()) return null;
        return enclosingClassNodes.getFirst();
    }

    /**
     * Returns the current stack of enclosing classes. The first
     * element is the top of the stack, that is to say the currently visited class.
     * @return an immutable list of class nodes.
     */
    public List<ClassNode> getEnclosingClassNodes() {
        return Collections.unmodifiableList(enclosingClassNodes);
    }

    public void pushTemporaryTypeInfo() {
        Map<Object, List<ClassNode>> potentialTypes = new HashMap<Object, List<ClassNode>>();
        temporaryIfBranchTypeInformation.push(potentialTypes);
    }

    public void popTemporaryTypeInfo() {
        temporaryIfBranchTypeInformation.pop();
    }


    /**
     * Pushes a property expression into the property expression stack.
     * @param propertyExpression the property expression to be pushed
     */
    public void pushEnclosingPropertyExpression(PropertyExpression propertyExpression) {
        enclosingPropertyExpressions.addFirst(propertyExpression);
    }

    /**
     * Pops a property expression from the property expression stack.
     * @return the popped property expression
     */
    public Expression popEnclosingPropertyExpression() {
        return enclosingPropertyExpressions.removeFirst();
    }

    /**
     * Returns the property expression which is on the top of the stack, or null
     * if there's no such element.
     * @return the property expression on top of the stack, or null if no such element.
     */
    public Expression getEnclosingPropertyExpression() {
        if (enclosingPropertyExpressions.isEmpty()) return null;
        return enclosingPropertyExpressions.getFirst();
    }

    /**
     * Returns the current stack of property expressions. The first
     * element is the top of the stack, that is to say the currently visited property expression.
     * @return an immutable list of property expressions.
     */
    public List<PropertyExpression> getEnclosingPropertyExpressions() {
        return Collections.unmodifiableList(enclosingPropertyExpressions);
    }

    /**
     * Pushes a method call into the method call stack.
     * @param call the call expression to be pushed, either a {@link MethodCallExpression} or a {@link StaticMethodCallExpression}
     */
    public void pushEnclosingMethodCall(Expression call) {
        if (call instanceof MethodCallExpression || call instanceof StaticMethodCallExpression) {
            enclosingMethodCalls.addFirst(call);
        } else {
            throw new IllegalArgumentException("Expression must be a method call or a static method call");
        }
    }

    /**
     * Pops a method call from the enclosing method call stack.
     * @return the popped call
     */
    public Expression popEnclosingMethodCall() {
        return enclosingMethodCalls.removeFirst();
    }

    /**
     * Returns the method call which is on the top of the stack, or null
     * if there's no such element.
     * @return the enclosing method call on top of the stack, or null if no such element.
     */
    public Expression getEnclosingMethodCall() {
        if (enclosingMethodCalls.isEmpty()) return null;
        return enclosingMethodCalls.getFirst();
    }

    /**
     * Returns the current stack of enclosing method calls. The first
     * element is the top of the stack, that is to say the currently visited method call.
     * @return an immutable list of enclosing method calls.
     */
    public List<Expression> getEnclosingMethodCalls() {
        return Collections.unmodifiableList(enclosingMethodCalls);
    }


    /**
     * Pushes a constructor call into the constructor call stack.
     * @param call the call expression to be pushed
     */
    public void pushEnclosingConstructorCall(ConstructorCallExpression call) {
        enclosingConstructorCalls.addFirst(call);
    }

    /**
     * Pops a constructor call from the enclosing constructor call stack.
     * @return the popped call
     */
    public ConstructorCallExpression popEnclosingConstructorCall() {
        return enclosingConstructorCalls.removeFirst();
    }

    /**
     * Returns the constructor call which is on the top of the stack, or null
     * if there's no such element.
     * @return the enclosing constructor call on top of the stack, or null if no such element.
     */
    public ConstructorCallExpression getEnclosingConstructorCall() {
        if (enclosingConstructorCalls.isEmpty()) return null;
        return enclosingConstructorCalls.getFirst();
    }

    /**
     * Returns the current stack of enclosing constructor calls. The first
     * element is the top of the stack, that is to say the currently visited constructor call.
     * @return an immutable list of enclosing constructor calls.
     */
    public List<ConstructorCallExpression> getEnclosingConstructorCalls() {
        return Collections.unmodifiableList(enclosingConstructorCalls);
    }

    public List<ErrorCollector> getErrorCollectors() {
        return Collections.unmodifiableList(errorCollectors);
    }

    public ErrorCollector getErrorCollector() {
        if (errorCollectors.isEmpty()) return null;
        return errorCollectors.getFirst();
    }


    public void pushErrorCollector(ErrorCollector collector) {
        errorCollectors.add(0, collector);
    }

    public ErrorCollector pushErrorCollector() {
        ErrorCollector current = getErrorCollector();
        ErrorCollector collector = new ErrorCollector(current.getConfiguration());
        errorCollectors.add(0, collector);
        return collector;
    }

    public ErrorCollector popErrorCollector() {
        return errorCollectors.removeFirst();
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    public void setCompilationUnit(final CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
    }

    public SourceUnit getSource() {
        return source;
    }

    /**
     * Represents the context of an enclosing closure. An enclosing closure wraps
     * a closure expression and the list of return types found in the closure body.
     */
    public static class EnclosingClosure {
        private final ClosureExpression closureExpression;
        private final List<ClassNode> returnTypes;

        public EnclosingClosure(final ClosureExpression closureExpression) {
            this.closureExpression = closureExpression;
            this.returnTypes = new LinkedList<ClassNode>();
        }

        public ClosureExpression getClosureExpression() {
            return closureExpression;
        }

        public List<ClassNode> getReturnTypes() {
            return returnTypes;
        }

        public void addReturnType(ClassNode type) {
            returnTypes.add(type);
        }

        @Override
        public String toString() {
            String sb = "EnclosingClosure" +
                    "{closureExpression=" + closureExpression.getText() +
                    ", returnTypes=" + returnTypes +
                    '}';
            return sb;
        }
    }
}