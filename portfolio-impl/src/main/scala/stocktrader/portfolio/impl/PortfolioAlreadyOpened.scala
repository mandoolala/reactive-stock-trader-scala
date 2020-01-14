package stocktrader.portfolio.impl

import stocktrader.PortfolioId

class PortfolioAlreadyOpened(portfolioId: PortfolioId) extends RuntimeException(s"PortfolioModel $portfolioId already initialized.")
