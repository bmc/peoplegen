# Change Log for _peoplegen_

**Version 2.1.4**

- Upgraded `sbt` version to 1.3.7.
- Got rid of Lightbend Activator.
- Upgraded to Scala 2.13 and fixed code that was using old APIs (e.g., now
  uses `LazyList` instead of `Stream`).
- Updated dependencies.

**Version 2.1.3**

Added verbose timings.

**Version 2.1.2**

Fixed bug in generation: Female names used for males.

**Version 2.1.1**

Fixed bug in generation of people: No males were being generated.

**Version 2.1.0**

Added `--id` option, allowing generation of unique per-row IDs.

**Version 2.0.0**

Ported from Python to Scala.
