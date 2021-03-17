# Codices 

The Codices quark establishes a framework for developing modular class interfaces using arbitrarily defined scriptable components. For instance, a class written in this framework that iteratively evaluates a function within a routine might implement only the routine, leaving the function itself to be defined later by the user. As a result, an object of this class can support any number of diverse configurations without compromising the functionality of the class itself. In this way, the framework aims to support a best-of-both-worlds situation that balances the specficity of a compiled class's interface with the open-ended potential of scripting.

For more on how to work with the Codices quark, consult the HelpSource guide.

## Installation

In SuperCollider, evaluate the following: 

`Quarks.install("https://github.com/ianmacdougald/Codices");`

## Changelog 

**0.3.4** 

Added pseudo-method lookups to CodexStorage dictionary from String instances. 

**0.4.0**

Changed String extension method for interacting with CodexStorage via pseudo methods

**0.5.0**

Separated non-core classes into their own [repository](https://github.com/ianmacdougald/CodicesMore).


