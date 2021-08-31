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
package org.bytesoft.bytetcc;

import java.io.Serializable;

import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.CompensableContext;
import org.bytesoft.compensable.CompensableManager;
import org.bytesoft.compensable.CompensableTransaction;
import org.bytesoft.compensable.aware.CompensableBeanFactoryAware;

/**
 * ByteTCC上下文
 * 1、获取ByteTCC的一些变量值
 */
public class CompensableContextImpl implements CompensableContext, CompensableBeanFactoryAware {
	@javax.inject.Inject
	private CompensableBeanFactory beanFactory;

	/**
	 * 当前TCC服务是否重试
	 * @return
	 * @throws IllegalStateException
	 */
	public boolean isCurrentCompensableServiceTried() throws IllegalStateException {
		CompensableManager compensableManager = this.beanFactory.getCompensableManager();
		if (compensableManager == null) {
			throw new IllegalStateException("org.bytesoft.compensable.CompensableManager is undefined!");
		}
		CompensableTransaction compensable = compensableManager.getCompensableTransactionQuietly();
		if (compensable == null) {
			throw new IllegalStateException("There is no active compensable transaction!");
		} else if (compensable.getTransactionContext().isCompensating()) {
			return compensable.isCurrentCompensableServiceTried();
		}

		return false;
	}

	/**
	 * 根据key值，获取补偿型事务的一些变量值
	 * @param key
	 * @return
	 */
	public Serializable getVariable(String key) {
		CompensableManager compensableManager = this.beanFactory.getCompensableManager();
		if (compensableManager == null) {
			throw new IllegalStateException("org.bytesoft.compensable.CompensableManager is undefined!");
		}
		CompensableTransaction compensable = compensableManager.getCompensableTransactionQuietly();
		if (compensable == null) {
			throw new IllegalStateException("There is no active compensable transaction!");
		}
		return compensable.getVariable(key);
	}

	/**
	 * 设置补偿型事务变量值
	 * @param key
	 * @param variable
	 */
	public void setVariable(String key, Serializable variable) {
		CompensableManager compensableManager = this.beanFactory.getCompensableManager();
		if (compensableManager == null) {
			throw new IllegalStateException("org.bytesoft.compensable.CompensableManager is undefined!");
		}
		CompensableTransaction compensable = compensableManager.getCompensableTransactionQuietly();
		if (compensable == null) {
			throw new IllegalStateException("There is no active compensable transaction!");
		} else if (compensable.getTransactionContext().isCompensating()) {
			throw new IllegalStateException("CompensableContext.setVariable(String) is forbidden in compensable phase!");
		}
		compensable.setVariable(key, variable);
	}

	public CompensableBeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setBeanFactory(CompensableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

}
