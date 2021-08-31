/**
 * Copyright 2014-2016 yangming.liu<bytefox@126.com>.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
package org.bytesoft.compensable;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compensable代理类注册管理
 * 1、基于线程为一个调度单位，管理Compensable代理类的注册和取消注册、以及代理类的获取
 */
public final class CompensableInvocationRegistry {
	static final CompensableInvocationRegistry instance = new CompensableInvocationRegistry();

	private Map<Thread, Stack<CompensableInvocation>> invocationMap = new ConcurrentHashMap<Thread, Stack<CompensableInvocation>>();

	private CompensableInvocationRegistry() {
	}

	/**
	 * 基于当前线程，注册代理类
	 * @param invocation
	 */
	public void register(CompensableInvocation invocation) {
		Thread current = Thread.currentThread();
		Stack<CompensableInvocation> stack = this.invocationMap.get(current);
		if (stack == null) {
			stack = new Stack<CompensableInvocation>();
			this.invocationMap.put(current, stack);
		}
		stack.push(invocation);
	}

	/**
	 * 基于当前线程，获取出当前的代理对象
	 * @return
	 */
	public CompensableInvocation getCurrent() {
		Thread current = Thread.currentThread();
		Stack<CompensableInvocation> stack = this.invocationMap.get(current);
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		return stack.peek();
	}

	/**
	 * 取消Compensable代理类的注册
	 * @return
	 */
	public CompensableInvocation unRegister() {
		Thread current = Thread.currentThread();
		Stack<CompensableInvocation> stack = this.invocationMap.get(current);
		if (stack == null || stack.isEmpty()) {
			// 如果当前线程没有注册到任何一个Compensable代理类
			return null;
		}

		// 获取当前线程注册的代理类的顶层代理类
		CompensableInvocation invocation = stack.pop();
		if (stack.isEmpty()) {
			// 如果当前线程Compensable注册表为空，删除注册表
			this.invocationMap.remove(current);
		}
		return invocation;
	}

	public static CompensableInvocationRegistry getInstance() {
		return instance;
	}
}
