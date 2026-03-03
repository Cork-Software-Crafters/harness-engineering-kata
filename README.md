# Harness engineering kata

Discover the power of (basic) harness engineering
Implement the same feature over and over again, improve the context to make it generate better an better versions of it

Basic prompt, use the same unless instructed otherwise:

    implement Feature: Stock reservation with expiry from readme.md.

## Steps
1. No harness (do not create a AGENTS.md, or CLAUDE.md)
2. Prompt about tests (add a mention about the need for tests for new code in the prompt)
3. Add test constraint into agents.md instead
4. Add instruction about tests b4 refactoring into agents file
5. Refactor it into what you want it to be
6. Ask the agent to extract that knowledge, retry
7. Have the agent do the code-review, instruct it to always run a code review when a feature is "done"
8. Have the agent fix the code review
9. Have it create architectural constraints (arch unit)
10. Have the agent research online what TDD, how it can be applied to agents and ask it to create a skill for it
11. Make the agent capable of running the app in debug mode, add skill to the repo


## Run

```bash
mvn -q compile
java -cp target/classes com.kata.warehouse.Main
```



## Missing feature to implement in the kata

### Feature: Stock reservation with expiry

1. Add command: `RESERVE;<customer>;<sku>;<qty>;<minutes>`
2. Reserve only if enough available stock exists.
3. Add command: `CONFIRM;<reservationId>` to convert reservation into a shipped order.
4. Add command: `RELEASE;<reservationId>` to release stock manually.
5. Reservations should expire automatically based on the configured minutes, returning stock to availability.

