/** *************************************************************************************
 * File         : RegMap.scala
 * Authors      : Yinyuan Zhao, Yan Tong
 * Date         : 03/04/2024
 * Description  : Scala implementation of Register Map
 * ************************************************************************************* */

package reg_renaming.model.reg_renaming_table

import reg_renaming.OpConfig

class RegMap(config: OpConfig) {
  private var _regMap: Array[Int] = Array.ofDim[Int](config.archIdNum)

  def getPtag(index: Int): Int = {
    require(index >= 0 && index < _regMap.length, "Invalid index for regMap")
    _regMap(index)
  }

  def setPtag(index: Int, value: Int): Unit = {
    require(index >= 0 && index < _regMap.length, "Invalid index for regMap")
    _regMap(index) = value
  }
}
