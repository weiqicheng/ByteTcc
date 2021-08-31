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
package org.bytesoft.bytetcc.xa;

import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XidFactory;

/**
 * 负责创建全局事务和分支事务XID
 */
public class XidFactoryImpl extends org.bytesoft.bytejta.xa.XidFactoryImpl implements XidFactory {

	/**
	 * 创建全局事务XID
	 * @return
	 */
	public TransactionXid createGlobalXid() {
		TransactionXid xid = super.createGlobalXid();
		xid.setFormatId(XidFactory.TCC_FORMAT_ID);
		return xid;
	}

	/**
	 * 根据全局事务XID的二进制流，创建全局事务XID
	 * @param globalTransactionId
	 * @return
	 */
	public TransactionXid createGlobalXid(byte[] globalTransactionId) {
		TransactionXid xid = super.createGlobalXid(globalTransactionId);
		xid.setFormatId(XidFactory.TCC_FORMAT_ID);
		return xid;
	}

	/**
	 * 根据全局事务XID创建分支事务XID
	 * @param globalXid
	 * @return
	 */
	public TransactionXid createBranchXid(TransactionXid globalXid) {
		TransactionXid xid = super.createBranchXid(globalXid);
		xid.setFormatId(XidFactory.TCC_FORMAT_ID);
		return xid;
	}

	/**
	 * 根据全局事务XID和分支事务标识二进制流创建分支事务XID
	 * @param globalXid
	 * @param branchQualifier
	 * @return
	 */
	public TransactionXid createBranchXid(TransactionXid globalXid, byte[] branchQualifier) {
		TransactionXid xid = super.createBranchXid(globalXid, branchQualifier);
		xid.setFormatId(XidFactory.TCC_FORMAT_ID);
		return xid;
	}

}
