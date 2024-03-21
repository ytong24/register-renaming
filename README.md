<div align="center" id="top"> 
  <img src="./.github/app.gif" alt="Register Naming" />

&#xa0;

  <!-- <a href="https://spaceinvaders.netlify.app">Demo</a> -->
</div>

<h1 align="center">Register Naming</h1>

<p align="center">
  <img alt="Github top language" src="https://img.shields.io/github/languages/top/ytong24/register-renaming?color=56BEB8">

  <img alt="Github language count" src="https://img.shields.io/github/languages/count/ytong24/register-renaming?color=56BEB8">

  <img alt="Repository size" src="https://img.shields.io/github/repo-size/ytong24/register-renaming?color=56BEB8">

[//]: # (  <img alt="License" src="https://img.shields.io/github/license/ytong24/register-renaming?color=56BEB8">)

  <!-- <img alt="Github issues" src="https://img.shields.io/github/issues/colbarron/spaceinvaders?color=56BEB8" /> -->

  <!-- <img alt="Github forks" src="https://img.shields.io/github/forks/colbarron/spaceinvaders?color=56BEB8" /> -->

  <!-- <img alt="Github stars" src="https://img.shields.io/github/stars/colbarron/spaceinvaders?color=56BEB8" /> -->
</p>

<!-- Status -->

<!-- <h4 align="center"> 
	🚧  Spaceinvaders 🚀 Under construction...  🚧
</h4> 

<hr> -->

<br>

## Overview
We implemented Register Renaming, which is the allocation stage in the pipeline of the microarchitecture of processors. Name dependencies are caused by the reuse of registers without involving any data dependencies. To maximize the instruction-level parallelism, Register Renaming entails changing the names of the register to avoid false dependencies.

## Verification
### Testing
Do the following command to run the test cases:
```bash
sbt test
```
### Scenario
 - FETCH -> <b>RENAMING</b> -> ISSUE -> EXECUTE -> COMMIT

## Design
### Structure
- Op
- RegisterRenamingTable
    - RegMap
    - RegFile
    - FreeList

### Interface
- Available
    - Before FETCH stage, the processor first checks if there are enough free registers.
- Process
    - After an operand is fetched, it reads data dependency and applies allocation in the RENAMING stage.
- Commit
    - Once an operand is retired, the previous registers shared same architectural ids can be released.

## Close the Loop

The project's foundation is a merged register file for register renaming, which comprises three main components:

- Register Map Table: Converts architectural register IDs to physical tags.
- Register File: Stores Register File entries.
- Free List: Tracks all free physical registers.

### **Loop 1: Scala Model Development and Testing**

- **Objective:** Design and test high-level interactions and functionalities of FreeList, RegMap, RegFile, and
  RegRenamingTable in Scala.
- **Key Steps:**
    - Define the functionalities and interactions of each component.
    - Ensure each component is focused on a single task to maintain module independence.
    - Validate the design and implementation through Scala test cases.

### **Loop 2: FreeList Chisel Implementation**

- **Component:** FreeList tracks all free physical registers with a stack mechanism.
- **Implementation:**
    - The stack pops a ptag when needed and pushes it back when it becomes free.
- **Testing:** Ensure the push/pop operations correctly reflect the free physical registers tracking.

### **Loop 3: Register Map Implementation**

- **Component:** The Register Map maintains the mapping between architectural IDs (archId) and physical tags (ptag).
- **Functionality:** Enables retrieval of ptag given an archId.
- **Testing:** Test cases verify the accurate and efficient mapping function.

### **Loop 4: Register File Implementation**

- **Component:** Register File consists of multiple entries, each containing ptag, archId, prevSameArchId, and regState.
- **Focus:** Prioritizes the elimination of false dependencies through Register Renaming.
- **Testing:** Validate functionalities of entries, state transitions, and handling of IDs.

### **Loop 5: RegRenamingTable "ProcessOp" and "Available"**

- **Operations:** Implement and test processOp and available operations for RegRenamingTable.
- **ProcessOp:** Involves reading srcArchId, allocating new dstPtag for dstArchId, and recording details.
- **Available:** Determines if an operation can be processed based on FreeList's new ptags availability.
- **Interaction:** Plan the interaction among FreeList, RegMap, and RegFile for synchronized component interactions.

### **Loop 6: RegRenamingTable CommitOp Implementation**

- **Operation:** Implement commitOp to release dst ptag of an operation, updating RegMap and RegFile entries.
- **Functionality:** Ensures committed operations free up resources and maintain system integrity.
- **Testing:** Confirm the correct functioning of the commit process, updates in RegMap and RegFile, and ptags freeing.

## License
This Project is made by Yan Tong and Yinyuan Zhao for CSE 228A - Agile Hardware Design
