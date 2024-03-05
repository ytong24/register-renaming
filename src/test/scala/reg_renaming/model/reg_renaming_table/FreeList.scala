/** *************************************************************************************
 * File         : FreeList.scala
 * Authors      : Yinyuan Zhao, Yan Tong
 * Date         : 03/04/2024
 * Description  : Scala implementation of Free List
 * ************************************************************************************* */

package reg_renaming.model.reg_renaming_table

import scala.collection.mutable

class FreeList(config: RegRenamingTableConfig) {
  private val _freeListStack = mutable.Stack[Int](0 until config.ptagNum: _*)

  def push(ptag: Int): Unit = {
    require(ptag >= 0 && ptag < config.ptagNum, "ptag out of range")
    _freeListStack.push(ptag)
  }

  def pop(): Int = {
    require(_freeListStack.nonEmpty, "Free list is empty")
    _freeListStack.pop()
  }

  def size(): Int = {
    _freeListStack.size
  }
}

