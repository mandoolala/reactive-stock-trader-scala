package stocktrader.portfolio.impl

import com.lightbend.lagom.scaladsl.api.transport.{TransportErrorCode, TransportException}

class InsufficientShares(message: String) extends TransportException(TransportErrorCode.BadRequest, message)
