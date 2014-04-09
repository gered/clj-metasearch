# clj-metasearch

Helper functions for searching through Clojure namespaces for Vars containing specific bits of metadata.

!["clj-metasearch version"](https://clojars.org/clj-metasearch/latest-version.svg)

## Usage

Finding Clojure Vars is fairly simple.

```clojure
(use 'clj-metasearch.core)

(find-vars #(= (:name %) 'pprint))
=> ((var clojure.pprint/pprint))
```

`find-vars` takes a predicate which will be run on the metadata for each Var being checked.

We can get the value of Vars we find by using `var-get` and then begin using the value right away. For example:

```clojure
(let [println-fn (-> (find-vars #(= (:name %) 'println))
                     (first)
                     (var-get))]
  (println-fn "Hello world!"))
Hello world!
=> nil
```

By default `find-vars` will search all Clojure namespaces it can find in the current classpath. We can filter
which Clojure namespaces are checked by supplying an additional predicate to `find-vars` under the
`:namespace-pred` argument. This predicate will be run on each namespace found (the namespace will be passed
as a symbol to the predicate).

```clojure
; no namespace filtering. all namespaces are checked
(find-vars #(= (:name %) 'find-namespaces))
=> ((var clj-metasearch.core/find-namespaces)
    (var clojure.tools.namespace.find/find-namespaces))

; using namespace filtering
(find-vars
  #(= (:name %) 'find-namespaces)
  :namespace-pred #(not= % 'clj-metasearch.core))
=> ((var clojure.tools.namespace.find/find-namespaces))
```

By default, to help avoid loading a bunch of libraries the first time `find-vars` is called namespaces are not
automatically loaded before being checked. Thusly, you will only be able to find Vars in namespaces that are
currently loaded.

`find-vars` accepts an option, `:require-all-namespaces?`, that allows you to change this
behaviour. Passing `true` will cause each namespace being checked to first be loaded via `require`.

```clojure
(find-vars #(= (:name %) 'parse))
=> ()

(find-vars
  #(= (:name %) 'parse)
  :require-all-namespaces? true)
=> ((var clojure.xml/parse))
```

When you use `true` for `:require-all-namespaces?`, it would normally be a good idea to supply a namespace
predicate via `:namespace-pred` if at all possible to avoid unnecessarily loading a whole bunch of extra
namespaces.

### Exceptions

By default `find-vars` suppresses any exceptions encountered while it searches through namespaces (including
any exceptions that might occur when loading new namespaces when `:require-all-namespaces?` is set). This
can be helpful, but sometimes it might be useful to know why something you might have been expecting to
find doesn't get returned by `find-vars`.

We can turn off exception suppression by using either the `throw-exceptions?` option or the
`throw-compiler-exceptions?` option in calls to `find-vars`. Setting `throw-exceptions?` to true will not
suppress any exceptions that are encountered, while setting `throw-compiler-exceptions?` to true will
suppress all exceptions except for `clojure.lang.Compiler.CompilerException` exceptions. This option can
be useful to catch any syntax or other errors in your own code.

## License

Distributed under the the MIT License. See LICENSE for more details.
