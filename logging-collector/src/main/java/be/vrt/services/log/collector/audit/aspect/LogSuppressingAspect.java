package be.vrt.services.log.collector.audit.aspect;

import be.vrt.services.logging.log.common.LogTransaction;
import static be.vrt.services.logging.log.common.LogTransaction.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;

@Aspect
@Order(-50)
public class LogSuppressingAspect {

	@Pointcut("within(@be.vrt.services.logging.api.audit.annotation.LogSuppress *) || @annotation(be.vrt.services.logging.api.audit.annotation.LogSuppress)")
	public void suppressing() {
	}

	@Pointcut("within(@be.vrt.services.logging.api.audit.annotation.LogUnsuppress *) || @annotation(be.vrt.services.logging.api.audit.annotation.LogUnsuppress)")
	public void unsuppress() {
	}

	@Pointcut("execution(public * *(..))")
	public void publicMethod() {
	}

	@Around("suppressing() ")
	public Object logSuppress(ProceedingJoinPoint joinPoint) throws Throwable {
		boolean suppressed = LogTransaction.isSuppressed();
		try {
			if (!suppressed) {
				LogTransaction.logSuppress(joinPoint.toShortString());
			}
			Object o = joinPoint.proceed();
			return o;
		} finally {
			if (!suppressed) {
				LogTransaction.logUnsuppress(joinPoint.toShortString());
			}

		}
	}

	@Around("unsuppress()")
	public Object logUnsuppress(ProceedingJoinPoint joinPoint) throws Throwable {
		boolean suppressed = LogTransaction.isSuppressed();
		try {
			if (suppressed) {
				LogTransaction.logUnsuppress(joinPoint.toShortString());
			}
			return joinPoint.proceed();
		} finally {
			if (suppressed) {
				LogTransaction.logSuppress(joinPoint.toShortString());
			}
		}

	}
}
 