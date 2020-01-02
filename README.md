# TopShell <img src="https://avatars2.githubusercontent.com/u/52890641?s=200&v=4" align="right" height="40">

*Purely functional, reactive scripting language.*

* Asynchronous I/O and reactive streaming with live updates
* Purely functional scripting with type inference and autocompletion
* Animated, graphical data visualization without leaving the editor
* Includes modules for working with SSH, files, processes, HTTP and more

**[Download now](https://github.com/topshell-language/topshell/releases)** (no installation required).


# Live demo

**[Open the playground](http://show.ahnfelt.net/topshell/)** (somewhat older than the latest release)

Press Ctrl+Enter to run a top level binding.

Press Ctrl+Space for autocompletion.

Press Ctrl+E to switch files. 

The online playground is restricted: no file I/O, no SSH, no HTTP proxying. 

The very top line and the very bottom line of the UI is not yet implemented.


# Examples

## SSH example

* Read a list of IP addresses from a local file.
* SSH into all of them, in parallel, to discover their local hostname.
* Append the resulting `ip hostname` lines to `/etc/hosts`.

```haskell
ipsText <- File.readText "ips.txt"

ips = String.lines ipsText 
    |> List.filter (x -> x != "")

hostTask = host ->
    Ssh.do {user: "root", host: host} (
        result <- Process.shell {command: "hostname"},
        Task.of {name: String.trim result.out, host: host}
    )
    
hosts <- Task.parallel (List.map hostTask ips)

lines = hosts |> List.map h -> h.host + " " + h.name

_ <- File.appendText "/etc/hosts" (
    "\n\n" + String.join "\n" lines + "\n\n"
)
```


## HTTP example

* Fetch some JSON from the `reqres.in` mock data API.
* Turn the JSON into a typed record.
* Show it in a graphical table with image avatars.

```haskell
json <- Http.fetchJson {url: "https://reqres.in/api/users?page=2"}

people : List {id: Int, "first_name": String, "last_name": String, avatar: String} = 
    Json.toAny json.data

htmlImage = url -> Html.tag "img" [Html.attributes ["src" ~> url]]

peopleWithImages = people |> List.map (
    p -> {image: htmlImage p.avatar, name: p."first_name" + " " + p."last_name"}
)

peopleWithImages |> View.table
```


## Stream example

* Make a stream that produces the current time each second.
* Draw an animated clock with SVG.

```haskell
interval = duration ->
    Stream.forever 0.0 t1 -> 
        t2 <- Task.now, 
        delta = t2 - t1,
        delta >= duration ? Task.of t2 ; 
        Task.sleep (duration - delta);
        Task.now

time <- interval 1.0

t = time / 60
a = t * Float.pi * 2.0

x = Float.cos a
y = Float.sin a

Html.tag "svg" [
    Html.attributes [
        "viewBox" ~> "-1 -1 2 2"
    ],
    Html.tag "circle" [
        Html.styles [
            "fill" ~> "#e0e0e0"
        ],
        Html.attributes [
            "cx" ~> "0", 
            "cy" ~> "0", 
            "r" ~> "1"
        ]
    ],
    Html.tag "line" [
        Html.styles [
            "stroke" ~> "cornflowerblue", 
            "stroke-width" ~> "0.1"
        ],
        Html.attributes [
            "x1" ~> "0", 
            "y1" ~> "0", 
            "x2" ~> String.ofFloat x, 
            "y2" ~> String.ofFloat y
        ]
    ],
]
```


# Literals

```haskell
"foo"           // String
42              // Int
7.3             // Float
[1, 2]          // List Int
{x: 7, y: 15}   // {x: Int, y: Int}
Some 42         // eg. [None, Some Int]
x -> x          // a -> a
```

Comments start with `//` and last to the end of the line. 

Strings with placeholders, such as `\{placeholder}`, are functions that take a record argument with a field for each placeholder, where every field must be naturally convertable to a string, enforced via the `Display` constraint.

# Lambda functions

Lambda functions are on the form `x -> e` where `x` is an arbitrary variable name `[a-z][a-zA-Z0-9]*` and `e` is an arbitrary expression.

Functions in TopShell are usually curried. For example, a function that adds two numbers may be written `x -> y -> x + y`. Let's call it `add`. To call it, you simply put the arguments after the function, eg. `add 1 2` evaluates to `3`. If the arguments are not simple literals, parenthesis can be used around each, eg. `add (2 * 3) (4 * 5)` evaluates to `26`.

A shorthand for functions like `x -> y -> x + y` is `(+)`, where `+` is any binary operator.

The pipe operator can be used to cut down on parenthesis and make the data flow from left to right. 
Instead of `f (a b) (g x (h y z))`, you can write `h y z |> g x |> f (a b)`.


# Let and bind

The "let" form introduces a variable. 

```haskell
add = x -> y -> x + y

add 8 9    // 17
```

Local lets are on the form `x = e1, e2` where `x` is a variable, `e1` is the expression whose result will be stored in `x`, and `e2` is an expression that may use `x`. At top level, we leave out the `, e2` part.

Binds are on the form `x <- e1, e2`. They are syntactic sugar for `flatMap (x -> e2) e1`, and useful for programming with monads, which is how you eg. write to a file in TopShell.

If the result of a bind is not of interest, you can ignore the result with a wildcard, `_ <- e1, e2`, or simply use a semicolon `e1; e2`.


# Lists

Lists can be constructed with a list literal, eg. `[1, 2, 3]` is a list of three elements 1, 2 and 3. Lists may be manipulated with the functions in the `List` module.

List supports spread syntax, eg. `[...xs, y, ...zs]` is a list of all of the elements of `xs`, followed by `y`, followed by all of the elements of `zs`. So `[...[1, 2], 3, ...[4, 5, 6]]` evaluates to `[1, 2, 3, 4, 5, 6]`.

Elements in lists may be conditional, eg. `[x | c, y]` omits the `x` element if `c` is false. This is especially useful when building HTML visualizations, where you often want eg. a style to be conditional.


# Records and fields

Records in TopShell are anonymous, ie. you don't have to declare them. They're introduced with `{...}` and their fields are accessed with the dot, as in the following example:

```haskell
magnitude = v -> Float.sqrt (v.x * v.x + v.y * v.y)

magnitude {x: 5.0, y: 7.0}    // 8.602
```

Records also support spread syntax, eg. `{z: 9.0, ...r}` creates a new record that's a copy of `r`, but with the `z` field added or replaced.

Record labels may be unquoted `[a-z][a-zA-Z0-9]*`, or if they contain other characters, enclosed in double quotes, eg. `{"my field": 42}`.

As a shorthand for `r -> r.l`, you can write `(.l)` for any label `l`.

As a shorthand for eg. `{name: name, age: age}`, you can use record punning `{name, age}`.

As a shorthand for eg. `n -> a -> {name: n, age: a}`, you can use `{-> name, age}`.

For pairs on the form `{key: k, value: v}`, you can use the shorthand `k ~> v`.


## Record types and field constraints

When accessing a field `v.x` as in magnitude above, the concrete type of `v` is not known yet. Thus, a field constraint is generated `| a.x: b`, where `a` is the type of the record and `b` is the type of the `x` field in this record. The complete type of `magnitude` is:

```haskell
magnitude : a -> Float | a.x: Float | a.y: Float
```

Meaning "magnitude is a function that takes in any type `a` and returns `Float`, as long as `a` has two fields `x` and `y` of type `Float`". When `magnitude` is later applied to `{x: 5.0, y: 7.0}`, the constraints are checked against the concrete record type `{x: Float, y: Float}`, and since it satisfies both constraints, it type checks.


## Optional fields

The `Http.fetchJson` function has the following signature:

```haskell
Http.fetchJson : c -> Task Json | c ~ {
    url : String 
    ?method : String 
    ?mode : String 
    ?body : String 
    ?check : Bool 
    ?headers : List {key: String, value: String}
}
```

This means that `c` is a record with a field called `url` of type `String`, and optionally one or more of the fields `method`, `mode`, `body`, `check` and `headers`. A common invocation is:

```haskell
json <- Http.fetchJson {url: "https://www.example.com/data.json", mode: "proxy"}
```

Since the `url` field is present, the `mode` field doesn't have the wrong type, and none of the left out fields are required, the above compiles.

Optional fields are accessed with the `.?` operator, eg. `r.?optionalField`, and they return `[None, Some a]`, where `a` is the field type. For open records, there is also the optional field type constraint `record.?label: type`.


## Modules are records

Modules in TopShell are simply records. This is possible because record fields can have type parameters. 

To get the record value from an imported module, use three dots after the module name, eg. `List...`.

The `List` module has type `{map: a => b => (a -> b) -> List a -> List b, ...}`, where `...` is the rest of the functions. The fat arrow `=>` is explicit syntax for a type parameter. 


# Sum types and pattern matching

Like records, sum types are also anonymous in TopShell. 
A value like `Some 42` can be used both where `[None, Some Int]` and eg. `[Some Int, All]` is expected.

```haskell
fallback = default -> {
    | None => default
    | Some x => x
}

fallback 0 (Some 42)    // 42
fallback 0 None         // 0
```

The `{| ... => ... }` syntax creates a lambda function that pattern matches on its argument. Each `|` begins a new pattern, followed by `=>` and then the corresponding expression, which may use captured variables from the pattern. Use `_` as a wildcard.

Note: Pattern matching is currently very limited.


## Sum types and sum type constraints

In the above, the inferred type is `fallback : a -> [None, Some a] -> a`. On the other hand, `Some 42` has type `a | Some : Int -> a`, which says that `Some` must be a constructor that takes an `Int` as a parameter. `[None, Some Int]` happens to be a type that satisfies this constraint. 


# If

The if-then-else construct is `condition ? thenBody ; elseBody`, eg.

```haskell
safeDivision = x -> y ->
    y == 0.0 ? None ;
    Some (x / y)
```

It's typically used the way that `if(condition) return value;` is used in imperative languages, eg. to return early if a condition is true. For example, if we define a stream that emits the time each second, we'd want to skip sleeping if enough time has already passed:

```haskell
interval = duration ->
    Stream.forever 0.0 t1 -> 
        t2 <- Task.now, 
        delta = t2 - t1,
        delta >= duration ? Task.of t2 ; 
        Task.sleep (duration - delta);
        Task.now
```


# Importing modules

When a module function is used, eg. `List.map f l`, the compiler first checks if `List` has been explicitly imported. If not, it implicitly imports a module from the standard library of the corresponding name, eg. `core/List.js`.

You can import modules explicitly with the following syntax:

```haskell
Matrix @ "https://www.example.com/topshell/Matrix.js"
```

Imported files such as `Matrix.js` must be annotated with TopShell types. Please see the modules in `core/...` for examples.


# The top level

Each expression, import or definition in the TopShell top level is ends either when encountering a new unindented non-space, non-closing-brace character, or when the file ends.

The top level is reactive - top level binds `x <- e` convert their right hand side to a `Stream` and definitions that depend on `x` will automatically be updated whenever the stream produces a new value. Top level binds are consumed at a pace of 1 element per 100ms (or less, if the stream is slower).

Lets will automatically be evaluated, but binds needs to be started manually by placing the cursor on the line of the bind and pressing `Ctrl+Enter`. This is because it may be a task that eg. writes to a file.


# Getting help

You're encouraged to [create an issue](https://github.com/Ahnfelt/topshell/issues/new) if you have a question about TopShell.

You can also read the [FAQ](https://github.com/topshell-language/topshell/wiki/FAQ).
