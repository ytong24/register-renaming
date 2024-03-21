package reg_renaming.tester

import chiseltest.ChiselScalatestTester
import org.scalatest.flatspec.AnyFlatSpec
import reg_renaming.OpConfig
import reg_renaming.model.Op
import reg_renaming.model.reg_renaming_table._
import reg_renaming.reg_renaming_table.RegRenamingTableConfig

class ModelTester extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "FreeList"
  it should "be initialized with the correct size" in {
    val config = RegRenamingTableConfig(ptagNum = 5)
    val freeList = new FreeList(config)
    assert(freeList.size == config.ptagNum, "Initial FreeList size should be equal to ptagNum")
  }

  it should "decrease in size by one after a pop" in {
    val config = RegRenamingTableConfig(ptagNum = 5)
    val freeList = new FreeList(config)
    val initialSize = freeList.size
    freeList.pop()
    assert(freeList.size == initialSize - 1, "FreeList size should decrease by 1 after pop")
  }

  it should "increase in size by one after a push" in {
    val config = RegRenamingTableConfig(ptagNum = 5)
    val freeList = new FreeList(config)
    val initialSize = freeList.size
    val popped = freeList.pop()
    freeList.push(popped)
    assert(freeList.size == initialSize, "FreeList size should increase by 1 after push")
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
    val initialSize = freeList.size
    val popped = freeList.pop()
    freeList.push(popped)
    assert(freeList.size == initialSize, "FreeList size should be restored after pushing the popped element back")
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
  it should "each RegFileEntry have a correct ptag" in {
    val regFile = new RegFile(RegRenamingTableConfig(ptagNum = 4))
    for (i <- 0 until 4) {
      val entry = regFile.getRegFileEntry(i)
      assert(entry.getRegPtag == i)
      assert(entry.getRegState == RegFileEntryState.FREE)
    }
  }

  it should "throw an exception for an invalid index" in {
    val config = RegRenamingTableConfig(ptagNum = 5)
    val regFile = new RegFile(config)

    assertThrows[IllegalArgumentException] {
      regFile.getRegFileEntry(6)
    }
  }

  behavior of "RegMap"
  it should "initialize with default values" in {
    val config = OpConfig(numSrcMax = 10, numDstMax = 5, archIdNum = 3)
    val regMap = new RegMap(config)
    for (i <- 0 until config.archIdNum) {
      assert(regMap.getPtag(i) === -1, s"RegMap entry $i should be initialized to 0")
    }
  }

  it should "set and get a ptag value correctly" in {
    val config = OpConfig(numSrcMax = 10, numDstMax = 5, archIdNum = 3)
    val regMap = new RegMap(config)
    val testIndex = 2 // Arbitrary index to test
    val testValue = 42 // Arbitrary value to set

    regMap.setPtag(testIndex, testValue)
    assert(regMap.getPtag(testIndex) === testValue, s"RegMap entry $testIndex should be $testValue after setPtag")
  }

  it should "throw an exception for invalid index on getPtag" in {
    val config = OpConfig(numSrcMax = 10, numDstMax = 5, archIdNum = 3)
    val regMap = new RegMap(config)
    val invalidIndex = config.archIdNum // Index out of bounds

    assertThrows[IllegalArgumentException] {
      regMap.getPtag(invalidIndex)
    }
  }

  it should "throw an exception for invalid index on setPtag" in {
    val config = OpConfig(numSrcMax = 10, numDstMax = 5, archIdNum = 3)
    val regMap = new RegMap(config)
    val invalidIndex = config.archIdNum // Index out of bounds
    val testValue = 42 // Arbitrary value to set

    assertThrows[IllegalArgumentException] {
      regMap.setPtag(invalidIndex, testValue)
    }
  }

  behavior of "RegRenamingTable"
  it should "available before alloc" in {
    val opConfig = OpConfig(numSrcMax = 2, numDstMax = 2, archIdNum = 4)
    val tableConfig = RegRenamingTableConfig(ptagNum = 8)
    val renamingTable = new RegRenamingTable(tableConfig, opConfig)

    val opInstance = new Op(opConfig, 0, 2, Array(), Array(0, 1))
    assert(renamingTable.available())
    assert(renamingTable.getFreeList.size == 8)
    renamingTable.process(opInstance)
    assert(renamingTable.getFreeList.size == 6)
  }

  it should "full when alloc all" in {
    val opConfig = OpConfig(numSrcMax = 2, numDstMax = 2, archIdNum = 4)
    val tableConfig = RegRenamingTableConfig(ptagNum = 8)
    val renamingTable = new RegRenamingTable(tableConfig, opConfig)

    for (i <- 0 until 4) {
      val opInstance = new Op(opConfig, 0, 2, Array(), Array(0, 1))
      assert(renamingTable.available())
      assert(renamingTable.getFreeList.size == 8 - i * 2)
      renamingTable.process(opInstance)
    }
    assert(!renamingTable.available())
  }

  it should "correct data in op dynamic info" in {
    val opConfig = OpConfig(numSrcMax = 2, numDstMax = 2, archIdNum = 2)
    val tableConfig = RegRenamingTableConfig(ptagNum = 4)
    val renamingTable = new RegRenamingTable(tableConfig, opConfig)

    val op_0 = new Op(opConfig, 0, 2, Array(), Array(0, 1))
    assert(renamingTable.available())
    renamingTable.process(op_0)
    assert(op_0.getPtagDstId(0) == 0)
    assert(op_0.getPtagDstId(1) == 1)

    val op_1 = new Op(opConfig, 1, 1, Array(1), Array(1))
    assert(renamingTable.available())
    renamingTable.process(op_1)
    assert(op_1.getPtagSrcId(0) == 1)
    assert(op_1.getPtagDstId(0) == 2)

    assert(renamingTable.getFreeList.size == 1)
    assert(!renamingTable.available())
  }

  it should "correct data in register map" in {
    val opConfig = OpConfig(numSrcMax = 2, numDstMax = 2, archIdNum = 2)
    val tableConfig = RegRenamingTableConfig(ptagNum = 4)

    val renamingTable = new RegRenamingTable(tableConfig, opConfig)
    val regMap = renamingTable.getRegMap

    val op_0 = new Op(opConfig, 0, 2, Array(), Array(0, 1))
    renamingTable.process(op_0)
    assert(regMap.getPtag(0) == 0)
    assert(regMap.getPtag(1) == 1)

    val op_1 = new Op(opConfig, 1, 1, Array(1), Array(1))
    renamingTable.process(op_1)
    assert(regMap.getPtag(0) == 0)
    assert(regMap.getPtag(1) == 2)
  }

  it should "correct data in register file" in {
    val opConfig = OpConfig(numSrcMax = 2, numDstMax = 2, archIdNum = 2)
    val tableConfig = RegRenamingTableConfig(ptagNum = 4)

    val renamingTable = new RegRenamingTable(tableConfig, opConfig)
    val regFile = renamingTable.getRegFile

    // first op
    val op_0 = new Op(opConfig, 0, 2, Array(), Array(0, 1))
    renamingTable.process(op_0)

    assert(regFile.getRegFileEntry(0).getRegState == RegFileEntryState.ALLOC)
    assert(regFile.getRegFileEntry(0).getRegArchId == 0)
    assert(regFile.getRegFileEntry(0).getRegPtag == 0)
    assert(regFile.getRegFileEntry(0).getPrevSameArchId == -1)

    assert(regFile.getRegFileEntry(1).getRegState == RegFileEntryState.ALLOC)
    assert(regFile.getRegFileEntry(1).getRegArchId == 1)
    assert(regFile.getRegFileEntry(1).getRegPtag == 1)
    assert(regFile.getRegFileEntry(1).getPrevSameArchId == -1)

    // second op
    val op_1 = new Op(opConfig, 1, 1, Array(1), Array(1))
    renamingTable.process(op_1)

    assert(regFile.getRegFileEntry(2).getRegState == RegFileEntryState.ALLOC)
    assert(regFile.getRegFileEntry(2).getRegArchId == 1)
    assert(regFile.getRegFileEntry(2).getRegPtag == 2)
    assert(regFile.getRegFileEntry(2).getPrevSameArchId == 1)
  }

  it should "remove previous same architectural register when commit current op" in {
    val opConfig = OpConfig(numSrcMax = 2, numDstMax = 2, archIdNum = 2)
    val tableConfig = RegRenamingTableConfig(ptagNum = 4)

    val renamingTable = new RegRenamingTable(tableConfig, opConfig)
    val regFile = renamingTable.getRegFile

    // insert 2 op
    val op_0 = new Op(opConfig, 0, 2, Array(), Array(0, 1))
    renamingTable.process(op_0)
    renamingTable.commit(op_0)
    val op_1 = new Op(opConfig, 1, 1, Array(1), Array(1))
    renamingTable.process(op_1)

    // before commit op_1
    assert(renamingTable.getFreeList.size == 1)
    assert(!renamingTable.available())
    assert(regFile.getRegFileEntry(1).getRegState == RegFileEntryState.COMMIT)
    assert(regFile.getRegFileEntry(1).getRegArchId == 1)
    assert(regFile.getRegFileEntry(1).getRegPtag == 1)
    assert(regFile.getRegFileEntry(1).getPrevSameArchId == -1)

    // after commit op_1
    renamingTable.commit(op_1)
    assert(renamingTable.getFreeList.size == 2)
    assert(renamingTable.available())
    assert(regFile.getRegFileEntry(1).getRegState == RegFileEntryState.FREE)
    assert(regFile.getRegFileEntry(1).getRegArchId == -1)
    assert(regFile.getRegFileEntry(1).getRegPtag == 1)
    assert(regFile.getRegFileEntry(1).getPrevSameArchId == -1)
  }
}
