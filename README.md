# Harness engineering kata

Experiment with tweaking and tooling the harness of agents so they produce the result you want.

Implement the same feature over and over again, improve the harness to make it generate better an better versions of it

By harness in this context we mean whatever influences the agent's behavior, and whatever feedback mechanisms you put in place.
- the AGENTS.md / CLAUDE.md file
- skills
- scripts (making results predictable)
- Architectural documents, and constraints (like arch unit)
- Process / workflow descriptions
- the README.md and other visible files in the root dir

## Prerequisites
Java JDK and Apache Maven installed.

## Steps
For each step, throw away all code and get back to main, use this prompt, unless instructed otherwise:

    implement the feature from feature.md

### 1. No Harness

  Start without any harness files, and do not create AGENTS.md or CLAUDE.md.

### 2. Add a Minimal Agent Instruction File

  Create either AGENTS.md or CLAUDE.md and add a simple instruction such as: "Add full test coverage for new features."

### 3. Add Regression-Protection Guidance

  Assume the agent may skip tests for existing untested code, and add an instruction like: "To protect against regressions, always add full coverage for existing code before modifying it."

### 4. Refactor Until Quality Is Acceptable

  Assume the initial code quality is weak, ask the agent to refactor repeatedly until you are reasonably satisfied, and then ask it to extract design principles from the conversation into a file such as
  docs/design-principles.md.

### 5. Reuse Design Principles and Retry

  Reference docs/design-principles.md from AGENTS.md or CLAUDE.md, then restart from scratch and compare whether the resulting code is similar to the improved result from the first iteration.

### 6. Add a Code Quality Gate via Hooks

  Experiment adding a stop Hook in [Cascade](https://docs.windsurf.com/windsurf/cascade/hooks) or [Claude](https://code.claude.com/docs/en/hooks). The reviewer runs the linter, reads offending functions, and decides whether to block or allow.

  Restart your Cascade / Claude Code / Copilot session, then run the prompt.

### 7. Add a Mechanical Quality Gate via Stop Hook (Hard Block)

  Replace the reviewer agent with a deterministic quality gate. The Stop hook runs a linter and custom checks — if any violation exists, exit code 2 blocks the agent from finishing. The agent is forced to iterate until the code is clean. 

  Restart your Claude Code / Copilot session, then run the prompt.

### 8. Add a TDD Skill Through Research and Reapplication

  Ask the agent to research what TDD is and how it applies to agent workflows, explain it clearly, create a dedicated TDD skill, and then re-implement the feature using that skill.

### 9. Add Debugging Capability and Extract a Skill

  Enable the agent to run the app in debug mode, introduce a bug, ask the agent to diagnose and fix it, and then extract that debugging workflow into a reusable skill added to the repository.


## Run

### Java

```bash
mvn compile
java -cp target/classes com.kata.warehouse.Main
```
## Test

### Java

```bash
mvn test
```


## Quality tools by language

### Java

Use [Checkstyle](https://checkstyle.org/) for the checks. A starter `checkstyle.xml` covering method length, parameter count, cyclomatic complexity, magic numbers, string literals, and file length is in `checkstyle.xml`. Add the maven-checkstyle-plugin to `pom.xml` and adapt quality-gate to call `mvn -q checkstyle:check`.

[ArchUnit](https://www.archunit.org/) can additionally enforce architectural constraints (package dependencies, layer separation) as executable tests.
