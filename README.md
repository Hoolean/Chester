Chester
=======

Chester... who else?
--------------------

Chester is an IRC chatbot.
He uses an implementation of the MegaHal algorithm to generate responses based on messages he has learnt from observing IRC conversation. 
MegaHal uses a 4th order Markov chain to generate almost-human responses by default.

Compiling
---------

This project uses Maven to compile. One can compile with the command:

```bash
mvn clean install
```

This automatically downloads all necessary dependencies and builds a jar at:

```
target/chester*.jar
```

Running
-------

The built Jar can be executed with the following command: *(the "java" executable must be in the prompt's path)*

```bash
java -jar chester*.jar
```

Todo
----

- Allow all configuration options
- Link AI to the IRC client
- Add commands
- Base command auth on those with permissions in #chester
