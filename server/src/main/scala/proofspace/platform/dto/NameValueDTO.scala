package proofspace.platform.dto

case class NameValueDTO(name: String, value:String)

object NameValueDTO {

  def merge(x: Seq[NameValueDTO], y:Seq[NameValueDTO]): Seq[NameValueDTO] = {
    x ++ y.flatMap{ yc =>
      x.find(_.name == yc.name) match
        case Some(xc) => if (xc.value == yc.value) None else throw IllegalArgumentException(s"Can't merge $x and $y")
        case None => Some(yc)
    }
  }

}

