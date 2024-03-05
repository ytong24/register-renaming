package reg_renaming.tester

import chiseltest.ChiselScalatestTester
import org.scalatest.flatspec.AnyFlatSpec
import reg_renaming.model.reg_renaming_table.{FreeList, RegRenamingTableConfig}

class ModelTester extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "FreeList"
  it should "be initialized with the correct size" in {
    val config = RegRenamingTableConfig(ptagNum = 5)
    val freeList = new FreeList(config)
    assert(freeList.size() == config.ptagNum, "Initial FreeList size should be equal to ptagNum")
  }

  it should "decrease in size by one after a pop" in {
    val config = RegRenamingTableConfig(ptagNum = 5)
    val freeList = new FreeList(config)
    val initialSize = freeList.size()
    freeList.pop()
    assert(freeList.size() == initialSize - 1, "FreeList size should decrease by 1 after pop")
  }

  it should "increase in size by one after a push" in {
    val config = RegRenamingTableConfig(ptagNum = 5)
    val freeList = new FreeList(config)
    val initialSize = freeList.size()
    val popped = freeList.pop()
    freeList.push(popped)
    assert(freeList.size() == initialSize, "FreeList size should increase by 1 after push")
  }

  it should "pop values within valid range" in {
    val config = RegRenamingTableConfig(ptagNum = 5)
    val freeList = new FreeList(config)
    val popped = freeList.pop()
    assert(popped >= 0 && popped < config.ptagNum, "Popped value should be within valid range")
  }

  it should "restore its size after popping and then pushing" in {
    val config = RegRenamingTableConfig(ptagNum = 5)
    val freeList = new FreeList(config)
    val initialSize = freeList.size()
    val popped = freeList.pop()
    freeList.push(popped)
    assert(freeList.size() == initialSize, "FreeList size should be restored after pushing the popped element back")
  }

it should "throw an exception when popping from an empty FreeList" in {
  val emptyConfig = RegRenamingTableConfig(ptagNum = 0)
  val emptyFreeList = new FreeList(emptyConfig)

  intercept[IllegalArgumentException] {
    emptyFreeList.pop()
  }
}

it should "throw an exception when pushing to a full FreeList" in {
  val config = RegRenamingTableConfig(ptagNum = 1)
  val freeList = new FreeList(config)

  freeList.pop()

  freeList.push(0)

  intercept[IllegalArgumentException] {
    freeList.push(1)
  }
}


behavior of "RegFile"



behavior of "RegMap"


}
