package be.vrt.services.log.collector.transaction.amqp;

import be.vrt.services.log.collector.exception.FailureException;
import be.vrt.services.log.collector.transaction.dto.AmqpTransactionLogDto;
import be.vrt.services.logging.log.common.Constants;
import be.vrt.services.logging.log.common.LogTransaction;
import be.vrt.services.logging.log.common.dto.LogType;
import be.vrt.services.logging.log.common.transaction.TransactionRegistery;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.Date;
import java.util.Map;

import static be.vrt.services.log.collector.util.ElasticNotAllowedCharactersFilter.filter;
import static be.vrt.services.log.collector.util.ElasticNotAllowedCharactersFilter.filter;

public class TransactionLoggerAmqpAdvice implements MethodInterceptor {

	private final Logger log = LoggerFactory.getLogger(TransactionLoggerAmqpAdvice.class);

	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		AmqpTransactionLogDto transaction = generateTransactionLogDtoFromAmqpMessage(mi);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		transaction.setStartDate(new Date(stopWatch.getStartTime()));
		transaction.setStatus(LogType.OK);
		try {
			return mi.proceed();
		} catch (FailureException e) {
			transaction.setErrorReason(e.getMessage());
			transaction.setStatus(LogType.FAILED);
			throw e;
		} catch (Throwable e) {
			transaction.setErrorReason(e.getMessage());
			transaction.setStatus(LogType.ERROR);
			throw e;
		} finally {
			stopWatch.stop();
			transaction.setFlowId(LogTransaction.flow());
			transaction.setDuration(stopWatch.getTime());
			log.info("Filter on : {} ", transaction.getQueueName(), transaction);
			TransactionRegistery.register(transaction);
			LogTransaction.resetThread();

		}
	}

	private AmqpTransactionLogDto generateTransactionLogDtoFromAmqpMessage(MethodInvocation mi) {
		AmqpTransactionLogDto transaction = new AmqpTransactionLogDto();

		String hostname;
		String headerFlowId = null;
		String originUser = null;
		hostname = LogTransaction.hostname();
		if (mi.getArguments().length == 2 && mi.getArguments()[1] instanceof Message) {
			MessageProperties props = ((Message) mi.getArguments()[1]).getMessageProperties();
			transaction.setExchange(props.getReceivedExchange());
			transaction.setQueueName(props.getConsumerQueue());
			transaction.setRoutingKey(props.getReceivedRoutingKey());
			transaction.setHeaders(filter(props.getHeaders()));
			headerFlowId = (String) props.getHeaders().get(Constants.FLOW_ID);
			originUser = (String) props.getHeaders().get(Constants.ORIGIN_USER);
		}

		String transactionUUID = LogTransaction.id();
		String flowId = LogTransaction.createFlowId(headerFlowId, originUser);
		transaction.setFlowId(flowId);
		transaction.setTransactionId(transactionUUID);
		transaction.setServerName(hostname);

//		transaction.setResource(request.getRequestURL().toString());
		return transaction;
	}
}
