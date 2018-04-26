## Notes

### General

This solution is based on reversed hashes, which, I assume, was the point.
The instructions were to make it work for "tens of thousands of indices,"
which seems reasonable on a modern machine. The idea is: if the machine
has enough RAM, with this solution, the queries should be very fast. Again,
in reality, I would use an actual database system with sharding for this,
but obviously the point of the exercise to see how I would do my own
searching, indexing and maybe parsing.

This was my first time writing a parser. Thankfully, the parser that comes
with Scala (it needs to be added as a dependency, but it's very tightly
linked to the language) was not too hard to figure out.

In order to save time on string comparisons, I decided to use symbols
instead. I don't really know how much of a speedup it provides, or even
if the conversion between string and symbol might slow down the program,
but it felt like, if speed was really important, string comparisons were
something to be avoided in the JVM. (I think they're okay in Python.)


### Building

Everything necessary to build and run should be in `build.sbt`. I tried
building using SBT alone (through IntelliJ Community) and it worked. If
I'm wrong and something is missing, let me know.


### Usage

Additional instructions are shown in the terminal and, of course, in the
source. The source is pretty short anyway.


### Comments

I usually try to fully comment my code when I write Python or Java. Scala
code is so fluid that it's harder to know exactly what to comment, but I
did comment everything that felt important or that looked like it should be
commented.


### Tests

On the phone, I was told to add tests to the code. There are a few unit
tests. The unit tests, as said in the email, do not test on very large
datasets. I'll admit that unit testing is not the thing I am the best at.
But the tests try to illustrate some of the main features of the program.
Some features, such as what happens if the user types a command that is
not recognized at all, could only be tested by end-to-end testing and I
don't have a framework for that, so there are no automated tests for it.
But I've tested those features myself and, as long as I've resisted the
temptation to make quick changes without testing them before sending
the link to the GitHub project, they should all still work.
