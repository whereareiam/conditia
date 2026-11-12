---
version: 0.1.0
---

# Update 0.1.0 - First release

Conditia 0.1.0 is the first release: a host declares the typed parts of its places, registers condition types and binds each `requires:` list at startup, where unknown types, parts and named lists stop it with their config path; a check runs the list in order and names the first condition that did not hold, with its reason. It ships with the types `time`, `date` and `chance`, named lists, scopes per owner, adapters for Configura and Chassis, and test helpers.
