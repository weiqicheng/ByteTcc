/**
 * Copyright 2014-2016 yangming.liu<bytefox@126.com>.
 * <p>
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
package org.bytesoft.bytetcc;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.aware.CompensableBeanFactoryAware;
import org.bytesoft.transaction.Transaction;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionParticipant;
import org.bytesoft.transaction.remote.RemoteAddr;
import org.bytesoft.transaction.remote.RemoteCoordinator;
import org.bytesoft.transaction.remote.RemoteNode;
import org.bytesoft.transaction.xa.XidFactory;

/**
 * 事务协调者
 * 1、处理事务的开启、提交、回滚、恢复、丢弃
 */
public class TransactionCoordinator implements RemoteCoordinator, CompensableBeanFactoryAware {

    @javax.inject.Inject
    private CompensableBeanFactory beanFactory;

    /**
     * 准备阶段
     * 根据XID判断当前事务属于哪种类型，分别使用不同的协调者来开启事务
     *
     * @param xid
     * @return
     * @throws XAException
     */
    public int prepare(Xid xid) throws XAException {
        // 获取补偿型事务协调者
        TransactionParticipant compensableCoordinator = this.beanFactory.getCompensableNativeParticipant();
        // 获取普通事务协调者
        TransactionParticipant transactionCoordinator = this.beanFactory.getTransactionNativeParticipant();

        int formatId = xid.getFormatId();
        if (XidFactory.JTA_FORMAT_ID == formatId) {
            // JTA事务
            return transactionCoordinator.prepare(xid);
        } else if (XidFactory.TCC_FORMAT_ID == formatId) {
            // TCC事务
            return compensableCoordinator.prepare(xid);
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }
    }

    /**
     * 提交阶段
     * 根据XID选择相应的事务协调者来提交事务
     *
     * @param xid
     * @param onePhase
     * @throws XAException
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        // 获取补偿型事务协调者
        TransactionParticipant compensableCoordinator = this.beanFactory.getCompensableNativeParticipant();
        // 获取普通事务协调者
        TransactionParticipant transactionCoordinator = this.beanFactory.getTransactionNativeParticipant();

        int formatId = xid.getFormatId();
        if (XidFactory.JTA_FORMAT_ID == formatId) {
            transactionCoordinator.commit(xid, onePhase);
        } else if (XidFactory.TCC_FORMAT_ID == formatId) {
            compensableCoordinator.commit(xid, onePhase);
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }
    }

    /**
     * 回滚阶段，和begin/commit阶段逻辑类似
     *
     * @param xid
     * @throws XAException
     */
    public void rollback(Xid xid) throws XAException {
        TransactionParticipant compensableCoordinator = this.beanFactory.getCompensableNativeParticipant();
        TransactionParticipant transactionCoordinator = this.beanFactory.getTransactionNativeParticipant();

        int formatId = xid.getFormatId();
        if (XidFactory.JTA_FORMAT_ID == formatId) {
            transactionCoordinator.rollback(xid);
        } else if (XidFactory.TCC_FORMAT_ID == formatId) {
            compensableCoordinator.rollback(xid);
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }
    }

    /**
     * 事务恢复
     * 分别进行补偿型事务恢复和普通事务恢复，
     * 返回的XID数组，合并返回
     *
     * @param flags
     * @return
     * @throws XAException
     */
    public Xid[] recover(int flags) throws XAException {
        TransactionParticipant compensableCoordinator = this.beanFactory.getCompensableNativeParticipant();
        TransactionParticipant transactionCoordinator = this.beanFactory.getTransactionNativeParticipant();

        Xid[] jtaXidArray = null;
        try {
            jtaXidArray = transactionCoordinator.recover(flags);
        } catch (Exception ex) {
            jtaXidArray = new Xid[0];
        }

        Xid[] tccXidArray = null;
        try {
            tccXidArray = compensableCoordinator.recover(flags);
        } catch (Exception ex) {
            tccXidArray = new Xid[0];
        }

        Xid[] resultArray = new Xid[jtaXidArray.length + tccXidArray.length];
        System.arraycopy(jtaXidArray, 0, resultArray, 0, jtaXidArray.length);
        System.arraycopy(tccXidArray, 0, resultArray, jtaXidArray.length, tccXidArray.length);
        return resultArray;
    }

    /** 遗忘事务，也就是丢弃事务
     * @param xid
     * @throws XAException
     */
    public void forget(Xid xid) throws XAException {
        TransactionParticipant compensableCoordinator = this.beanFactory.getCompensableNativeParticipant();
        TransactionParticipant transactionCoordinator = this.beanFactory.getTransactionNativeParticipant();

        int formatId = xid.getFormatId();
        if (XidFactory.JTA_FORMAT_ID == formatId) {
            transactionCoordinator.forget(xid);
        } else if (XidFactory.TCC_FORMAT_ID == formatId) {
            compensableCoordinator.forget(xid);
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }
    }

    /**
     * 静默丢弃事务
     * @param xid
     */
    public void forgetQuietly(Xid xid) {
        TransactionParticipant compensableCoordinator = this.beanFactory.getCompensableNativeParticipant();
        TransactionParticipant transactionCoordinator = this.beanFactory.getTransactionNativeParticipant();

        int formatId = xid.getFormatId();
        if (XidFactory.JTA_FORMAT_ID == formatId) {
            transactionCoordinator.forgetQuietly(xid);
        } else if (XidFactory.TCC_FORMAT_ID == formatId) {
            compensableCoordinator.forgetQuietly(xid);
        }
    }

    public RemoteAddr getRemoteAddr() {
        return ((RemoteCoordinator) this.beanFactory.getCompensableNativeParticipant()).getRemoteAddr();
    }

    public RemoteNode getRemoteNode() {
        return ((RemoteCoordinator) this.beanFactory.getCompensableNativeParticipant()).getRemoteNode();
    }

    public String getApplication() {
        return ((RemoteCoordinator) this.beanFactory.getCompensableNativeParticipant()).getApplication();
    }

    public String getIdentifier() {
        return ((RemoteCoordinator) this.beanFactory.getCompensableNativeParticipant()).getIdentifier();
    }

    public void start(Xid xid, int flags) throws XAException {
        throw new XAException(XAException.XAER_RMERR);
    }

    public void end(Xid xid, int flags) throws XAException {
        throw new XAException(XAException.XAER_RMERR);
    }

    public Transaction start(TransactionContext transactionContext, int flags) throws XAException {
        throw new XAException(XAException.XAER_RMERR);
    }

    public Transaction end(TransactionContext transactionContext, int flags) throws XAException {
        throw new XAException(XAException.XAER_RMERR);
    }

    public boolean isSameRM(XAResource xares) throws XAException {
        throw new XAException(XAException.XAER_RMERR);
    }

    public boolean setTransactionTimeout(int arg0) throws XAException {
        throw new XAException(XAException.XAER_RMERR);
    }

    public int getTransactionTimeout() throws XAException {
        throw new XAException(XAException.XAER_RMERR);
    }

    public CompensableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    public void setBeanFactory(CompensableBeanFactory tbf) {
        this.beanFactory = tbf;
    }

}
