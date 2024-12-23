package proofspace.trustregistry.common

import scalus.Compile
import scalus.builtin.{Builtins, Data}
import scalus.builtin.Data.{FromData, ToData}

@Compile
object PreludeListData {
  
  def listToData[T:ToData](l: scalus.prelude.List[T]): scalus.builtin.Data = {
    val s0: scalus.builtin.List[Data] = scalus.builtin.List.empty
    val s = scalus.prelude.List.foldLeft(l, s0) { (acc, x) =>
      val xData = summon[ToData[T]](x)
      Builtins.mkCons(xData, acc)
    }
    Builtins.listData(s)
  }
  
  def listFromData[T:FromData](data: scalus.builtin.Data): scalus.prelude.List[T] = {
    val s = Builtins.unListData(data)
    // here are reversed order of elements
    buidtinToPrelude(s, scalus.prelude.List.Nil)
  }
  
  def buidtinToPrelude[T:FromData](bultinList: scalus.builtin.List[Data], acc: scalus.prelude.List[T]): scalus.prelude.List[T] = {
    if (bultinList.isEmpty) then
      acc
    else
      val x = summon[FromData[T]](bultinList.head)
      buidtinToPrelude(bultinList.tail, scalus.prelude.List.Cons(x, acc))
  }
  
  
} 
