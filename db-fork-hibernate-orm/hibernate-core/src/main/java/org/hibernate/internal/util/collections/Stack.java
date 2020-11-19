/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Stack implementation exposing useful methods for Hibernate needs.
 *
 * @param <T> The type of things stored in the stack
 *
 * @author Steve Ebersole
 */
public interface Stack<T> {
	/**
	 * Push the new element on the top of the stack
	 */
	void push(T newCurrent);

	/**
	 * Pop (remove and return) the current element off the stack
	 */
	T pop();

	/**
	 * The element currently at the top of the stack
	 */
	T getCurrent();

	/**
	 * The element previously at the top of the stack before the current one
	 */
	T getPrevious();

	/**
	 * How many elements are currently on the stack?
	 */
	int depth();

	/**
	 * Are there no elements currently in the stack?
	 */
	boolean isEmpty();

	/**
	 * Remmove all elements from the stack
	 */
	void clear();

	/**
	 * Visit all elements in the stack, starting with the current and working back
	 */
	void visitCurrentFirst(Consumer<T> action);

	/**
	 * Find an element on the stack and return a value.  The first non-null element
	 * returned from `action` stops the iteration and is returned from here
	 */
	<X> X findCurrentFirst(Function<T, X> action);
}
