# Harness engineering kata

Experiment with tweaking and tooling the harness of agents so they produce the result you want.

Implement the same feature over and over again, improve the harness to make it generate better an better versions of it

By harness in this context we mean whatever influences the agent's behavior, and whatever feedback mechanisms you put in place.
- the AGENTS.md / CLAUDE.md file
- Skills
- scripts (making results predictable)
- Architectural documents, and constraints (like arch unit)
- Process / Workflow descriptions
- the README.md and other visible files in the root dir

## Prerequisites
- Java JDK and Apache Maven installed.

## Considerations
> [!NOTE]
> For consistency, we will all use the free SWE‑1.6 model.

> [!WARNING]
> Deactivate any global rules you may have defined before starting.

> [!WARNING]
> Start each step in a new chat to ensure a clean context.


## Steps

### Part 1

### Step 1: No Harness

Start without any harness files, and do not create AGENTS.md or CLAUDE.md.

Use this prompt:
```console
Implement the feature from feature.md
```

### Step 2: Add a Minimal Agent Instruction File

Throw away all code generated in previous step.

Create an *AGENTS.md* file and add a simple instruction such as: 

**AGENTS.md**
```markdown
Add unit tests for new features.
```

In a new chat, promt:
```console
Implement the feature from feature.md
```

### Step 3: Add Regression-Protection Guidance

Throw away all code generated in previous step.

Assume the agent may skip tests for existing untested code, and add to the *AGENTS.md* file a new instruction like: 

**AGENTS.md**
```diff
Add unit tests for new features.
+ To protect against regressions, always add full coverage for existing code before modifying it.
```

In a new chat, promt:
```console
Implement the feature from feature.md
```

### Step 4: Iterate to improve the Agent Instruction File
Update the *AGENTS.md* file with additional information about your preferences for test design. For example, you might want it to use a particular test framework or assertion library (e.g., AssertJ), name tests in a certain way, test only public methods, or also write acceptance tests.

Repeat the process of reverting the code (while keeping your changes in the AGENTS.md file) and use a new context window with the same prompt every time until the code it writes has good enough tests.

At the end of the exercise, commit your code so you can resume with Part 2 in the next Learning Hour.

## Part 2

### Step 1: Refactor Until Quality Is Acceptable

Assume the initial code quality is weak, ask the agent to refactor repeatedly until you are reasonably satisfied, and then ask it to extract design principles from the conversation into a file such as *docs/design-principles.md*.

### Step 2: Reuse Design Principles and Retry

Reference *docs/design-principles.md* from AGENTS.md, then restart from scratch and compare whether the resulting code is similar to the improved result from the first iteration.

### Step 3: Add a Deterministic Code Quality Gate via Skill
We will define a Windsurf Cascade Skill to enforce code quality automatically using static analysis.
Windsurf Skills let you package repeatable workflows (like linting and code checks) that the agent can execute as part of its iteration loop.
See: https://docs.windsurf.com/windsurf/cascade/skills

This Skill will act as a mechanical quality gate:
- It runs Checkstyle.
- If any violation is found, it fails.
- The agent must fix the code before continuing.

Once you have the Skill defined, restart from scratch and compare whether the resulting code is similar to the improved result from the previous iterations.

## Part 3

### Step 1: Add a TDD Skill Through Research and Reapplication

  Ask the agent to research what TDD is and how it applies to agent workflows, explain it clearly, create a dedicated TDD skill, and then re-implement the feature using that skill.

### Step 2: Add Debugging Capability and Extract a Skill

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

Use [Checkstyle](https://checkstyle.org/) for the checks. A starter `checkstyle.xml` covering method length, parameter count, cyclomatic complexity, magic numbers, string literals, and file length is in `checkstyle.xml`. Add the maven-checkstyle-plugin to `pom.xml` and adapt quality-gate to call `mvn checkstyle:checkstyle`.

```xml
<project>
...
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
                <version>3.3.1</version>
                <configuration>
                    <configLocation>checkstyle.xml</configLocation>
                    <consoleOutput>true</consoleOutput>
                    <failsOnError>true</failsOnError>
                    <linkXRef>false</linkXRef>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

[ArchUnit](https://www.archunit.org/) can additionally enforce architectural constraints (package dependencies, layer separation) as executable tests.
