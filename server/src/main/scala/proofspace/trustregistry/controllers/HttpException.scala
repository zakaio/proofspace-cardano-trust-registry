package proofspace.trustregistry.controllers

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import sttp.model.StatusCode

case class HttpException(statusCode: StatusCode,
                         message: String,
                         errorId: Option[String]=None,
                         toLog: Boolean = true,
                         cause:Throwable=null) extends Exception(message, cause) {

  def toDTO: HttpExceptionDTO = HttpExceptionDTO(statusCode.code, message, errorId)

}

object HttpException {

  given JsonValueCodec[HttpException] = new JsonValueCodec[HttpException] {
    override def decodeValue(in: JsonReader, default: HttpException): HttpException = {
      summon[JsonValueCodec[HttpExceptionDTO]].decodeValue(in, default.toDTO).unDTO
    }

    override def encodeValue(x: HttpException, out: JsonWriter): Unit = {
      summon[JsonValueCodec[HttpExceptionDTO]].encodeValue(x.toDTO, out)
    }

    override def nullValue: HttpException = {
      if (summon[JsonValueCodec[HttpExceptionDTO]].nullValue == null) null
      else summon[JsonValueCodec[HttpExceptionDTO]].nullValue.unDTO
    }
  }

}

case class HttpExceptionDTO(
                           statusCode: Int,
                           message: String,
                           errorId: Option[String]=None
                           ) {

  def unDTO: HttpException = HttpException(StatusCode(statusCode), message, errorId)

}

object HttpExceptionDTO {

  given JsonValueCodec[HttpExceptionDTO] = JsonCodecMaker.make

}