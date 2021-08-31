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
package org.bytesoft.bytetcc.supports.rpc;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.bytesoft.bytejta.supports.resource.RemoteResourceDescriptor;
import org.bytesoft.bytejta.supports.rpc.TransactionRequestImpl;
import org.bytesoft.bytejta.supports.rpc.TransactionResponseImpl;
import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.CompensableManager;
import org.bytesoft.compensable.CompensableTransaction;
import org.bytesoft.compensable.aware.CompensableBeanFactoryAware;
import org.bytesoft.transaction.TransactionContext;
import org.bytesoft.transaction.TransactionParticipant;
import org.bytesoft.transaction.remote.RemoteCoordinator;
import org.bytesoft.transaction.supports.rpc.TransactionInterceptor;
import org.bytesoft.transaction.supports.rpc.TransactionRequest;
import org.bytesoft.transaction.supports.rpc.TransactionResponse;
import org.bytesoft.transaction.xa.TransactionXid;
import org.bytesoft.transaction.xa.XidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compensable拦截器
 * 处理在发送请求、接收请求、处理请求后返回、接收请求处理返回的逻辑处理
 */
public class CompensableInterceptorImpl implements TransactionInterceptor, CompensableBeanFactoryAware {
    static final Logger logger = LoggerFactory.getLogger(CompensableInterceptorImpl.class);

    @javax.inject.Inject
    private CompensableBeanFactory beanFactory;

