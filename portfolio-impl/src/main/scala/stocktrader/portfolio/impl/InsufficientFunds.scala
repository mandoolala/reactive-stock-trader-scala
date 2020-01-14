package stocktrader.portfolio.impl

import com.lightbend.lagom.scaladsl.api.transport.{TransportErrorCode, TransportException}

class InsufficientFunds(message: String) extends TransportException(TransportErrorCode.BadRequest, message)

