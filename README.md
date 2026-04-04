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


## Steps
For each step, throw away all code and get back to main, use this prompt, unless instructed otherwise:

    implement the feature from feature.md

### 1. No Harness

  Start without any harness files, and do not create AGENTS.md or CLAUDE.md.

### 2. Add a Minimal Agent Instruction File

  Create either AGENTS.md or CLAUDE.md and add a simple instruction such as: “Add full test coverage for new features.”

### 3. Add Regression-Protection Guidance

  Assume the agent may skip tests for existing untested code, and add an instruction like: “To protect against regressions, always add full coverage for existing code before modifying it.”

### 4. Refactor Until Quality Is Acceptable

  Assume the initial code quality is weak, ask the agent to refactor repeatedly until you are reasonably satisfied, and then ask it to extract design principles from the conversation into a file such as
  docs/design-principles.md.

### 5. Reuse Design Principles and Retry

  Reference docs/design-principles.md from AGENTS.md or CLAUDE.md, then restart from scratch and compare whether the resulting code is similar to the improved result from the first iteration.

### 6. Add a Code Quality Gate via Stop Hook (Reviewer Agent)

  Add a Stop hook that spawns a reviewer sub-agent after each agent turn. The reviewer runs the linter, reads offending functions, and decides whether to block or allow. This uses LLM judgment to gate completion.

  **Setup:**

  Install quality checking tools:

  ```bash
  # Python
  pip install -r python/requirements-dev.txt

  # Java
  # See "Quality tools by language" section below
  ```

  Create `.claude/settings.json`:

  ```json
  {
    "hooks": {
      "Stop": [
        {
          "hooks": [
            {
              "type": "agent",
              "prompt": "Review all Python files in python/src/ for quality violations. Run: cd python && flake8 src/ to get violations. If there are real violations, return decision 'block' with specific refactoring suggestions. If clean, return decision 'allow'.",
              "timeout": 60
            }
          ]
        }
      ]
    }
  }
  ```

  > **Important:** After creating or modifying `.claude/settings.json`, restart your Claude Code or Copilot session so the hooks are loaded.

### 7. Add a Mechanical Quality Gate via Stop Hook (Hard Block)

  Replace the reviewer agent with a deterministic quality gate script. The Stop hook runs a linter and custom checks — if any violation exists, exit code 2 blocks the agent from finishing. The agent is forced to iterate until the code is clean.

  This is the most effective technique discovered in the quality gate experiment (see [Discussion](#discussion) below).

  **Setup:**

  Create `.claude/hooks/stop-quality-gate.sh`:

  ```bash
  #!/bin/bash
  cd "$(git rev-parse --show-toplevel)/python" 2>/dev/null || exit 0
  VIOLATIONS=$("$(git rev-parse --show-toplevel)/.claude/hooks/check-quality.sh" 2>&1)
  if [ -n "$VIOLATIONS" ]; then
    echo "QUALITY GATE FAILED — fix these violations before finishing:" >&2
    echo "" >&2
    echo "$VIOLATIONS" >&2
    echo "" >&2
    echo "Refactor: extract methods, reduce nesting, split files, introduce constants." >&2
    exit 2
  fi
  exit 0
  ```

  ```bash
  chmod +x .claude/hooks/stop-quality-gate.sh
  ```

  Update `.claude/settings.json`:

  ```json
  {
    "hooks": {
      "Stop": [
        {
          "hooks": [
            {
              "type": "command",
              "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/stop-quality-gate.sh"
            }
          ]
        }
      ]
    }
  }
  ```

  > **Important:** After creating or modifying hook scripts or `.claude/settings.json`, restart your Claude Code or Copilot session. Hooks are loaded at session start — changes to hook files are picked up on the next session, but changes to `settings.json` require a restart.

  The quality rules are defined in `python/.flake8` and `.claude/hooks/check-quality.sh`. See those files for the full list of checks (function length, cognitive complexity, magic numbers, string constants, class attributes, file length).

### 8. Add Architectural Constraints

  Introduce architectural constraints (for example, ArchUnit-style rules) and verify whether the agent uses them to guide feedback and implementation decisions.

### 9. Add a TDD Skill Through Research and Reapplication

  Ask the agent to research what TDD is and how it applies to agent workflows, explain it clearly, create a dedicated TDD skill, and then re-implement the feature using that skill.

### 10. Add Debugging Capability and Extract a Skill

  Enable the agent to run the app in debug mode, introduce a bug, ask the agent to diagnose and fix it, and then extract that debugging workflow into a reusable skill added to the repository.


## Quality tools by language

### Python

```bash
pip install -r python/requirements-dev.txt
# Includes: flake8, flake8-functions, flake8-cognitive-complexity,
#           flake8-bugbear, flake8-simplify, wemake-python-styleguide,
#           radon, xenon
```

Rules are configured in `python/.flake8`. Custom checks (file length, class instance attribute count) are in `.claude/hooks/check-quality.sh`.

### Java

Use [Checkstyle](https://checkstyle.org/) with a custom configuration:

```xml
<!-- checkstyle.xml -->
<module name="Checker">
  <module name="TreeWalker">
    <module name="MethodLength"><property name="max" value="30"/></module>
    <module name="ParameterNumber"><property name="max" value="4"/></module>
    <module name="CyclomaticComplexity"><property name="max" value="10"/></module>
    <module name="MagicNumber"/>
    <module name="MultipleStringLiterals"><property name="allowedDuplicates" value="3"/></module>
    <module name="ClassDataAbstractionCoupling"><property name="max" value="6"/></module>
  </module>
  <module name="FileLength"><property name="max" value="150"/></module>
</module>
```

Add to `pom.xml`:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-checkstyle-plugin</artifactId>
  <version>3.6.0</version>
  <configuration>
    <configLocation>checkstyle.xml</configLocation>
    <failOnViolation>true</failOnViolation>
  </configuration>
</plugin>
```

Run: `mvn checkstyle:check`

Adapt `.claude/hooks/stop-quality-gate.sh` to call `mvn -q checkstyle:check` instead of `flake8`.

Alternatively, [ArchUnit](https://www.archunit.org/) can enforce architectural constraints (package dependencies, layer separation) as executable tests.


## Run

### Java

```bash
cd java
mvn -q compile
java -cp target/classes com.kata.warehouse.Main
```

### Python

```bash
cd python
python main.py
```


## Discussion

### Quality gate experiment

An experiment was conducted comparing 6 different harness techniques for making AI agents refactor proactively, across 3 rounds of increasing rule strictness. The full results are documented in [`docs/experiment-summary.md`](docs/experiment-summary.md).

**Techniques tested:** soft context injection (UserPromptSubmit hook), `ask`-based PreToolUse hooks (shell script and LLM agent), hard-blocking Stop hooks (shell script and LLM reviewer), and deferred enforcement (pre-commit violations log).

**Key finding:** Mechanical hard blocking (Stop hook with exit code 2) is the only technique that reliably scales. The agent is prevented from finishing until a deterministic quality script returns exit 0. Every rule added to the script is enforced — the agent keeps iterating until the code is clean. Soft techniques (context injection, deferred logging) were consistently ignored. LLM-based reviewers worked partially but exercised judgment that sometimes let violations slide.

**Conclusion:** The agent doesn't need to understand *why* it should refactor. It needs to be mechanically prevented from finishing until the code is clean. Understanding is optional; enforcement is not.

