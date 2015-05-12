package be.vrt.services.logging.log.consumer.appender;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.MDC;

import be.vrt.services.logging.log.common.Constants;
import be.vrt.services.logging.log.consumer.config.EnvironmentSetting;
import be.vrt.services.logging.log.consumer.dto.JsonLogWrapperDto;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractJsonAppender extends AppenderBase<ILoggingEvent> implements Constants {

	private ObjectMapper mapper = new ObjectMapper();

	@Override
	protected void append(ILoggingEvent logEvent) {

//		if(!e.getMDCPropertyMap().containsKey(TRANSACTION_ID)){
//			return;
//		}
		JsonLogWrapperDto dto = new JsonLogWrapperDto();
		Object[] objects = logEvent.getArgumentArray();
		String json;
		try {
			dto.setDate(new Date());
			dto.setTransactionId(MDC.get(TRANSACTION_ID));
			String hostname;
			try {
				hostname = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException ex) {
				hostname = "<UnknownHost>";
			}
			dto.setHostName(hostname);

			if (objects != null) {
				for (Object object : objects) {
					if (object == null) {
						dto.getContent().put("noValue", "null");
					}
					dto.getContent().put(object.getClass().getSimpleName(), object);
				}
			}
			dto.setLogComment(logEvent.getMessage());
			dto.setDate(new Date(logEvent.getTimeStamp()));

			dto.setClassName(logEvent.getCallerData()[0].getClassName());
			dto.setMethodName(logEvent.getCallerData()[0].getMethodName());
			dto.setLineNumber(logEvent.getCallerData()[0].getLineNumber());
			dto.setEnvironmentInfo(EnvironmentSetting.log);
			dto.setLoggerName(logEvent.getLoggerName());
			dto.setLogLevel(logEvent.getLevel().toString());
			try {
				json = mapper.writeValueAsString(dto);
			} catch (Exception ex) {
				dto.setContent(null);
				dto.setLogComment(logEvent.getFormattedMessage());
				json = mapper.writeValueAsString(dto);
			}
			persist(json);
		} catch (Exception ex) {
			System.err.println("Failed to process Json2: " + ex.getMessage());
		}
	}

	protected abstract void persist(String json);

	protected abstract Logger getLogger();
}