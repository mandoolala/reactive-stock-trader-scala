import java.util.UUID

//import com.lightbend.lagom.scaladsl.api.deser.PathParamSerializer
//case class X_PortfolioId(id: String)
//Implicit val paramSerializer: PathParamSerializer[X_PortfolioId] = {
//    PathParamSerializer.required("PortfolioId")(X_PortfolioId(_))(_.id)
//  }

package object stocktrader {

  type PortfolioId = String

  object PortfolioId {
    def apply(id: String): PortfolioId = id
    def newId: PortfolioId = UUID.randomUUID().toString
  }

  type OrderId = String

  object OrderId {
    def apply(id: String): OrderId = id
    def newId: OrderId = UUID.randomUUID().toString
  }

  type TransferId = String

  object TransferId {
    def apply(id: String): TransferId = id
    def newId: TransferId = UUID.randomUUID().toString
  }

  type AccountId = String

  object AccountId {
    def apply(id: String): AccountId = id
    //def newId: AccountId = UUID.randomUUID().toString
  }

}