    /**
     * 发送请求之前处理的事情
     * 1、处理事务传播的问题
     *
     * @param request
     * @throws IllegalStateException
     */
    public void beforeSendRequest(TransactionRequest request) throws IllegalStateException {
        // 获取Compensable管理者
        CompensableManager compensableManager = this.beanFactory.getCompensableManager();
        // 获取XID工厂
        XidFactory xidFactory = this.beanFactory.getCompensableXidFactory();
        // 获取补偿型事务
        CompensableTransaction transaction = compensableManager.getCompensableTransactionQuietly();
        if (transaction == null) {
            return;
        }

        // 获取事务上下文，构建新的事务上下文，设置到request域
        TransactionContext srcTransactionContext = transaction.getTransactionContext();
        TransactionContext transactionContext = srcTransactionContext.clone();
        TransactionXid currentXid = srcTransactionContext.getXid();
        TransactionXid globalXid = xidFactory.createGlobalXid(currentXid.getGlobalTransactionId());
        transactionContext.setXid(globalXid);
        request.setTransactionContext(transactionContext);

        if (transaction.getTransactionStatus() == Status.STATUS_MARKED_ROLLBACK) {
            // 只回滚事务，无法传播到远程事务分支
            throw new IllegalStateException(
                    "Transaction has been marked as rollback only, can not propagate its context to remote branch.");
        } // end-if (transaction.getTransactionStatus() == Status.STATUS_MARKED_ROLLBACK)

        try {
            RemoteCoordinator resource = request.getTargetTransactionCoordinator();
            RemoteResourceDescriptor descriptor = new RemoteResourceDescriptor();
            descriptor.setDelegate(resource);
            descriptor.setIdentifier(resource.getIdentifier());

            boolean participantEnlisted = transaction.enlistResource(descriptor);
            ((TransactionRequestImpl) request).setParticipantEnlistFlag(participantEnlisted);
        } catch (IllegalStateException ex) {
            logger.error("CompensableInterceptorImpl.beforeSendRequest({})", request, ex);
            throw ex;
        } catch (RollbackException ex) {
            transaction.setRollbackOnlyQuietly();
            logger.error("CompensableInterceptorImpl.beforeSendRequest({})", request, ex);
            throw new IllegalStateException(ex);
        } catch (SystemException ex) {
            logger.error("CompensableInterceptorImpl.beforeSendRequest({})", request, ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 接收到请求后处理的事情
     *
     * @param request
     * @throws IllegalStateException
     */
    public void afterReceiveRequest(TransactionRequest request) throws IllegalStateException {
        TransactionContext srcTransactionContext = request.getTransactionContext();
        if (srcTransactionContext == null) {
            return;
        }

        TransactionParticipant compensableCoordinator = this.beanFactory.getCompensableNativeParticipant();
        TransactionContext transactionContext = srcTransactionContext.clone();
        transactionContext.setPropagatedBy(srcTransactionContext.getPropagatedBy());
        try {
            compensableCoordinator.start(transactionContext, XAResource.TMNOFLAGS);
        } catch (XAException ex) {
            logger.error("CompensableInterceptorImpl.afterReceiveRequest({})", request, ex);
            IllegalStateException exception = new IllegalStateException();
            exception.initCause(ex);
            throw exception;
        }

    }

    /**
     * 返回远程调用前需要处理的事情
     *
     * @param response
     * @throws IllegalStateException
     */
    public void beforeSendResponse(TransactionResponse response) throws IllegalStateException {
        CompensableManager compensableManager = this.beanFactory.getCompensableManager();
        CompensableTransaction transaction = compensableManager.getCompensableTransactionQuietly();
        if (transaction == null) {
            return;
        }

        TransactionParticipant compensableCoordinator = this.beanFactory.getCompensableNativeParticipant();

        TransactionContext srcTransactionContext = transaction.getTransactionContext();
        TransactionContext transactionContext = srcTransactionContext.clone();
        transactionContext.setPropagatedBy(srcTransactionContext.getPropagatedBy());
        response.setTransactionContext(transactionContext);
        try {
            compensableCoordinator.end(transactionContext, XAResource.TMSUCCESS);
        } catch (XAException ex) {
            logger.error("CompensableInterceptorImpl.beforeSendResponse({})", response, ex);
            IllegalStateException exception = new IllegalStateException();
            exception.initCause(ex);
            throw exception;
        }
    }

    /**
     * 接收到被调用者和返回时需要处理的事情
     *
     * @param response
     * @throws IllegalStateException
     */
    public void afterReceiveResponse(TransactionResponse response) throws IllegalStateException {
        CompensableManager compensableManager = this.beanFactory.getCompensableManager();
        TransactionContext remoteTransactionContext = response.getTransactionContext();
        CompensableTransaction transaction = compensableManager.getCompensableTransactionQuietly();

        boolean participantEnlistFlag = ((TransactionResponseImpl) response).isParticipantEnlistFlag();
        boolean participantDelistFlag = ((TransactionResponseImpl) response).isParticipantDelistFlag();

        RemoteCoordinator resource = response.getSourceTransactionCoordinator();

        if (transaction == null || remoteTransactionContext == null) {
            return;
        } else if (participantEnlistFlag == false) {
            return;
        } else if (resource == null) {
            logger.error("CompensableInterceptorImpl.afterReceiveResponse(TransactionRequest): remote coordinator is null.");
            throw new IllegalStateException("remote coordinator is null.");
        }

        try {
            RemoteResourceDescriptor descriptor = new RemoteResourceDescriptor();
            descriptor.setDelegate(resource);
            // descriptor.setIdentifier(resource.getIdentifier());

            transaction.delistResource(descriptor, participantDelistFlag ? XAResource.TMFAIL : XAResource.TMSUCCESS);
        } catch (IllegalStateException ex) {
            logger.error("CompensableInterceptorImpl.afterReceiveResponse({})", response, ex);
            throw ex;
        } catch (SystemException ex) {
            logger.error("CompensableInterceptorImpl.afterReceiveResponse({})", response, ex);
            throw new IllegalStateException(ex);
        }
    }

    public void setBeanFactory(CompensableBeanFactory tbf) {
        this.beanFactory = tbf;
    }

    public CompensableBeanFactory getBeanFactory() {
        return beanFactory;
    }

}
